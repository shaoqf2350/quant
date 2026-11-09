package com.quantpf.marketdata.service;

import com.quantpf.domain.bo.DataSourceBo;
import com.quantpf.domain.entity.DataSourceField;
import com.quantpf.domain.entity.DataSourceInterface;
import com.quantpf.domain.vo.DataSourceCatalogVo;
import com.quantpf.domain.vo.DataSourceVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.List;

/**
 * 数据源管理服务（《详设_1 §5》）。
 */
public interface IDataSourceService {

    /**
     * 内置数据源目录（§25.2.1，事实源在后端）。供「新增数据源」下拉选择与编码校验。
     *
     * @return 数据源目录列表
     */
    List<DataSourceCatalogVo> listCatalog();

    /**
     * 分页查询数据源。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 数据源分页结果
     */
    PageResult<DataSourceVo> queryPage(DataSourceBo bo, PageQuery pageQuery);

    /**
     * 查询数据源详情。
     *
     * @param pkId 主键
     * @return 数据源详情；不存在返回 null
     */
    DataSourceVo queryById(Long pkId);

    /**
     * 新增数据源。
     *
     * @param bo 表单
     */
    void add(DataSourceBo bo);

    /**
     * 修改数据源。
     *
     * @param bo 表单
     */
    void edit(DataSourceBo bo);

    /**
     * 批量删除数据源。
     *
     * @param pkIds 主键集合
     */
    void remove(List<Long> pkIds);

    /**
     * 查询数据源下的接口目录。
     *
     * @param sourceId 数据源ID
     * @return 接口目录列表
     */
    List<DataSourceInterface> listInterface(Long sourceId);

    /**
     * 查询接口下的字段级授权目录。
     *
     * @param interfaceId 接口ID
     * @return 字段目录列表
     */
    List<DataSourceField> listField(Long interfaceId);
}