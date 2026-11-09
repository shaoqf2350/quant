/**
 * 交易日历与日线就绪类型定义（《详设_1 §12》）
 */

/** 日历定义分页查询 */
export interface CalendarDefQuery {
  pageNum: number;
  pageSize: number;
  calendarCode?: string;
  calendarName?: string;
  marketType?: string;
  status?: string;
}

/** 日历定义表单 */
export interface CalendarDefForm {
  pkId?: number | string;
  calendarCode: string;
  calendarName?: string;
  marketType?: string;
  regionCode?: string;
  venueCode?: string;
  timezone?: string;
  currency?: string;
  calendarLevel?: string;
  sourcePolicy?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  status?: string;
  version?: string;
  remark?: string;
}

/** 日历定义视图对象 */
export interface CalendarDefVO {
  pkId: number | string;
  calendarCode: string;
  calendarName?: string;
  marketType?: string;
  regionCode?: string;
  venueCode?: string;
  timezone?: string;
  currency?: string;
  calendarLevel?: string;
  sourcePolicy?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  status?: string;
  version?: string;
  createTime?: string;
  updateTime?: string;
  remark?: string;
}

/** 交易日分页查询 */
export interface CalendarDayQuery {
  pageNum: number;
  pageSize: number;
  calendarId?: number | string;
  startDate?: string;
  endDate?: string;
}

/** 交易日视图对象 */
export interface CalendarDayVO {
  pkId: number | string;
  calendarId: number | string;
  marketLocalDate: string;
  isOpen?: boolean;
  dayStatus?: string;
  dayStatusText?: string;
  previousTradeDate?: string;
  nextTradeDate?: string;
  holidayCode?: string;
  specialReason?: string;
  qualityStatus?: string;
  qualityStatusText?: string;
  createTime?: string;
}

/** 日线就绪分页查询 */
export interface ReadinessQuery {
  pageNum: number;
  pageSize: number;
  calendarId?: number | string;
  blockReason?: string;
}

/** 日线就绪视图对象 */
export interface ReadinessVO {
  pkId: number | string;
  calendarId: number | string;
  marketLocalDate: string;
  expectedFlag?: boolean;
  receivedFlag?: boolean;
  validatedFlag?: boolean;
  strategyReadyFlag?: boolean;
  paperReadyFlag?: boolean;
  blockReason?: string;
  createTime?: string;
}

/** 日历冲突分页查询 */
export interface CalendarConflictQuery {
  pageNum: number;
  pageSize: number;
  calendarId?: number | string;
  status?: string;
}

/** 日历冲突视图对象 */
export interface CalendarConflictVO {
  pkId: number | string;
  conflictCode: string;
  calendarId: number | string;
  marketLocalDate?: string;
  conflictType?: string;
  sourceA?: string;
  sourceB?: string;
  description?: string;
  status?: string;
  statusText?: string;
  overrideDecision?: string;
  impactScope?: string;
  createTime?: string;
}

/** 日历来源快照分页查询 */
export interface CalendarSourceSnapshotQuery {
  pageNum: number;
  pageSize: number;
  sourceName?: string;
  endpointName?: string;
}

/** 日历来源快照视图对象 */
export interface CalendarSourceSnapshotVO {
  pkId: number | string;
  sourceName?: string;
  endpointName?: string;
  requestParams?: string;
  rawPayloadHash?: string;
  retrievedAt?: string;
  createTime?: string;
}

/** 外汇时段规则分页查询 */
export interface FxRuleQuery {
  pageNum: number;
  pageSize: number;
  classify?: string;
}

/** 外汇时段规则视图对象 */
export interface FxRuleVO {
  pkId: number | string;
  tsCode?: string;
  classify?: string;
  exchange?: string;
  tradingHours?: string;
  breakTime?: string;
  normalizedSessions?: string;
  timezone?: string;
  validFrom?: string;
  validTo?: string;
  version?: string;
  createTime?: string;
}