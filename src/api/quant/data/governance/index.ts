import request from '@/utils/request';
import type { AxiosPromise } from '@/utils/api-types';
import type { PageResult } from '@/api/types';
import type {
  CollectionQuery,
  CollectionVO,
  PointInTimeQuery,
  PointInTimeVO,
  QualityQuery,
  QualityResolveForm,
  QualityVO,
  SnapshotQuery,
  SnapshotVO
} from './types';

// 分页查询原始快照
export function listSnapshot(query: SnapshotQuery): AxiosPromise<PageResult<SnapshotVO>> {
  return request({ url: '/api/quant/data/snapshot/page', method: 'get', params: query });
}

// 查询原始快照详情
export function getSnapshot(pkId: number | string): AxiosPromise<SnapshotVO> {
  return request({ url: '/api/quant/data/snapshot/' + pkId, method: 'get' });
}

// 分页查询采集任务
export function listCollection(query: CollectionQuery): AxiosPromise<PageResult<CollectionVO>> {
  return request({ url: '/api/quant/data/collection/page', method: 'get', params: query });
}

// 分页查询数据质量问题
export function listQuality(query: QualityQuery): AxiosPromise<PageResult<QualityVO>> {
  return request({ url: '/api/quant/data/quality/page', method: 'get', params: query });
}

// 处理数据质量问题
export function resolveQuality(pkId: number | string, data: QualityResolveForm) {
  return request({ url: '/api/quant/data/quality/' + pkId + '/resolve', method: 'post', data });
}

// 分页查询点时数据版本
export function listPointInTime(query: PointInTimeQuery): AxiosPromise<PageResult<PointInTimeVO>> {
  return request({ url: '/api/quant/data/point-in-time/page', method: 'get', params: query });
}