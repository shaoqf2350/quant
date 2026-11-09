import request from '@/utils/request';
import type { AxiosPromise } from '@/utils/api-types';
import type { PageResult } from '@/api/types';
import type {
  CapacityQuery,
  CapacityVO,
  FeatureQuery,
  FeatureVO,
  MarketBarForm,
  MarketBarQuery,
  MarketBarVO,
  MarketStateQuery,
  MarketStateVO
} from './types';

// 分页查询市场状态
export function listMarketState(query: MarketStateQuery): AxiosPromise<PageResult<MarketStateVO>> {
  return request({ url: '/api/quant/market/state/page', method: 'get', params: query });
}

// 分页查询容量评估
export function listCapacity(query: CapacityQuery): AxiosPromise<PageResult<CapacityVO>> {
  return request({ url: '/api/quant/capacity/page', method: 'get', params: query });
}

// 分页查询量价特征快照
export function listFeature(query: FeatureQuery): AxiosPromise<PageResult<FeatureVO>> {
  return request({ url: '/api/quant/feature/page', method: 'get', params: query });
}

// 分页查询K线
export function listMarketBar(query: MarketBarQuery): AxiosPromise<PageResult<MarketBarVO>> {
  return request({ url: '/api/quant/market/bar/page', method: 'get', params: query });
}

// K线增量导入
export function importMarketBar(data: MarketBarForm) {
  return request({ url: '/api/quant/market/bar/import', method: 'post', data });
}