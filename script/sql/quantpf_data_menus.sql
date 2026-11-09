-- =====================================================================
-- QuantPF 模块一「数据与标的基础」：菜单与权限种子（幂等，可重复执行）
-- 对应 contract/menu/quantpf-menu-facts.yaml 的 quant.data-source / quant.data-quality
-- 说明：
--   1) 超级管理员（admin，role 的 admin 标记=1）默认拥有全部权限，无需按角色授权；
--   2) 其他角色需在「角色管理」中勾选下述菜单/按钮权限；
--   3) 菜单 component 对应 plus-ui 的 src/views/quant/** 页面。
-- =====================================================================

-- 一级目录：量化数据
INSERT IGNORE INTO sys_menu VALUES (1761400000000002000, '量化数据', 0, 10, 'quant', NULL, '', 'N', 'Y', 'M', '0', '0', '', 'chart', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '量化交易数据治理目录');

-- 二级菜单：数据源管理
INSERT IGNORE INTO sys_menu VALUES (1761400000000002001, '数据源管理', 1761400000000002000, 1, 'data-source', 'quant/data-source/index', '', 'N', 'Y', 'C', '0', '0', 'quant:data:list', 'table', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '数据源与来源组管理菜单');

-- 二级菜单：数据质量（对应 contract/menu 的 quant.data-quality，页面 quant/data-quality/index，
-- 含质量问题/原始快照/采集任务/点时版本四个 Tab）
INSERT IGNORE INTO sys_menu VALUES (1761400000000002002, '数据质量', 1761400000000002000, 2, 'data-quality', 'quant/data-quality/index', '', 'N', 'Y', 'C', '0', '0', 'quant:data:list', 'edit', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '数据质量问题/原始快照/采集任务/点时版本菜单');
-- 幂等修正：已按旧标题/路径（数据治理/data-governance）种过菜单的环境对齐（不改 menu_id 与权限）
UPDATE sys_menu SET menu_name = '数据质量', path = 'data-quality', component = 'quant/data-quality/index', remark = '数据质量问题/原始快照/采集任务/点时版本菜单' WHERE menu_id = 1761400000000002002;

-- 按钮权限（F）
INSERT IGNORE INTO sys_menu VALUES (1761400000000002003, '数据查询', 1761400000000002001, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:data:query', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000002004, '数据新增', 1761400000000002001, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:data:add', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000002005, '数据修改', 1761400000000002001, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:data:edit', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000002006, '数据删除', 1761400000000002001, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:data:remove', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000002007, '质量问题处理', 1761400000000002002, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:data:resolve', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');