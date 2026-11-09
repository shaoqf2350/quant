<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  addContractEvent,
  addCorporateAction,
  delContractEvent,
  delCorporateAction,
  listContractEvent,
  listCorporateAction,
  updateContractEvent,
  updateCorporateAction
} from '@/api/quant/event';
import type {
  ContractEventForm,
  ContractEventVO,
  CorporateActionForm,
  CorporateActionVO
} from '@/api/quant/event/types';
import { listInstrument } from '@/api/quant/instrument';
import type { InstrumentVO } from '@/api/quant/instrument/types';
import { useDict } from '@/composables/useDict';

const activeTab = ref('corporate');

// 标的/合约下拉选项（避免手填数字 ID）
const instrumentOptions = ref<InstrumentVO[]>([]);
async function loadInstrumentOptions() {
  try {
    const res = await listInstrument({ pageNum: 1, pageSize: 500 });
    instrumentOptions.value = res.data?.rows ?? [];
  } catch {
    instrumentOptions.value = [];
  }
}

// 枚举下拉统一走后端字典接口（事实源在后端，禁止前端硬编码）
const { options: statusOptions } = useDict('LifecycleEventStatus');
const { options: corporateTypeOptions } = useDict('CorporateActionType');
const { options: contractTypeOptions } = useDict('ContractEventType');

// ---- 公司行为 ----
const caLoading = ref(false);
const caTotal = ref(0);
const caList = ref<CorporateActionVO[]>([]);
const caDialogVisible = ref(false);
const caDialogTitle = ref('');

const caQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  instrumentId: undefined as number | string | undefined,
  actionType: '',
  status: ''
});

const caForm = reactive<CorporateActionForm>({
  actionType: 'DIVIDEND',
  status: 'DISCOVERED'
});

// ---- 合约事件 ----
const ceLoading = ref(false);
const ceTotal = ref(0);
const ceList = ref<ContractEventVO[]>([]);
const ceDialogVisible = ref(false);
const ceDialogTitle = ref('');

const ceQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  instrumentId: undefined as number | string | undefined,
  eventType: '',
  status: ''
});

const ceForm = reactive<ContractEventForm>({
  eventType: 'MARGIN_CHANGE',
  status: 'DISCOVERED'
});

async function getCaList() {
  caLoading.value = true;
  try {
    const res = await listCorporateAction(caQuery);
    caList.value = res.data?.rows ?? [];
    caTotal.value = res.data?.total ?? 0;
  } finally {
    caLoading.value = false;
  }
}

async function getCeList() {
  ceLoading.value = true;
  try {
    const res = await listContractEvent(ceQuery);
    ceList.value = res.data?.rows ?? [];
    ceTotal.value = res.data?.total ?? 0;
  } finally {
    ceLoading.value = false;
  }
}

function caQueryClick() {
  caQuery.pageNum = 1;
  getCaList();
}

function ceQueryClick() {
  ceQuery.pageNum = 1;
  getCeList();
}

function resetCaForm() {
  Object.assign(caForm, {
    pkId: undefined,
    instrumentId: undefined,
    actionType: 'DIVIDEND',
    actionCode: '',
    effectiveDate: '',
    announceDate: '',
    status: 'DISCOVERED',
    detail: '',
    sourceId: undefined
  });
}

function resetCeForm() {
  Object.assign(ceForm, {
    pkId: undefined,
    instrumentId: undefined,
    eventType: 'MARGIN_CHANGE',
    eventCode: '',
    effectiveDate: '',
    status: 'DISCOVERED',
    detail: '',
    sourceId: undefined
  });
}

function handleCaAdd() {
  resetCaForm();
  caDialogTitle.value = '新增公司行为';
  caDialogVisible.value = true;
}

function handleCaEdit(row: Partial<CorporateActionVO>) {
  Object.assign(caForm, {
    pkId: row.pkId,
    instrumentId: row.instrumentId,
    actionType: row.actionType,
    actionCode: row.actionCode,
    effectiveDate: row.effectiveDate,
    announceDate: row.announceDate,
    status: row.status,
    detail: row.detail,
    sourceId: row.sourceId
  });
  caDialogTitle.value = '修改公司行为';
  caDialogVisible.value = true;
}

function handleCaDelete(row: Partial<CorporateActionVO>) {
  ElMessageBox.confirm(`确认删除公司行为「${row.actionCode}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await delCorporateAction(row.pkId);
      ElMessage.success('删除成功');
      getCaList();
    })
    .catch(() => {});
}

async function submitCa() {
  if (caForm.pkId) {
    await updateCorporateAction(caForm);
  } else {
    await addCorporateAction(caForm);
  }
  ElMessage.success(caForm.pkId ? '修改成功' : '新增成功');
  caDialogVisible.value = false;
  getCaList();
}

function handleCeAdd() {
  resetCeForm();
  ceDialogTitle.value = '新增合约事件';
  ceDialogVisible.value = true;
}

function handleCeEdit(row: Partial<ContractEventVO>) {
  Object.assign(ceForm, {
    pkId: row.pkId,
    instrumentId: row.instrumentId,
    eventType: row.eventType,
    eventCode: row.eventCode,
    effectiveDate: row.effectiveDate,
    status: row.status,
    detail: row.detail,
    sourceId: row.sourceId
  });
  ceDialogTitle.value = '修改合约事件';
  ceDialogVisible.value = true;
}

function handleCeDelete(row: Partial<ContractEventVO>) {
  ElMessageBox.confirm(`确认删除合约事件「${row.eventCode}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await delContractEvent(row.pkId);
      ElMessage.success('删除成功');
      getCeList();
    })
    .catch(() => {});
}

async function submitCe() {
  if (ceForm.pkId) {
    await updateContractEvent(ceForm);
  } else {
    await addContractEvent(ceForm);
  }
  ElMessage.success(ceForm.pkId ? '修改成功' : '新增成功');
  ceDialogVisible.value = false;
  getCeList();
}

onMounted(() => {
  loadInstrumentOptions();
  getCaList();
  getCeList();
});
</script>

<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <!-- 公司行为 -->
      <el-tab-pane label="公司行为" name="corporate">
        <el-form :inline="true" :model="caQuery" class="search-form">
          <el-form-item label="标的ID">
            <el-select v-model="caQuery.instrumentId" filterable clearable placeholder="标的" style="width: 230px">
            <el-option v-for="inst in instrumentOptions" :key="inst.id" :label="`${inst.instrumentCode} ${inst.instrumentName || ''}`" :value="inst.id" />
          </el-select>
          </el-form-item>
          <el-form-item label="行为类型">
            <el-select v-model="caQuery.actionType" placeholder="全部" clearable style="width: 150px">
              <el-option v-for="opt in corporateTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="caQuery.status" placeholder="全部" clearable style="width: 140px">
              <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="caQueryClick">查询</el-button>
            <el-button v-hasPermi="['quant:instrument:edit']" type="primary" @click="handleCaAdd">新增</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="caLoading" :data="caList" border>
          <el-table-column prop="actionCode" label="行为编码" min-width="160" />
          <el-table-column prop="instrumentId" label="标的ID" width="140" />
          <el-table-column prop="actionTypeText" label="行为类型" width="110" />
          <el-table-column prop="effectiveDate" label="生效时间" width="170" />
          <el-table-column prop="statusText" label="状态" width="90" />
          <el-table-column prop="detail" label="明细" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="scope">
              <el-button v-hasPermi="['quant:instrument:edit']" link type="primary" @click="handleCaEdit(scope.row)">修改</el-button>
              <el-button v-hasPermi="['quant:instrument:edit']" link type="danger" @click="handleCaDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-show="caTotal > 0"
          v-model:current-page="caQuery.pageNum"
          v-model:page-size="caQuery.pageSize"
          :total="caTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="caQueryClick"
          @current-change="getCaList"
        />
      </el-tab-pane>

      <!-- 合约事件 -->
      <el-tab-pane label="合约事件" name="contract">
        <el-form :inline="true" :model="ceQuery" class="search-form">
          <el-form-item label="合约ID">
            <el-select v-model="ceQuery.instrumentId" filterable clearable placeholder="合约" style="width: 230px">
            <el-option v-for="inst in instrumentOptions" :key="inst.id" :label="`${inst.instrumentCode} ${inst.instrumentName || ''}`" :value="inst.id" />
          </el-select>
          </el-form-item>
          <el-form-item label="事件类型">
            <el-select v-model="ceQuery.eventType" placeholder="全部" clearable style="width: 150px">
              <el-option v-for="opt in contractTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="ceQuery.status" placeholder="全部" clearable style="width: 140px">
              <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="ceQueryClick">查询</el-button>
            <el-button v-hasPermi="['quant:instrument:edit']" type="primary" @click="handleCeAdd">新增</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="ceLoading" :data="ceList" border>
          <el-table-column prop="eventCode" label="事件编码" min-width="160" />
          <el-table-column prop="instrumentId" label="合约ID" width="140" />
          <el-table-column prop="eventTypeText" label="事件类型" width="110" />
          <el-table-column prop="effectiveDate" label="生效时间" width="170" />
          <el-table-column prop="statusText" label="状态" width="90" />
          <el-table-column prop="detail" label="明细" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="scope">
              <el-button v-hasPermi="['quant:instrument:edit']" link type="primary" @click="handleCeEdit(scope.row)">修改</el-button>
              <el-button v-hasPermi="['quant:instrument:edit']" link type="danger" @click="handleCeDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-show="ceTotal > 0"
          v-model:current-page="ceQuery.pageNum"
          v-model:page-size="ceQuery.pageSize"
          :total="ceTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="ceQueryClick"
          @current-change="getCeList"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 公司行为对话框 -->
    <el-dialog v-model="caDialogVisible" :title="caDialogTitle" width="560px" append-to-body>
      <el-form :model="caForm" label-width="100px">
        <el-form-item label="标的" required>
          <el-select v-model="caForm.instrumentId" filterable clearable placeholder="请选择标的" style="width: 100%">
            <el-option v-for="inst in instrumentOptions" :key="inst.id" :label="`${inst.instrumentCode} ${inst.instrumentName || ''}`" :value="inst.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="行为类型" required>
          <el-select v-model="caForm.actionType" style="width: 100%">
            <el-option v-for="opt in corporateTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker v-model="caForm.effectiveDate" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择生效时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="公告时间">
          <el-date-picker v-model="caForm.announceDate" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择公告时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="caForm.status" style="width: 100%">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="明细">
          <el-input v-model="caForm.detail" type="textarea" placeholder='明细（JSON）示例：{"rate":0.5,"exDate":"2026-05-01"}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="caDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCa">确定</el-button>
      </template>
    </el-dialog>

    <!-- 合约事件对话框 -->
    <el-dialog v-model="ceDialogVisible" :title="ceDialogTitle" width="560px" append-to-body>
      <el-form :model="ceForm" label-width="100px">
        <el-form-item label="合约" required>
          <el-select v-model="ceForm.instrumentId" filterable clearable placeholder="请选择合约" style="width: 100%">
            <el-option v-for="inst in instrumentOptions" :key="inst.id" :label="`${inst.instrumentCode} ${inst.instrumentName || ''}`" :value="inst.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件类型" required>
          <el-select v-model="ceForm.eventType" style="width: 100%">
            <el-option v-for="opt in contractTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker v-model="ceForm.effectiveDate" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择生效时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="ceForm.status" style="width: 100%">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="明细">
          <el-input v-model="ceForm.detail" type="textarea" placeholder='明细（JSON）示例：{"newMarginRate":0.12,"effectiveDate":"2026-05-01"}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCe">确定</el-button>
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