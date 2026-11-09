import request from '@/utils/request';
import type { AxiosPromise } from '@/utils/api-types';
import type { PageResult } from '@/api/types';
import type {
  AiResearchDraftVO,
  EventEvidenceVO,
  ResearchEventQuery,
  ResearchEventVO,
  ResearchMaterialQuery,
  ResearchMaterialVO,
  ResearchTaskForm,
  ResearchTaskQuery,
  ResearchTaskVO
} from './types';

// 分页查询研究任务
export function listResearchTask(query: ResearchTaskQuery): AxiosPromise<PageResult<ResearchTaskVO>> {
  return request({ url: '/api/quant/research/task/page', method: 'get', params: query });
}

// 创建研究任务
export function createResearchTask(data: ResearchTaskForm): AxiosPromise<number | string> {
  return request({ url: '/api/quant/research/task', method: 'post', data });
}

// 查询研究任务详情
export function getResearchTask(pkId: number | string): AxiosPromise<ResearchTaskVO> {
  return request({ url: '/api/quant/research/task/' + pkId, method: 'get' });
}

// 提交任务给 Dify/TradingAgents 分析
export function submitResearchTask(pkId: number | string) {
  return request({ url: '/api/quant/research/task/' + pkId + '/submit', method: 'post' });
}

// 查询任务下的 AI 草稿列表
export function listResearchDraft(pkId: number | string): AxiosPromise<AiResearchDraftVO[]> {
  return request({ url: '/api/quant/research/task/' + pkId + '/draft', method: 'get' });
}

// 分页查询研究事件
export function listResearchEvent(query: ResearchEventQuery): AxiosPromise<PageResult<ResearchEventVO>> {
  return request({ url: '/api/quant/research/event/page', method: 'get', params: query });
}

// 查询研究事件详情
export function getResearchEvent(pkId: number | string): AxiosPromise<ResearchEventVO> {
  return request({ url: '/api/quant/research/event/' + pkId, method: 'get' });
}

// 查询事件证据链
export function listResearchEvidence(pkId: number | string): AxiosPromise<EventEvidenceVO[]> {
  return request({ url: '/api/quant/research/event/' + pkId + '/evidence', method: 'get' });
}

// 分页查询研究材料清单（原始快照）
export function listResearchMaterial(query: ResearchMaterialQuery): AxiosPromise<PageResult<ResearchMaterialVO>> {
  return request({ url: '/api/quant/research/material/page', method: 'get', params: query });
}