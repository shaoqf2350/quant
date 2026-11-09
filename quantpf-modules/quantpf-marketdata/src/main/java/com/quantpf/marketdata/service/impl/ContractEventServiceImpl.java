package com.quantpf.marketdata.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quantpf.domain.bo.ContractEventBo;
import com.quantpf.domain.entity.ContractEvent;
import com.quantpf.domain.enums.ContractEventType;
import com.quantpf.domain.enums.LifecycleEventStatus;
import com.quantpf.domain.event.ContractEventUpdatedEvent;
import com.quantpf.domain.persistence.mapper.ContractEventMapper;
import com.quantpf.domain.vo.ContractEventVo;
import com.quantpf.marketdata.service.IContractEventService;
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
 * 期货合约事件服务实现（《详设_1 §13.2》）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ContractEventServiceImpl implements IContractEventService {

    private final ContractEventMapper contractEventMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Snowflake snowflake;

    @Override
    public PageResult<ContractEventVo> queryPage(ContractEventBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ContractEvent> wrapper = new LambdaQueryWrapper<ContractEvent>()
            .eq(bo.getInstrumentId() != null, ContractEvent::getInstrumentId, bo.getInstrumentId())
            .eq(StrUtil.isNotBlank(bo.getEventType()), ContractEvent::getEventType, bo.getEventType())
            .eq(StrUtil.isNotBlank(bo.getStatus()), ContractEvent::getStatus, bo.getStatus())
            .orderByDesc(ContractEvent::getEffectiveDate);
        Page<ContractEvent> page = contractEventMapper.selectPage(pageQuery.build(), wrapper);
        List<ContractEventVo> rows = page.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public ContractEventVo queryById(Long pkId) {
        ContractEvent entity = contractEventMapper.selectById(pkId);
        return entity == null ? null : toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(ContractEventBo bo) {
        ContractEvent entity = new ContractEvent();
        BeanUtils.copyProperties(bo, entity);
        entity.setPkId(snowflake.nextId());
        if (StrUtil.isBlank(entity.getEventCode())) {
            entity.setEventCode("CE-" + snowflake.nextId());
        }
        // 生命周期状态机（§13.3）：新建只允许 DISCOVERED 起步，未核验的事件不得直接生效
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus(LifecycleEventStatus.DISCOVERED.name());
        } else if (!LifecycleEventStatus.DISCOVERED.name().equals(entity.getStatus())) {
            throw new ServiceException("新增合约事件状态只能从 DISCOVERED 起步（QPF-INSTRUMENT-002）：" + entity.getStatus());
        }
        entity.setVersion("1.0");
        contractEventMapper.insert(entity);
        applicationEventPublisher.publishEvent(new ContractEventUpdatedEvent(
            entity.getInstrumentId(), entity.getEventCode(), entity.getEventType()));
        log.info("新增合约事件成功，eventCode={}，instrumentId={}，eventType={}",
            entity.getEventCode(), entity.getInstrumentId(), entity.getEventType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(ContractEventBo bo) {
        if (bo.getPkId() == null) {
            throw new ServiceException("修改合约事件时主键不能为空");
        }
        ContractEvent entity = contractEventMapper.selectById(bo.getPkId());
        if (entity == null) {
            throw new ServiceException("合约事件不存在（QPF-INSTRUMENT-002）");
        }
        // 状态机迁移校验（§13.3）：DISCOVERED→VERIFIED→SCHEDULED→EFFECTIVE→EXPIRED，任意态→SUPERSEDED
        if (StrUtil.isNotBlank(bo.getStatus()) && !bo.getStatus().equals(entity.getStatus())) {
            if (!LifecycleEventStatus.canTransition(entity.getStatus(), bo.getStatus())) {
                throw new ServiceException(
                    "合约事件状态迁移非法（QPF-INSTRUMENT-002）：" + entity.getStatus() + " → " + bo.getStatus());
            }
        }
        BeanUtils.copyProperties(bo, entity);
        contractEventMapper.updateById(entity);
        applicationEventPublisher.publishEvent(new ContractEventUpdatedEvent(
            entity.getInstrumentId(), entity.getEventCode(), entity.getEventType()));
        log.info("修改合约事件成功，pkId={}", entity.getPkId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> pkIds) {
        if (pkIds == null || pkIds.isEmpty()) {
            return;
        }
        contractEventMapper.deleteBatchIds(pkIds);
        log.info("批量删除合约事件，pkIds={}", pkIds);
    }

    private ContractEventVo toVo(ContractEvent source) {
        ContractEventVo vo = new ContractEventVo();
        BeanUtils.copyProperties(source, vo);
        vo.setStatusText(LifecycleEventStatus.descOf(source.getStatus()));
        vo.setEventTypeText(ContractEventType.descOf(source.getEventType()));
        return vo;
    }
}