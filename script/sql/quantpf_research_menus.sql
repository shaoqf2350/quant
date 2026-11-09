-- =====================================================================
-- QuantPF Phase5：新闻研究链 菜单与权限种子（幂等）
-- 对应 contract/menu/quantpf-menu-facts.yaml 的 quant.research
-- 挂在既有「量化数据」目录（menu_id = 1761400000000002000）下
-- 权限码对齐《量化交易系统完整设计方案说明书_V5.md》§15.2：quant:news:*
-- =====================================================================

-- 二级菜单：研究
INSERT IGNORE INTO sys_menu VALUES (1761400000000002200, '研究', 1761400000000002000, 5, 'research', 'quant/research/index', '', 'N', 'Y', 'C', '0', '0', 'quant:news:list', 'documentation', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '新闻研究链菜单');

-- 按钮权限（F）
INSERT IGNORE INTO sys_menu VALUES (1761400000000002201, '研究查看', 1761400000000002200, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:news:view', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000002202, '重新分析', 1761400000000002200, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'quant:news:reanalyze', '#', '', '', NULL, NULL, SYSDATE(), NULL, NULL, '');