package com.quantpf.marketdata.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quantpf.domain.bo.CorporateActionBo;
import com.quantpf.domain.entity.CorporateAction;
import com.quantpf.domain.enums.CorporateActionType;
import com.quantpf.domain.enums.LifecycleEventStatus;
import com.quantpf.domain.event.CorporateActionUpdatedEvent;
import com.quantpf.domain.persistence.mapper.CorporateActionMapper;
import com.quantpf.domain.vo.CorporateActionVo;
import com.quantpf.marketdata.service.ICorporateActionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 公司行为服务实现（《详设_1 §13.1》）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CorporateActionServiceImpl implements ICorporateActionService {

    private final CorporateActionMapper corporateActionMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Snowflake snowflake;

    @Override
    public PageResult<CorporateActionVo> queryPage(CorporateActionBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CorporateAction> wrapper = new LambdaQueryWrapper<CorporateAction>()
            .eq(bo.getInstrumentId() != null, CorporateAction::getInstrumentId, bo.getInstrumentId())
            .eq(StrUtil.isNotBlank(bo.getActionType()), CorporateAction::getActionType, bo.getActionType())
            .eq(StrUtil.isNotBlank(bo.getStatus()), CorporateAction::getStatus, bo.getStatus())
            .orderByDesc(CorporateAction::getEffectiveDate);
        Page<CorporateAction> page = corporateActionMapper.selectPage(pageQuery.build(), wrapper);
        List<CorporateActionVo> rows = page.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public CorporateActionVo queryById(Long pkId) {
        CorporateAction entity = corporateActionMapper.selectById(pkId);
        return entity == null ? null : toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(CorporateActionBo bo) {
        CorporateAction entity = new CorporateAction();
        BeanUtils.copyProperties(bo, entity);
        entity.setPkId(snowflake.nextId());
        if (StrUtil.isBlank(entity.getActionCode())) {
            entity.setActionCode("CA-" + snowflake.nextId());
        }
        // 生命周期状态机（§13.3）：新建只允许 DISCOVERED 起步（空值默认），未核验的事件不得直接生效
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus(LifecycleEventStatus.DISCOVERED.name());
        } else if (!LifecycleEventStatus.DISCOVERED.name().equals(entity.getStatus())) {
            throw new ServiceException("新增公司行为状态只能从 DISCOVERED 起步（QPF-INSTRUMENT-002）：" + entity.getStatus());
        }
        entity.setVersion("1.0");
        corporateActionMapper.insert(entity);
        applicationEventPublisher.publishEvent(new CorporateActionUpdatedEvent(
            entity.getInstrumentId(), entity.getActionCode(), entity.getActionType()));
        log.info("新增公司行为成功，actionCode={}，instrumentId={}，actionType={}",
            entity.getActionCode(), entity.getInstrumentId(), entity.getActionType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(CorporateActionBo bo) {
        if (bo.getPkId() == null) {
            throw new ServiceException("修改公司行为时主键不能为空");
        }
        CorporateAction entity = corporateActionMapper.selectById(bo.getPkId());
        if (entity == null) {
            throw new ServiceException("公司行为不存在（QPF-INSTRUMENT-002）");
        }
        // 状态机迁移校验（§13.3）：DISCOVERED→VERIFIED→SCHEDULED→EFFECTIVE→EXPIRED，任意态→SUPERSEDED
        if (StrUtil.isNotBlank(bo.getStatus()) && !bo.getStatus().equals(entity.getStatus())) {
            if (!LifecycleEventStatus.canTransition(entity.getStatus(), bo.getStatus())) {
                throw new ServiceException(
                    "公司行为状态迁移非法（QPF-INSTRUMENT-002）：" + entity.getStatus() + " → " + bo.getStatus());
            }
        }
        BeanUtils.copyProperties(bo, entity);
        corporateActionMapper.updateById(entity);
        applicationEventPublisher.publishEvent(new CorporateActionUpdatedEvent(
            entity.getInstrumentId(), entity.getActionCode(), entity.getActionType()));
        log.info("修改公司行为成功，pkId={}，status={}", entity.getPkId(), entity.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> pkIds) {
        if (pkIds == null || pkIds.isEmpty()) {
            return;
        }
        corporateActionMapper.deleteBatchIds(pkIds);
        log.info("批量删除公司行为，pkIds={}", pkIds);
    }

    private CorporateActionVo toVo(CorporateAction source) {
        CorporateActionVo vo = new CorporateActionVo();
        BeanUtils.copyProperties(source, vo);
        vo.setStatusText(LifecycleEventStatus.descOf(source.getStatus()));
        vo.setActionTypeText(CorporateActionType.descOf(source.getActionType()));
        return vo;
    }
}