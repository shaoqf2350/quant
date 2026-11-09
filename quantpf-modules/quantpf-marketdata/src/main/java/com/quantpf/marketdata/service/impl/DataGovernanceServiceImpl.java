package com.quantpf.marketdata.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quantpf.domain.entity.DataCollectionTask;
import com.quantpf.domain.entity.DataPointInTime;
import com.quantpf.domain.entity.DataQualityIssue;
import com.quantpf.domain.entity.DataRawSnapshot;
import com.quantpf.domain.enums.CollectionTaskStatus;
import com.quantpf.domain.enums.DataQualityIssueStatus;
import com.quantpf.domain.enums.DataQualityStatus;
import com.quantpf.domain.persistence.mapper.DataCollectionTaskMapper;
import com.quantpf.domain.persistence.mapper.DataPointInTimeMapper;
import com.quantpf.domain.persistence.mapper.DataQualityIssueMapper;
import com.quantpf.domain.persistence.mapper.DataRawSnapshotMapper;
import com.quantpf.domain.vo.DataCollectionTaskVo;
import com.quantpf.domain.vo.DataPointInTimeVo;
import com.quantpf.domain.vo.DataQualityIssueVo;
import com.quantpf.domain.vo.DataRawSnapshotVo;
import com.quantpf.marketdata.service.IDataGovernanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据治理服务实现（《详设_1 §7/§9》）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DataGovernanceServiceImpl implements IDataGovernanceService {

    private final DataRawSnapshotMapper snapshotMapper;
    private final DataCollectionTaskMapper collectionTaskMapper;
    private final DataQualityIssueMapper qualityIssueMapper;
    private final DataPointInTimeMapper pointInTimeMapper;

    @Override
    public PageResult<DataRawSnapshotVo> querySnapshotPage(Long sourceId, Long sourceGroupId, PageQuery pageQuery) {
        LambdaQueryWrapper<DataRawSnapshot> wrapper = new LambdaQueryWrapper<DataRawSnapshot>()
            .eq(sourceId != null, DataRawSnapshot::getSourceId, sourceId)
            .eq(sourceGroupId != null, DataRawSnapshot::getSourceGroupId, sourceGroupId)
            .orderByDesc(DataRawSnapshot::getObservedAt);
        Page<DataRawSnapshot> page = snapshotMapper.selectPage(pageQuery.build(), wrapper);
        List<DataRawSnapshotVo> rows = page.getRecords().stream().map(this::toSnapshotVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public DataRawSnapshotVo querySnapshotById(Long pkId) {
        DataRawSnapshot entity = snapshotMapper.selectById(pkId);
        if (entity == null) {
            return null;
        }
        DataRawSnapshotVo vo = new DataRawSnapshotVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    public PageResult<DataCollectionTaskVo> queryCollectionPage(String status, String taskType, PageQuery pageQuery) {
        LambdaQueryWrapper<DataCollectionTask> wrapper = new LambdaQueryWrapper<DataCollectionTask>()
            .eq(StrUtil.isNotBlank(status), DataCollectionTask::getStatus, status)
            .eq(StrUtil.isNotBlank(taskType), DataCollectionTask::getTaskType, taskType)
            .orderByDesc(DataCollectionTask::getCreateTime);
        Page<DataCollectionTask> page = collectionTaskMapper.selectPage(pageQuery.build(), wrapper);
        List<DataCollectionTaskVo> rows = page.getRecords().stream().map(this::toCollectionVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public PageResult<DataQualityIssueVo> queryQualityPage(String issueStatus, String qualityStatus, PageQuery pageQuery) {
        LambdaQueryWrapper<DataQualityIssue> wrapper = new LambdaQueryWrapper<DataQualityIssue>()
            .eq(StrUtil.isNotBlank(issueStatus), DataQualityIssue::getIssueStatus, issueStatus)
            .eq(StrUtil.isNotBlank(qualityStatus), DataQualityIssue::getQualityStatus, qualityStatus)
            .orderByDesc(DataQualityIssue::getCreateTime);
        Page<DataQualityIssue> page = qualityIssueMapper.selectPage(pageQuery.build(), wrapper);
        List<DataQualityIssueVo> rows = page.getRecords().stream().map(this::toQualityVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveQuality(Long pkId, String handler, String actionTaken, String basisRef, String fixVersion) {
        DataQualityIssue issue = qualityIssueMapper.selectById(pkId);
        if (issue == null) {
            throw new ServiceException("数据质量问题不存在（QPF-QUALITY-001）");
        }
        // 状态机校验（§9.5）：只能从 OPEN/ANALYZING/WAITING_SOURCE/WAITING_HUMAN 迁移到 RESOLVED；
        // 终态（RESOLVED/VERIFIED/CLOSED）不可重复处理
        String currentStatus = issue.getIssueStatus();
        if (!DataQualityIssueStatus.OPEN.name().equals(currentStatus)
            && !DataQualityIssueStatus.ANALYZING.name().equals(currentStatus)
            && !DataQualityIssueStatus.WAITING_SOURCE.name().equals(currentStatus)
            && !DataQualityIssueStatus.WAITING_HUMAN.name().equals(currentStatus)) {
            throw new ServiceException(
                "数据质量问题状态不允许处理（QPF-QUALITY-001）：当前状态 " + currentStatus + " 已是终态或非法迁移");
        }
        if (StrUtil.isBlank(actionTaken)) {
            throw new ServiceException("处理动作不能为空（QPF-QUALITY-001）");
        }
        // 单表修改统一使用 LambdaUpdateWrapper（框架规则）。
        // 质量状态不在此强制置 AVAILABLE：修复版本经过验证后才升级（OPEN→…→RESOLVED→VERIFIED→CLOSED），
        // resolve 只推进问题生命周期，不改写质量状态（§9.5 问题记录须关联处理人/动作/依据/修复版本）。
        LambdaUpdateWrapper<DataQualityIssue> wrapper = new LambdaUpdateWrapper<DataQualityIssue>()
            .eq(DataQualityIssue::getPkId, pkId)
            .set(DataQualityIssue::getIssueStatus, DataQualityIssueStatus.RESOLVED.name())
            .set(DataQualityIssue::getHandler, handler)
            .set(DataQualityIssue::getActionTaken, actionTaken)
            .set(DataQualityIssue::getBasisRef, basisRef)
            .set(DataQualityIssue::getFixVersion, fixVersion);
        qualityIssueMapper.update(null, wrapper);
        log.info("数据质量问题已处理，pkId={}，fromStatus={}，handler={}，fixVersion={}",
            pkId, currentStatus, handler, fixVersion);
    }

    @Override
    public PageResult<DataPointInTimeVo> queryPointInTimePage(String subjectType, Long subjectId, PageQuery pageQuery) {
        LambdaQueryWrapper<DataPointInTime> wrapper = new LambdaQueryWrapper<DataPointInTime>()
            .eq(StrUtil.isNotBlank(subjectType), DataPointInTime::getSubjectType, subjectType)
            .eq(subjectId != null, DataPointInTime::getSubjectId, subjectId)
            .orderByDesc(DataPointInTime::getDecisionTime);
        Page<DataPointInTime> page = pointInTimeMapper.selectPage(pageQuery.build(), wrapper);
        List<DataPointInTimeVo> rows = page.getRecords().stream().map(this::toPointInTimeVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public DataPointInTimeVo queryPointInTimeById(Long pkId) {
        DataPointInTime entity = pointInTimeMapper.selectById(pkId);
        if (entity == null) {
            return null;
        }
        DataPointInTimeVo vo = new DataPointInTimeVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private DataPointInTimeVo toPointInTimeVo(DataPointInTime source) {
        DataPointInTimeVo vo = new DataPointInTimeVo();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }

    private DataRawSnapshotVo toSnapshotVo(DataRawSnapshot source) {
        DataRawSnapshotVo vo = new DataRawSnapshotVo();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }

    private DataCollectionTaskVo toCollectionVo(DataCollectionTask source) {
        DataCollectionTaskVo vo = new DataCollectionTaskVo();
        BeanUtils.copyProperties(source, vo);
        vo.setStatusText(CollectionTaskStatus.descOf(source.getStatus()));
        return vo;
    }

    private DataQualityIssueVo toQualityVo(DataQualityIssue source) {
        DataQualityIssueVo vo = new DataQualityIssueVo();
        BeanUtils.copyProperties(source, vo);
        vo.setQualityStatusText(DataQualityStatus.descOf(source.getQualityStatus()));
        vo.setIssueStatusText(DataQualityIssueStatus.descOf(source.getIssueStatus()));
        return vo;
    }
}