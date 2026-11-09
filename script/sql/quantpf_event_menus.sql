-- =====================================================================
-- QuantPF Phase6：公司行为/合约事件 菜单与权限种子（幂等）
-- 对应 contract/menu/quantpf-menu-facts.yaml 的 quant.event
-- 挂在既有「量化数据」目录（menu_id = 1761400000000002000）下
-- =====================================================================

-- 二级菜单：事件
INSERT IGNORE INTO sys_menu VALUES (1761400000000002300, '事件', 1761400000000002000, 6, 'event', 'quant/event/index', '', 'N', 'Y', 'C', '0', '0', 'quant:instrument:list', 'bell', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '公司行为与合约事件菜单');

-- 按钮权限（F）：事件维护（复用标的写权限 quant:instrument:edit）
INSERT IGNORE INTO sys_menu VALUES (1761400000000002301, '事件维护', 1761400000000002300, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:instrument:edit', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');