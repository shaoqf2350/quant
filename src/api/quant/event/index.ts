import request from '@/utils/request';
import type { AxiosPromise } from '@/utils/api-types';
import type { PageResult } from '@/api/types';
import type {
  ContractEventForm,
  ContractEventQuery,
  ContractEventVO,
  CorporateActionForm,
  CorporateActionQuery,
  CorporateActionVO
} from './types';

// 分页查询公司行为
export function listCorporateAction(query: CorporateActionQuery): AxiosPromise<PageResult<CorporateActionVO>> {
  return request({ url: '/api/quant/event/corporate/page', method: 'get', params: query });
}

// 新增公司行为
export function addCorporateAction(data: CorporateActionForm) {
  return request({ url: '/api/quant/event/corporate', method: 'post', data });
}

// 修改公司行为
export function updateCorporateAction(data: CorporateActionForm) {
  return request({ url: '/api/quant/event/corporate', method: 'put', data });
}

// 删除公司行为
export function delCorporateAction(pkIds: number | string) {
  return request({ url: '/api/quant/event/corporate/' + pkIds, method: 'delete' });
}

// 分页查询合约事件
export function listContractEvent(query: ContractEventQuery): AxiosPromise<PageResult<ContractEventVO>> {
  return request({ url: '/api/quant/event/contract/page', method: 'get', params: query });
}

// 新增合约事件
export function addContractEvent(data: ContractEventForm) {
  return request({ url: '/api/quant/event/contract', method: 'post', data });
}

// 修改合约事件
export function updateContractEvent(data: ContractEventForm) {
  return request({ url: '/api/quant/event/contract', method: 'put', data });
}

// 删除合约事件
export function delContractEvent(pkIds: number | string) {
  return request({ url: '/api/quant/event/contract/' + pkIds, method: 'delete' });
}