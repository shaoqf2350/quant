import request from '@/utils/request';
import type { AxiosPromise } from '@/utils/api-types';
import type { PageResult } from '@/api/types';
import type {
  CalendarConflictQuery,
  CalendarConflictVO,
  CalendarDayQuery,
  CalendarDayVO,
  CalendarDefForm,
  CalendarDefQuery,
  CalendarDefVO,
  CalendarSourceSnapshotQuery,
  CalendarSourceSnapshotVO,
  FxRuleQuery,
  FxRuleVO,
  ReadinessQuery,
  ReadinessVO
} from './types';

// 分页查询日历定义
export function listCalendarDef(query: CalendarDefQuery): AxiosPromise<PageResult<CalendarDefVO>> {
  return request({ url: '/api/quant/calendar/page', method: 'get', params: query });
}

// 新增日历定义
export function addCalendarDef(data: CalendarDefForm) {
  return request({ url: '/api/quant/calendar', method: 'post', data });
}

// 修改日历定义
export function updateCalendarDef(data: CalendarDefForm) {
  return request({ url: '/api/quant/calendar', method: 'put', data });
}

// 删除日历定义
export function delCalendarDef(pkIds: number | string) {
  return request({ url: '/api/quant/calendar/' + pkIds, method: 'delete' });
}

// 分页查询交易日记录
export function listCalendarDay(query: CalendarDayQuery): AxiosPromise<PageResult<CalendarDayVO>> {
  return request({ url: '/api/quant/calendar/day/page', method: 'get', params: query });
}

// 分页查询日线数据就绪状态
export function listReadiness(query: ReadinessQuery): AxiosPromise<PageResult<ReadinessVO>> {
  return request({ url: '/api/quant/calendar/readiness/page', method: 'get', params: query });
}

// 分页查询日历冲突
export function listCalendarConflict(query: CalendarConflictQuery): AxiosPromise<PageResult<CalendarConflictVO>> {
  return request({ url: '/api/quant/calendar/conflict/page', method: 'get', params: query });
}

// 人工覆盖日历冲突
export function overrideCalendarConflict(pkId: number | string, overrideDecision: string) {
  return request({ url: '/api/quant/calendar/conflict/' + pkId + '/override', method: 'post', params: { overrideDecision } });
}

// 分页查询日历来源原始快照
export function listCalendarSourceSnapshot(query: CalendarSourceSnapshotQuery): AxiosPromise<PageResult<CalendarSourceSnapshotVO>> {
  return request({ url: '/api/quant/calendar/source-snapshot/page', method: 'get', params: query });
}

// 分页查询外汇时段规则
export function listFxRule(query: FxRuleQuery): AxiosPromise<PageResult<FxRuleVO>> {
  return request({ url: '/api/quant/calendar/fx-session-rule/page', method: 'get', params: query });
}
// 导入交易日历（施工期 Mock：按端点隔离生成交易日，冲突不静默择一）
export function importCalendar(pkId: number | string, endpoint: string, startDate: string, endDate: string) {
  return request({
    url: '/api/quant/calendar/' + pkId + '/import',
    method: 'post',
    params: { endpoint, startDate, endDate }
  });
}
