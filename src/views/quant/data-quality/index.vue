<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { listCollection, listPointInTime, listQuality, listSnapshot, resolveQuality } from '@/api/quant/data/governance';
import type {
  CollectionVO,
  PointInTimeVO,
  QualityResolveForm,
  QualityVO,
  SnapshotVO
} from '@/api/quant/data/governance/types';
import { listDataSource, listSourceGroup } from '@/api/quant/data/source';
import type { DataSourceVO, SourceGroupVO } from '@/api/quant/data/source/types';
import { useDict } from '@/composables/useDict';

const activeTab = ref('quality');

// 枚举下拉统一走后端字典接口（事实源在后端，禁止前端硬编码）
const { options: issueStatusOptions } = useDict('DataQualityIssueStatus');
const { options: qualityStatusOptions } = useDict('DataQualityStatus');
const { options: collectionStatusOptions } = useDict('CollectionTaskStatus');
const { options: collectionTaskTypeOptions } = useDict('CollectionTaskType');
const { options: subjectTypeOptions } = useDict('SubjectType');

// 数据源/来源组下拉（原始快照筛选，避免手填数字 ID）
const dataSourceOptions = ref<DataSourceVO[]>([]);
const sourceGroupOptions = ref<SourceGroupVO[]>([]);
async function loadSourceOptions() {
  try {
    const res = await listDataSource({ pageNum: 1, pageSize: 500 } as any);
    dataSourceOptions.value = res.data?.rows ?? [];
  } catch {
    dataSourceOptions.value = [];
  }
}
async function loadSourceGroupOptions() {
  try {
    const res = await listSourceGroup({ pageNum: 1, pageSize: 500 } as any);
    sourceGroupOptions.value = res.data?.rows ?? [];
  } catch {
    sourceGroupOptions.value = [];
  }
}

// ---- 质量问题 ----
const loading = ref(false);
const total = ref(0);
const qualityList = ref<QualityVO[]>([]);
const resolveVisible = ref(false);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  issueStatus: '',
  qualityStatus: ''
});

const resolveForm = reactive<QualityResolveForm>({
  handler: '',
  actionTaken: '',
  basisRef: '',
  fixVersion: ''
});

const currentIssue = ref<Partial<QualityVO>>();

// ---- 原始快照 ----
const snapshotLoading = ref(false);
const snapshotTotal = ref(0);
const snapshotList = ref<SnapshotVO[]>([]);
const snapshotQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  sourceId: undefined as number | string | undefined,
  sourceGroupId: undefined as number | string | undefined
});

// ---- 采集任务 ----
const collectionLoading = ref(false);
const collectionTotal = ref(0);
const collectionList = ref<CollectionVO[]>([]);
const collectionQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  status: '',
  taskType: ''
});

// ---- 点时版本 ----
const pitLoading = ref(false);
const pitTotal = ref(0);
const pitList = ref<PointInTimeVO[]>([]);
const pitQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  subjectType: '',
  subjectId: undefined as number | undefined
});

async function getList() {
  loading.value = true;
  try {
    const res = await listQuality(queryParams);
    qualityList.value = res.data?.rows ?? [];
    total.value = res.data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

async function getSnapshotList() {
  snapshotLoading.value = true;
  try {
    const res = await listSnapshot(snapshotQuery);
    snapshotList.value = res.data?.rows ?? [];
    snapshotTotal.value = res.data?.total ?? 0;
  } finally {
    snapshotLoading.value = false;
  }
}

async function getCollectionList() {
  collectionLoading.value = true;
  try {
    const res = await listCollection(collectionQuery);
    collectionList.value = res.data?.rows ?? [];
    collectionTotal.value = res.data?.total ?? 0;
  } finally {
    collectionLoading.value = false;
  }
}

async function getPitList() {
  pitLoading.value = true;
  try {
    const res = await listPointInTime(pitQuery);
    pitList.value = res.data?.rows ?? [];
    pitTotal.value = res.data?.total ?? 0;
  } finally {
    pitLoading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.issueStatus = '';
  queryParams.qualityStatus = '';
  handleQuery();
}

function handleSnapshotQuery() {
  snapshotQuery.pageNum = 1;
  getSnapshotList();
}

function handleCollectionQuery() {
  collectionQuery.pageNum = 1;
  getCollectionList();
}

function handlePitQuery() {
  pitQuery.pageNum = 1;
  getPitList();
}

function handleResolve(row: Partial<QualityVO>) {
  currentIssue.value = row;
  Object.assign(resolveForm, {
    handler: '',
    actionTaken: '',
    basisRef: '',
    fixVersion: ''
  });
  resolveVisible.value = true;
}

async function submitResolve() {
  if (!currentIssue.value) {
    return;
  }
  await resolveQuality(currentIssue.value.pkId, resolveForm);
  ElMessage.success('处理成功');
  resolveVisible.value = false;
  getList();
}

onMounted(() => {
  loadSourceOptions();
  loadSourceGroupOptions();
  getList();
  getSnapshotList();
  getCollectionList();
  getPitList();
});
</script>

<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <!-- 数据质量问题 -->
      <el-tab-pane label="质量问题" name="quality">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="问题状态">
            <el-select v-model="queryParams.issueStatus" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="opt in issueStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="质量状态">
            <el-select v-model="queryParams.qualityStatus" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="opt in qualityStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="qualityList" border>
          <el-table-column prop="issueCode" label="问题编码" min-width="160" />
          <el-table-column prop="dimension" label="质量维度" width="120" />
          <el-table-column prop="qualityStatusText" label="质量状态" width="100" />
          <el-table-column prop="issueStatusText" label="问题状态" width="100" />
          <el-table-column prop="severity" label="严重级别" width="100" />
          <el-table-column prop="businessImpact" label="业务影响" min-width="180" show-overflow-tooltip />
          <el-table-column prop="handler" label="处理人" width="100" />
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button v-hasPermi="['quant:data:resolve']" link type="primary" @click="handleResolve(scope.row)">处理</el-button>
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
      </el-tab-pane>

      <!-- 原始快照 -->
      <el-tab-pane label="原始快照" name="snapshot">
        <el-form :inline="true" :model="snapshotQuery" class="search-form">
          <el-form-item label="数据源">
            <el-select v-model="snapshotQuery.sourceId" filterable clearable placeholder="选择数据源" style="width: 200px">
              <el-option v-for="ds in dataSourceOptions" :key="ds.pkId" :label="`${ds.sourceCode} ${ds.sourceName || ''}`" :value="ds.pkId" />
            </el-select>
          </el-form-item>
          <el-form-item label="来源组">
            <el-select v-model="snapshotQuery.sourceGroupId" filterable clearable placeholder="选择来源组" style="width: 200px">
              <el-option v-for="g in sourceGroupOptions" :key="g.pkId" :label="`${g.groupCode} ${g.groupName || ''}`" :value="g.pkId" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSnapshotQuery">查询</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="snapshotLoading" :data="snapshotList" border>
          <el-table-column prop="snapshotId" label="快照ID" min-width="160" />
          <el-table-column prop="sourceId" label="数据源ID" width="140" />
          <el-table-column prop="externalId" label="外部ID" min-width="140" show-overflow-tooltip />
          <el-table-column prop="contentHash" label="内容哈希" min-width="180" show-overflow-tooltip />
          <el-table-column prop="publishedAt" label="发布时间" width="170" />
          <el-table-column prop="availableAt" label="可见时间" width="170" />
          <el-table-column prop="observedAt" label="观察时间" width="170" />
          <el-table-column prop="parserVersion" label="解析版本" width="90" />
        </el-table>

        <el-pagination
          v-show="snapshotTotal > 0"
          v-model:current-page="snapshotQuery.pageNum"
          v-model:page-size="snapshotQuery.pageSize"
          :total="snapshotTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="handleSnapshotQuery"
          @current-change="getSnapshotList"
        />
      </el-tab-pane>

      <!-- 采集任务 -->
      <el-tab-pane label="采集任务" name="collection">
        <el-form :inline="true" :model="collectionQuery" class="search-form">
          <el-form-item label="任务状态">
            <el-select v-model="collectionQuery.status" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="opt in collectionStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="采集类型">
            <el-select v-model="collectionQuery.taskType" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="opt in collectionTaskTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleCollectionQuery">查询</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="collectionLoading" :data="collectionList" border>
          <el-table-column prop="taskCode" label="任务编码" min-width="160" />
          <el-table-column prop="taskType" label="类型" width="100" />
          <el-table-column prop="statusText" label="状态" width="130" />
          <el-table-column prop="successScope" label="成功范围" min-width="140" show-overflow-tooltip />
          <el-table-column prop="failureScope" label="失败范围" min-width="140" show-overflow-tooltip />
          <el-table-column prop="errorCode" label="异常码" width="140" />
          <el-table-column prop="retryCount" label="重试次数" width="90" />
          <el-table-column prop="createTime" label="创建时间" width="170" />
        </el-table>

        <el-pagination
          v-show="collectionTotal > 0"
          v-model:current-page="collectionQuery.pageNum"
          v-model:page-size="collectionQuery.pageSize"
          :total="collectionTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="handleCollectionQuery"
          @current-change="getCollectionList"
        />
      </el-tab-pane>

      <!-- 点时版本 -->
      <el-tab-pane label="点时版本" name="point-in-time">
        <el-form :inline="true" :model="pitQuery" class="search-form">
          <el-form-item label="对象类型">
            <el-select v-model="pitQuery.subjectType" placeholder="全部" clearable style="width: 180px">
              <el-option v-for="opt in subjectTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="对象ID">
            <el-input-number v-model="pitQuery.subjectId" :min="1" controls-position="right" placeholder="对象ID（可选高级过滤）" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handlePitQuery">查询</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="pitLoading" :data="pitList" border>
          <el-table-column prop="subjectType" label="对象类型" width="140" />
          <el-table-column prop="subjectId" label="对象ID" width="140" />
          <el-table-column prop="snapshotVersion" label="快照版本" width="120" />
          <el-table-column prop="decisionTime" label="决策时点" width="170" />
          <el-table-column prop="visibleAt" label="可见时间" width="170" />
          <el-table-column prop="revisedAt" label="修订时间" width="170" />
          <el-table-column prop="revisionReason" label="修订原因" min-width="180" show-overflow-tooltip />
          <el-table-column prop="needRecompute" label="需重算" width="80" align="center">
            <template #default="scope">{{ scope.row.needRecompute ? '是' : '否' }}</template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-show="pitTotal > 0"
          v-model:current-page="pitQuery.pageNum"
          v-model:page-size="pitQuery.pageSize"
          :total="pitTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="handlePitQuery"
          @current-change="getPitList"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="resolveVisible" title="处理数据质量问题" width="560px" append-to-body>
      <el-form :model="resolveForm" label-width="100px">
        <el-form-item label="问题编码">
          <el-input :model-value="currentIssue?.issueCode" disabled />
        </el-form-item>
        <el-form-item label="处理人">
          <el-input v-model="resolveForm.handler" placeholder="请输入处理人" />
        </el-form-item>
        <el-form-item label="处理动作" required>
          <el-input v-model="resolveForm.actionTaken" type="textarea" placeholder="请输入处理动作" />
        </el-form-item>
        <el-form-item label="处理依据">
          <el-input v-model="resolveForm.basisRef" type="textarea" placeholder="请输入处理依据" />
        </el-form-item>
        <el-form-item label="修复版本">
          <el-input v-model="resolveForm.fixVersion" placeholder="请输入修复版本" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResolve">确定</el-button>
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
