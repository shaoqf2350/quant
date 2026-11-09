<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { importMarketBar, listCapacity, listFeature, listMarketBar, listMarketState } from '@/api/quant/market';
import { listInstrument } from '@/api/quant/instrument';
import type { InstrumentVO } from '@/api/quant/instrument/types';
import type { CapacityVO, FeatureVO, MarketBarForm, MarketBarVO, MarketStateVO } from '@/api/quant/market/types';
import { useDict } from '@/composables/useDict';

const activeTab = ref('state');

// 枚举下拉统一走后端字典接口（事实源在后端，禁止前端硬编码）
const { options: stateOptions } = useDict('MarketStateCode');
const { options: capacityStatusOptions } = useDict('CapacityStatus');
const { options: timeframeOptions } = useDict('BarTimeframe');
const { options: sourceCatalogOptions } = useDict('DataSourceCatalog');
// K线来源：手动录入 + 内置数据源目录
const sourceOptions = computed(() => [
  { value: 'MANUAL', label: '手动录入' },
  ...sourceCatalogOptions.value
]);

// 标的下拉选项（避免手填数字 ID）
const instrumentOptions = ref<InstrumentVO[]>([]);
async function loadInstrumentOptions() {
  try {
    const res = await listInstrument({ pageNum: 1, pageSize: 500 });
    instrumentOptions.value = res.data?.rows ?? [];
  } catch {
    instrumentOptions.value = [];
  }
}

// ---- 市场状态 ----
const stateLoading = ref(false);
const stateTotal = ref(0);
const stateList = ref<MarketStateVO[]>([]);
const stateQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  instrumentId: undefined as number | string | undefined,
  stateCode: ''
});

// ---- 容量评估 ----
const capacityLoading = ref(false);
const capacityTotal = ref(0);
const capacityList = ref<CapacityVO[]>([]);
const capacityQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  instrumentId: undefined as number | string | undefined,
  capacityStatus: ''
});

// ---- 量价特征 ----
const featureLoading = ref(false);
const featureTotal = ref(0);
const featureList = ref<FeatureVO[]>([]);
const featureQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  instrumentId: undefined as number | string | undefined,
  timeframe: ''
});

// ---- K线 ----
const barLoading = ref(false);
const barTotal = ref(0);
const barList = ref<MarketBarVO[]>([]);
const barQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  instrumentId: undefined as number | string | undefined,
  timeframe: ''
});

async function getStateList() {
  stateLoading.value = true;
  try {
    const res = await listMarketState(stateQuery);
    stateList.value = res.data?.rows ?? [];
    stateTotal.value = res.data?.total ?? 0;
  } finally {
    stateLoading.value = false;
  }
}

async function getCapacityList() {
  capacityLoading.value = true;
  try {
    const res = await listCapacity(capacityQuery);
    capacityList.value = res.data?.rows ?? [];
    capacityTotal.value = res.data?.total ?? 0;
  } finally {
    capacityLoading.value = false;
  }
}

async function getFeatureList() {
  featureLoading.value = true;
  try {
    const res = await listFeature(featureQuery);
    featureList.value = res.data?.rows ?? [];
    featureTotal.value = res.data?.total ?? 0;
  } finally {
    featureLoading.value = false;
  }
}

async function getBarList() {
  barLoading.value = true;
  try {
    const res = await listMarketBar(barQuery);
    barList.value = res.data?.rows ?? [];
    barTotal.value = res.data?.total ?? 0;
  } finally {
    barLoading.value = false;
  }
}

function stateQueryClick() {
  stateQuery.pageNum = 1;
  getStateList();
}

function capacityQueryClick() {
  capacityQuery.pageNum = 1;
  getCapacityList();
}

function featureQueryClick() {
  featureQuery.pageNum = 1;
  getFeatureList();
}

function barQueryClick() {
  barQuery.pageNum = 1;
  getBarList();
}

// ---- K线增量导入（§14.3 INCREMENTAL）----
const importVisible = ref(false);
const importing = ref(false);
const importForm = reactive<MarketBarForm>({
  instrumentId: undefined as unknown as number | string,
  timeframe: 'DAY',
  barTime: '',
  openPrice: undefined,
  highPrice: undefined,
  lowPrice: undefined,
  closePrice: undefined,
  source: 'MANUAL'
});

function handleImport() {
  importVisible.value = true;
}

async function submitImport() {
  if (!importForm.instrumentId) {
    ElMessage.warning('请先选择标的');
    return;
  }
  if (!importForm.barTime) {
    ElMessage.warning('请选择K线时间');
    return;
  }
  importing.value = true;
  try {
    await importMarketBar({ ...importForm });
    ElMessage.success('K线导入成功（已存在则幂等跳过）');
    importVisible.value = false;
    barQuery.pageNum = 1;
    getBarList();
  } finally {
    importing.value = false;
  }
}

onMounted(() => {
  loadInstrumentOptions();
  getStateList();
  getCapacityList();
  getFeatureList();
  getBarList();
});
</script>

<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <!-- 市场状态 -->
      <el-tab-pane label="市场状态" name="state">
        <el-form :inline="true" :model="stateQuery" class="search-form">
          <el-form-item label="标的">
            <el-select v-model="stateQuery.instrumentId" filterable clearable placeholder="标的（空=市场级）" style="width: 230px">
              <el-option v-for="inst in instrumentOptions" :key="inst.id" :label="`${inst.instrumentCode} ${inst.instrumentName || ''}`" :value="inst.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="stateQuery.stateCode" placeholder="全部" clearable style="width: 150px">
              <el-option v-for="opt in stateOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="stateQueryClick">查询</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="stateLoading" :data="stateList" border>
          <el-table-column prop="marketLocalDate" label="市场本地日期" width="120" />
          <el-table-column prop="instrumentId" label="标的ID" width="160" />
          <el-table-column prop="stateCodeText" label="市场状态" width="120" />
          <el-table-column prop="stateReason" label="状态原因" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createTime" label="创建时间" width="170" />
        </el-table>

        <el-pagination
          v-show="stateTotal > 0"
          v-model:current-page="stateQuery.pageNum"
          v-model:page-size="stateQuery.pageSize"
          :total="stateTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="stateQueryClick"
          @current-change="getStateList"
        />
      </el-tab-pane>

      <!-- 容量评估 -->
      <el-tab-pane label="容量评估" name="capacity">
        <el-form :inline="true" :model="capacityQuery" class="search-form">
          <el-form-item label="标的">
            <el-select v-model="capacityQuery.instrumentId" filterable clearable placeholder="标的" style="width: 200px">
              <el-option v-for="inst in instrumentOptions" :key="inst.id" :label="`${inst.instrumentCode} ${inst.instrumentName || ''}`" :value="inst.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="容量状态">
            <el-select v-model="capacityQuery.capacityStatus" placeholder="全部" clearable style="width: 150px">
              <el-option v-for="opt in capacityStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="capacityQueryClick">查询</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="capacityLoading" :data="capacityList" border>
          <el-table-column prop="assessDate" label="评估日期" width="120" />
          <el-table-column prop="instrumentId" label="标的ID" width="160" />
          <el-table-column prop="capacityStatusText" label="容量状态" width="110" />
          <el-table-column prop="avgTurnover" label="日均成交额" width="130" />
          <el-table-column prop="turnoverRate" label="换手率" width="100" />
          <el-table-column prop="plannedRatio" label="计划占比" width="100" />
          <el-table-column prop="slippageEstimate" label="预计滑点" width="100" />
          <el-table-column prop="detail" label="明细" min-width="180" show-overflow-tooltip />
        </el-table>

        <el-pagination
          v-show="capacityTotal > 0"
          v-model:current-page="capacityQuery.pageNum"
          v-model:page-size="capacityQuery.pageSize"
          :total="capacityTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="capacityQueryClick"
          @current-change="getCapacityList"
        />
      </el-tab-pane>

      <!-- 量价特征 -->
      <el-tab-pane label="量价特征" name="feature">
        <el-form :inline="true" :model="featureQuery" class="search-form">
          <el-form-item label="标的">
            <el-select v-model="featureQuery.instrumentId" filterable clearable placeholder="标的" style="width: 200px">
              <el-option v-for="inst in instrumentOptions" :key="inst.id" :label="`${inst.instrumentCode} ${inst.instrumentName || ''}`" :value="inst.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="周期">
            <el-select v-model="featureQuery.timeframe" placeholder="全部" clearable style="width: 140px">
              <el-option v-for="opt in timeframeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="featureQueryClick">查询</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="featureLoading" :data="featureList" border>
          <el-table-column prop="marketLocalDate" label="市场本地日期" width="120" />
          <el-table-column prop="instrumentId" label="标的ID" width="160" />
          <el-table-column prop="timeframe" label="周期" width="80" />
          <el-table-column prop="featureDefinitionVersion" label="特征版本" width="110" />
          <el-table-column prop="returnValue" label="收益" width="100" />
          <el-table-column prop="volatility" label="波动" width="100" />
          <el-table-column prop="relativeStrength" label="相对强弱" width="100" />
          <el-table-column prop="volumePercentile" label="成交量分位" width="110" />
          <el-table-column prop="pricePosition" label="价格位置" width="100" />
        </el-table>

        <el-pagination
          v-show="featureTotal > 0"
          v-model:current-page="featureQuery.pageNum"
          v-model:page-size="featureQuery.pageSize"
          :total="featureTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="featureQueryClick"
          @current-change="getFeatureList"
        />
      </el-tab-pane>

      <!-- K线 -->
      <el-tab-pane label="K线" name="bar">
        <el-form :inline="true" :model="barQuery" class="search-form">
          <el-form-item label="标的">
            <el-select v-model="barQuery.instrumentId" filterable clearable placeholder="标的" style="width: 200px">
              <el-option v-for="inst in instrumentOptions" :key="inst.id" :label="`${inst.instrumentCode} ${inst.instrumentName || ''}`" :value="inst.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="周期">
            <el-select v-model="barQuery.timeframe" placeholder="全部" clearable style="width: 140px">
              <el-option v-for="opt in timeframeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="barQueryClick">查询</el-button>
            <el-button v-hasPermi="['quant:kline:import']" type="warning" @click="handleImport">导入K线</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="barLoading" :data="barList" border>
          <el-table-column prop="barTime" label="K线时间" width="170" />
          <el-table-column prop="instrumentId" label="标的ID" width="160" />
          <el-table-column prop="timeframe" label="周期" width="80" />
          <el-table-column prop="openPrice" label="开盘" width="100" />
          <el-table-column prop="highPrice" label="最高" width="100" />
          <el-table-column prop="lowPrice" label="最低" width="100" />
          <el-table-column prop="closePrice" label="收盘" width="100" />
          <el-table-column prop="volume" label="成交量" width="120" />
          <el-table-column prop="turnover" label="成交额" width="130" />
          <el-table-column prop="source" label="来源" width="100" />
        </el-table>

        <el-pagination
          v-show="barTotal > 0"
          v-model:current-page="barQuery.pageNum"
          v-model:page-size="barQuery.pageSize"
          :total="barTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="barQueryClick"
          @current-change="getBarList"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="importVisible" title="K线增量导入" width="560px" append-to-body>
      <el-form :model="importForm" label-width="100px">
        <el-form-item label="标的" required>
          <el-select v-model="importForm.instrumentId" filterable clearable placeholder="请选择标的" style="width: 100%">
            <el-option v-for="inst in instrumentOptions" :key="inst.id" :label="`${inst.instrumentCode} ${inst.instrumentName || ''}`" :value="inst.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期" required>
          <el-select v-model="importForm.timeframe" style="width: 100%">
            <el-option v-for="opt in timeframeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="K线时间" required>
          <el-date-picker v-model="importForm.barTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择K线时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开盘价" required>
          <el-input-number v-model="importForm.openPrice" :precision="4" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最高价" required>
          <el-input-number v-model="importForm.highPrice" :precision="4" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最低价" required>
          <el-input-number v-model="importForm.lowPrice" :precision="4" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="收盘价" required>
          <el-input-number v-model="importForm.closePrice" :precision="4" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="成交量">
          <el-input-number v-model="importForm.volume" :precision="2" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="成交额">
          <el-input-number v-model="importForm.turnover" :precision="2" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="importForm.source" style="width: 100%">
            <el-option v-for="opt in sourceOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.search-form {
  margin-bottom: 10px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>