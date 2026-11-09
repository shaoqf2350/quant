package com.quantpf.marketdata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.quantpf.domain.bo.DataSourceBo;
import com.quantpf.domain.entity.DataSource;
import com.quantpf.domain.entity.DataSourceField;
import com.quantpf.domain.entity.DataSourceInterface;
import com.quantpf.domain.enums.DataSourceCatalog;
import com.quantpf.domain.enums.DataSourceStatus;
import com.quantpf.domain.persistence.mapper.DataSourceFieldMapper;
import com.quantpf.domain.persistence.mapper.DataSourceInterfaceMapper;
import com.quantpf.domain.persistence.mapper.DataSourceMapper;
import com.quantpf.domain.vo.DataSourceCatalogVo;
import com.quantpf.domain.vo.DataSourceVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据源管理服务实现（《详设_1 §5》）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DataSourceServiceImpl implements IDataSourceService {

    private final DataSourceMapper dataSourceMapper;
    private final DataSourceInterfaceMapper interfaceMapper;
    private final DataSourceFieldMapper fieldMapper;
    private final Snowflake snowflake;

    @Override
    public PageResult<DataSourceVo> queryPage(DataSourceBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(bo.getSourceCode()), DataSource::getSourceCode, bo.getSourceCode())
               .like(StrUtil.isNotBlank(bo.getSourceName()), DataSource::getSourceName, bo.getSourceName())
               .eq(StrUtil.isNotBlank(bo.getStatus()), DataSource::getStatus, bo.getStatus())
               .orderByDesc(DataSource::getCreateTime);
        Page<DataSource> page = dataSourceMapper.selectPage(pageQuery.build(), wrapper);
        List<DataSourceVo> rows = page.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public DataSourceVo queryById(Long pkId) {
        DataSource entity = dataSourceMapper.selectById(pkId);
        return entity == null ? null : toVo(entity);
    }

    @Override
    public List<DataSourceCatalogVo> listCatalog() {
        return DataSourceCatalog.list().stream().map(item -> {
            DataSourceCatalogVo vo = new DataSourceCatalogVo();
            vo.setCode(item.getCode());
            vo.setName(item.getDefaultName());
            vo.setDescription(item.getDescription());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(DataSourceBo bo) {
        // 目录校验（§25.2.1 事实源在后端）：source_code 必须命中内置目录，未登记来源禁止新增
        DataSourceCatalog catalog = DataSourceCatalog.of(bo.getSourceCode());
        if (catalog == null) {
            throw new ServiceException("数据源编码不在内置目录中（QPF-DATA-002）："
                + bo.getSourceCode() + "，请先在后端目录登记该来源");
        }
        checkSourceCodeUnique(bo.getSourceCode(), null);
        DataSource entity = new DataSource();
        BeanUtils.copyProperties(bo, entity);
        entity.setPkId(snowflake.nextId());
        // 名称缺省时按目录默认名称兜底（后端生成默认名，少输入）
        if (StrUtil.isBlank(entity.getSourceName())) {
            entity.setSourceName(catalog.getDefaultName());
        }
        // 新建数据源只允许从 DRAFT/PENDING_CONFIRMATION 起步（§5.3 状态机），施工期数据源未授权（§25.2.1）
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus(DataSourceStatus.DRAFT.name());
        }
        if (!DataSourceStatus.DRAFT.name().equals(entity.getStatus())
            && !DataSourceStatus.PENDING_CONFIRMATION.name().equals(entity.getStatus())) {
            throw new ServiceException("新增数据源状态只能从 DRAFT/PENDING_CONFIRMATION 起步（QPF-DATA-002）");
        }
        dataSourceMapper.insert(entity);
        log.info("新增数据源成功，sourceCode={}，pkId={}，status={}", entity.getSourceCode(), entity.getPkId(), entity.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(DataSourceBo bo) {
        if (bo.getPkId() == null) {
            throw new ServiceException("修改数据源时主键不能为空");
        }
        checkSourceCodeUnique(bo.getSourceCode(), bo.getPkId());
        DataSource entity = dataSourceMapper.selectById(bo.getPkId());
        if (entity == null) {
            throw new ServiceException("数据源不存在（QPF-DATA-001）");
        }
        // 状态值合法性 + 状态机迁移校验（§5.3）：新状态必须来自既有状态的合法迁移
        if (StrUtil.isNotBlank(bo.getStatus()) && !bo.getStatus().equals(entity.getStatus())) {
            validateStatusTransition(entity.getStatus(), bo.getStatus());
        }
        BeanUtils.copyProperties(bo, entity);
        // 状态为空时不覆盖（部分更新语义：BO 空字段不清空已有值）
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus(null);
            DataSource update = new DataSource();
            update.setPkId(entity.getPkId());
            BeanUtils.copyProperties(bo, update);
            update.setStatus(dataSourceMapper.selectById(bo.getPkId()).getStatus());
            dataSourceMapper.updateById(update);
            log.info("修改数据源成功（未变更状态），pkId={}", update.getPkId());
            return;
        }
        dataSourceMapper.updateById(entity);
        log.info("修改数据源成功，pkId={}", entity.getPkId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> pkIds) {
        if (pkIds == null || pkIds.isEmpty()) {
            return;
        }
        dataSourceMapper.deleteBatchIds(pkIds);
        log.info("批量删除数据源，pkIds={}", pkIds);
    }

    @Override
    public List<DataSourceInterface> listInterface(Long sourceId) {
        return interfaceMapper.selectList(
            new LambdaQueryWrapper<DataSourceInterface>().eq(DataSourceInterface::getSourceId, sourceId));
    }

    @Override
    public List<DataSourceField> listField(Long interfaceId) {
        return fieldMapper.selectList(
            new LambdaQueryWrapper<DataSourceField>().eq(DataSourceField::getInterfaceId, interfaceId));
    }

    /**
     * 校验数据源编码唯一（《详设_1 §22.1》：控制器不直接写表，唯一性在服务层保证）。
     *
     * @param sourceCode       数据源编码
     * @param excludePrimaryId 排除的主键（编辑时排除自身）
     */
    private void checkSourceCodeUnique(String sourceCode, Long excludePrimaryId) {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<DataSource>()
            .eq(DataSource::getSourceCode, sourceCode)
            .ne(excludePrimaryId != null, DataSource::getPkId, excludePrimaryId);
        if (dataSourceMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("数据源编码已存在：" + sourceCode);
        }
    }

    /**
     * 数据源状态机迁移校验（《详设_1 §5.3》）。
     * <pre>
     * DRAFT → PENDING_CONFIRMATION → ACTIVE → DEGRADED / EXPIRING → EXPIRED → RETIRED
     * 任意状态可 SUSPENDED（人工暂停），SUSPENDED 可回到暂停前状态。
     * 越级迁移（如 DRAFT→ACTIVE、EXPIRED→ACTIVE）必须拒绝，防止未授权数据直接启用。
     * </pre>
     */
    private void validateStatusTransition(String from, String to) {
        DataSourceStatus fromStatus = DataSourceStatus.of(from);
        DataSourceStatus toStatus = DataSourceStatus.of(to);
        if (fromStatus == null || toStatus == null) {
            throw new ServiceException("数据源状态值非法（QPF-DATA-002）：" + from + " → " + to);
        }
        boolean legal = switch (fromStatus) {
            case DRAFT -> toStatus == DataSourceStatus.PENDING_CONFIRMATION
                || toStatus == DataSourceStatus.SUSPENDED || toStatus == DataSourceStatus.RETIRED;
            case PENDING_CONFIRMATION -> toStatus == DataSourceStatus.ACTIVE
                || toStatus == DataSourceStatus.DRAFT || toStatus == DataSourceStatus.SUSPENDED;
            case ACTIVE -> toStatus == DataSourceStatus.DEGRADED || toStatus == DataSourceStatus.EXPIRING
                || toStatus == DataSourceStatus.SUSPENDED;
            case DEGRADED -> toStatus == DataSourceStatus.ACTIVE || toStatus == DataSourceStatus.EXPIRING
                || toStatus == DataSourceStatus.SUSPENDED;
            case EXPIRING -> toStatus == DataSourceStatus.EXPIRED || toStatus == DataSourceStatus.ACTIVE
                || toStatus == DataSourceStatus.SUSPENDED;
            case EXPIRED -> toStatus == DataSourceStatus.RETIRED || toStatus == DataSourceStatus.SUSPENDED;
            case SUSPENDED -> toStatus == DataSourceStatus.ACTIVE || toStatus == DataSourceStatus.DRAFT
                || toStatus == DataSourceStatus.RETIRED;
            case RETIRED -> false;
        };
        if (!legal) {
            throw new ServiceException(
                "数据源状态迁移非法（QPF-DATA-002）：" + from + " → " + to);
        }
    }

    /**
     * 实体转视图对象，补充状态中文描述。
     */
    private DataSourceVo toVo(DataSource source) {
        DataSourceVo vo = new DataSourceVo();
        BeanUtils.copyProperties(source, vo);
        vo.setStatusText(DataSourceStatus.descOf(source.getStatus()));
        return vo;
    }
}