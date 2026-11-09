package com.quantpf.domain.persistence.mapper;

import com.quantpf.domain.entity.MarketBar;
import com.quantpf.domain.vo.MarketBarVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * K 线行情 Mapper（MyBatis Plus，映射既有表 quantpf_market_bar）。
 */
public interface MarketBarMapper extends BaseMapperPlus<MarketBar, MarketBarVo> {
}