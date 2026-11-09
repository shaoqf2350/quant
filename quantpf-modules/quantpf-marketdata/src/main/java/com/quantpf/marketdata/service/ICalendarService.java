package com.quantpf.marketdata.service;

import com.quantpf.domain.bo.CalendarDefBo;
import com.quantpf.domain.vo.CalendarConflictVo;
import com.quantpf.domain.vo.CalendarDayVo;
import com.quantpf.domain.vo.CalendarDefVo;
import com.quantpf.domain.vo.CalendarSessionVo;
import com.quantpf.domain.vo.CalendarSourceSnapshotVo;
import com.quantpf.domain.vo.FxSessionRuleVo;
import com.quantpf.domain.vo.InstrumentCalendarBindingVo;
import com.quantpf.domain.vo.MarketDataReadinessVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.time.LocalDate;
import java.util.List;

/**
 * 交易日历与日线就绪服务（《详设_1 §12》）。
 */
public interface ICalendarService {

    /**
     * 分页查询日历定义。
     */
    PageResult<CalendarDefVo> queryPage(CalendarDefBo bo, PageQuery pageQuery);

    /**
     * 查询日历定义详情。
     */
    CalendarDefVo queryById(Long pkId);

    /**
     * 新增日历定义。
     */
    void add(CalendarDefBo bo);

    /**
     * 修改日历定义。
     */
    void edit(CalendarDefBo bo);

    /**
     * 批量删除日历定义。
     */
    void remove(List<Long> pkIds);

    /**
     * 分页查询交易日记录。
     *
     * @param calendarId 日历ID（可空）
     * @param startDate  开始日期（可空）
     * @param endDate    结束日期（可空）
     */
    PageResult<CalendarDayVo> queryDayPage(Long calendarId, LocalDate startDate, LocalDate endDate, PageQuery pageQuery);

    /**
     * 查询某市场本地日期的交易日详情（含前后交易日）。
     */
    CalendarDayVo queryDayDetail(Long calendarId, LocalDate date);

    /**
     * 查询日历下的交易时段模板。
     */
    List<CalendarSessionVo> listSession(Long calendarId);

    /**
     * 查询标的/日历绑定。
     *
     * @param instrumentId 标的ID（可空）
     * @param calendarId   日历ID（可空）
     */
    List<InstrumentCalendarBindingVo> listBinding(Long instrumentId, Long calendarId);

    /**
     * 分页查询日线数据就绪状态。
     */
    PageResult<MarketDataReadinessVo> queryReadinessPage(Long calendarId, String blockReason, PageQuery pageQuery);

    /**
     * 分页查询日历来源原始快照（§12.2.6，专业详情）。
     */
    PageResult<CalendarSourceSnapshotVo> querySourceSnapshotPage(String sourceName, String endpointName, PageQuery pageQuery);

    /**
     * 分页查询日历冲突（§12.2.2）。
     */
    PageResult<CalendarConflictVo> queryConflictPage(Long calendarId, String status, PageQuery pageQuery);

    /**
     * 人工覆盖日历冲突（§12.2.2，冲突不静默择一）。
     */
    void overrideConflict(Long pkId, String overrideDecision);

    /**
     * 导入交易日历（§12.2 Tushare 适配入口 / 指导文档 §7.1 POST /calendar/{id}/import）。
     * <p>当前为 Mock 适配（施工期 Tushare 未授权，§25.2.1）：按端点隔离生成交易日记录，
     * 保存来源原始快照与 is_open 原始值；与既有记录冲突时不静默择一，写入 CalendarConflict 待人工覆盖。</p>
     *
     * @param calendarId   日历ID
     * @param endpointName 来源接口名（trade_cal/fut_trade_cal/hk_tradecal/us_tradecal）
     * @param startDate    导入开始日期
     * @param endDate      导入结束日期
     * @return 导入结果（imported/conflicts/skipped 计数）
     */
    java.util.Map<String, Object> importCalendar(Long calendarId, String endpointName, LocalDate startDate, LocalDate endDate);

    /**
     * 分页查询外汇时段规则（§12.2.5）。
     */
    PageResult<FxSessionRuleVo> queryFxSessionRulePage(String classify, PageQuery pageQuery);
}