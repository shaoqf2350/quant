<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  createResearchTask,
  listResearchDraft,
  listResearchEvent,
  listResearchEvidence,
  listResearchMaterial,
  listResearchTask,
  submitResearchTask
} from '@/api/quant/research';
import type {
  AiResearchDraftVO,
  EventEvidenceVO,
  ResearchEventVO,
  ResearchMaterialQuery,
  ResearchMaterialVO,
  ResearchTaskForm,
  ResearchTaskVO
} from '@/api/quant/research/types';
import { listInstrument } from '@/api/quant/instrument';
import type { InstrumentVO } from '@/api/quant/instrument/types';
import { useDict } from '@/composables/useDict';

const activeTab = ref('task');

// 枚举下拉统一走后端字典接口（事实源在后端，禁止前端硬编码）
const { options: taskTypeOptions, labelOf: taskTypeLabel } = useDict('ResearchTaskType');
const { options: taskStatusOptions } = useDict('ResearchTaskStatus');
const { options: eventStatusOptions } = useDict('ResearchEventStatus');
const { options: subjectTypeOptions } = useDict('SubjectType');

// ---- 研究任务 ----
const taskLoading = ref(false);
const taskTotal = ref(0);
const taskList = ref<ResearchTaskVO[]>([]);
const taskQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  status: '',
  taskType: ''
});

const taskDialogVisible = ref(false);
const taskForm = reactive<ResearchTaskForm>({
  taskType: 'NEWS_ANALYSIS'
});

const draftDialogVisible = ref(false);
const draftList = ref<AiResearchDraftVO[]>([]);

// ---- 研究事件 ----
const eventLoading = ref(false);
const eventTotal = ref(0);
const eventList = ref<ResearchEventVO[]>([]);
const eventQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  status: '',
  subjectType: ''
});

const evidenceDialogVisible = ref(false);
const evidenceList = ref<EventEvidenceVO[]>([]);

// ---- 材料清单多选 ----
const materialDialogVisible = ref(false);
const materialLoading = ref(false);
const materialTotal = ref(0);
const materialList = ref<ResearchMaterialVO[]>([]);
const materialQuery = reactive<ResearchMaterialQuery>({ pageNum: 1, pageSize: 10, keyword: '' });
const materialSelection = ref<ResearchMaterialVO[]>([]);
const selectedMaterials = ref<ResearchMaterialVO[]>([]);
const materialTableRef = ref<ElTableInstance>();

// ---- 标的多选 ----
const subjectDialogVisible = ref(false);
const subjectLoading = ref(false);
const subjectTotal = ref(0);
const subjectList = ref<InstrumentVO[]>([]);
const subjectQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '' });
const subjectSelection = ref<InstrumentVO[]>([]);
const selectedSubjects = ref<InstrumentVO[]>([]);
const subjectTableRef = ref<ElTableInstance>();

async function getTaskList() {
  taskLoading.value = true;
  try {
    const res = await listResearchTask(taskQuery);
    taskList.value = res.data?.rows ?? [];
    taskTotal.value = res.data?.total ?? 0;
  } finally {
    taskLoading.value = false;
  }
}

async function getEventList() {
  eventLoading.value = true;
  try {
    const res = await listResearchEvent(eventQuery);
    eventList.value = res.data?.rows ?? [];
    eventTotal.value = res.data?.total ?? 0;
  } finally {
    eventLoading.value = false;
  }
}

function taskQueryClick() {
  taskQuery.pageNum = 1;
  getTaskList();
}

function taskResetClick() {
  taskQuery.status = '';
  taskQuery.taskType = '';
  taskQueryClick();
}

function eventQueryClick() {
  eventQuery.pageNum = 1;
  getEventList();
}

function eventResetClick() {
  eventQuery.status = '';
  eventQuery.subjectType = '';
  eventQueryClick();
}

function resetTaskForm() {
  Object.assign(taskForm, {
    taskType: 'NEWS_ANALYSIS',
    materialIds: '',
    subjectRefs: '',
    snapshotId: undefined,
    workflowVersion: '',
    promptVersion: '',
    idempotencyKey: ''
  });
  selectedMaterials.value = [];
  selectedSubjects.value = [];
  materialSelection.value = [];
  subjectSelection.value = [];
}

function handleTaskAdd() {
  resetTaskForm();
  taskDialogVisible.value = true;
  // 清除上次任务在多选表格里残留的勾选（表格首次渲染后 ref 才可用）
  nextTick(() => {
    materialTableRef.value?.clearSelection();
    subjectTableRef.value?.clearSelection();
  });
}

async function submitTaskForm() {
  await createResearchTask(taskForm);
  ElMessage.success('创建成功');
  taskDialogVisible.value = false;
  getTaskList();
}

// ---- 材料清单多选 ----
async function getMaterialList() {
  materialLoading.value = true;
  try {
    const res = await listResearchMaterial(materialQuery);
    materialList.value = res.data?.rows ?? [];
    materialTotal.value = res.data?.total ?? 0;
  } finally {
    materialLoading.value = false;
  }
}

function materialQueryClick() {
  materialQuery.pageNum = 1;
  getMaterialList();
}

function openMaterialDialog() {
  materialSelection.value = [...selectedMaterials.value];
  materialDialogVisible.value = true;
  getMaterialList();
}

function onMaterialSelectionChange(rows: ResearchMaterialVO[]) {
  materialSelection.value = rows;
}

function confirmMaterialSelect() {
  selectedMaterials.value = [...materialSelection.value];
  taskForm.materialIds = selectedMaterials.value.map(m => m.pkId).join(',');
  materialDialogVisible.value = false;
}

function removeMaterial(pkId: number | string) {
  selectedMaterials.value = selectedMaterials.value.filter(m => m.pkId !== pkId);
  taskForm.materialIds = selectedMaterials.value.map(m => m.pkId).join(',');
}

// ---- 标的多选 ----
async function getSubjectList() {
  subjectLoading.value = true;
  try {
    const res = await listInstrument(subjectQuery);
    subjectList.value = res.data?.rows ?? [];
    subjectTotal.value = res.data?.total ?? 0;
  } finally {
    subjectLoading.value = false;
  }
}

function subjectQueryClick() {
  subjectQuery.pageNum = 1;
  getSubjectList();
}

function openSubjectDialog() {
  subjectSelection.value = [...selectedSubjects.value];
  subjectDialogVisible.value = true;
  getSubjectList();
}

function onSubjectSelectionChange(rows: InstrumentVO[]) {
  subjectSelection.value = rows;
}

function confirmSubjectSelect() {
  selectedSubjects.value = [...subjectSelection.value];
  taskForm.subjectRefs = selectedSubjects.value.map(s => s.id).join(',');
  subjectDialogVisible.value = false;
}

function removeSubject(id: number | string) {
  selectedSubjects.value = selectedSubjects.value.filter(s => s.id !== id);
  taskForm.subjectRefs = selectedSubjects.value.map(s => s.id).join(',');
}

async function handleSubmit(row: Partial<ResearchTaskVO>) {
  await submitResearchTask(row.pkId);
  ElMessage.success('已提交分析');
  getTaskList();
  getEventList();
}

async function handleDraft(row: Partial<ResearchTaskVO>) {
  draftList.value = (await listResearchDraft(row.pkId)).data ?? [];
  draftDialogVisible.value = true;
}

async function handleEvidence(row: Partial<ResearchEventVO>) {
  evidenceList.value = (await listResearchEvidence(row.pkId)).data ?? [];
  evidenceDialogVisible.value = true;
}

onMounted(() => {
  getTaskList();
  getEventList();
});
</script>

<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <!-- 研究任务 -->
      <el-tab-pane label="研究任务" name="task">
        <el-form :inline="true" :model="taskQuery" class="search-form">
          <el-form-item label="状态">
            <el-select v-model="taskQuery.status" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="opt in taskStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务类型">
            <el-select v-model="taskQuery.taskType" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="opt in taskTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="taskQueryClick">查询</el-button>
            <el-button @click="taskResetClick">重置</el-button>
            <el-button v-hasPermi="['quant:news:reanalyze']" type="primary" @click="handleTaskAdd">新增任务</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="taskLoading" :data="taskList" border>
          <el-table-column prop="taskCode" label="任务编码" min-width="160" />
          <el-table-column label="任务类型" width="100">
            <template #default="scope">{{ taskTypeLabel(scope.row.taskType) }}</template>
          </el-table-column>
          <el-table-column prop="statusText" label="状态" width="90" />
          <el-table-column prop="materialIds" label="材料ID" min-width="140" show-overflow-tooltip />
          <el-table-column prop="runId" label="运行ID" min-width="140" show-overflow-tooltip />
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="scope">
              <el-button v-hasPermi="['quant:news:reanalyze']" link type="primary" @click="handleSubmit(scope.row)">提交分析</el-button>
              <el-button link type="primary" @click="handleDraft(scope.row)">草稿</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-show="taskTotal > 0"
          v-model:current-page="taskQuery.pageNum"
          v-model:page-size="taskQuery.pageSize"
          :total="taskTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="taskQueryClick"
          @current-change="getTaskList"
        />
      </el-tab-pane>

      <!-- 研究事件 -->
      <el-tab-pane label="研究事件" name="event">
        <el-form :inline="true" :model="eventQuery" class="search-form">
          <el-form-item label="状态">
            <el-select v-model="eventQuery.status" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="opt in eventStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="主体类型">
            <el-select v-model="eventQuery.subjectType" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="opt in subjectTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="eventQueryClick">查询</el-button>
            <el-button @click="eventResetClick">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="eventLoading" :data="eventList" border>
          <el-table-column prop="eventCode" label="事件编码" min-width="160" />
          <el-table-column prop="eventType" label="事件类型" width="120" />
          <el-table-column prop="impactScope" label="影响层级" width="100" />
          <el-table-column prop="impactLevel" label="重大程度" width="100" />
          <el-table-column prop="directionText" label="方向" width="90" />
          <el-table-column prop="statusText" label="状态" width="90" />
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleEvidence(scope.row)">证据</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-show="eventTotal > 0"
          v-model:current-page="eventQuery.pageNum"
          v-model:page-size="eventQuery.pageSize"
          :total="eventTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="eventQueryClick"
          @current-change="getEventList"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 新增任务对话框 -->
    <el-dialog v-model="taskDialogVisible" title="新增研究任务" width="560px" append-to-body>
      <el-form :model="taskForm" label-width="100px">
        <el-form-item label="任务类型" required>
          <el-select v-model="taskForm.taskType" style="width: 100%">
            <el-option v-for="opt in taskTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="材料">
          <div style="width: 100%">
            <el-button size="small" @click="openMaterialDialog">选择材料</el-button>
            <el-tag
              v-for="m in selectedMaterials"
              :key="m.pkId"
              closable
              style="margin: 2px 4px 2px 0"
              @close="removeMaterial(m.pkId)"
            >
              {{ m.snapshotId || m.pkId }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="标的引用">
          <div style="width: 100%">
            <el-button size="small" @click="openSubjectDialog">选择标的</el-button>
            <el-tag
              v-for="s in selectedSubjects"
              :key="s.id"
              closable
              style="margin: 2px 4px 2px 0"
              @close="removeSubject(s.id)"
            >
              {{ s.instrumentCode }} {{ s.instrumentName }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="Workflow版本">
          <el-input v-model="taskForm.workflowVersion" placeholder="Dify Workflow 版本" />
        </el-form-item>
        <el-form-item label="Prompt版本">
          <el-input v-model="taskForm.promptVersion" placeholder="Prompt 版本" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTaskForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 草稿对话框 -->
    <el-dialog v-model="draftDialogVisible" title="AI 研究草稿" width="760px" append-to-body>
      <el-table :data="draftList" border>
        <el-table-column prop="runId" label="运行ID" min-width="140" />
        <el-table-column prop="modelVersion" label="模型版本" width="120" />
        <el-table-column prop="summary" label="摘要" min-width="220" show-overflow-tooltip />
        <el-table-column prop="directionCandidate" label="方向候选" width="100" />
        <el-table-column prop="validationStatusText" label="校验状态" width="100" />
      </el-table>
    </el-dialog>

    <!-- 证据对话框 -->
    <el-dialog v-model="evidenceDialogVisible" title="事件证据链" width="760px" append-to-body>
      <el-table :data="evidenceList" border>
        <el-table-column prop="evidenceTypeText" label="证据类型" width="100" />
        <el-table-column prop="sourceRef" label="来源引用" min-width="180" show-overflow-tooltip />
        <el-table-column prop="publishedAt" label="发布时间" width="170" />
        <el-table-column prop="quoteText" label="原文片段" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <!-- 材料清单多选对话框 -->
    <el-dialog v-model="materialDialogVisible" title="选择材料" width="860px" append-to-body>
      <el-form :inline="true" :model="materialQuery" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="materialQuery.keyword" placeholder="快照ID / 外部ID" clearable style="width: 220px" @keyup.enter="materialQueryClick" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="materialQueryClick">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table
        ref="materialTableRef"
        v-loading="materialLoading"
        :data="materialList"
        border
        row-key="pkId"
        @selection-change="onMaterialSelectionChange"
      >
        <el-table-column type="selection" width="50" reserve-selection />
        <el-table-column prop="snapshotId" label="快照ID" min-width="150" />
        <el-table-column prop="externalId" label="外部ID" min-width="140" show-overflow-tooltip />
        <el-table-column prop="sourceName" label="数据源" min-width="130" />
        <el-table-column prop="publishedAt" label="来源发布时间" width="170" />
        <el-table-column prop="authorizationStatus" label="授权状态" width="100" />
      </el-table>
      <el-pagination
        v-show="materialTotal > 0"
        v-model:current-page="materialQuery.pageNum"
        v-model:page-size="materialQuery.pageSize"
        :total="materialTotal"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="materialQueryClick"
        @current-change="getMaterialList"
      />
      <template #footer>
        <el-button @click="materialDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmMaterialSelect">确定</el-button>
      </template>
    </el-dialog>

    <!-- 标的多选对话框 -->
    <el-dialog v-model="subjectDialogVisible" title="选择标的" width="860px" append-to-body>
      <el-form :inline="true" :model="subjectQuery" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="subjectQuery.keyword" placeholder="代码 / 名称" clearable style="width: 220px" @keyup.enter="subjectQueryClick" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="subjectQueryClick">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table
        ref="subjectTableRef"
        v-loading="subjectLoading"
        :data="subjectList"
        border
        row-key="id"
        @selection-change="onSubjectSelectionChange"
      >
        <el-table-column type="selection" width="50" reserve-selection />
        <el-table-column prop="instrumentCode" label="代码" width="140" />
        <el-table-column prop="instrumentName" label="名称" min-width="150" />
        <el-table-column prop="productType" label="产品类型" width="120" />
        <el-table-column prop="exchangeCode" label="交易所" width="110" />
        <el-table-column prop="statusText" label="状态" width="90" />
      </el-table>
      <el-pagination
        v-show="subjectTotal > 0"
        v-model:current-page="subjectQuery.pageNum"
        v-model:page-size="subjectQuery.pageSize"
        :total="subjectTotal"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="subjectQueryClick"
        @current-change="getSubjectList"
      />
      <template #footer>
        <el-button @click="subjectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSubjectSelect">确定</el-button>
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