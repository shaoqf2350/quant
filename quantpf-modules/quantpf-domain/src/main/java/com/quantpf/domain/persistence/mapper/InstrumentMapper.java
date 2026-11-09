package com.quantpf.domain.persistence.mapper;

import com.quantpf.domain.entity.InstrumentEntity;
import com.quantpf.domain.vo.InstrumentVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 标的台账 Mapper（MyBatis Plus，映射既有表 quantpf_instrument）。
 */
public interface InstrumentMapper extends BaseMapperPlus<InstrumentEntity, InstrumentVo> {
}