package com.quantpf.marketdata.service;

import com.quantpf.domain.vo.InstrumentVersionVo;
import com.quantpf.domain.vo.InstrumentVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.List;
import java.util.Map;

/**
 * 标的身份服务（《详设_1 §11》）。
 */
public interface IInstrumentService {

    /**
     * 分页查询标的（股票/期货）。
     *
     * @param keyword     代码或名称关键字（可空）
     * @param productType 品种类型（可空）
     * @param status      状态（可空）
     * @param pageQuery   分页参数
     * @return 标的分页结果
     */
    PageResult<InstrumentVo> queryPage(String keyword, String productType, String status, PageQuery pageQuery);

    /**
     * 查询标的详情。
     *
     * @param id 主键（既有表 id）
     * @return 标的详情；不存在返回 null
     */
    InstrumentVo queryById(Long id);

    /**
     * 查询标的交易属性版本链。
     *
     * @param instrumentId 标的ID
     * @return 版本列表（按生效时间倒序）
     */
    List<InstrumentVersionVo> listVersion(Long instrumentId);

    /**
     * 标的同步（股票/期货基础信息，§11；当前为占位，待 Tushare 授权后接入适配器）。
     */
    void syncInstruments();

    /**
     * 可交易资格判定（§12.3.4：叠加日历/上市/停牌/数据就绪）。
     *
     * @param instrumentId 标的ID
     * @return 可交易资格判定结果（instrumentId/instrumentStatus/bindingTradable/qualityStatus/capacityStatus/
     *         readiness/paperReady/eligibility/tradable/reason/nextStep）
     */
    Map<String, Object> tradability(Long instrumentId);
}