package com.quantpf.marketdata.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quantpf.domain.bo.CalendarDefBo;
import com.quantpf.domain.entity.CalendarDay;
import com.quantpf.domain.entity.CalendarDef;
import com.quantpf.domain.entity.CalendarSession;
import com.quantpf.domain.entity.InstrumentCalendarBinding;
import com.quantpf.domain.entity.MarketDataReadiness;
import com.quantpf.domain.enums.CalendarBindingType;
import com.quantpf.domain.enums.CalendarDayStatus;
import com.quantpf.domain.enums.DataQualityStatus;
import com.quantpf.domain.enums.SessionType;
import com.quantpf.domain.enums.TradabilityStatus;
import com.quantpf.domain.persistence.mapper.CalendarDayMapper;
import com.quantpf.domain.persistence.mapper.CalendarDefMapper;
import com.quantpf.domain.persistence.mapper.CalendarSessionMapper;
import com.quantpf.domain.persistence.mapper.InstrumentCalendarBindingMapper;
import com.quantpf.domain.persistence.mapper.MarketDataReadinessMapper;
import com.quantpf.domain.vo.CalendarDayVo;
import com.quantpf.domain.vo.CalendarDefVo;
import com.quantpf.domain.vo.CalendarSessionVo;
import com.quantpf.domain.vo.InstrumentCalendarBindingVo;
import com.quantpf.domain.vo.MarketDataReadinessVo;
import com.quantpf.marketdata.service.ICalendarService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.quantpf.domain.entity.CalendarConflict;
import com.quantpf.domain.entity.CalendarSourceSnapshot;
import com.quantpf.domain.entity.FxSessionRule;
import com.quantpf.domain.enums.CalendarConflictStatus;
import com.quantpf.domain.persistence.mapper.CalendarConflictMapper;
import com.quantpf.domain.persistence.mapper.CalendarSourceSnapshotMapper;
import com.quantpf.domain.persistence.mapper.FxSessionRuleMapper;
import com.quantpf.domain.vo.CalendarConflictVo;
import com.quantpf.domain.vo.CalendarSourceSnapshotVo;
import com.quantpf.domain.vo.FxSessionRuleVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 交易日历与日线就绪服务实现（《详设_1 §12》）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CalendarServiceImpl implements ICalendarService {

    private final CalendarDefMapper calendarDefMapper;
    private final CalendarDayMapper calendarDayMapper;
    private final CalendarSessionMapper calendarSessionMapper;
    private final InstrumentCalendarBindingMapper bindingMapper;
    private final MarketDataReadinessMapper readinessMapper;
    private final Snowflake snowflake;
    private final CalendarConflictMapper conflictMapper;
    private final CalendarSourceSnapshotMapper sourceSnapshotMapper;
    private final FxSessionRuleMapper fxSessionRuleMapper;

    @Override
    public PageResult<CalendarDefVo> queryPage(CalendarDefBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CalendarDef> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(bo.getCalendarCode()), CalendarDef::getCalendarCode, bo.getCalendarCode())
               .like(StrUtil.isNotBlank(bo.getCalendarName()), CalendarDef::getCalendarName, bo.getCalendarName())
               .eq(StrUtil.isNotBlank(bo.getMarketType()), CalendarDef::getMarketType, bo.getMarketType())
               .eq(StrUtil.isNotBlank(bo.getStatus()), CalendarDef::getStatus, bo.getStatus())
               .orderByAsc(CalendarDef::getCalendarCode);
        Page<CalendarDef> page = calendarDefMapper.selectPage(pageQuery.build(), wrapper);
        List<CalendarDefVo> rows = page.getRecords().stream().map(this::toDefVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public CalendarDefVo queryById(Long pkId) {
        CalendarDef entity = calendarDefMapper.selectById(pkId);
        return entity == null ? null : toDefVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(CalendarDefBo bo) {
        checkCalendarCodeUnique(bo.getCalendarCode(), null);
        CalendarDef entity = new CalendarDef();
        BeanUtils.copyProperties(bo, entity);
        entity.setPkId(snowflake.nextId());
        calendarDefMapper.insert(entity);
        log.info("新增日历定义成功，calendarCode={}，pkId={}", entity.getCalendarCode(), entity.getPkId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(CalendarDefBo bo) {
        if (bo.getPkId() == null) {
            throw new ServiceException("修改日历定义时主键不能为空");
        }
        checkCalendarCodeUnique(bo.getCalendarCode(), bo.getPkId());
        CalendarDef entity = calendarDefMapper.selectById(bo.getPkId());
        if (entity == null) {
            throw new ServiceException("日历定义不存在");
        }
        BeanUtils.copyProperties(bo, entity);
        calendarDefMapper.updateById(entity);
        log.info("修改日历定义成功，pkId={}", entity.getPkId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> pkIds) {
        if (pkIds == null || pkIds.isEmpty()) {
            return;
        }
        calendarDefMapper.deleteBatchIds(pkIds);
        log.info("批量删除日历定义，pkIds={}", pkIds);
    }

    @Override
    public PageResult<CalendarDayVo> queryDayPage(Long calendarId, LocalDate startDate, LocalDate endDate, PageQuery pageQuery) {
        LambdaQueryWrapper<CalendarDay> wrapper = new LambdaQueryWrapper<CalendarDay>()
            .eq(calendarId != null, CalendarDay::getCalendarId, calendarId)
            .ge(startDate != null, CalendarDay::getMarketLocalDate, startDate)
            .le(endDate != null, CalendarDay::getMarketLocalDate, endDate)
            .orderByAsc(CalendarDay::getMarketLocalDate);
        Page<CalendarDay> page = calendarDayMapper.selectPage(pageQuery.build(), wrapper);
        List<CalendarDayVo> rows = page.getRecords().stream().map(this::toDayVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public CalendarDayVo queryDayDetail(Long calendarId, LocalDate date) {
        CalendarDay entity = calendarDayMapper.selectOne(
            new LambdaQueryWrapper<CalendarDay>()
                .eq(CalendarDay::getCalendarId, calendarId)
                .eq(CalendarDay::getMarketLocalDate, date));
        return entity == null ? null : toDayVo(entity);
    }

    @Override
    public List<CalendarSessionVo> listSession(Long calendarId) {
        List<CalendarSession> list = calendarSessionMapper.selectList(
            new LambdaQueryWrapper<CalendarSession>()
                .eq(CalendarSession::getCalendarId, calendarId)
                .orderByAsc(CalendarSession::getSessionType)
                .orderByAsc(CalendarSession::getSequenceNo));
        return list.stream().map(this::toSessionVo).collect(Collectors.toList());
    }

    @Override
    public List<InstrumentCalendarBindingVo> listBinding(Long instrumentId, Long calendarId) {
        List<InstrumentCalendarBinding> list = bindingMapper.selectList(
            new LambdaQueryWrapper<InstrumentCalendarBinding>()
                .eq(instrumentId != null, InstrumentCalendarBinding::getInstrumentId, instrumentId)
                .eq(calendarId != null, InstrumentCalendarBinding::getCalendarId, calendarId));
        return list.stream().map(this::toBindingVo).collect(Collectors.toList());
    }

    @Override
    public PageResult<MarketDataReadinessVo> queryReadinessPage(Long calendarId, String blockReason, PageQuery pageQuery) {
        LambdaQueryWrapper<MarketDataReadiness> wrapper = new LambdaQueryWrapper<MarketDataReadiness>()
            .eq(calendarId != null, MarketDataReadiness::getCalendarId, calendarId)
            .like(StrUtil.isNotBlank(blockReason), MarketDataReadiness::getBlockReason, blockReason)
            .orderByDesc(MarketDataReadiness::getMarketLocalDate);
        Page<MarketDataReadiness> page = readinessMapper.selectPage(pageQuery.build(), wrapper);
        List<MarketDataReadinessVo> rows = page.getRecords().stream().map(this::toReadinessVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public PageResult<CalendarSourceSnapshotVo> querySourceSnapshotPage(String sourceName, String endpointName, PageQuery pageQuery) {
        LambdaQueryWrapper<CalendarSourceSnapshot> wrapper = new LambdaQueryWrapper<CalendarSourceSnapshot>()
            .eq(StrUtil.isNotBlank(sourceName), CalendarSourceSnapshot::getSourceName, sourceName)
            .eq(StrUtil.isNotBlank(endpointName), CalendarSourceSnapshot::getEndpointName, endpointName)
            .orderByDesc(CalendarSourceSnapshot::getRetrievedAt);
        Page<CalendarSourceSnapshot> page = sourceSnapshotMapper.selectPage(pageQuery.build(), wrapper);
        List<CalendarSourceSnapshotVo> rows = page.getRecords().stream().map(this::toSourceSnapshotVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public PageResult<CalendarConflictVo> queryConflictPage(Long calendarId, String status, PageQuery pageQuery) {
        LambdaQueryWrapper<CalendarConflict> wrapper = new LambdaQueryWrapper<CalendarConflict>()
            .eq(calendarId != null, CalendarConflict::getCalendarId, calendarId)
            .eq(StrUtil.isNotBlank(status), CalendarConflict::getStatus, status)
            .orderByDesc(CalendarConflict::getCreateTime);
        Page<CalendarConflict> page = conflictMapper.selectPage(pageQuery.build(), wrapper);
        List<CalendarConflictVo> rows = page.getRecords().stream().map(this::toConflictVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void overrideConflict(Long pkId, String overrideDecision) {
        CalendarConflict conflict = conflictMapper.selectById(pkId);
        if (conflict == null) {
            throw new ServiceException("日历冲突不存在（QPF-CALENDAR-001）");
        }
        // 状态校验：仅 OPEN/REVIEWED 可人工覆盖，已 OVERRIDDEN/CLOSED 不得重复覆盖（§12.2.2 冲突不静默择一）
        String currentStatus = conflict.getStatus();
        if (!CalendarConflictStatus.OPEN.name().equals(currentStatus)
            && !CalendarConflictStatus.REVIEWED.name().equals(currentStatus)) {
            throw new ServiceException(
                "日历冲突状态不允许覆盖（QPF-CALENDAR-001）：当前状态 " + currentStatus);
        }
        if (StrUtil.isBlank(overrideDecision)) {
            throw new ServiceException("人工覆盖结论不能为空（QPF-CALENDAR-001）");
        }
        conflictMapper.update(null, new LambdaUpdateWrapper<CalendarConflict>()
            .eq(CalendarConflict::getPkId, pkId)
            .set(CalendarConflict::getStatus, CalendarConflictStatus.OVERRIDDEN.name())
            .set(CalendarConflict::getOverrideDecision, overrideDecision));
        log.info("日历冲突已人工覆盖，pkId={}，fromStatus={}，overrideDecision={}", pkId, currentStatus, overrideDecision);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importCalendar(Long calendarId, String endpointName,
                                              LocalDate startDate, LocalDate endDate) {
        CalendarDef def = calendarDefMapper.selectById(calendarId);
        if (def == null) {
            throw new ServiceException("日历定义不存在（QPF-CALENDAR-002）");
        }
        if (StrUtil.isBlank(endpointName)) {
            throw new ServiceException("来源接口名不能为空（QPF-CALENDAR-002）");
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new ServiceException("导入日期区间非法（QPF-CALENDAR-002）");
        }
        if (startDate.plusDays(366).isBefore(endDate)) {
            throw new ServiceException("单次导入区间不得超过一年（QPF-CALENDAR-002）");
        }
        java.util.Map<String, Object> result = new LinkedHashMap<>();
        int imported = 0;
        int skipped = 0;
        int conflicts = 0;

        // 1. 保存来源原始快照（§12.2.6：请求参数与响应哈希，幂等由唯一键保证）
        String requestParams = "{\"calendarCode\":\"" + def.getCalendarCode()
            + "\",\"endpoint\":\"" + endpointName + "\",\"start\":\"" + startDate + "\",\"end\":\"" + endDate + "\"}";
        String payloadHash = cn.hutool.crypto.SecureUtil.sha256(
            def.getCalendarCode() + "#" + endpointName + "#" + startDate + "#" + endDate);
        CalendarSourceSnapshot snapshot = new CalendarSourceSnapshot();
        snapshot.setPkId(snowflake.nextId());
        snapshot.setSourceName("TUSHARE_PRO");
        snapshot.setEndpointName(endpointName);
        snapshot.setRequestParams(requestParams);
        snapshot.setResponseSchemaVersion("1");
        snapshot.setRawPayloadHash(payloadHash);
        snapshot.setRawPayloadRef("mock://" + endpointName + "/" + def.getCalendarCode());
        snapshot.setRetrievedAt(LocalDateTime.now());
        snapshot.setPermissionProfile("MOCK（施工期未授权，详设_1 §25.2.1）");
        try {
            sourceSnapshotMapper.insert(snapshot);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 相同区间重复导入：快照幂等命中，继续做交易日记录的增量对账
            log.info("日历来源快照幂等命中，calendarCode={}，endpoint={}", def.getCalendarCode(), endpointName);
        }

        // 2. Mock 适配生成交易日（§12.2 按接口隔离；周末休市、工作日开市；
        //    is_open 原始值与标准化布尔同时保存，解析失败不得默认开市）
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            boolean isOpen = date.getDayOfWeek() != java.time.DayOfWeek.SATURDAY
                && date.getDayOfWeek() != java.time.DayOfWeek.SUNDAY;
            String isOpenRaw = isOpen ? "1" : "0";
            String recordHash = cn.hutool.crypto.SecureUtil.sha256(
                def.getCalendarCode() + "#" + date + "#" + isOpenRaw);
            CalendarDay existing = calendarDayMapper.selectOne(
                new LambdaQueryWrapper<CalendarDay>()
                    .eq(CalendarDay::getCalendarId, calendarId)
                    .eq(CalendarDay::getMarketLocalDate, date));
            if (existing != null && existing.getIsOpen() != null && existing.getIsOpen() == isOpen) {
                skipped++;
                continue;
            }
            if (existing != null) {
                // 冲突：与既有记录开闭市结论不一致 → 写 CalendarConflict，不静默择一（§12.2.2）
                CalendarConflict conflict = new CalendarConflict();
                conflict.setPkId(snowflake.nextId());
                conflict.setConflictCode("CC-" + snowflake.nextId());
                conflict.setCalendarId(calendarId);
                conflict.setMarketLocalDate(date);
                conflict.setConflictType("SOURCE_CONFLICT");
                conflict.setSourceA("existing:" + (existing.getIsOpen() ? "is_open=1" : "is_open=0")
                    + (existing.getSourceRecordHash() == null ? "" : "#" + existing.getSourceRecordHash()));
                conflict.setSourceB(endpointName + ":is_open=" + isOpenRaw + "#" + recordHash);
                conflict.setDescription("日历导入与既有记录开闭市结论冲突："
                    + (existing.getIsOpen() ? "既有记录开市" : "既有记录休市") + "，新来源"
                    + (isOpen ? "开市" : "休市") + "，待人工覆盖（不静默择一）");
                conflict.setStatus(CalendarConflictStatus.OPEN.name());
                conflict.setImpactScope("影响交易日历判断与下游交易日/时段资格");
                conflictMapper.insert(conflict);
                conflicts++;
                continue;
            }
            CalendarDay day = new CalendarDay();
            day.setPkId(snowflake.nextId());
            day.setCalendarId(calendarId);
            day.setMarketLocalDate(date);
            day.setIsOpen(isOpen);
            day.setIsOpenRaw(isOpenRaw);
            day.setDayStatus(isOpen ? CalendarDayStatus.OPEN.name() : CalendarDayStatus.CLOSED.name());
            day.setSourceRecordHash(recordHash);
            day.setQualityStatus(DataQualityStatus.AVAILABLE.name());
            day.setVersion("1.0");
            day.setCurrentFlag(true);
            calendarDayMapper.insert(day);
            imported++;
        }

        // 3. 回填前后交易日（导入区间内可计算部分）
        recomputePrevNextTradeDates(calendarId, startDate, endDate);

        result.put("calendarId", calendarId);
        result.put("endpoint", endpointName);
        result.put("range", startDate + " ~ " + endDate);
        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("conflicts", conflicts);
        log.info("交易日历导入完成（Mock），calendarId={}，endpoint={}，imported={}，skipped={}，conflicts={}",
            calendarId, endpointName, imported, skipped, conflicts);
        return result;
    }

    /**
     * 重算区间内交易日的前/后交易日（区间边界取可用数据，不跨界推断）。
     */
    private void recomputePrevNextTradeDates(Long calendarId, LocalDate startDate, LocalDate endDate) {
        List<CalendarDay> days = calendarDayMapper.selectList(
            new LambdaQueryWrapper<CalendarDay>()
                .eq(CalendarDay::getCalendarId, calendarId)
                .ge(CalendarDay::getMarketLocalDate, startDate)
                .le(CalendarDay::getMarketLocalDate, endDate)
                .eq(CalendarDay::getIsOpen, true)
                .orderByAsc(CalendarDay::getMarketLocalDate));
        for (int i = 0; i < days.size(); i++) {
            CalendarDay day = days.get(i);
            LocalDate prev = i > 0 ? days.get(i - 1).getMarketLocalDate() : day.getPreviousTradeDate();
            LocalDate next = i < days.size() - 1 ? days.get(i + 1).getMarketLocalDate() : day.getNextTradeDate();
            boolean changed = !java.util.Objects.equals(prev, day.getPreviousTradeDate())
                || !java.util.Objects.equals(next, day.getNextTradeDate());
            if (changed) {
                calendarDayMapper.update(null, new LambdaUpdateWrapper<CalendarDay>()
                    .eq(CalendarDay::getPkId, day.getPkId())
                    .set(CalendarDay::getPreviousTradeDate, prev)
                    .set(CalendarDay::getNextTradeDate, next));
            }
        }
    }

    @Override
    public PageResult<FxSessionRuleVo> queryFxSessionRulePage(String classify, PageQuery pageQuery) {
        LambdaQueryWrapper<FxSessionRule> wrapper = new LambdaQueryWrapper<FxSessionRule>()
            .eq(StrUtil.isNotBlank(classify), FxSessionRule::getClassify, classify)
            .orderByAsc(FxSessionRule::getTsCode);
        Page<FxSessionRule> page = fxSessionRuleMapper.selectPage(pageQuery.build(), wrapper);
        List<FxSessionRuleVo> rows = page.getRecords().stream().map(this::toFxSessionRuleVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    private CalendarSourceSnapshotVo toSourceSnapshotVo(CalendarSourceSnapshot source) {
        CalendarSourceSnapshotVo vo = new CalendarSourceSnapshotVo();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }

    private CalendarConflictVo toConflictVo(CalendarConflict source) {
        CalendarConflictVo vo = new CalendarConflictVo();
        BeanUtils.copyProperties(source, vo);
        vo.setStatusText(CalendarConflictStatus.descOf(source.getStatus()));
        return vo;
    }

    private FxSessionRuleVo toFxSessionRuleVo(FxSessionRule source) {
        FxSessionRuleVo vo = new FxSessionRuleVo();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }

    private void checkCalendarCodeUnique(String calendarCode, Long excludePrimaryId) {
        LambdaQueryWrapper<CalendarDef> wrapper = new LambdaQueryWrapper<CalendarDef>()
            .eq(CalendarDef::getCalendarCode, calendarCode)
            .ne(excludePrimaryId != null, CalendarDef::getPkId, excludePrimaryId);
        if (calendarDefMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("日历编码已存在：" + calendarCode);
        }
    }

    private CalendarDefVo toDefVo(CalendarDef source) {
        CalendarDefVo vo = new CalendarDefVo();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }

    private CalendarDayVo toDayVo(CalendarDay source) {
        CalendarDayVo vo = new CalendarDayVo();
        BeanUtils.copyProperties(source, vo);
        vo.setDayStatusText(CalendarDayStatus.descOf(source.getDayStatus()));
        vo.setQualityStatusText(DataQualityStatus.descOf(source.getQualityStatus()));
        return vo;
    }

    private CalendarSessionVo toSessionVo(CalendarSession source) {
        CalendarSessionVo vo = new CalendarSessionVo();
        BeanUtils.copyProperties(source, vo);
        vo.setSessionTypeText(SessionType.descOf(source.getSessionType()));
        return vo;
    }

    private InstrumentCalendarBindingVo toBindingVo(InstrumentCalendarBinding source) {
        InstrumentCalendarBindingVo vo = new InstrumentCalendarBindingVo();
        BeanUtils.copyProperties(source, vo);
        vo.setBindingTypeText(CalendarBindingType.descOf(source.getBindingType()));
        vo.setTradabilityStatusText(TradabilityStatus.descOf(source.getTradabilityStatus()));
        return vo;
    }

    private MarketDataReadinessVo toReadinessVo(MarketDataReadiness source) {
        MarketDataReadinessVo vo = new MarketDataReadinessVo();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }
}