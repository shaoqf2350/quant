package com.quantpf.marketdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quantpf.domain.bo.MarketBarBo;
import com.quantpf.domain.entity.MarketBar;
import com.quantpf.domain.persistence.mapper.MarketBarMapper;
import com.quantpf.domain.vo.MarketBarVo;
import com.quantpf.marketdata.service.IMarketBarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * K 线行情服务实现（《详设_1 §14》）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MarketBarServiceImpl implements IMarketBarService {

    private final MarketBarMapper marketBarMapper;
    private final cn.hutool.core.lang.Snowflake snowflake;

    @Override
    public PageResult<MarketBarVo> queryBarPage(Long instrumentId, String timeframe,
                                                LocalDateTime startTime, LocalDateTime endTime, PageQuery pageQuery) {
        LambdaQueryWrapper<MarketBar> wrapper = new LambdaQueryWrapper<MarketBar>()
            .eq(instrumentId != null, MarketBar::getInstrumentId, instrumentId)
            .eq(timeframe != null && !timeframe.isBlank(), MarketBar::getTimeframe, timeframe)
            .ge(startTime != null, MarketBar::getBarTime, startTime)
            .le(endTime != null, MarketBar::getBarTime, endTime)
            .orderByAsc(MarketBar::getBarTime);
        Page<MarketBar> page = marketBarMapper.selectPage(pageQuery.build(), wrapper);
        List<MarketBarVo> rows = page.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public MarketBarVo queryBarById(Long id) {
        MarketBar entity = marketBarMapper.selectById(id);
        return entity == null ? null : toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importBar(MarketBarBo bo) {
        validateOhlc(bo);

        // §14.3 INCREMENTAL：按 (标的, 周期, bar_time) 增量去重，命中则跳过；
        // check-then-act 之外以唯一键兜底并发导入 DuplicateKeyException（§21.2 幂等）
        Long count = marketBarMapper.selectCount(new LambdaQueryWrapper<MarketBar>()
            .eq(MarketBar::getInstrumentId, bo.getInstrumentId())
            .eq(MarketBar::getTimeframe, bo.getTimeframe())
            .eq(MarketBar::getBarTime, bo.getBarTime()));
        if (count != null && count > 0) {
            log.info("K线已存在，跳过增量导入，instrumentId={}，timeframe={}，barTime={}",
                bo.getInstrumentId(), bo.getTimeframe(), bo.getBarTime());
            return;
        }

        MarketBar entity = new MarketBar();
        BeanUtils.copyProperties(bo, entity);
        // V1_17_0 起主键为雪花 pk_id（IdType.INPUT，应用层填充）
        entity.setId(snowflake.nextId());
        try {
            marketBarMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // 并发重复导入：唯一键兜底，幂等跳过（返回成功语义，不抛 500）
            log.info("K线并发导入幂等命中（唯一键兜底），instrumentId={}，timeframe={}，barTime={}",
                bo.getInstrumentId(), bo.getTimeframe(), bo.getBarTime());
            return;
        }
        log.info("K线增量导入成功，instrumentId={}，timeframe={}，barTime={}，close={}",
            entity.getInstrumentId(), entity.getTimeframe(), entity.getBarTime(), entity.getClosePrice());
    }

    /**
     * OHLC 合法性校验（《详设_1 §14.2》）：high>=low、high>=open/close、low<=open/close、量额非负。
     * <p>空值守卫：被 Job/其他服务直接调用时无控制器 @Validated 兜底，此处判空防 NPE。</p>
     */
    private void validateOhlc(MarketBarBo bo) {
        if (bo.getOpenPrice() == null || bo.getHighPrice() == null
            || bo.getLowPrice() == null || bo.getClosePrice() == null) {
            throw new ServiceException("K线合法性校验失败（QPF-QUALITY-001）：OHLC 价格字段缺失");
        }
        if (bo.getHighPrice().compareTo(bo.getLowPrice()) < 0) {
            throw new ServiceException("K线合法性校验失败（QPF-QUALITY-001）：high < low");
        }
        if (bo.getHighPrice().compareTo(bo.getOpenPrice()) < 0 || bo.getHighPrice().compareTo(bo.getClosePrice()) < 0) {
            throw new ServiceException("K线合法性校验失败（QPF-QUALITY-001）：high 小于 open/close");
        }
        if (bo.getLowPrice().compareTo(bo.getOpenPrice()) > 0 || bo.getLowPrice().compareTo(bo.getClosePrice()) > 0) {
            throw new ServiceException("K线合法性校验失败（QPF-QUALITY-001）：low 大于 open/close");
        }
        if (bo.getVolume() != null && bo.getVolume().signum() < 0) {
            throw new ServiceException("K线合法性校验失败（QPF-QUALITY-001）：成交量为负");
        }
        if (bo.getTurnover() != null && bo.getTurnover().signum() < 0) {
            throw new ServiceException("K线合法性校验失败（QPF-QUALITY-001）：成交额为负");
        }
    }

    private MarketBarVo toVo(MarketBar source) {
        MarketBarVo vo = new MarketBarVo();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }
}