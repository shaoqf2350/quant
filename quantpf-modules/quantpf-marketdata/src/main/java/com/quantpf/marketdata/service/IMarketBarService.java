package com.quantpf.marketdata.service;

import com.quantpf.domain.bo.MarketBarBo;
import com.quantpf.domain.vo.MarketBarVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.time.LocalDateTime;

/**
 * K 线行情服务（《详设_1 §14》）。
 */
public interface IMarketBarService {

    /**
     * 分页查询 K 线。
     *
     * @param instrumentId 标的ID（可空）
     * @param timeframe    周期（可空）
     * @param startTime    开始时间（可空）
     * @param endTime      结束时间（可空）
     * @param pageQuery    分页参数
     */
    PageResult<MarketBarVo> queryBarPage(Long instrumentId, String timeframe,
                                         LocalDateTime startTime, LocalDateTime endTime, PageQuery pageQuery);

    /**
     * 查询单条 K 线详情。
     */
    MarketBarVo queryBarById(Long id);

    /**
     * K 线增量导入（§14.2 合法性校验 + §14.3 INCREMENTAL 增量去重）。
     *
     * @param bo K线表单
     */
    void importBar(MarketBarBo bo);
}