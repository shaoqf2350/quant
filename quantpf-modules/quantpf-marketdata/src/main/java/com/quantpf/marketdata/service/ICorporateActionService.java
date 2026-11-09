package com.quantpf.marketdata.service;

import com.quantpf.domain.bo.CorporateActionBo;
import com.quantpf.domain.vo.CorporateActionVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.List;

/**
 * 公司行为服务（《详设_1 §13.1》）。
 */
public interface ICorporateActionService {

    /**
     * 分页查询公司行为。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     */
    PageResult<CorporateActionVo> queryPage(CorporateActionBo bo, PageQuery pageQuery);

    /**
     * 查询公司行为详情。
     */
    CorporateActionVo queryById(Long pkId);

    /**
     * 新增公司行为。
     */
    void add(CorporateActionBo bo);

    /**
     * 修改公司行为。
     */
    void edit(CorporateActionBo bo);

    /**
     * 批量删除公司行为。
     */
    void remove(List<Long> pkIds);
}