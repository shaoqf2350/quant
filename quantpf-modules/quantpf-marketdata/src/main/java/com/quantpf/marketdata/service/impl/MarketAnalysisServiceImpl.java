package com.quantpf.marketdata.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quantpf.domain.bo.CapacityAssessmentBo;
import com.quantpf.domain.bo.MarketStateBo;
import com.quantpf.domain.bo.QuantFeatureSnapshotBo;
import com.quantpf.domain.entity.CapacityAssessment;
import com.quantpf.domain.entity.MarketState;
import com.quantpf.domain.entity.QuantFeatureSnapshot;
import com.quantpf.domain.enums.CapacityStatus;
import com.quantpf.domain.enums.MarketStateCode;
import com.quantpf.domain.event.CapacityStatusChangedEvent;
import com.quantpf.domain.event.MarketStateChangedEvent;
import com.quantpf.domain.persistence.mapper.CapacityAssessmentMapper;
import com.quantpf.domain.persistence.mapper.MarketStateMapper;
import com.quantpf.domain.persistence.mapper.QuantFeatureSnapshotMapper;
import com.quantpf.domain.vo.CapacityAssessmentVo;
import com.quantpf.domain.vo.MarketStateVo;
import com.quantpf.domain.vo.QuantFeatureSnapshotVo;
import com.quantpf.marketdata.service.IMarketAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 市场分析服务实现（《详设_1 §14/§15/§23》）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MarketAnalysisServiceImpl implements IMarketAnalysisService {

    private final MarketStateMapper marketStateMapper;
    private final CapacityAssessmentMapper capacityAssessmentMapper;
    private final QuantFeatureSnapshotMapper featureMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Snowflake snowflake;

    // ============ 市场状态 ============

    @Override
    public PageResult<MarketStateVo> queryMarketStatePage(Long instrumentId, Long calendarId, String stateCode, PageQuery pageQuery) {
        LambdaQueryWrapper<MarketState> wrapper = new LambdaQueryWrapper<MarketState>()
            .eq(instrumentId != null, MarketState::getInstrumentId, instrumentId)
            .eq(calendarId != null, MarketState::getCalendarId, calendarId)
            .eq(StrUtil.isNotBlank(stateCode), MarketState::getStateCode, stateCode)
            .orderByDesc(MarketState::getMarketLocalDate);
        Page<MarketState> page = marketStateMapper.selectPage(pageQuery.build(), wrapper);
        List<MarketStateVo> rows = page.getRecords().stream().map(this::toStateVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public MarketStateVo queryMarketStateById(Long pkId) {
        MarketState entity = marketStateMapper.selectById(pkId);
        return entity == null ? null : toStateVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMarketState(MarketStateBo bo) {
        MarketState entity = new MarketState();
        BeanUtils.copyProperties(bo, entity);
        entity.setPkId(snowflake.nextId());
        entity.setVersion("1.0");
        marketStateMapper.insert(entity);
        applicationEventPublisher.publishEvent(new MarketStateChangedEvent(
            entity.getInstrumentId(), entity.getStateCode()));
        log.info("记录市场状态，instrumentId={}，stateCode={}，date={}",
            entity.getInstrumentId(), entity.getStateCode(), entity.getMarketLocalDate());
    }

    // ============ 容量评估 ============

    @Override
    public PageResult<CapacityAssessmentVo> queryCapacityPage(Long instrumentId, String capacityStatus, PageQuery pageQuery) {
        LambdaQueryWrapper<CapacityAssessment> wrapper = new LambdaQueryWrapper<CapacityAssessment>()
            .eq(instrumentId != null, CapacityAssessment::getInstrumentId, instrumentId)
            .eq(StrUtil.isNotBlank(capacityStatus), CapacityAssessment::getCapacityStatus, capacityStatus)
            .orderByDesc(CapacityAssessment::getAssessDate);
        Page<CapacityAssessment> page = capacityAssessmentMapper.selectPage(pageQuery.build(), wrapper);
        List<CapacityAssessmentVo> rows = page.getRecords().stream().map(this::toCapacityVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public CapacityAssessmentVo queryCapacityById(Long pkId) {
        CapacityAssessment entity = capacityAssessmentMapper.selectById(pkId);
        return entity == null ? null : toCapacityVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCapacity(CapacityAssessmentBo bo) {
        if (bo.getInstrumentId() == null) {
            throw new ServiceException("容量评估标的ID不能为空（QPF-DATA-001）");
        }
        CapacityAssessment entity = new CapacityAssessment();
        BeanUtils.copyProperties(bo, entity);
        entity.setPkId(snowflake.nextId());
        // 容量状态由确定性规则计算（§15.4），不信任调用方传入值：
        // 计划金额占比（plannedRatio = 计划金额 / 日均成交额）为分级核心输入，
        // 日均成交额缺失 → UNKNOWN（不得生成新开仓计划）；占比越高冲击越大。
        entity.setCapacityStatus(determineCapacityStatus(bo));
        capacityAssessmentMapper.insert(entity);
        applicationEventPublisher.publishEvent(new CapacityStatusChangedEvent(
            entity.getInstrumentId(), entity.getCapacityStatus()));
        log.info("记录容量评估（确定性规则），instrumentId={}，capacityStatus={}，plannedRatio={}，date={}",
            entity.getInstrumentId(), entity.getCapacityStatus(), bo.getPlannedRatio(), entity.getAssessDate());
    }

    /**
     * 容量状态确定性规则（《详设_1 §15》）。
     * <p>输入缺失（日均成交额或占比未知）→ UNKNOWN；占比分级：
     * &lt;5% 充足 SUFFICIENT；5%~20% 受限 LIMITED（限制风险规模）；20%~50% 高冲击 HIGH_IMPACT（仅观察或低规模 Paper）；
     * ≥50% 不可交易 UNTRADEABLE；日均成交额为 0 或负 → UNTRADEABLE。</p>
     */
    private String determineCapacityStatus(CapacityAssessmentBo bo) {
        if (bo.getAvgTurnover() == null || bo.getPlannedRatio() == null) {
            return CapacityStatus.UNKNOWN.name();
        }
        if (bo.getAvgTurnover().signum() <= 0) {
            return CapacityStatus.UNTRADEABLE.name();
        }
        double ratio = bo.getPlannedRatio().doubleValue();
        if (ratio < 0.05) {
            return CapacityStatus.SUFFICIENT.name();
        }
        if (ratio < 0.20) {
            return CapacityStatus.LIMITED.name();
        }
        if (ratio < 0.50) {
            return CapacityStatus.HIGH_IMPACT.name();
        }
        return CapacityStatus.UNTRADEABLE.name();
    }

    // ============ 量价特征 ============

    @Override
    public PageResult<QuantFeatureSnapshotVo> queryFeaturePage(Long instrumentId, String timeframe, PageQuery pageQuery) {
        LambdaQueryWrapper<QuantFeatureSnapshot> wrapper = new LambdaQueryWrapper<QuantFeatureSnapshot>()
            .eq(instrumentId != null, QuantFeatureSnapshot::getInstrumentId, instrumentId)
            .eq(StrUtil.isNotBlank(timeframe), QuantFeatureSnapshot::getTimeframe, timeframe)
            .orderByDesc(QuantFeatureSnapshot::getMarketLocalDate);
        Page<QuantFeatureSnapshot> page = featureMapper.selectPage(pageQuery.build(), wrapper);
        List<QuantFeatureSnapshotVo> rows = page.getRecords().stream().map(this::toFeatureVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public QuantFeatureSnapshotVo queryFeatureById(Long pkId) {
        QuantFeatureSnapshot entity = featureMapper.selectById(pkId);
        return entity == null ? null : toFeatureVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFeature(QuantFeatureSnapshotBo bo) {
        QuantFeatureSnapshot entity = new QuantFeatureSnapshot();
        BeanUtils.copyProperties(bo, entity);
        entity.setPkId(snowflake.nextId());
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus("CALCULATED");
        }
        featureMapper.insert(entity);
        log.info("记录量价特征快照，instrumentId={}，timeframe={}，date={}",
            entity.getInstrumentId(), entity.getTimeframe(), entity.getMarketLocalDate());
    }

    private MarketStateVo toStateVo(MarketState source) {
        MarketStateVo vo = new MarketStateVo();
        BeanUtils.copyProperties(source, vo);
        vo.setStateCodeText(MarketStateCode.descOf(source.getStateCode()));
        return vo;
    }

    private CapacityAssessmentVo toCapacityVo(CapacityAssessment source) {
        CapacityAssessmentVo vo = new CapacityAssessmentVo();
        BeanUtils.copyProperties(source, vo);
        vo.setCapacityStatusText(CapacityStatus.descOf(source.getCapacityStatus()));
        return vo;
    }

    private QuantFeatureSnapshotVo toFeatureVo(QuantFeatureSnapshot source) {
        QuantFeatureSnapshotVo vo = new QuantFeatureSnapshotVo();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }
}