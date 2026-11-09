/**
 * 枚举字典选项（后端统一字典接口 /api/quant/dict/{enumName} 返回，事实源统一在后端）
 */
export interface DictOption {
  value: string;
  label: string;
  description?: string;
  order?: number;
}
