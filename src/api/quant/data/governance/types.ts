/**
 * 数据治理（原始快照/采集任务/数据质量）类型定义（《详设_1 §7/§9》）
 */

/** 原始快照分页查询 */
export interface SnapshotQuery {
  pageNum: number;
  pageSize: number;
  sourceId?: number | string;
  sourceGroupId?: number | string;
}

/** 原始快照视图对象 */
export interface SnapshotVO {
  pkId: number | string;
  snapshotId: string;
  sourceId?: number | string;
  sourceGroupId?: number | string;
  externalId?: string;
  requestTime?: string;
  observedAt?: string;
  publishedAt?: string;
  availableAt?: string;
  contentHash?: string;
  normalizedTitleHash?: string;
  rawContentReference?: string;
  responseStatus?: string;
  parserVersion?: string;
  responseSchemaVersion?: string;
  authorizationStatus?: string;
  idempotencyKey?: string;
  createTime?: string;
}

/** 采集任务分页查询 */
export interface CollectionQuery {
  pageNum: number;
  pageSize: number;
  status?: string;
  taskType?: string;
}

/** 采集任务视图对象 */
export interface CollectionVO {
  pkId: number | string;
  taskCode: string;
  sourceId?: number | string;
  sourceGroupId?: number | string;
  taskType?: string;
  requestParams?: string;
  status?: string;
  statusText?: string;
  successScope?: string;
  failureScope?: string;
  errorCode?: string;
  retryCount?: number;
  startedAt?: string;
  finishedAt?: string;
  schedulerType?: string;
  createTime?: string;
}

/** 数据质量分页查询 */
export interface QualityQuery {
  pageNum: number;
  pageSize: number;
  issueStatus?: string;
  qualityStatus?: string;
}

/** 数据质量问题视图对象 */
export interface QualityVO {
  pkId: number | string;
  issueCode: string;
  snapshotId?: number | string;
  sourceId?: number | string;
  subjectType?: string;
  subjectId?: number | string;
  dimension?: string;
  qualityStatus?: string;
  qualityStatusText?: string;
  issueStatus?: string;
  issueStatusText?: string;
  severity?: string;
  businessImpact?: string;
  handler?: string;
  actionTaken?: string;
  basisRef?: string;
  fixVersion?: string;
  createTime?: string;
  updateTime?: string;
}

/** 数据质量问题处理表单 */
export interface QualityResolveForm {
  handler?: string;
  actionTaken: string;
  basisRef?: string;
  fixVersion?: string;
}

/** 点时数据版本分页查询 */
export interface PointInTimeQuery {
  pageNum: number;
  pageSize: number;
  subjectType?: string;
  subjectId?: number | string;
}

/** 点时数据版本视图对象 */
export interface PointInTimeVO {
  pkId: number | string;
  subjectType?: string;
  subjectId?: number | string;
  snapshotVersion?: string;
  decisionTime?: string;
  visibleAt?: string;
  effectiveAt?: string;
  revisedAt?: string;
  revisionReason?: string;
  revisionSource?: string;
  payloadRef?: string;
  affectedObjects?: string;
  needRecompute?: boolean;
  createTime?: string;
}