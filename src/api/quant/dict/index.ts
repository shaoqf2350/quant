import request from '@/utils/request';
import type { AxiosPromise } from '@/utils/api-types';
import type { DictOption } from './types';

// 模块级缓存：字典为静态事实源，首次拉取后复用，避免每页重复请求（《补充规范》§3.4/§4.1）
const dictCache = new Map<string, AxiosPromise<DictOption[]>>();

// 查询枚举字典选项（value=存库值、label=中文标签、description=可选说明、order=可选排序）
export function getDict(enumName: string): AxiosPromise<DictOption[]> {
  if (!dictCache.has(enumName)) {
    dictCache.set(
      enumName,
      request({ url: '/api/quant/dict/' + enumName, method: 'get' }).catch((err: unknown) => {
        // 失败不缓存，下次重试
        dictCache.delete(enumName);
        throw err;
      })
    );
  }
  return dictCache.get(enumName)!;
}
