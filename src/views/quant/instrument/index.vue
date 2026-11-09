<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { getTradability, listInstrument, listInstrumentVersion, syncInstrument } from '@/api/quant/instrument';
import type { InstrumentVO, InstrumentVersionVO, TradabilityVO } from '@/api/quant/instrument/types';
import { useDict } from '@/composables/useDict';

// 枚举下拉统一走后端字典接口（事实源在后端，禁止前端硬编码）
const { options: productTypeOptions, labelOf: productTypeLabel } = useDict('ProductType');
// 标的状态：服务层采用 InstrumentIdentityStatus（补充规范 §3.2 误写 InstrumentStatus）
const { options: statusOptions } = useDict('InstrumentIdentityStatus');

const loading = ref(false);
const total = ref(0);
const instrumentList = ref<InstrumentVO[]>([]);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  productType: '',
  status: ''
});

const versionVisible = ref(false);
const versionLoading = ref(false);
const versionList = ref<InstrumentVersionVO[]>([]);
const currentInstrument = ref<Partial<InstrumentVO>>();
const tradabilityVisible = ref(false);
const tradabilityInfo = ref<TradabilityVO>();
const syncing = ref(false);

async function getList() {
  loading.value = true;
  try {
    const res = await listInstrument(queryParams);
    instrumentList.value = res.data?.rows ?? [];
    total.value = res.data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.keyword = '';
  queryParams.productType = '';
  queryParams.status = '';
  handleQuery();
}

async function handleVersion(row: Partial<InstrumentVO>) {
  currentInstrument.value = row;
  versionVisible.value = true;
  versionLoading.value = true;
  try {
    versionList.value = (await listInstrumentVersion(row.id)).data ?? [];
  } finally {
    versionLoading.value = false;
  }
}

async function handleTradability(row: Partial<InstrumentVO>) {
  currentInstrument.value = row;
  tradabilityInfo.value = (await getTradability(row.id)).data;
  tradabilityVisible.value = true;
}

/** 同步标的基础信息（占位：待 Tushare 授权后接入真实适配器） */
async function handleSync() {
  syncing.value = true;
  try {
    await syncInstrument();
    ElMessage.success('同步任务已触发（当前为占位实现，待数据源授权后生效）');
  } finally {
    syncing.value = false;
  }
}

onMounted(getList);
</script>

<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="关键字">
        <el-input v-model="queryParams.keyword" placeholder="代码/名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="品种类型">
        <el-select v-model="queryParams.productType" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="opt in productTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button
          v-hasPermi="['quant:instrument:sync']"
          type="warning"
          :loading="syncing"
          @click="handleSync"
        >同步标的</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="instrumentList" border>
      <el-table-column prop="instrumentCode" label="标的代码" min-width="120" />
      <el-table-column prop="instrumentName" label="标的名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="品种" width="80" align="center">
        <template #default="scope">{{ productTypeLabel(scope.row.productType) }}</template>
      </el-table-column>
      <el-table-column prop="exchangeCode" label="交易所" width="100" />
      <el-table-column prop="currency" label="币种" width="80" />
      <el-table-column prop="lotSize" label="最小单位" width="100" />
      <el-table-column prop="statusText" label="状态" width="80" />
      <el-table-column prop="listedDate" label="上市日期" width="110" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="handleVersion(scope.row)">版本</el-button>
          <el-button link type="primary" @click="handleTradability(scope.row)">资格</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-show="total > 0"
      v-model:current-page="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @size-change="handleQuery"
      @current-change="getList"
    />

    <el-dialog v-model="versionVisible" :title="`交易属性版本 - ${currentInstrument?.instrumentName || ''}`" width="760px" append-to-body>
      <el-table v-loading="versionLoading" :data="versionList" border>
        <el-table-column prop="instrumentVersion" label="版本号" width="100" />
        <el-table-column prop="validFrom" label="生效开始" width="170" />
        <el-table-column prop="validUntil" label="生效结束" width="170" />
        <el-table-column prop="lotSize" label="最小单位" width="90" />
        <el-table-column prop="marginRate" label="保证金率" width="90" />
        <el-table-column prop="priceLimit" label="涨跌停" width="100" />
        <el-table-column prop="changeReason" label="变更原因" min-width="140" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <el-dialog v-model="tradabilityVisible" :title="`可交易资格 - ${currentInstrument?.instrumentName || ''}`" width="560px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标的ID">{{ tradabilityInfo?.instrumentId }}</el-descriptions-item>
        <el-descriptions-item label="标的状态">{{ tradabilityInfo?.instrumentStatusText || tradabilityInfo?.instrumentStatus }}</el-descriptions-item>
        <el-descriptions-item label="日历绑定可交易">{{ tradabilityInfo?.bindingTradable ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="数据质量状态">{{ tradabilityInfo?.qualityStatusText || tradabilityInfo?.qualityStatus }}</el-descriptions-item>
        <el-descriptions-item label="容量状态">{{ tradabilityInfo?.capacityStatusText || tradabilityInfo?.capacityStatus }}</el-descriptions-item>
        <el-descriptions-item label="Paper资格">
          <el-tag :type="tradabilityInfo?.tradable ? 'success' : 'danger'">{{ tradabilityInfo?.tradable ? '允许新开仓' : '禁止新开仓' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="可交易">
          <el-tag :type="tradabilityInfo?.tradable ? 'success' : 'danger'">{{ tradabilityInfo?.tradable ? '是' : '否' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="原因">{{ tradabilityInfo?.reason }}</el-descriptions-item>
        <el-descriptions-item label="下一步">{{ tradabilityInfo?.nextStep }}</el-descriptions-item>
      </el-descriptions>
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