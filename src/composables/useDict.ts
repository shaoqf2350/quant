import { onMounted, ref, type Ref } from 'vue';
import { getDict } from '@/api/quant/dict';
import type { DictOption } from '@/api/quant/dict/types';

/**
 * 拉取并复用后端枚举字典（《详设_1_补充规范_前端输入最小化与枚举字典》§4.1）。
 * 后端 /api/quant/dict/{enumName} 是唯一事实源，前端不再硬编码选项。
 *
 * 用法：const { options, labelOf } = useDict('DataSourceStatus')
 * - options：下拉选项（value/label/description/order）
 * - labelOf：按 value 取中文标签（表格列展示用，未命中回退为原始值）
 */
export function useDict(name: string): {
  options: Ref<DictOption[]>;
  labelOf: (value: string | number | null | undefined) => string;
} {
  const options = ref<DictOption[]>([]);

  const labelOf = (value: string | number | null | undefined): string => {
    const opt = options.value.find((o) => o.value === value);
    return opt?.label ?? String(value ?? '');
  };

  onMounted(async () => {
    try {
      const res = await getDict(name);
      options.value = res.data ?? [];
    } catch {
      options.value = [];
    }
  });

  return { options, labelOf };
}
