package com.quantpf.marketdata.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quantpf.domain.bo.SourceGroupBo;
import com.quantpf.domain.entity.DataQualityIssue;
import com.quantpf.domain.entity.DataSource;
import com.quantpf.domain.entity.SourceGroup;
import com.quantpf.domain.enums.DataQualityIssueStatus;
import com.quantpf.domain.enums.DataQualityStatus;
import com.quantpf.domain.enums.DataSourceStatus;
import com.quantpf.domain.enums.SourceGroupStatus;
import com.quantpf.domain.event.DataSourceHealthChangedEvent;
import com.quantpf.domain.persistence.mapper.DataQualityIssueMapper;
import com.quantpf.domain.persistence.mapper.DataSourceMapper;
import com.quantpf.domain.persistence.mapper.SourceGroupMapper;
import com.quantpf.domain.vo.SourceGroupVo;
import com.quantpf.marketdata.service.ISourceGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 来源组管理服务实现（《详设_1 §6》）。
 * <p>交叉核验执行（§6.3）：按主源/备源健康状态确定性推导来源组状态，主源失败备源可用时切换，
 * 冲突上报 DataQualityIssue（CONFLICTED）并发布 DataSourceHealthChangedEvent（AFTER_COMMIT）。</p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SourceGroupServiceImpl implements ISourceGroupService {

    private final SourceGroupMapper sourceGroupMapper;
    private final DataSourceMapper dataSourceMapper;
    private final DataQualityIssueMapper qualityIssueMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Snowflake snowflake;

    @Override
    public PageResult<SourceGroupVo> queryPage(SourceGroupBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SourceGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(bo.getGroupCode()), SourceGroup::getGroupCode, bo.getGroupCode())
               .like(StrUtil.isNotBlank(bo.getGroupName()), SourceGroup::getGroupName, bo.getGroupName())
               .eq(StrUtil.isNotBlank(bo.getStatus()), SourceGroup::getStatus, bo.getStatus())
               .orderByDesc(SourceGroup::getCreateTime);
        Page<SourceGroup> page = sourceGroupMapper.selectPage(pageQuery.build(), wrapper);
        List<SourceGroupVo> rows = page.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public SourceGroupVo queryById(Long pkId) {
        SourceGroup entity = sourceGroupMapper.selectById(pkId);
        return entity == null ? null : toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(SourceGroupBo bo) {
        checkGroupCodeUnique(bo.getGroupCode(), null);
        SourceGroup entity = new SourceGroup();
        BeanUtils.copyProperties(bo, entity);
        entity.setPkId(snowflake.nextId());
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus(SourceGroupStatus.CONFIGURED.name());
        }
        sourceGroupMapper.insert(entity);
        log.info("新增来源组成功，groupCode={}，pkId={}", entity.getGroupCode(), entity.getPkId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(SourceGroupBo bo) {
        if (bo.getPkId() == null) {
            throw new ServiceException("修改来源组时主键不能为空");
        }
        checkGroupCodeUnique(bo.getGroupCode(), bo.getPkId());
        SourceGroup entity = sourceGroupMapper.selectById(bo.getPkId());
        if (entity == null) {
            throw new ServiceException("来源组不存在");
        }
        BeanUtils.copyProperties(bo, entity);
        sourceGroupMapper.updateById(entity);
        log.info("修改来源组成功，pkId={}", entity.getPkId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> pkIds) {
        if (pkIds == null || pkIds.isEmpty()) {
            return;
        }
        sourceGroupMapper.deleteBatchIds(pkIds);
        log.info("批量删除来源组，pkIds={}", pkIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> verifyGroupHealth(Long sourceGroupId) {
        SourceGroup group = sourceGroupMapper.selectById(sourceGroupId);
        if (group == null) {
            throw new ServiceException("来源组不存在（QPF-DATA-001）");
        }
        String fromStatus = group.getStatus();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupCode", group.getGroupCode());

        // 1. 收集主源/备源数据源与其状态（§6.1 主备关系）
        List<Long> primaryIds = new ArrayList<>();
        if (group.getPrimarySourceId() != null) {
            primaryIds.add(group.getPrimarySourceId());
        }
        List<Long> backupIds = StrUtil.isBlank(group.getBackupSourceIds()) ? List.of()
            : StrUtil.split(group.getBackupSourceIds(), ',').stream()
                .map(String::trim).filter(StrUtil::isNotBlank)
                .map(Long::valueOf).collect(Collectors.toList());
        List<Long> allIds = new ArrayList<>(primaryIds);
        allIds.addAll(backupIds);
        List<DataSource> sources = allIds.isEmpty() ? List.of()
            : dataSourceMapper.selectBatchIds(allIds);

        long activeCount = sources.stream()
            .filter(s -> DataSourceStatus.ACTIVE.name().equals(s.getStatus())).count();
        long availableCount = sources.stream()
            .filter(s -> isActiveOrDegraded(s.getStatus())).count();
        boolean primaryOk = group.getPrimarySourceId() == null
            || sources.stream().anyMatch(s -> s.getPkId().equals(group.getPrimarySourceId())
                && isActiveOrDegraded(s.getStatus()));
        result.put("configuredSources", sources.size());
        result.put("availableSources", availableCount);

        // 2. 确定性规则推导来源组状态（§6.2 / 指导文档 §9.3 降级矩阵）
        SourceGroupStatus toStatus;
        boolean primarySwitched = false;
        if (sources.isEmpty() || availableCount == 0) {
            toStatus = SourceGroupStatus.UNAVAILABLE;
        } else if (availableCount == sources.size() && activeCount == sources.size()) {
            toStatus = SourceGroupStatus.HEALTHY;
        } else if (!primaryOk && !backupIds.isEmpty()) {
            // 主源失败、备源可用：执行主备切换（§6.3 切换并标记来源）
            Long newPrimary = sources.stream()
                .filter(s -> backupIds.contains(s.getPkId()) && isActiveOrDegraded(s.getStatus()))
                .map(DataSource::getPkId).findFirst().orElse(null);
            if (newPrimary != null) {
                primarySwitched = true;
                sourceGroupMapper.update(null, new LambdaUpdateWrapper<SourceGroup>()
                    .eq(SourceGroup::getPkId, group.getPkId())
                    .set(SourceGroup::getPrimarySourceId, newPrimary)
                    .set(SourceGroup::getBackupSourceIds, joinBackupIds(group, newPrimary)));
                result.put("newPrimarySourceId", newPrimary);
            } else {
                sourceGroupMapper.update(null, new LambdaUpdateWrapper<SourceGroup>()
                    .eq(SourceGroup::getPkId, group.getPkId())
                    .set(SourceGroup::getStatus, SourceGroupStatus.CONFLICTED.name()));
                raiseQualityIssue(group, "主源与全部备源均不可用");
            }
            toStatus = SourceGroupStatus.PARTIALLY_AVAILABLE;
        } else if (availableCount < sources.size()) {
            toStatus = SourceGroupStatus.PARTIALLY_AVAILABLE;
        } else {
            // 全部可用但非全部 ACTIVE（施工期 PENDING_CONFIRMATION/DEGRADED 起步）：DEGRADED，不伪造 HEALTHY
            toStatus = SourceGroupStatus.DEGRADED;
        }

        // 3. 主备切换后仍以组状态落库；冲突场景转 CONFLICTED 并上报质量问题（→ 人工复核，§6.3）
        if (toStatus == SourceGroupStatus.PARTIALLY_AVAILABLE && primarySwitched && !primaryOk) {
            // 切换成功：组降级为部分可用；原主源恢复后可人工切回
        }
        if (!toStatus.name().equals(fromStatus)) {
            sourceGroupMapper.update(null, new LambdaUpdateWrapper<SourceGroup>()
                .eq(SourceGroup::getPkId, group.getPkId())
                .set(SourceGroup::getStatus, toStatus.name()));
        }
        result.put("fromStatus", fromStatus);
        result.put("toStatus", toStatus.name());
        result.put("primarySwitched", primarySwitched);

        // 4. 发布健康变更事件（AFTER_COMMIT 消费；状态无变化也发布供观测，由消费端按需去重）
        applicationEventPublisher.publishEvent(new DataSourceHealthChangedEvent(
            group.getPkId(), group.getGroupCode(), toStatus.name(), primarySwitched));
        log.info("来源组核验完成，groupCode={}，fromStatus={}，toStatus={}，primarySwitched={}",
            group.getGroupCode(), fromStatus, toStatus.name(), primarySwitched);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> verifyAllGroups() {
        List<SourceGroup> groups = sourceGroupMapper.selectList(new LambdaQueryWrapper<>());
        List<Map<String, Object>> results = new ArrayList<>();
        for (SourceGroup group : groups) {
            try {
                results.add(verifyGroupHealth(group.getPkId()));
            } catch (Exception e) {
                log.warn("来源组核验失败，groupCode={}", group.getGroupCode(), e);
            }
        }
        log.info("全部来源组核验完成，共 {} 组", results.size());
        return results;
    }

    /**
     * 数据源状态是否视为"可用"（ACTIVE 或 DEGRADED；DEGRADED 降级可用但须受限，§6.2）。
     */
    private boolean isActiveOrDegraded(String status) {
        return DataSourceStatus.ACTIVE.name().equals(status)
            || DataSourceStatus.DEGRADED.name().equals(status);
    }

    /**
     * 主备切换后重组备源ID串：为空或拆分后仅含原主源时返回原值语义。
     */
    private String joinBackupIds(SourceGroup group, Long newPrimary) {
        List<Long> backupIds = StrUtil.isBlank(group.getBackupSourceIds()) ? List.of()
            : StrUtil.split(group.getBackupSourceIds(), ',').stream()
                .map(String::trim).filter(StrUtil::isNotBlank)
                .map(Long::valueOf).collect(Collectors.toList());
        List<String> newBackups = new ArrayList<>();
        if (group.getPrimarySourceId() != null) {
            newBackups.add(String.valueOf(group.getPrimarySourceId()));
        }
        backupIds.stream()
            .filter(id -> !id.equals(newPrimary))
            .forEach(id -> newBackups.add(String.valueOf(id)));
        return String.join(",", newBackups);
    }

    /**
     * 来源冲突/不可用上报 DataQualityIssue（CONFLICTED，待人工复核；不静默忽略 §2.5 硬约束5）。
     */
    private void raiseQualityIssue(SourceGroup group, String impact) {
        DataQualityIssue issue = new DataQualityIssue();
        issue.setPkId(snowflake.nextId());
        issue.setIssueCode("DQ-" + snowflake.nextId());
        issue.setSubjectType("MATERIAL");
        issue.setQualityStatus(DataQualityStatus.CONFLICTED.name());
        issue.setIssueStatus(DataQualityIssueStatus.WAITING_HUMAN.name());
        issue.setDimension("CONSISTENCY");
        issue.setSeverity("HIGH");
        issue.setBusinessImpact("来源组 " + group.getGroupCode() + " " + impact
            + "；按用途资格矩阵禁止筛选/回测/Paper 新开仓，待人工复核（§9.4）");
        qualityIssueMapper.insert(issue);
        log.warn("来源组冲突已上报质量问题，groupCode={}，issueCode={}，impact={}",
            group.getGroupCode(), issue.getIssueCode(), impact);
    }

    /**
     * 校验来源组编码唯一。
     */
    private void checkGroupCodeUnique(String groupCode, Long excludePrimaryId) {
        LambdaQueryWrapper<SourceGroup> wrapper = new LambdaQueryWrapper<SourceGroup>()
            .eq(SourceGroup::getGroupCode, groupCode)
            .ne(excludePrimaryId != null, SourceGroup::getPkId, excludePrimaryId);
        if (sourceGroupMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("来源组编码已存在：" + groupCode);
        }
    }

    /**
     * 实体转视图对象，补充状态中文描述。
     */
    private SourceGroupVo toVo(SourceGroup group) {
        SourceGroupVo vo = new SourceGroupVo();
        BeanUtils.copyProperties(group, vo);
        vo.setStatusText(SourceGroupStatus.descOf(group.getStatus()));
        return vo;
    }
}