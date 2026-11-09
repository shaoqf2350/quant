<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  addDataSource,
  addSourceGroup,
  delDataSource,
  delSourceGroup,
  listDataSource,
  listSourceCatalog,
  listSourceGroup,
  updateDataSource,
  updateSourceGroup,
  verifyAllSourceGroups,
  verifySourceGroup
} from '@/api/quant/data/source';
import type { DataSourceForm, DataSourceVO, SourceGroupForm, SourceGroupVO } from '@/api/quant/data/source/types';
import { useDict } from '@/composables/useDict';

const activeTab = ref('source');

// 枚举/目录下拉统一走后端字典接口（事实源在后端，禁止前端硬编码）
const { options: dataSourceStatusOptions } = useDict('DataSourceStatus');
const { options: sourceGroupStatusOptions } = useDict('SourceGroupStatus');
const { options: groupCodeOptions } = useDict('SourceGroupCatalog');

// ---- 数据源 ----
const loading = ref(false);
const total = ref(0);
const sourceList = ref<DataSourceVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('');

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sourceCode: '',
  sourceName: '',
  status: ''
});

const form = reactive<DataSourceForm>({
  sourceCode: '',
  sourceName: '',
  status: 'DRAFT'
});

const rules = {
  sourceCode: [{ required: true, message: '数据源编码不能为空', trigger: 'blur' }],
  sourceName: [{ required: true, message: '数据源名称不能为空', trigger: 'blur' }]
};

// ---- 来源组 ----
const groupLoading = ref(false);
const groupTotal = ref(0);
const groupList = ref<SourceGroupVO[]>([]);
const groupDialogVisible = ref(false);
const groupDialogTitle = ref('');

const groupQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  groupName: '',
  status: ''
});

const groupForm = reactive<SourceGroupForm>({
  groupCode: '',
  groupName: '',
  status: 'CONFIGURED'
});

const groupRules = {
  groupCode: [{ required: true, message: '来源组编码不能为空', trigger: 'blur' }],
  groupName: [{ required: true, message: '来源组名称不能为空', trigger: 'blur' }]
};

// 内置数据源目录：后端事实源（§25.2.1），前端动态拉取，不再硬编码
const sourceCodeOptions = ref<Array<{ value: string; label: string; name: string }>>([]);
async function loadSourceCatalog() {
  try {
    const res = await listSourceCatalog();
    sourceCodeOptions.value = (res.data ?? []).map((c) => ({
      value: c.code,
      label: `${c.code}（${c.name}）`,
      name: c.name,
      description: c.description
    }));
  } catch {
    sourceCodeOptions.value = [];
  }
}

// 数据源下拉选项（来源组主/备源引用，避免手填数字 ID）
const dataSourceOptions = ref<DataSourceVO[]>([]);
async function loadDataSourceOptions() {
  try {
    const res = await listDataSource({ pageNum: 1, pageSize: 500 } as any);
    dataSourceOptions.value = res.data?.rows ?? [];
  } catch {
    dataSourceOptions.value = [];
  }
}

// 备源多选数组（与 groupForm.backupSourceIds 逗号串互转）
const backupSourceIdsArr = ref<Array<number | string>>([]);

/** 拉取数据源列表 */
async function getList() {
  loading.value = true;
  try {
    const res = await listDataSource(queryParams);
    sourceList.value = res.data?.rows ?? [];
    total.value = res.data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

/** 拉取来源组列表 */
async function getGroupList() {
  groupLoading.value = true;
  try {
    const res = await listSourceGroup(groupQuery);
    groupList.value = res.data?.rows ?? [];
    groupTotal.value = res.data?.total ?? 0;
  } finally {
    groupLoading.value = false;
  }
}

/** 查询 */
function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

/** 重置 */
function resetQuery() {
  queryParams.sourceCode = '';
  queryParams.sourceName = '';
  queryParams.status = '';
  handleQuery();
}

/** 来源组查询/重置 */
function handleGroupQuery() {
  groupQuery.pageNum = 1;
  getGroupList();
}

function resetGroupQuery() {
  groupQuery.groupName = '';
  groupQuery.status = '';
  handleGroupQuery();
}

/** 新增 */
function handleAdd() {
  resetForm();
  dialogTitle.value = '新增数据源';
  dialogVisible.value = true;
}

/** 修改 */
function handleEdit(row: Partial<DataSourceVO>) {
  Object.assign(form, {
    pkId: row.pkId,
    sourceCode: row.sourceCode,
    sourceName: row.sourceName,
    vendorName: row.vendorName,
    accountType: row.accountType,
    contractRef: row.contractRef,
    expiryDate: row.expiryDate,
    primarySource: row.primarySource,
    status: row.status,
    remark: row.remark
  });
  dialogTitle.value = '修改数据源';
  dialogVisible.value = true;
}

/** 删除 */
function handleDelete(row: Partial<DataSourceVO>) {
  ElMessageBox.confirm(`确认删除数据源「${row.sourceName}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await delDataSource(row.pkId);
      ElMessage.success('删除成功');
      getList();
    })
    .catch(() => {});
}

/** 选择来源目录 → 自动带出数据源名称（少输入；名称缺省后端也按目录兜底） */
function onSourceCodeChange(code: string) {
  const opt = sourceCodeOptions.value.find((o) => o.value === code);
  if (opt && !form.sourceName) {
    form.sourceName = opt.name;
  }
}

/** 提交表单 */
async function submit() {
  if (form.pkId) {
    await updateDataSource(form);
  } else {
    await addDataSource(form);
  }
  ElMessage.success(form.pkId ? '修改成功' : '新增成功');
  dialogVisible.value = false;
  getList();
}

function resetForm() {
  Object.assign(form, {
    pkId: undefined,
    sourceCode: '',
    sourceName: '',
    vendorName: '',
    accountType: '',
    contractRef: '',
    expiryDate: '',
    primarySource: false,
    status: 'DRAFT',
    remark: ''
  });
}

/** 来源组 CRUD */
function handleGroupAdd() {
  resetGroupForm();
  groupDialogTitle.value = '新增来源组';
  groupDialogVisible.value = true;
}

function handleGroupEdit(row: Partial<SourceGroupVO>) {
  Object.assign(groupForm, {
    pkId: row.pkId,
    groupCode: row.groupCode,
    groupName: row.groupName,
    primarySourceId: row.primarySourceId,
    backupSourceIds: row.backupSourceIds,
    verifyRule: row.verifyRule,
    reprintPolicy: row.reprintPolicy,
    status: row.status
  });
  backupSourceIdsArr.value = String(row.backupSourceIds || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((s) => (Number.isNaN(Number(s)) ? s : Number(s)));
  groupDialogTitle.value = '修改来源组';
  groupDialogVisible.value = true;
}

function handleGroupDelete(row: Partial<SourceGroupVO>) {
  ElMessageBox.confirm(`确认删除来源组「${row.groupName}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await delSourceGroup(row.pkId);
      ElMessage.success('删除成功');
      getGroupList();
    })
    .catch(() => {});
}

async function submitGroup() {
  // 备源多选数组 → 逗号串，供后端存储
  groupForm.backupSourceIds = backupSourceIdsArr.value.map(String).join(',');
  if (groupForm.pkId) {
    await updateSourceGroup(groupForm);
  } else {
    await addSourceGroup(groupForm);
  }
  ElMessage.success(groupForm.pkId ? '修改成功' : '新增成功');
  groupDialogVisible.value = false;
  getGroupList();
}

function resetGroupForm() {
  Object.assign(groupForm, {
    pkId: undefined,
    groupCode: '',
    groupName: '',
    primarySourceId: undefined,
    backupSourceIds: '',
    verifyRule: '',
    reprintPolicy: '',
    status: 'CONFIGURED'
  });
  backupSourceIdsArr.value = [];
}

// ---- 来源组健康核验（§6.3 主备切换/冲突上报）----
async function handleVerify(row: Partial<SourceGroupVO>) {
  const res = await verifySourceGroup(row.pkId);
  const data = res.data as { fromStatus?: string; toStatus?: string; primarySwitched?: boolean } | undefined;
  const switched = data?.primarySwitched ? '，已执行主备切换' : '';
  ElMessage.success(`核验完成：${data?.fromStatus ?? ''} → ${data?.toStatus ?? ''}${switched}`);
  getGroupList();
}

async function handleVerifyAll() {
  const res = await verifyAllSourceGroups();
  const list = (res.data as Array<Record<string, unknown>>) ?? [];
  ElMessage.success(`全部来源组核验完成，共 ${list.length} 组`);
  getGroupList();
}

onMounted(() => {
  loadDataSourceOptions();
  getList();
  getGroupList();
});
</script>

<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <!-- 数据源 -->
      <el-tab-pane label="数据源" name="source">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="数据源编码">
            <el-input v-model="queryParams.sourceCode" placeholder="请输入数据源编码" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="数据源名称">
            <el-input v-model="queryParams.sourceName" placeholder="请输入数据源名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="opt in dataSourceStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
            <el-button v-hasPermi="['quant:data:add']" type="primary" @click="handleAdd">新增</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="sourceList" border>
          <el-table-column prop="sourceCode" label="数据源编码" min-width="140" />
          <el-table-column prop="sourceName" label="数据源名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="vendorName" label="供应商" min-width="140" show-overflow-tooltip />
          <el-table-column prop="accountType" label="账号类型" width="100" />
          <el-table-column label="主源" width="70" align="center">
            <template #default="scope">{{ scope.row.primarySource ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column prop="statusText" label="状态" width="100" />
          <el-table-column prop="expiryDate" label="到期日" width="110" />
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="scope">
              <el-button v-hasPermi="['quant:data:edit']" link type="primary" @click="handleEdit(scope.row)">修改</el-button>
              <el-button v-hasPermi="['quant:data:remove']" link type="danger" @click="handleDelete(scope.row)">删除</el-button>
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

      <!-- 来源组 -->
      <el-tab-pane label="来源组" name="group">
        <el-form :inline="true" :model="groupQuery" class="search-form">
          <el-form-item label="来源组名称">
            <el-input v-model="groupQuery.groupName" placeholder="请输入来源组名称" clearable @keyup.enter="handleGroupQuery" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="groupQuery.status" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="opt in sourceGroupStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleGroupQuery">查询</el-button>
            <el-button @click="resetGroupQuery">重置</el-button>
            <el-button v-hasPermi="['quant:data:add']" type="primary" @click="handleGroupAdd">新增</el-button>
            <el-button v-hasPermi="['quant:data:edit']" type="warning" @click="handleVerifyAll">核验全部</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="groupLoading" :data="groupList" border>
          <el-table-column prop="groupCode" label="来源组编码" min-width="140">
            <template #default="scope">{{ groupCodeOptions.find(o => o.value === scope.row.groupCode)?.label || scope.row.groupCode }}</template>
          </el-table-column>
          <el-table-column prop="groupName" label="来源组名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="statusText" label="状态" width="110" />
          <el-table-column prop="verifyRule" label="核验规则" min-width="180" show-overflow-tooltip />
          <el-table-column prop="reprintPolicy" label="转载/去重策略" min-width="140" show-overflow-tooltip />
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="scope">
              <el-button v-hasPermi="['quant:data:edit']" link type="primary" @click="handleGroupEdit(scope.row)">修改</el-button>
              <el-button v-hasPermi="['quant:data:remove']" link type="danger" @click="handleGroupDelete(scope.row)">删除</el-button>
              <el-button v-hasPermi="['quant:data:edit']" link type="warning" @click="handleVerify(scope.row)">核验</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-show="groupTotal > 0"
          v-model:current-page="groupQuery.pageNum"
          v-model:page-size="groupQuery.pageSize"
          :total="groupTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="handleGroupQuery"
          @current-change="getGroupList"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" append-to-body>
      <el-form :model="form" :rules="rules" label-width="100px">
        <el-form-item label="数据源编码" prop="sourceCode">
          <el-select v-model="form.sourceCode" filterable placeholder="选择数据源类型" style="width: 100%" @change="onSourceCodeChange">
            <el-option v-for="opt in sourceCodeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据源名称" prop="sourceName">
          <el-input v-model="form.sourceName" placeholder="请输入数据源名称" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="form.vendorName" placeholder="请输入供应商组织名" />
        </el-form-item>
        <el-form-item label="账号类型">
          <el-input v-model="form.accountType" placeholder="请输入账号类型" />
        </el-form-item>
        <el-form-item label="合同标识">
          <el-input v-model="form.contractRef" placeholder="请输入合同/订阅标识" />
        </el-form-item>
        <el-form-item label="到期日">
          <el-date-picker v-model="form.expiryDate" type="date" value-format="YYYY-MM-DD" placeholder="选择到期日" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="opt in dataSourceStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否主源">
          <el-switch v-model="form.primarySource" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="groupDialogVisible" :title="groupDialogTitle" width="560px" append-to-body>
      <el-form :model="groupForm" :rules="groupRules" label-width="120px">
        <el-form-item label="来源组编码" prop="groupCode">
          <el-select v-model="groupForm.groupCode" placeholder="选择来源组编码" style="width: 100%">
            <el-option v-for="opt in groupCodeOptions" :key="opt.value" :label="`${opt.label}（${opt.value}）`" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源组名称" prop="groupName">
          <el-input v-model="groupForm.groupName" placeholder="如 新闻/政策来源组" />
        </el-form-item>
        <el-form-item label="主源数据源">
          <el-select v-model="groupForm.primarySourceId" filterable clearable placeholder="选择主源数据源" style="width: 100%">
            <el-option v-for="ds in dataSourceOptions" :key="ds.pkId" :label="`${ds.sourceCode} ${ds.sourceName || ''}`" :value="ds.pkId" />
          </el-select>
        </el-form-item>
        <el-form-item label="备源数据源">
          <el-select v-model="backupSourceIdsArr" multiple filterable clearable collapse-tags collapse-tags-tooltip placeholder="选择备源数据源" style="width: 100%">
            <el-option v-for="ds in dataSourceOptions" :key="ds.pkId" :label="`${ds.sourceCode} ${ds.sourceName || ''}`" :value="ds.pkId" />
          </el-select>
        </el-form-item>
        <el-form-item label="交叉核验规则">
          <el-input v-model="groupForm.verifyRule" type="textarea" placeholder="发布时间/原文/转载/数值一致" />
        </el-form-item>
        <el-form-item label="转载/去重策略">
          <el-input v-model="groupForm.reprintPolicy" placeholder="如：相同原文不视为独立证据" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="groupForm.status" style="width: 100%">
            <el-option v-for="opt in sourceGroupStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGroup">确定</el-button>
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
