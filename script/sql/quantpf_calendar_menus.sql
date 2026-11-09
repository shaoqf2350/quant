-- =====================================================================
-- QuantPF 模块一「数据与标的基础」Phase4：标的/交易日历 菜单与权限种子（幂等）
-- 对应 contract/menu/quantpf-menu-facts.yaml 的 quant.instrument / quant.calendar
-- 挂在既有「量化数据」目录（menu_id = 1761400000000002000）下
-- =====================================================================

-- 二级菜单：标的
INSERT IGNORE INTO sys_menu VALUES (1761400000000002100, '标的', 1761400000000002000, 3, 'instrument', 'quant/instrument/index', '', 'N', 'Y', 'C', '0', '0', 'quant:instrument:list', 'list', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '标的台账菜单');

-- 二级菜单：交易日历
INSERT IGNORE INTO sys_menu VALUES (1761400000000002101, '交易日历', 1761400000000002000, 4, 'calendar', 'quant/calendar/index', '', 'N', 'Y', 'C', '0', '0', 'quant:calendar:list', 'date-range', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '交易日历与日线就绪菜单');

-- 按钮权限（F）
INSERT IGNORE INTO sys_menu VALUES (1761400000000002102, '标的查询', 1761400000000002100, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:instrument:view', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000002103, '日历维护', 1761400000000002101, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:calendar:import', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000002104, '时段查询', 1761400000000002101, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:calendar:sessions', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');