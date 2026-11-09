-- =====================================================================
-- QuantPF 详设1 审查补齐：K线 / 标的同步 权限按钮种子（幂等）
-- 补齐遗漏的权限码：quant:kline:list / quant:kline:import / quant:instrument:sync
-- =====================================================================

-- 市场菜单（1761400000000002400）下：K线读取 / K线导入
INSERT IGNORE INTO sys_menu VALUES (1761400000000002402, 'K线读取', 1761400000000002400, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:kline:list', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000002403, 'K线导入', 1761400000000002400, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:kline:import', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');

-- 标的菜单（1761400000000002100）下：标的同步
INSERT IGNORE INTO sys_menu VALUES (1761400000000002105, '标的同步', 1761400000000002100, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:instrument:sync', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');