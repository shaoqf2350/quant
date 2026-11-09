package com.quantpf.marketdata.service;

import com.quantpf.domain.bo.ContractEventBo;
import com.quantpf.domain.vo.ContractEventVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.List;

/**
 * 期货合约事件服务（《详设_1 §13.2》）。
 */
public interface IContractEventService {

    /**
     * 分页查询合约事件。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     */
    PageResult<ContractEventVo> queryPage(ContractEventBo bo, PageQuery pageQuery);

    /**
     * 查询合约事件详情。
     */
    ContractEventVo queryById(Long pkId);

    /**
     * 新增合约事件。
     */
    void add(ContractEventBo bo);

    /**
     * 修改合约事件。
     */
    void edit(ContractEventBo bo);

    /**
     * 批量删除合约事件。
     */
    void remove(List<Long> pkIds);
}