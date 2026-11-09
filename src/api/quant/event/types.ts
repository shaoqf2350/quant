/**
 * 公司行为与合约事件类型定义（《详设_1 §13》）
 */

/** 公司行为分页查询 */
export interface CorporateActionQuery {
  pageNum: number;
  pageSize: number;
  instrumentId?: number | string;
  actionType?: string;
  status?: string;
}

/** 公司行为表单 */
export interface CorporateActionForm {
  pkId?: number | string;
  instrumentId?: number | string;
  actionType: string;
  actionCode?: string;
  effectiveDate?: string;
  announceDate?: string;
  status?: string;
  detail?: string;
  sourceId?: number | string;
}

/** 公司行为视图对象 */
export interface CorporateActionVO {
  pkId: number | string;
  instrumentId: number | string;
  actionType?: string;
  actionTypeText?: string;
  actionCode: string;
  effectiveDate?: string;
  announceDate?: string;
  status?: string;
  statusText?: string;
  detail?: string;
  sourceId?: number | string;
  createTime?: string;
  updateTime?: string;
}

/** 合约事件分页查询 */
export interface ContractEventQuery {
  pageNum: number;
  pageSize: number;
  instrumentId?: number | string;
  eventType?: string;
  status?: string;
}

/** 合约事件表单 */
export interface ContractEventForm {
  pkId?: number | string;
  instrumentId?: number | string;
  eventType: string;
  eventCode?: string;
  effectiveDate?: string;
  status?: string;
  detail?: string;
  sourceId?: number | string;
}

/** 合约事件视图对象 */
export interface ContractEventVO {
  pkId: number | string;
  instrumentId: number | string;
  eventType?: string;
  eventTypeText?: string;
  eventCode: string;
  effectiveDate?: string;
  status?: string;
  statusText?: string;
  detail?: string;
  sourceId?: number | string;
  createTime?: string;
  updateTime?: string;
}