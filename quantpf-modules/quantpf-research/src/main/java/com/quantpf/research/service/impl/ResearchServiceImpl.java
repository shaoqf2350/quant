package com.quantpf.research.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quantpf.domain.bo.ResearchTaskBo;
import com.quantpf.domain.entity.AiResearchDraft;
import com.quantpf.domain.entity.DataRawSnapshot;
import com.quantpf.domain.entity.DataSource;
import com.quantpf.domain.entity.EventEvidence;
import com.quantpf.domain.entity.ResearchEvent;
import com.quantpf.domain.entity.ResearchTask;
import com.quantpf.domain.enums.AiDraftValidationStatus;
import com.quantpf.domain.enums.EvidenceType;
import com.quantpf.domain.enums.ResearchDirection;
import com.quantpf.domain.enums.ResearchEventStatus;
import com.quantpf.domain.enums.ResearchTaskStatus;
import com.quantpf.domain.event.ResearchEventPublishedEvent;
import com.quantpf.domain.persistence.mapper.AiResearchDraftMapper;
import com.quantpf.domain.persistence.mapper.DataRawSnapshotMapper;
import com.quantpf.domain.persistence.mapper.DataSourceMapper;
import com.quantpf.domain.persistence.mapper.EventEvidenceMapper;
import com.quantpf.domain.persistence.mapper.ResearchEventMapper;
import com.quantpf.domain.persistence.mapper.ResearchTaskMapper;
import com.quantpf.domain.vo.AiResearchDraftVo;
import com.quantpf.domain.vo.EventEvidenceVo;
import com.quantpf.domain.vo.ResearchEventVo;
import com.quantpf.domain.vo.ResearchMaterialVo;
import com.quantpf.domain.vo.ResearchTaskVo;
import com.quantpf.research.adaptor.DifyDraftResult;
import com.quantpf.research.adaptor.IDifyResearchAdapter;
import com.quantpf.research.service.IResearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 新闻研究链服务实现（《详设_1 §10》）。
 * <p>职责边界：Dify/TradingAgents 输出一律视为 AI 草稿，经 Spring Boot 确定性校验后才能升级为研究事件；
 * 适配器不可用或校验未通过时任务转 PENDING_REVIEW 待人工复核，不判失败、不伪造结论（详设_0 §5.7 降级语义）。</p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ResearchServiceImpl implements IResearchService {

    private final ResearchTaskMapper taskMapper;
    private final AiResearchDraftMapper draftMapper;
    private final ResearchEventMapper eventMapper;
    private final EventEvidenceMapper evidenceMapper;
    private final DataRawSnapshotMapper snapshotMapper;
    private final DataSourceMapper dataSourceMapper;
    private final IDifyResearchAdapter difyAdapter;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Snowflake snowflake;

    @Override
    public PageResult<ResearchTaskVo> queryTaskPage(String status, String taskType, PageQuery pageQuery) {
        LambdaQueryWrapper<ResearchTask> wrapper = new LambdaQueryWrapper<ResearchTask>()
            .eq(StrUtil.isNotBlank(status), ResearchTask::getStatus, status)
            .eq(StrUtil.isNotBlank(taskType), ResearchTask::getTaskType, taskType)
            .orderByDesc(ResearchTask::getCreateTime);
        Page<ResearchTask> page = taskMapper.selectPage(pageQuery.build(), wrapper);
        List<ResearchTaskVo> rows = page.getRecords().stream().map(this::toTaskVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public ResearchTaskVo queryTaskById(Long pkId) {
        ResearchTask entity = taskMapper.selectById(pkId);
        return entity == null ? null : toTaskVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(ResearchTaskBo bo) {
        // 幂等（§21.2）：同幂等键重复提交返回原任务，不重复创建
        String idempotencyKey = bo.getIdempotencyKey();
        if (StrUtil.isNotBlank(idempotencyKey)) {
            ResearchTask exist = taskMapper.selectOne(
                new LambdaQueryWrapper<ResearchTask>().eq(ResearchTask::getIdempotencyKey, idempotencyKey));
            if (exist != null) {
                log.info("研究任务幂等命中，返回原任务，idempotencyKey={}，taskCode={}", idempotencyKey, exist.getTaskCode());
                return exist.getPkId();
            }
        }
        ResearchTask task = new ResearchTask();
        BeanUtils.copyProperties(bo, task);
        task.setPkId(snowflake.nextId());
        task.setTaskCode("RT-" + snowflake.nextId());
        task.setIdempotencyKey(StrUtil.isBlank(idempotencyKey) ? task.getTaskCode() : idempotencyKey);
        task.setStatus(ResearchTaskStatus.NEW.name());
        try {
            taskMapper.insert(task);
        } catch (DuplicateKeyException e) {
            // 唯一键兜底：并发重复提交时查原任务返回
            ResearchTask exist = taskMapper.selectOne(
                new LambdaQueryWrapper<ResearchTask>().eq(ResearchTask::getIdempotencyKey, task.getIdempotencyKey()));
            if (exist != null) {
                log.info("研究任务幂等命中（唯一键兜底），返回原任务，taskCode={}", exist.getTaskCode());
                return exist.getPkId();
            }
            throw new ServiceException("研究任务创建冲突（QPF-RESEARCH-EVENT-001）：幂等键已被占用");
        }
        log.info("创建研究任务成功，taskCode={}，taskType={}", task.getTaskCode(), bo.getTaskType());
        return task.getPkId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTask(Long taskId) {
        ResearchTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("研究任务不存在（QPF-RESEARCH-TASK-001）");
        }
        // 幂等（§10.6）：已成功的任务重复提交直接忽略，不重复产生研究事件与下游副作用
        if (ResearchTaskStatus.SUCCEEDED.name().equals(task.getStatus())) {
            log.info("研究任务已成功，重复提交忽略，taskId={}", taskId);
            return;
        }
        // 并发防重入：RUNNING 状态不再发起第二次分析
        if (ResearchTaskStatus.RUNNING.name().equals(task.getStatus())) {
            log.info("研究任务运行中，跳过重复提交，taskId={}", taskId);
            return;
        }
        updateTaskStatus(taskId, ResearchTaskStatus.RUNNING.name());

        // 1. 调用 Dify/TradingAgents 获取研究草稿（校验前一律视为草稿）；
        //    适配器不可用/超时不判任务失败，转 PENDING_REVIEW 待人工复核或补偿重试（详设_0 §5.7）
        DifyDraftResult result;
        try {
            result = difyAdapter.analyze(task);
        } catch (Exception e) {
            log.warn("Dify/TradingAgents 适配器异常，任务转待人工复核（AI_ANALYSIS_PENDING），taskId={}", taskId, e);
            markTaskPendingReview(taskId, "QPF-AI-001");
            return;
        }
        if (result == null) {
            log.warn("Dify/TradingAgents 适配器返回空草稿，任务转待人工复核，taskId={}", taskId);
            markTaskPendingReview(taskId, "QPF-AI-002");
            return;
        }

        // 2. 保存原始 AI 草稿（raw_response 保留，非事实主表）
        AiResearchDraft draft = new AiResearchDraft();
        BeanUtils.copyProperties(result, draft);
        draft.setPkId(snowflake.nextId());
        draft.setResearchTaskId(taskId);
        draft.setValidationStatus(AiDraftValidationStatus.PENDING.name());
        draftMapper.insert(draft);

        // 3. Spring Boot 确定性校验（§10.3）：摘要与引用非空视为最低校验门槛；
        //    未通过时草稿置 CLAIM_UNVERIFIED，任务转 PENDING_REVIEW 待人工复核（不判失败、不升级事件）
        boolean valid = StrUtil.isNotBlank(result.getSummary());
        if (!valid) {
            log.warn("研究草稿校验未通过（摘要为空），草稿置 CLAIM_UNVERIFIED，任务转待人工复核，taskId={}", taskId);
            draftMapper.update(null, new LambdaUpdateWrapper<AiResearchDraft>()
                .eq(AiResearchDraft::getPkId, draft.getPkId())
                .set(AiResearchDraft::getValidationStatus, AiDraftValidationStatus.CLAIM_UNVERIFIED.name()));
            markTaskPendingReview(taskId, "QPF-AI-003");
            return;
        }
        draftMapper.update(null, new LambdaUpdateWrapper<AiResearchDraft>()
            .eq(AiResearchDraft::getPkId, draft.getPkId())
            .set(AiResearchDraft::getValidationStatus, AiDraftValidationStatus.VERIFIED.name()));

        // 4. 保存核验后的研究事件 + 证据（事件幂等键与任务绑定，重复提交不会重复创建）
        ResearchEvent existEvent = eventMapper.selectOne(
            new LambdaQueryWrapper<ResearchEvent>().eq(ResearchEvent::getIdempotencyKey, task.getIdempotencyKey() + "-event"));
        if (existEvent != null) {
            log.info("研究事件幂等命中，跳过重复创建，eventCode={}", existEvent.getEventCode());
            updateTaskStatus(taskId, ResearchTaskStatus.SUCCEEDED.name());
            updateTaskRunId(taskId, result.getRunId());
            return;
        }
        ResearchEvent event = new ResearchEvent();
        event.setPkId(snowflake.nextId());
        event.setEventCode("RE-" + snowflake.nextId());
        event.setEventType(task.getTaskType());
        event.setImpactScope(result.getImpactScopeCandidate());
        event.setImpactLevel(result.getImpactLevelCandidate());
        event.setDirection(result.getDirectionCandidate());
        // 状态机（§10.4）：草稿经校验后置 VERIFIED，随发布流转 PUBLISHED
        event.setStatus(ResearchEventStatus.VERIFIED.name());
        event.setVersion("1.0");
        event.setSourceRefs(task.getMaterialIds());
        event.setIdempotencyKey(task.getIdempotencyKey() + "-event");
        try {
            eventMapper.insert(event);
        } catch (DuplicateKeyException e) {
            log.info("研究事件幂等命中（唯一键兜底），跳过重复创建，taskId={}", taskId);
            updateTaskStatus(taskId, ResearchTaskStatus.SUCCEEDED.name());
            updateTaskRunId(taskId, result.getRunId());
            return;
        }

        EventEvidence evidence = new EventEvidence();
        evidence.setPkId(snowflake.nextId());
        evidence.setResearchEventId(event.getPkId());
        evidence.setEvidenceType(EvidenceType.FACT.name());
        evidence.setSourceRef(result.getCitations());
        evidenceMapper.insert(evidence);

        // 状态机：VERIFIED → PUBLISHED（发布后事件进入下游可见状态）
        eventMapper.update(null, new LambdaUpdateWrapper<ResearchEvent>()
            .eq(ResearchEvent::getPkId, event.getPkId())
            .set(ResearchEvent::getStatus, ResearchEventStatus.PUBLISHED.name()));

        updateTaskStatus(taskId, ResearchTaskStatus.SUCCEEDED.name());
        updateTaskRunId(taskId, result.getRunId());

        // 5. 发布研究事件（AFTER_COMMIT 监听，事务回滚不会泄漏事件，重复幂等键不重复产生下游副作用）
        applicationEventPublisher.publishEvent(new ResearchEventPublishedEvent(
            event.getPkId(), event.getEventCode(), event.getIdempotencyKey()));
        log.info("研究事件已发布，eventCode={}，taskId={}", event.getEventCode(), taskId);
    }

    @Override
    public PageResult<ResearchEventVo> queryEventPage(String status, String subjectType, PageQuery pageQuery) {
        LambdaQueryWrapper<ResearchEvent> wrapper = new LambdaQueryWrapper<ResearchEvent>()
            .eq(StrUtil.isNotBlank(status), ResearchEvent::getStatus, status)
            .eq(StrUtil.isNotBlank(subjectType), ResearchEvent::getSubjectType, subjectType)
            .orderByDesc(ResearchEvent::getCreateTime);
        Page<ResearchEvent> page = eventMapper.selectPage(pageQuery.build(), wrapper);
        List<ResearchEventVo> rows = page.getRecords().stream().map(this::toEventVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public ResearchEventVo queryEventById(Long pkId) {
        ResearchEvent entity = eventMapper.selectById(pkId);
        return entity == null ? null : toEventVo(entity);
    }

    @Override
    public List<AiResearchDraftVo> listDraftByTask(Long taskId) {
        List<AiResearchDraft> list = draftMapper.selectList(
            new LambdaQueryWrapper<AiResearchDraft>()
                .eq(AiResearchDraft::getResearchTaskId, taskId)
                .orderByDesc(AiResearchDraft::getCreateTime));
        return list.stream().map(this::toDraftVo).collect(Collectors.toList());
    }

    @Override
    public List<EventEvidenceVo> listEvidenceByEvent(Long eventId) {
        List<EventEvidence> list = evidenceMapper.selectList(
            new LambdaQueryWrapper<EventEvidence>()
                .eq(EventEvidence::getResearchEventId, eventId)
                .orderByAsc(EventEvidence::getPkId));
        return list.stream().map(this::toEvidenceVo).collect(Collectors.toList());
    }

    /**
     * 任务转待人工复核（降级语义）：AI 适配器异常或草稿校验未通过时使用，
     * 任务保留 PENDING_REVIEW 状态等待人工复核或 researchTaskRetryJob 补偿，不判失败。
     */
    private void markTaskPendingReview(Long taskId, String errorCode) {
        taskMapper.update(null, new LambdaUpdateWrapper<ResearchTask>()
            .eq(ResearchTask::getPkId, taskId)
            .set(ResearchTask::getStatus, ResearchTaskStatus.PENDING_REVIEW.name())
            .set(ResearchTask::getErrorCode, errorCode));
    }

    private void updateTaskStatus(Long taskId, String status) {
        taskMapper.update(null, new LambdaUpdateWrapper<ResearchTask>()
            .eq(ResearchTask::getPkId, taskId)
            .set(ResearchTask::getStatus, status));
    }

    private void updateTaskRunId(Long taskId, String runId) {
        taskMapper.update(null, new LambdaUpdateWrapper<ResearchTask>()
            .eq(ResearchTask::getPkId, taskId)
            .set(ResearchTask::getRunId, runId));
    }

    private ResearchTaskVo toTaskVo(ResearchTask source) {
        ResearchTaskVo vo = new ResearchTaskVo();
        BeanUtils.copyProperties(source, vo);
        vo.setStatusText(ResearchTaskStatus.descOf(source.getStatus()));
        return vo;
    }

    private ResearchEventVo toEventVo(ResearchEvent source) {
        ResearchEventVo vo = new ResearchEventVo();
        BeanUtils.copyProperties(source, vo);
        vo.setStatusText(ResearchEventStatus.descOf(source.getStatus()));
        vo.setDirectionText(ResearchDirection.descOf(source.getDirection()));
        return vo;
    }

    private AiResearchDraftVo toDraftVo(AiResearchDraft source) {
        AiResearchDraftVo vo = new AiResearchDraftVo();
        BeanUtils.copyProperties(source, vo);
        vo.setValidationStatusText(AiDraftValidationStatus.descOf(source.getValidationStatus()));
        return vo;
    }

    private EventEvidenceVo toEvidenceVo(EventEvidence source) {
        EventEvidenceVo vo = new EventEvidenceVo();
        BeanUtils.copyProperties(source, vo);
        vo.setEvidenceTypeText(EvidenceType.descOf(source.getEvidenceType()));
        return vo;
    }

    @Override
    public PageResult<ResearchMaterialVo> queryMaterialPage(String keyword, PageQuery pageQuery) {
        LambdaQueryWrapper<DataRawSnapshot> wrapper = new LambdaQueryWrapper<DataRawSnapshot>()
            .and(StrUtil.isNotBlank(keyword), w -> w
                .like(DataRawSnapshot::getSnapshotId, keyword)
                .or()
                .like(DataRawSnapshot::getExternalId, keyword))
            .orderByDesc(DataRawSnapshot::getObservedAt);
        Page<DataRawSnapshot> page = snapshotMapper.selectPage(pageQuery.build(), wrapper);
        List<ResearchMaterialVo> rows = page.getRecords().stream()
            .map(this::toMaterialVo)
            .collect(Collectors.toList());
        fillMaterialSourceNames(rows);
        return PageResult.build(rows, page.getTotal());
    }

    private ResearchMaterialVo toMaterialVo(DataRawSnapshot source) {
        ResearchMaterialVo vo = new ResearchMaterialVo();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }

    private void fillMaterialSourceNames(List<ResearchMaterialVo> rows) {
        List<Long> sourceIds = rows.stream()
            .map(ResearchMaterialVo::getSourceId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (sourceIds.isEmpty()) {
            return;
        }
        Map<Long, DataSource> sourceMap = dataSourceMapper.selectBatchIds(sourceIds).stream()
            .collect(Collectors.toMap(DataSource::getPkId, s -> s, (a, b) -> a));
        for (ResearchMaterialVo row : rows) {
            if (row.getSourceId() == null) {
                continue;
            }
            DataSource ds = sourceMap.get(row.getSourceId());
            if (ds != null) {
                row.setSourceCode(ds.getSourceCode());
                row.setSourceName(ds.getSourceName());
            }
        }
    }
}
