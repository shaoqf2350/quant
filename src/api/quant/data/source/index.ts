import request from '@/utils/request';
import type { AxiosPromise } from '@/utils/api-types';
import type { PageResult } from '@/api/types';
import type {
  DataSourceCatalogVO,
  DataSourceForm,
  DataSourceQuery,
  DataSourceVO,
  SourceGroupForm,
  SourceGroupQuery,
  SourceGroupVO
} from './types';

// 内置数据源目录（后端事实源）
export function listSourceCatalog(): AxiosPromise<DataSourceCatalogVO[]> {
  return request({ url: '/api/quant/data/source/catalog', method: 'get' });
}

// 分页查询数据源
export function listDataSource(query: DataSourceQuery): AxiosPromise<PageResult<DataSourceVO>> {
  return request({ url: '/api/quant/data/source/page', method: 'get', params: query });
}

// 查询数据源详情
export function getDataSource(pkId: number | string): AxiosPromise<DataSourceVO> {
  return request({ url: '/api/quant/data/source/' + pkId, method: 'get' });
}

// 新增数据源
export function addDataSource(data: DataSourceForm) {
  return request({ url: '/api/quant/data/source', method: 'post', data });
}

// 修改数据源
export function updateDataSource(data: DataSourceForm) {
  return request({ url: '/api/quant/data/source', method: 'put', data });
}

// 删除数据源（逗号分隔多个主键）
export function delDataSource(pkIds: number | string) {
  return request({ url: '/api/quant/data/source/' + pkIds, method: 'delete' });
}

// 分页查询来源组
export function listSourceGroup(query: SourceGroupQuery): AxiosPromise<PageResult<SourceGroupVO>> {
  return request({ url: '/api/quant/data/source/group/page', method: 'get', params: query });
}

// 查询来源组详情
export function getSourceGroup(pkId: number | string): AxiosPromise<SourceGroupVO> {
  return request({ url: '/api/quant/data/source/group/' + pkId, method: 'get' });
}

// 新增来源组
export function addSourceGroup(data: SourceGroupForm) {
  return request({ url: '/api/quant/data/source/group', method: 'post', data });
}

// 修改来源组
export function updateSourceGroup(data: SourceGroupForm) {
  return request({ url: '/api/quant/data/source/group', method: 'put', data });
}

// 删除来源组（逗号分隔多个主键）
export function delSourceGroup(pkIds: number | string) {
  return request({ url: '/api/quant/data/source/group/' + pkIds, method: 'delete' });
}
// 执行来源组健康核验（主备切换/冲突上报/状态流转）
export function verifySourceGroup(pkId: number | string) {
  return request({ url: '/api/quant/data/source/group/' + pkId + '/verify', method: 'post' });
}

// 执行全部来源组核验（手动触发；调度由 qualityCheckJob 周期执行）
export function verifyAllSourceGroups() {
  return request({ url: '/api/quant/data/source/group/verify-all', method: 'post' });
}
