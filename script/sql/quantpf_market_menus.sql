-- =====================================================================
-- QuantPF Phase7：市场分析 菜单与权限种子（幂等）
-- 对应 contract/menu/quantpf-menu-facts.yaml 的 quant.market
-- 挂在既有「量化数据」目录（menu_id = 1761400000000002000）下
-- =====================================================================

-- 二级菜单：市场
INSERT IGNORE INTO sys_menu VALUES (1761400000000002400, '市场', 1761400000000002000, 7, 'market', 'quant/market/index', '', 'N', 'Y', 'C', '0', '0', 'quant:market:view', 'chart', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '市场状态/容量/量价特征菜单');

-- 按钮权限（F）：市场记录（供 XXL-JOB/人工维护）
INSERT IGNORE INTO sys_menu VALUES (1761400000000002401, '市场记录', 1761400000000002400, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:market:edit', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');