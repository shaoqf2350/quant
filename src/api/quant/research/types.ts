/**
 * 新闻研究链类型定义（《详设_1 §10》）
 */

/** 研究任务分页查询 */
export interface ResearchTaskQuery {
  pageNum: number;
  pageSize: number;
  status?: string;
  taskType?: string;
}

/** 研究任务视图对象 */
export interface ResearchTaskVO {
  pkId: number | string;
  taskCode: string;
  taskType?: string;
  status?: string;
  statusText?: string;
  materialIds?: string;
  subjectRefs?: string;
  workflowVersion?: string;
  promptVersion?: string;
  runId?: string;
  errorCode?: string;
  createTime?: string;
  updateTime?: string;
}

/** 研究任务创建表单 */
export interface ResearchTaskForm {
  taskType: string;
  materialIds?: string;
  subjectRefs?: string;
  snapshotId?: number | string;
  workflowVersion?: string;
  promptVersion?: string;
  idempotencyKey?: string;
}

/** AI 研究草稿视图对象 */
export interface AiResearchDraftVO {
  pkId: number | string;
  researchTaskId: number | string;
  runId?: string;
  modelVersion?: string;
  summary?: string;
  factCandidates?: string;
  impactScopeCandidate?: string;
  impactLevelCandidate?: string;
  directionCandidate?: string;
  subjectCandidates?: string;
  citations?: string;
  counterQuestions?: string;
  warnings?: string;
  validationStatus?: string;
  validationStatusText?: string;
  createTime?: string;
}

/** 研究事件分页查询 */
export interface ResearchEventQuery {
  pageNum: number;
  pageSize: number;
  status?: string;
  subjectType?: string;
}

/** 研究事件视图对象 */
export interface ResearchEventVO {
  pkId: number | string;
  eventCode: string;
  eventType?: string;
  impactScope?: string;
  impactLevel?: string;
  direction?: string;
  directionText?: string;
  subjectType?: string;
  subjectId?: number | string;
  status?: string;
  statusText?: string;
  sourceRefs?: string;
  createTime?: string;
}

/** 事件证据视图对象 */
export interface EventEvidenceVO {
  pkId: number | string;
  researchEventId: number | string;
  evidenceType?: string;
  evidenceTypeText?: string;
  sourceId?: number | string;
  sourceRef?: string;
  publishedAt?: string;
  contentHash?: string;
  quoteText?: string;
  createTime?: string;
}

/** 研究材料清单分页查询 */
export interface ResearchMaterialQuery {
  pageNum: number;
  pageSize: number;
  keyword?: string;
}

/** 研究材料视图对象（原始快照，供创建研究任务时勾选） */
export interface ResearchMaterialVO {
  pkId: number | string;
  snapshotId?: string;
  externalId?: string;
  sourceId?: number | string;
  sourceCode?: string;
  sourceName?: string;
  sourceGroupId?: number | string;
  publishedAt?: string;
  observedAt?: string;
  authorizationStatus?: string;
}