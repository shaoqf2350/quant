/**
 * 市场分析（市场状态/容量评估/量价特征）类型定义（《详设_1 §14/§15/§23》）
 */

/** 市场状态分页查询 */
export interface MarketStateQuery {
  pageNum: number;
  pageSize: number;
  instrumentId?: number | string;
  calendarId?: number | string;
  stateCode?: string;
}

/** 市场状态视图对象 */
export interface MarketStateVO {
  pkId: number | string;
  instrumentId?: number | string;
  calendarId?: number | string;
  marketLocalDate: string;
  stateCode?: string;
  stateCodeText?: string;
  stateReason?: string;
  createTime?: string;
}

/** 容量评估分页查询 */
export interface CapacityQuery {
  pageNum: number;
  pageSize: number;
  instrumentId?: number | string;
  capacityStatus?: string;
}

/** 容量评估视图对象 */
export interface CapacityVO {
  pkId: number | string;
  instrumentId: number | string;
  assessDate: string;
  capacityStatus?: string;
  capacityStatusText?: string;
  avgTurnover?: number | string;
  avgVolume?: number | string;
  bidAskSpread?: number | string;
  turnoverRate?: number | string;
  floatMarketCap?: number | string;
  plannedRatio?: number | string;
  slippageEstimate?: number | string;
  detail?: string;
  createTime?: string;
}

/** 量价特征分页查询 */
export interface FeatureQuery {
  pageNum: number;
  pageSize: number;
  instrumentId?: number | string;
  timeframe?: string;
}

/** 量价特征视图对象 */
export interface FeatureVO {
  pkId: number | string;
  instrumentId: number | string;
  marketLocalDate: string;
  timeframe?: string;
  featureDefinitionVersion?: string;
  returnValue?: number | string;
  volatility?: number | string;
  relativeStrength?: number | string;
  volumePercentile?: number | string;
  pricePosition?: number | string;
  status?: string;
  createTime?: string;
}

/** K线分页查询 */
export interface MarketBarQuery {
  pageNum: number;
  pageSize: number;
  instrumentId?: number | string;
  timeframe?: string;
  startTime?: string;
  endTime?: string;
}

/** K线视图对象 */
export interface MarketBarVO {
  id: number | string;
  instrumentId: number | string;
  timeframe?: string;
  barTime?: string;
  openPrice?: number | string;
  highPrice?: number | string;
  lowPrice?: number | string;
  closePrice?: number | string;
  volume?: number | string;
  turnover?: number | string;
  source?: string;
  createdAt?: string;
}

/** K线导入表单 */
export interface MarketBarForm {
  instrumentId: number | string;
  timeframe: string;
  barTime: string;
  openPrice: number;
  highPrice: number;
  lowPrice: number;
  closePrice: number;
  volume?: number;
  turnover?: number;
  source: string;
}