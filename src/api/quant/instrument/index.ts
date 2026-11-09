import request from '@/utils/request';
import type { AxiosPromise } from '@/utils/api-types';
import type { PageResult } from '@/api/types';
import type { InstrumentQuery, InstrumentVO, InstrumentVersionVO, TradabilityVO } from './types';

// 分页查询标的
export function listInstrument(query: InstrumentQuery): AxiosPromise<PageResult<InstrumentVO>> {
  return request({ url: '/api/quant/instrument/page', method: 'get', params: query });
}

// 查询标的详情
export function getInstrument(id: number | string): AxiosPromise<InstrumentVO> {
  return request({ url: '/api/quant/instrument/' + id, method: 'get' });
}

// 查询标的交易属性版本链
export function listInstrumentVersion(instrumentId: number | string): AxiosPromise<InstrumentVersionVO[]> {
  return request({ url: '/api/quant/instrument/' + instrumentId + '/version', method: 'get' });
}

// 标的同步（基础信息）
export function syncInstrument() {
  return request({ url: '/api/quant/instrument/sync', method: 'post' });
}

// 可交易资格判定
export function getTradability(id: number | string): AxiosPromise<TradabilityVO> {
  return request({ url: '/api/quant/instrument/tradability/' + id, method: 'get' });
}