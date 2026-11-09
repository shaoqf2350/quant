/**
 * 标的身份类型定义（《详设_1 §11》）
 */

/** 标的分页查询 */
export interface InstrumentQuery {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  productType?: string;
  status?: string;
}

/** 标的视图对象 */
export interface InstrumentVO {
  id: number | string;
  instrumentCode: string;
  instrumentName: string;
  productType: string;
  exchangeCode: string;
  currency?: string;
  tickSize?: number | string;
  lotSize?: number | string;
  status?: string;
  statusText?: string;
  listedDate?: string;
  delistedDate?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** 标的交易属性版本视图对象 */
export interface InstrumentVersionVO {
  pkId: number | string;
  instrumentId: number | string;
  instrumentVersion: string;
  validFrom?: string;
  validUntil?: string;
  changeReason?: string;
  sourceRef?: string;
  tickSize?: number | string;
  lotSize?: number | string;
  marginRate?: number | string;
  priceLimit?: string;
  tradingSessions?: string;
  delistingRule?: string;
  createTime?: string;
}

/** 可交易资格判定结果（§9.4 用途资格矩阵 + §16.1 输入组合） */
export interface TradabilityVO {
  instrumentId: number | string;
  instrumentStatus?: string;
  instrumentStatusText?: string;
  bindingTradable?: boolean;
  qualityStatus?: string;
  qualityStatusText?: string;
  capacityStatus?: string;
  capacityStatusText?: string;
  readinessDate?: string;
  paperReady?: boolean;
  /** 各用途资格（key 为 DataUsageEligibility 枚举值） */
  eligibility?: Record<string, boolean>;
  tradable: boolean;
  reason?: string;
  nextStep?: string;
}