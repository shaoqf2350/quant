<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  addCalendarDef,
  delCalendarDef,
  importCalendar,
  listCalendarConflict,
  listCalendarDay,
  listCalendarDef,
  listReadiness,
  overrideCalendarConflict,
  updateCalendarDef
} from '@/api/quant/calendar';
import type {
  CalendarConflictVO,
  CalendarDayVO,
  CalendarDefForm,
  CalendarDefVO,
  ReadinessVO
} from '@/api/quant/calendar/types';
import { useDict } from '@/composables/useDict';

const activeTab = ref('def');

// 枚举/目录下拉统一走后端字典接口（事实源在后端，禁止前端硬编码）
const { options: marketTypeOptions, labelOf: marketTypeLabel } = useDict('MarketType');
const { options: calendarLevelOptions } = useDict('CalendarLevel');
const { options: calendarCodeOptions } = useDict('CalendarCodeCatalog');
const { options: conflictStatusOptions } = useDict('CalendarConflictStatus');
const { options: regionCodeOptions } = useDict('RegionCode');
const { options: venueCodeOptions } = useDict('VenueCode');
const { options: timezoneOptions } = useDict('MarketTimezone');
const { options: currencyOptions } = useDict('CurrencyCode');

// 日历下拉选项（避免手填数字 ID）
const calendarDefOptions = ref<CalendarDefVO[]>([]);
async function loadCalendarDefOptions() {
  try {
    const res = await listCalendarDef({ pageNum: 1, pageSize: 500 } as any);
    calendarDefOptions.value = res.data?.rows ?? [];
  } catch {
    calendarDefOptions.value = [];
  }
}

// ---- 日历定义 ----
const defLoading = ref(false);
const defTotal = ref(0);
const defList = ref<CalendarDefVO[]>([]);
const defDialogVisible = ref(false);
const defDialogTitle = ref('');

const defQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  calendarCode: '',
  calendarName: '',
  marketType: '',
  status: ''
});

const defForm = reactive<CalendarDefForm>({
  calendarCode: '',
  status: 'ACTIVE'
});

// ---- 交易日 ----
const dayLoading = ref(false);
const dayTotal = ref(0);
const dayList = ref<CalendarDayVO[]>([]);
const dayQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  calendarId: undefined as number | string | undefined,
  startDate: '',
  endDate: ''
});

// ---- 日线就绪 ----
const readinessLoading = ref(false);
const readinessTotal = ref(0);
const readinessList = ref<ReadinessVO[]>([]);
const readinessQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  calendarId: undefined as number | string | undefined,
  blockReason: ''
});

// ---- 日历冲突 ----
const conflictLoading = ref(false);
const conflictTotal = ref(0);
const conflictList = ref<CalendarConflictVO[]>([]);
const conflictQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  calendarId: undefined as number | string | undefined,
  status: ''
});

async function getDefList() {
  defLoading.value = true;
  try {
    const res = await listCalendarDef(defQuery);
    defList.value = res.data?.rows ?? [];
    defTotal.value = res.data?.total ?? 0;
  } finally {
    defLoading.value = false;
  }
}

async function getDayList() {
  dayLoading.value = true;
  try {
    const res = await listCalendarDay(dayQuery);
    dayList.value = res.data?.rows ?? [];
    dayTotal.value = res.data?.total ?? 0;
  } finally {
    dayLoading.value = false;
  }
}

async function getReadinessList() {
  readinessLoading.value = true;
  try {
    const res = await listReadiness(readinessQuery);
    readinessList.value = res.data?.rows ?? [];
    readinessTotal.value = res.data?.total ?? 0;
  } finally {
    readinessLoading.value = false;
  }
}

async function getConflictList() {
  conflictLoading.value = true;
  try {
    const res = await listCalendarConflict(conflictQuery);
    conflictList.value = res.data?.rows ?? [];
    conflictTotal.value = res.data?.total ?? 0;
  } finally {
    conflictLoading.value = false;
  }
}

function defQueryClick() {
  defQuery.pageNum = 1;
  getDefList();
}

function defResetClick() {
  defQuery.calendarCode = '';
  defQuery.calendarName = '';
  defQuery.marketType = '';
  defQuery.status = '';
  defQueryClick();
}

function resetDefForm() {
  Object.assign(defForm, {
    pkId: undefined,
    calendarCode: '',
    calendarName: '',
    marketType: 'STOCK',
    regionCode: 'CN',
    venueCode: '',
    timezone: 'Asia/Shanghai',
    currency: 'CNY',
    calendarLevel: 'MARKET',
    sourcePolicy: '',
    effectiveFrom: '',
    effectiveTo: '',
    status: 'ACTIVE',
    remark: ''
  });
}

function handleDefAdd() {
  resetDefForm();
  defDialogTitle.value = '新增日历定义';
  defDialogVisible.value = true;
}

function handleDefEdit(row: Partial<CalendarDefVO>) {
  Object.assign(defForm, {
    pkId: row.pkId,
    calendarCode: row.calendarCode,
    calendarName: row.calendarName,
    marketType: row.marketType,
    regionCode: row.regionCode,
    venueCode: row.venueCode,
    timezone: row.timezone,
    currency: row.currency,
    calendarLevel: row.calendarLevel,
    sourcePolicy: row.sourcePolicy,
    effectiveFrom: row.effectiveFrom,
    effectiveTo: row.effectiveTo,
    status: row.status,
    remark: row.remark
  });
  defDialogTitle.value = '修改日历定义';
  defDialogVisible.value = true;
}

function handleDefDelete(row: Partial<CalendarDefVO>) {
  ElMessageBox.confirm(`确认删除日历「${row.calendarName || row.calendarCode}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await delCalendarDef(row.pkId);
      ElMessage.success('删除成功');
      getDefList();
    })
    .catch(() => {});
}

async function submitDef() {
  if (defForm.pkId) {
    await updateCalendarDef(defForm);
  } else {
    await addCalendarDef(defForm);
  }
  ElMessage.success(defForm.pkId ? '修改成功' : '新增成功');
  defDialogVisible.value = false;
  getDefList();
}

function dayQueryClick() {
  dayQuery.pageNum = 1;
  getDayList();
}

// ---- 日历导入（§12.2 施工期 Mock 适配）----
const importDialogVisible = ref(false);
const importing = ref(false);
const importTarget = ref<Partial<CalendarDefVO>>();
const importForm = reactive({
  endpoint: 'trade_cal',
  startDate: '',
  endDate: ''
});

// 日历导入来源接口（施工期 Mock 适配，后端 CalendarEndpoint 目录）
const { options: endpointOptions } = useDict('CalendarEndpoint');

function handleImport(row: Partial<CalendarDefVO>) {
  importTarget.value = row;
  const today = new Date();
  const year = today.getFullYear();
  importForm.endpoint = row.marketType === 'FUTURE' ? 'fut_trade_cal' : 'trade_cal';
  importForm.startDate = `${year}-01-01`;
  importForm.endDate = `${year}-12-31`;
  importDialogVisible.value = true;
}

async function submitImport() {
  if (!importTarget.value) {
    return;
  }
  if (!importForm.startDate || !importForm.endDate) {
    ElMessage.warning('请选择导入日期区间');
    return;
  }
  importing.value = true;
  try {
    const res = await importCalendar(
      importTarget.value.pkId,
      importForm.endpoint,
      importForm.startDate,
      importForm.endDate
    );
    const data = res.data as { imported?: number; conflicts?: number; skipped?: number } | undefined;
    ElMessage.success(
      `导入完成：新增 ${data?.imported ?? 0} 个交易日，幂等跳过 ${data?.skipped ?? 0}，冲突 ${data?.conflicts ?? 0}（冲突记录待人工覆盖）`
    );
    importDialogVisible.value = false;
    activeTab.value = 'day';
    dayQuery.calendarId = importTarget.value.pkId;
    dayQueryClick();
    getConflictList();
  } finally {
    importing.value = false;
  }
}

function dayResetClick() {
  dayQuery.calendarId = undefined;
  dayQuery.startDate = '';
  dayQuery.endDate = '';
  dayQueryClick();
}

function readinessQueryClick() {
  readinessQuery.pageNum = 1;
  getReadinessList();
}

function readinessResetClick() {
  readinessQuery.calendarId = undefined;
  readinessQuery.blockReason = '';
  readinessQueryClick();
}

function conflictQueryClick() {
  conflictQuery.pageNum = 1;
  getConflictList();
}

function conflictResetClick() {
  conflictQuery.calendarId = undefined;
  conflictQuery.status = '';
  conflictQueryClick();
}

function handleConflictOverride(row: Partial<CalendarConflictVO>) {
  ElMessageBox.prompt('请输入人工覆盖结论（处理人/依据/生效版本）', '覆盖日历冲突', { type: 'warning' })
    .then(async ({ value }) => {
      await overrideCalendarConflict(row.pkId, value);
      ElMessage.success('覆盖成功');
      getConflictList();
    })
    .catch(() => {});
}

onMounted(() => {
  loadCalendarDefOptions();
  getDefList();
  getDayList();
  getReadinessList();
  getConflictList();
});
</script>

<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <!-- 日历定义 -->
      <el-tab-pane label="日历定义" name="def">
        <el-form :inline="true" :model="defQuery" class="search-form">
          <el-form-item label="日历编码">
            <el-input v-model="defQuery.calendarCode" placeholder="日历编码" clearable @keyup.enter="defQueryClick" />
          </el-form-item>
          <el-form-item label="日历名称">
            <el-input v-model="defQuery.calendarName" placeholder="日历名称" clearable @keyup.enter="defQueryClick" />
          </el-form-item>
          <el-form-item label="市场类型">
            <el-select v-model="defQuery.marketType" placeholder="全部" clearable style="width: 140px">
              <el-option v-for="opt in marketTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="defQueryClick">查询</el-button>
            <el-button @click="defResetClick">重置</el-button>
            <el-button v-hasPermi="['quant:calendar:import']" type="primary" @click="handleDefAdd">新增</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="defLoading" :data="defList" border>
          <el-table-column prop="calendarCode" label="日历编码" min-width="180" />
          <el-table-column prop="calendarName" label="日历名称" min-width="140" />
          <el-table-column label="市场类型" width="90">
            <template #default="scope">{{ marketTypeLabel(scope.row.marketType) }}</template>
          </el-table-column>
          <el-table-column prop="regionCode" label="地区" width="70" />
          <el-table-column prop="venueCode" label="交易场所" width="100" />
          <el-table-column prop="timezone" label="时区" width="120" />
          <el-table-column prop="currency" label="币种" width="80" />
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="scope">
              <el-button v-hasPermi="['quant:calendar:import']" link type="primary" @click="handleDefEdit(scope.row)">修改</el-button>
              <el-button v-hasPermi="['quant:calendar:import']" link type="danger" @click="handleDefDelete(scope.row)">删除</el-button>
              <el-button v-hasPermi="['quant:calendar:import']" link type="warning" @click="handleImport(scope.row)">导入</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-show="defTotal > 0"
          v-model:current-page="defQuery.pageNum"
          v-model:page-size="defQuery.pageSize"
          :total="defTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="defQueryClick"
          @current-change="getDefList"
        />
      </el-tab-pane>

      <!-- 交易日 -->
      <el-tab-pane label="交易日" name="day">
        <el-form :inline="true" :model="dayQuery" class="search-form">
          <el-form-item label="日历ID">
            <el-select v-model="dayQuery.calendarId" filterable clearable placeholder="日历" style="width: 220px">
              <el-option v-for="cd in calendarDefOptions" :key="cd.pkId" :label="`${cd.calendarCode} ${cd.calendarName || ''}`" :value="cd.pkId" />
            </el-select>
          </el-form-item>
          <el-form-item label="开始日期">
            <el-date-picker v-model="dayQuery.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width: 160px" />
          </el-form-item>
          <el-form-item label="结束日期">
            <el-date-picker v-model="dayQuery.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" style="width: 160px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="dayQueryClick">查询</el-button>
            <el-button @click="dayResetClick">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="dayLoading" :data="dayList" border>
          <el-table-column prop="marketLocalDate" label="市场本地日期" width="120" />
          <el-table-column label="开市" width="80" align="center">
            <template #default="scope">{{ scope.row.isOpen ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column prop="dayStatusText" label="日状态" width="100" />
          <el-table-column prop="previousTradeDate" label="上一交易日" width="120" />
          <el-table-column prop="nextTradeDate" label="下一交易日" width="120" />
          <el-table-column prop="holidayCode" label="节假日编码" width="110" />
          <el-table-column prop="specialReason" label="特殊原因" min-width="140" show-overflow-tooltip />
          <el-table-column prop="qualityStatusText" label="质量状态" width="90" />
        </el-table>

        <el-pagination
          v-show="dayTotal > 0"
          v-model:current-page="dayQuery.pageNum"
          v-model:page-size="dayQuery.pageSize"
          :total="dayTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="dayQueryClick"
          @current-change="getDayList"
        />
      </el-tab-pane>

      <!-- 日线就绪 -->
      <el-tab-pane label="日线就绪" name="readiness">
        <el-form :inline="true" :model="readinessQuery" class="search-form">
          <el-form-item label="日历ID">
            <el-select v-model="readinessQuery.calendarId" filterable clearable placeholder="日历" style="width: 220px">
              <el-option v-for="cd in calendarDefOptions" :key="cd.pkId" :label="`${cd.calendarCode} ${cd.calendarName || ''}`" :value="cd.pkId" />
            </el-select>
          </el-form-item>
          <el-form-item label="阻断原因">
            <el-input v-model="readinessQuery.blockReason" placeholder="阻断原因" clearable style="width: 180px" @keyup.enter="readinessQueryClick" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="readinessQueryClick">查询</el-button>
            <el-button @click="readinessResetClick">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="readinessLoading" :data="readinessList" border>
          <el-table-column prop="marketLocalDate" label="市场本地日期" width="120" />
          <el-table-column label="应到" width="70" align="center">
            <template #default="scope">{{ scope.row.expectedFlag ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="已到" width="70" align="center">
            <template #default="scope">{{ scope.row.receivedFlag ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="校验通过" width="90" align="center">
            <template #default="scope">{{ scope.row.validatedFlag ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="策略可用" width="90" align="center">
            <template #default="scope">{{ scope.row.strategyReadyFlag ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="Paper可用" width="90" align="center">
            <template #default="scope">{{ scope.row.paperReadyFlag ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column prop="blockReason" label="阻断原因" min-width="160" show-overflow-tooltip />
        </el-table>

        <el-pagination
          v-show="readinessTotal > 0"
          v-model:current-page="readinessQuery.pageNum"
          v-model:page-size="readinessQuery.pageSize"
          :total="readinessTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="readinessQueryClick"
          @current-change="getReadinessList"
        />
      </el-tab-pane>

      <!-- 日历冲突 -->
      <el-tab-pane label="日历冲突" name="conflict">
        <el-form :inline="true" :model="conflictQuery" class="search-form">
          <el-form-item label="日历ID">
            <el-select v-model="conflictQuery.calendarId" filterable clearable placeholder="日历" style="width: 220px">
              <el-option v-for="cd in calendarDefOptions" :key="cd.pkId" :label="`${cd.calendarCode} ${cd.calendarName || ''}`" :value="cd.pkId" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="conflictQuery.status" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="opt in conflictStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="conflictQueryClick">查询</el-button>
            <el-button @click="conflictResetClick">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="conflictLoading" :data="conflictList" border>
          <el-table-column prop="conflictCode" label="冲突编码" min-width="160" />
          <el-table-column prop="calendarId" label="日历ID" width="160" />
          <el-table-column prop="marketLocalDate" label="涉及日期" width="120" />
          <el-table-column prop="conflictType" label="冲突类型" width="130" />
          <el-table-column prop="statusText" label="状态" width="90" />
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column prop="overrideDecision" label="覆盖结论" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="scope">
              <el-button v-hasPermi="['quant:calendar:import']" link type="primary" @click="handleConflictOverride(scope.row)">覆盖</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-show="conflictTotal > 0"
          v-model:current-page="conflictQuery.pageNum"
          v-model:page-size="conflictQuery.pageSize"
          :total="conflictTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="conflictQueryClick"
          @current-change="getConflictList"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 日历定义表单弹窗（§12.3.1 日历定义 CRUD） -->
    <el-dialog v-model="defDialogVisible" :title="defDialogTitle" width="640px" append-to-body>
      <el-form :model="defForm" label-width="100px">
        <el-form-item label="日历编码" required>
          <el-select v-model="defForm.calendarCode" filterable :disabled="!!defForm.pkId" placeholder="选择日历编码" style="width: 100%">
            <el-option v-for="opt in calendarCodeOptions" :key="opt.value" :label="`${opt.value}（${opt.label}）`" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="日历名称">
          <el-input v-model="defForm.calendarName" placeholder="请输入日历名称" />
        </el-form-item>
        <el-form-item label="市场类型" required>
          <el-select v-model="defForm.marketType" style="width: 100%">
            <el-option v-for="opt in marketTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="日历级别">
          <el-select v-model="defForm.calendarLevel" style="width: 100%">
            <el-option v-for="opt in calendarLevelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="地区">
          <el-select v-model="defForm.regionCode" clearable placeholder="选择地区" style="width: 100%">
            <el-option v-for="opt in regionCodeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易场所">
          <el-select v-model="defForm.venueCode" clearable placeholder="选择交易场所" style="width: 100%">
            <el-option v-for="opt in venueCodeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="市场时区">
          <el-select v-model="defForm.timezone" clearable placeholder="选择时区" style="width: 100%">
            <el-option v-for="opt in timezoneOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="币种">
          <el-select v-model="defForm.currency" clearable placeholder="选择币种" style="width: 100%">
            <el-option v-for="opt in currencyOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="defForm.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="defDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDef">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" :title="`导入交易日历 - ${importTarget?.calendarName || ''}`" width="520px" append-to-body>
      <el-form :model="importForm" label-width="110px">
        <el-form-item label="日历编码">
          <el-input :model-value="importTarget?.calendarCode" disabled />
        </el-form-item>
        <el-form-item label="来源接口" required>
          <el-select v-model="importForm.endpoint" style="width: 100%">
            <el-option v-for="opt in endpointOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker v-model="importForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker v-model="importForm.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="施工期说明"
          description="当前为 Mock 适配（Tushare 未授权）；按周末休市/工作日开市生成交易日并保留 is_open 原始值，与既有记录冲突时写入日历冲突待人工覆盖，不静默择一。"
        />
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">导入</el-button>
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