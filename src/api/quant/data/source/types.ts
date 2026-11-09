/**
 * 数据源与来源组 类型定义（《详设_1 §5/§6》）
 */

/** 数据源分页查询 */
export interface DataSourceQuery {
  pageNum: number;
  pageSize: number;
  sourceCode?: string;
  sourceName?: string;
  status?: string;
}

/** 数据源视图对象 */
export interface DataSourceVO {
  pkId: number | string;
  sourceCode: string;
  sourceName: string;
  vendorName?: string;
  accountType?: string;
  contractRef?: string;
  expiryDate?: string;
  primarySource?: boolean;
  status?: string;
  statusText?: string;
  createTime?: string;
  updateTime?: string;
  remark?: string;
}

/** 数据源表单 */
export interface DataSourceForm {
  pkId?: number | string;
  sourceCode: string;
  sourceName: string;
  vendorName?: string;
  accountType?: string;
  contractRef?: string;
  expiryDate?: string;
  primarySource?: boolean;
  status: string;
  remark?: string;
}

/** 来源组分页查询 */
export interface SourceGroupQuery {
  pageNum: number;
  pageSize: number;
  groupCode?: string;
  groupName?: string;
  status?: string;
}

/** 来源组视图对象 */
export interface SourceGroupVO {
  pkId: number | string;
  groupCode: string;
  groupName: string;
  primarySourceId?: number | string;
  backupSourceIds?: string;
  verifyRule?: string;
  reprintPolicy?: string;
  status?: string;
  statusText?: string;
  createTime?: string;
  updateTime?: string;
}

/** 来源组表单 */
export interface SourceGroupForm {
  pkId?: number | string;
  groupCode: string;
  groupName: string;
  primarySourceId?: number | string;
  backupSourceIds?: string;
  verifyRule?: string;
  reprintPolicy?: string;
  status: string;
}

/** 数据源目录项（后端事实源，前端下拉动态拉取） */
export interface DataSourceCatalogVO {
  code: string;
  name: string;
  description: string;
}