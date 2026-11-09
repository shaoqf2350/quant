-- ============================================================
-- QuantPF / RuoYi 基础系统表初始化脚本（目标 schema：quantpf）
-- 适用环境：MySQL 5.7+；请先确认当前连接 schema 为 quantpf
-- 来源：RuoYi-Vue-Plus/script/sql/ry_vue.sql（24 张 RuoYi 基础/生成器/测试表）
--
-- 重要边界：
-- 1. 本脚本只负责 sys_*、gen_*、test_* 基础表及其初始化数据；
-- 2. QuantPF quantpf_* 业务表由 db/migration/V1_*__*.sql 管理，本脚本不创建、不删除；
-- 3. 本脚本不执行 DROP TABLE，不执行数据库迁移，不修改已有表结构；
-- 4. CREATE TABLE IF NOT EXISTS + INSERT IGNORE 允许重复执行且保留已有记录；
-- 5. 如已有表结构与 RuoYi 版本不一致，必须单独做 schema diff，不得依赖本脚本静默修复。
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 以下仅关闭当前会话的外键检查，脚本结束时恢复；不执行 DROP/DELETE。
-- ============================================================

-- ============================================================
CREATE TABLE IF NOT EXISTS sys_social
(
    id                 BIGINT           NOT NULL        COMMENT '主键',
    user_id            BIGINT           NOT NULL        COMMENT '用户ID',
    auth_id            VARCHAR(255)     NOT NULL        COMMENT '平台+平台唯一id',
    source             VARCHAR(255)     NOT NULL        COMMENT '用户来源',
    open_id            VARCHAR(255)     DEFAULT NULL    COMMENT '平台编号唯一id',
    user_name          VARCHAR(30)      NOT NULL        COMMENT '登录账号',
    nick_name          VARCHAR(30)      DEFAULT ''      COMMENT '用户昵称',
    email              VARCHAR(255)     DEFAULT ''      COMMENT '用户邮箱',
    avatar             VARCHAR(500)     DEFAULT ''      COMMENT '头像地址',
    access_token       VARCHAR(2000)    NOT NULL        COMMENT '用户的授权令牌',
    expire_in          INT              DEFAULT NULL    COMMENT '用户的授权令牌的有效期，部分平台可能没有',
    refresh_token      VARCHAR(2000)    DEFAULT NULL    COMMENT '刷新令牌，部分平台可能没有',
    access_code        VARCHAR(255)     DEFAULT NULL    COMMENT '平台的授权信息，部分平台可能没有',
    union_id           VARCHAR(255)     DEFAULT NULL    COMMENT '用户的 unionid',
    scope              VARCHAR(255)     DEFAULT NULL    COMMENT '授予的权限，部分平台可能没有',
    token_type         VARCHAR(255)     DEFAULT NULL    COMMENT '个别平台的授权信息，部分平台可能没有',
    id_token           VARCHAR(2000)    DEFAULT NULL    COMMENT 'id token，部分平台可能没有',
    mac_algorithm      VARCHAR(255)     DEFAULT NULL    COMMENT '小米平台用户的附带属性，部分平台可能没有',
    mac_key            VARCHAR(255)     DEFAULT NULL    COMMENT '小米平台用户的附带属性，部分平台可能没有',
    code               VARCHAR(255)     DEFAULT NULL    COMMENT '用户的授权code，部分平台可能没有',
    oauth_token        VARCHAR(255)     DEFAULT NULL    COMMENT 'Twitter平台用户的附带属性，部分平台可能没有',
    oauth_token_secret VARCHAR(255)     DEFAULT NULL    COMMENT 'Twitter平台用户的附带属性，部分平台可能没有',
    create_dept        BIGINT(20)                       COMMENT '创建部门',
    create_by          BIGINT(20)                       COMMENT '创建者',
    create_time        DATETIME                         COMMENT '创建时间',
    update_by          BIGINT(20)                       COMMENT '更新者',
    update_time        DATETIME                         COMMENT '更新时间',
    del_flag           CHAR(1)          DEFAULT '0'     COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT = '社会化关系表';

-- ============================================================
-- 2. 部门表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_dept
(
    dept_id       BIGINT(20)   NOT NULL                   COMMENT '部门id',
    parent_id     BIGINT(20)   DEFAULT 0                  COMMENT '父部门id',
    ancestors     VARCHAR(500) DEFAULT ''                 COMMENT '祖级列表',
    dept_name     VARCHAR(30)  DEFAULT ''                 COMMENT '部门名称',
    dept_category VARCHAR(100) DEFAULT NULL               COMMENT '部门类别编码',
    order_num     INT(4)       DEFAULT 0                  COMMENT '显示顺序',
    leader        BIGINT(20)   DEFAULT NULL               COMMENT '负责人',
    phone         VARCHAR(11)  DEFAULT NULL               COMMENT '联系电话',
    email         VARCHAR(50)  DEFAULT NULL               COMMENT '邮箱',
    status        CHAR(1)      DEFAULT '0'                COMMENT '部门状态（0正常 1停用）',
    del_flag      CHAR(1)      DEFAULT '0'                COMMENT '删除标志（0代表存在 1代表删除）',
    create_dept   BIGINT(20)   DEFAULT NULL               COMMENT '创建部门',
    create_by     BIGINT(20)   DEFAULT NULL               COMMENT '创建者',
    create_time   DATETIME                                COMMENT '创建时间',
    update_by     BIGINT(20)   DEFAULT NULL               COMMENT '更新者',
    update_time   DATETIME                                COMMENT '更新时间',
    PRIMARY KEY (dept_id),
    KEY idx_sys_dept_parent_id (parent_id)
) ENGINE=InnoDB COMMENT = '部门表';

-- 初始化部门数据
INSERT IGNORE INTO sys_dept VALUES (1761000000000000100, 0, '0', 'XXX科技', NULL, 0, NULL, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL);
INSERT IGNORE INTO sys_dept VALUES (1761000000000000101, 1761000000000000100, '0,1761000000000000100', '深圳总公司', NULL, 1, NULL, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL);
INSERT IGNORE INTO sys_dept VALUES (1761000000000000102, 1761000000000000100, '0,1761000000000000100', '长沙分公司', NULL, 2, NULL, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL);
INSERT IGNORE INTO sys_dept VALUES (1761000000000000103, 1761000000000000101, '0,1761000000000000100,1761000000000000101', '研发部门', NULL, 1, 1761100000000000001, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL);
INSERT IGNORE INTO sys_dept VALUES (1761000000000000104, 1761000000000000101, '0,1761000000000000100,1761000000000000101', '市场部门', NULL, 2, NULL, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL);
INSERT IGNORE INTO sys_dept VALUES (1761000000000000105, 1761000000000000101, '0,1761000000000000100,1761000000000000101', '测试部门', NULL, 3, NULL, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL);
INSERT IGNORE INTO sys_dept VALUES (1761000000000000106, 1761000000000000101, '0,1761000000000000100,1761000000000000101', '财务部门', NULL, 4, NULL, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL);
INSERT IGNORE INTO sys_dept VALUES (1761000000000000107, 1761000000000000101, '0,1761000000000000100,1761000000000000101', '运维部门', NULL, 5, NULL, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL);
INSERT IGNORE INTO sys_dept VALUES (1761000000000000108, 1761000000000000102, '0,1761000000000000100,1761000000000000102', '市场部门', NULL, 1, NULL, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL);
INSERT IGNORE INTO sys_dept VALUES (1761000000000000109, 1761000000000000102, '0,1761000000000000100,1761000000000000102', '财务部门', NULL, 2, NULL, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL);

-- ============================================================
-- 3. 用户信息表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user
(
    user_id      BIGINT(20)   NOT NULL                   COMMENT '用户ID',
    dept_id      BIGINT(20)   DEFAULT NULL               COMMENT '部门ID',
    user_name    VARCHAR(30)  NOT NULL                   COMMENT '用户账号',
    nick_name    VARCHAR(30)  NOT NULL                   COMMENT '用户昵称',
    user_type    VARCHAR(10)  DEFAULT 'sys_user'         COMMENT '用户类型（sys_user系统用户）',
    email        VARCHAR(50)  DEFAULT ''                 COMMENT '用户邮箱',
    phone_number VARCHAR(11)  DEFAULT ''                 COMMENT '手机号码',
    gender       CHAR(1)      DEFAULT '0'                COMMENT '用户性别（0男 1女 2未知）',
    avatar       BIGINT(20)                              COMMENT '头像地址',
    password     VARCHAR(100) DEFAULT ''                 COMMENT '密码',
    status       CHAR(1)      DEFAULT '0'                COMMENT '账号状态（0正常 1停用）',
    del_flag     CHAR(1)      DEFAULT '0'                COMMENT '删除标志（0代表存在 1代表删除）',
    login_ip     VARCHAR(128) DEFAULT ''                 COMMENT '最后登录IP',
    login_date   DATETIME                                COMMENT '最后登录时间',
    create_dept  BIGINT(20)   DEFAULT NULL               COMMENT '创建部门',
    create_by    BIGINT(20)   DEFAULT NULL               COMMENT '创建者',
    create_time  DATETIME                                COMMENT '创建时间',
    update_by    BIGINT(20)   DEFAULT NULL               COMMENT '更新者',
    update_time  DATETIME                                COMMENT '更新时间',
    remark       VARCHAR(500) DEFAULT NULL               COMMENT '备注',
    PRIMARY KEY (user_id),
    KEY idx_sys_user_dept_id   (dept_id),
    KEY idx_sys_user_create_by (create_by),
    KEY idx_sys_user_user_name (user_name),
    KEY idx_sys_user_phone     (phone_number)
) ENGINE=InnoDB COMMENT = '用户信息表';

-- 初始化用户数据（admin/admin123, test/666666, test1/666666）
INSERT IGNORE INTO sys_user VALUES (1761100000000000001, 1761000000000000103, 'admin', '疯狂的狮子Li', 'sys_user', 'crazyLionLi@163.com', '15888888888', '1', NULL, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', SYSDATE(), 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '管理员');
INSERT IGNORE INTO sys_user VALUES (1761100000000000003, 1761000000000000108, 'test', '本部门及以下 密码666666', 'sys_user', '', '', '0', NULL, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', SYSDATE(), 1761000000000000103, 1761100000000000001, SYSDATE(), 1761100000000000003, SYSDATE(), NULL);
INSERT IGNORE INTO sys_user VALUES (1761100000000000004, 1761000000000000102, 'test1', '仅本人 密码666666', 'sys_user', '', '', '0', NULL, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', SYSDATE(), 1761000000000000103, 1761100000000000001, SYSDATE(), 1761100000000000004, SYSDATE(), NULL);

-- ============================================================
-- 4. 岗位信息表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_post
(
    post_id       BIGINT(20)   NOT NULL                   COMMENT '岗位ID',
    dept_id       BIGINT(20)   NOT NULL                   COMMENT '部门id',
    post_code     VARCHAR(64)  NOT NULL                   COMMENT '岗位编码',
    post_category VARCHAR(100) DEFAULT NULL               COMMENT '岗位类别编码',
    post_name     VARCHAR(50)  NOT NULL                   COMMENT '岗位名称',
    post_sort     INT(4)       NOT NULL                   COMMENT '显示顺序',
    status        CHAR(1)      NOT NULL                   COMMENT '状态（0正常 1停用）',
    del_flag      CHAR(1)      DEFAULT '0'                COMMENT '删除标志（0代表存在 1代表删除）',
    create_dept   BIGINT(20)   DEFAULT NULL               COMMENT '创建部门',
    create_by     BIGINT(20)   DEFAULT NULL               COMMENT '创建者',
    create_time   DATETIME                                COMMENT '创建时间',
    update_by     BIGINT(20)   DEFAULT NULL               COMMENT '更新者',
    update_time   DATETIME                                COMMENT '更新时间',
    remark        VARCHAR(500) DEFAULT NULL               COMMENT '备注',
    PRIMARY KEY (post_id),
    KEY idx_sys_post_dept_id (dept_id)
) ENGINE=InnoDB COMMENT = '岗位信息表';

-- 初始化岗位数据
INSERT IGNORE INTO sys_post VALUES (1761200000000000001, 1761000000000000103, 'ceo', NULL, '董事长', 1, '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_post VALUES (1761200000000000002, 1761000000000000100, 'se', NULL, '项目经理', 2, '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_post VALUES (1761200000000000003, 1761000000000000100, 'hr', NULL, '人力资源', 3, '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_post VALUES (1761200000000000004, 1761000000000000100, 'user', NULL, '普通员工', 4, '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');

-- ============================================================
-- 5. 角色信息表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role
(
    role_id             BIGINT(20)   NOT NULL                   COMMENT '角色ID',
    role_name           VARCHAR(30)  NOT NULL                   COMMENT '角色名称',
    role_key            VARCHAR(100) NOT NULL                   COMMENT '角色权限字符串',
    role_sort           INT(4)       NOT NULL                   COMMENT '显示顺序',
    data_scope          CHAR(1)      DEFAULT '1'                COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）',
    menu_check_strictly TINYINT(1)   DEFAULT 1                  COMMENT '菜单树选择项是否关联显示',
    dept_check_strictly TINYINT(1)   DEFAULT 1                  COMMENT '部门树选择项是否关联显示',
    status              CHAR(1)      NOT NULL                   COMMENT '角色状态（0正常 1停用）',
    del_flag            CHAR(1)      DEFAULT '0'                COMMENT '删除标志（0代表存在 1代表删除）',
    create_dept         BIGINT(20)   DEFAULT NULL               COMMENT '创建部门',
    create_by           BIGINT(20)   DEFAULT NULL               COMMENT '创建者',
    create_time         DATETIME                                COMMENT '创建时间',
    update_by           BIGINT(20)   DEFAULT NULL               COMMENT '更新者',
    update_time         DATETIME                                COMMENT '更新时间',
    remark              VARCHAR(500) DEFAULT NULL               COMMENT '备注',
    PRIMARY KEY (role_id),
    KEY idx_sys_role_create_dept (create_dept),
    KEY idx_sys_role_create_by   (create_by)
) ENGINE=InnoDB COMMENT = '角色信息表';

-- 初始化角色数据
INSERT IGNORE INTO sys_role VALUES (1761300000000000001, '超级管理员', 'superadmin', 1, 1, 1, 1, '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '超级管理员');
INSERT IGNORE INTO sys_role VALUES (1761300000000000003, '本部门及以下', 'test1', 3, 4, 1, 1, '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_role VALUES (1761300000000000004, '仅本人', 'test2', 4, 5, 1, 1, '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');

-- ============================================================
-- 6. 菜单权限表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_menu
(
    menu_id     BIGINT(20)   NOT NULL                   COMMENT '菜单ID',
    menu_name   VARCHAR(50)  NOT NULL                   COMMENT '菜单名称',
    parent_id   BIGINT(20)   DEFAULT 0                  COMMENT '父菜单ID',
    order_num   INT(4)       DEFAULT 0                  COMMENT '显示顺序',
    path        VARCHAR(200) DEFAULT ''                 COMMENT '路由地址',
    component   VARCHAR(255) DEFAULT NULL               COMMENT '组件路径',
    query_param VARCHAR(255) DEFAULT NULL               COMMENT '路由参数',
    is_frame    CHAR(1)      DEFAULT 'N'                COMMENT '是否为外链（Y是 N否）',
    is_cache    CHAR(1)      DEFAULT 'Y'                COMMENT '是否缓存（Y缓存 N不缓存）',
    menu_type   CHAR(1)      DEFAULT ''                 COMMENT '菜单类型（M目录 C菜单 F按钮）',
    visible     CHAR(1)      DEFAULT '0'                COMMENT '显示状态（0显示 1隐藏）',
    status      CHAR(1)      DEFAULT '0'                COMMENT '菜单状态（0正常 1停用）',
    perms       VARCHAR(100) DEFAULT NULL               COMMENT '权限标识',
    icon        VARCHAR(100) DEFAULT '#'                COMMENT '菜单图标',
    active_menu VARCHAR(255) DEFAULT ''                 COMMENT '激活菜单路径',
    ext         VARCHAR(2000) DEFAULT ''                COMMENT '扩展字段',
    create_dept BIGINT(20)   DEFAULT NULL               COMMENT '创建部门',
    create_by   BIGINT(20)   DEFAULT NULL               COMMENT '创建者',
    create_time DATETIME                                COMMENT '创建时间',
    update_by   BIGINT(20)   DEFAULT NULL               COMMENT '更新者',
    update_time DATETIME                                COMMENT '更新时间',
    remark      VARCHAR(500) DEFAULT ''                 COMMENT '备注',
    PRIMARY KEY (menu_id)
) ENGINE=InnoDB COMMENT = '菜单权限表';

-- 初始化菜单数据
-- 一级菜单
INSERT IGNORE INTO sys_menu VALUES (1761400000000000001, '系统管理', 0, 1, 'system', NULL, '', 'N', 'Y', 'M', '0', '0', '', 'system', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '系统管理目录');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000002, '系统监控', 0, 3, 'monitor', NULL, '', 'N', 'Y', 'M', '0', '0', '', 'monitor', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '系统监控目录');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000003, '系统工具', 0, 4, 'tool', NULL, '', 'N', 'Y', 'M', '0', '0', '', 'tool', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '系统工具目录');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000005, '测试菜单', 0, 5, 'demo', NULL, '', 'N', 'Y', 'M', '0', '0', '', 'star', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '测试菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000008, 'AI会话', 0, 8, 'aichat', 'ai/chat/index', '', 'N', 'Y', 'C', '1', '0', '', 'checkbox', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, 'AI聊天菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000004, 'PLUS官网', 0, 9, 'https://gitee.com/dromara/RuoYi-Vue-Plus', NULL, '', 'Y', 'Y', 'M', '0', '0', '', 'guide', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, 'RuoYi-Vue-Plus官网地址');
-- 二级菜单
INSERT IGNORE INTO sys_menu VALUES (1761400000000000100, '用户管理', 1761400000000000001, 1, 'user', 'system/user/index', '', 'N', 'Y', 'C', '0', '0', 'system:user:list', 'user', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '用户管理菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000101, '角色管理', 1761400000000000001, 2, 'role', 'system/role/index', '', 'N', 'Y', 'C', '0', '0', 'system:role:list', 'peoples', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '角色管理菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000102, '菜单管理', 1761400000000000001, 3, 'menu', 'system/menu/index', '', 'N', 'Y', 'C', '0', '0', 'system:menu:list', 'tree-table', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '菜单管理菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000103, '部门管理', 1761400000000000001, 4, 'dept', 'system/dept/index', '', 'N', 'Y', 'C', '0', '0', 'system:dept:list', 'tree', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '部门管理菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000104, '岗位管理', 1761400000000000001, 5, 'post', 'system/post/index', '', 'N', 'Y', 'C', '0', '0', 'system:post:list', 'post', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '岗位管理菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000105, '字典管理', 1761400000000000001, 6, 'dict', 'system/dict/index', '', 'N', 'Y', 'C', '0', '0', 'system:dict:list', 'dict', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '字典管理菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000106, '参数设置', 1761400000000000001, 7, 'config', 'system/config/index', '', 'N', 'Y', 'C', '0', '0', 'system:config:list', 'edit', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '参数设置菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000107, '通知公告', 1761400000000000001, 8, 'notice', 'system/notice/index', '', 'N', 'Y', 'C', '0', '0', 'system:notice:list', 'message', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '通知公告菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000108, '日志管理', 1761400000000000001, 9, 'log', '', '', 'N', 'Y', 'M', '0', '0', '', 'log', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '日志管理菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000109, '在线用户', 1761400000000000002, 1, 'online', 'monitor/online/index', '', 'N', 'Y', 'C', '0', '0', 'monitor:online:list', 'online', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '在线用户菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000113, '缓存监控', 1761400000000000002, 5, 'cache', 'monitor/cache/index', '', 'N', 'Y', 'C', '0', '0', 'monitor:cache:list', 'redis', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '缓存监控菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000115, '代码生成', 1761400000000000003, 2, 'gen', 'tool/gen/index', '', 'N', 'Y', 'C', '0', '0', 'tool:gen:list', 'code', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '代码生成菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000123, '客户端管理', 1761400000000000001, 11, 'client', 'system/client/index', '', 'N', 'Y', 'C', '0', '0', 'system:client:list', 'international', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '客户端管理菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000116, '修改生成配置', 1761400000000000003, 2, 'gen-edit/index/:tableId', 'tool/gen/editTable', '', 'N', 'N', 'C', '1', '0', 'tool:gen:edit', '#', '/tool/gen', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000130, '分配用户', 1761400000000000001, 2, 'role-auth/user/:roleId', 'system/role/authUser', '', 'N', 'N', 'C', '1', '0', 'system:role:edit', '#', '/system/role', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000131, '分配角色', 1761400000000000001, 1, 'user-auth/role/:userId', 'system/user/authRole', '', 'N', 'N', 'C', '1', '0', 'system:user:edit', '#', '/system/user', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000133, '文件配置管理', 1761400000000000001, 10, 'oss-config/index', 'system/oss/config', '', 'N', 'N', 'C', '1', '0', 'system:ossConfig:list', '#', '/system/oss', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000117, 'Admin监控', 1761400000000000002, 5, 'Admin', 'monitor/admin/index', '', 'N', 'Y', 'C', '0', '0', 'monitor:admin:list', 'dashboard', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, 'Admin监控菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000118, '文件管理', 1761400000000000001, 10, 'oss', 'system/oss/index', '', 'N', 'Y', 'C', '0', '0', 'system:oss:list', 'upload', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '文件管理菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000120, '任务调度中心', 1761400000000000002, 6, 'snailjob', 'monitor/snailjob/index', '', 'N', 'Y', 'C', '0', '0', 'monitor:snailjob:list', 'job', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, 'SnailJob控制台菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000121, 'AI控制台', 1761400000000000002, 7, 'snailai', 'monitor/snailai/index', '', 'N', 'Y', 'C', '0', '0', 'monitor:snailai:list', 'checkbox', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, 'AI控制台菜单');
-- 三级菜单
INSERT IGNORE INTO sys_menu VALUES (1761400000000000500, '操作日志', 1761400000000000108, 1, 'operlog', 'monitor/operlog/index', '', 'N', 'Y', 'C', '0', '0', 'monitor:operlog:list', 'form', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '操作日志菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000000501, '登录日志', 1761400000000000108, 2, 'logininfo', 'monitor/logininfo/index', '', 'N', 'Y', 'C', '0', '0', 'monitor:logininfo:list', 'logininfo', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '登录日志菜单');
-- 用户管理按钮
INSERT IGNORE INTO sys_menu VALUES (1761400000000001001, '用户查询', 1761400000000000100, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:user:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001002, '用户新增', 1761400000000000100, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:user:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001003, '用户修改', 1761400000000000100, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:user:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001004, '用户删除', 1761400000000000100, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:user:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001005, '用户导出', 1761400000000000100, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:user:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001006, '用户导入', 1761400000000000100, 6, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:user:import', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001007, '重置密码', 1761400000000000100, 7, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:user:resetPwd', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
-- 角色管理按钮
INSERT IGNORE INTO sys_menu VALUES (1761400000000001008, '角色查询', 1761400000000000101, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:role:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001009, '角色新增', 1761400000000000101, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:role:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001010, '角色修改', 1761400000000000101, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:role:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001011, '角色删除', 1761400000000000101, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:role:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001012, '角色导出', 1761400000000000101, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:role:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
-- 菜单管理按钮
INSERT IGNORE INTO sys_menu VALUES (1761400000000001013, '菜单查询', 1761400000000000102, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:menu:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001014, '菜单新增', 1761400000000000102, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:menu:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001015, '菜单修改', 1761400000000000102, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:menu:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001016, '菜单删除', 1761400000000000102, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:menu:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
-- 部门管理按钮
INSERT IGNORE INTO sys_menu VALUES (1761400000000001017, '部门查询', 1761400000000000103, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:dept:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001018, '部门新增', 1761400000000000103, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:dept:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001019, '部门修改', 1761400000000000103, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:dept:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001020, '部门删除', 1761400000000000103, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:dept:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
-- 岗位管理按钮
INSERT IGNORE INTO sys_menu VALUES (1761400000000001021, '岗位查询', 1761400000000000104, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:post:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001022, '岗位新增', 1761400000000000104, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:post:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001023, '岗位修改', 1761400000000000104, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:post:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001024, '岗位删除', 1761400000000000104, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:post:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001025, '岗位导出', 1761400000000000104, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:post:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
-- 字典管理按钮
INSERT IGNORE INTO sys_menu VALUES (1761400000000001026, '字典查询', 1761400000000000105, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:dict:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001027, '字典新增', 1761400000000000105, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:dict:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001028, '字典修改', 1761400000000000105, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:dict:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001029, '字典删除', 1761400000000000105, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:dict:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001030, '字典导出', 1761400000000000105, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:dict:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
-- 参数设置按钮
INSERT IGNORE INTO sys_menu VALUES (1761400000000001031, '参数查询', 1761400000000000106, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:config:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001032, '参数新增', 1761400000000000106, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:config:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001033, '参数修改', 1761400000000000106, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:config:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001034, '参数删除', 1761400000000000106, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:config:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001035, '参数导出', 1761400000000000106, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:config:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
-- 通知公告按钮
INSERT IGNORE INTO sys_menu VALUES (1761400000000001036, '公告查询', 1761400000000000107, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:notice:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001037, '公告新增', 1761400000000000107, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:notice:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001038, '公告修改', 1761400000000000107, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:notice:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001039, '公告删除', 1761400000000000107, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:notice:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
-- 操作日志按钮
INSERT IGNORE INTO sys_menu VALUES (1761400000000001040, '操作查询', 1761400000000000500, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:operlog:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001041, '操作删除', 1761400000000000500, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:operlog:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001042, '日志导出', 1761400000000000500, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:operlog:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
-- 登录日志按钮
INSERT IGNORE INTO sys_menu VALUES (1761400000000001043, '登录查询', 1761400000000000501, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:logininfo:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001044, '登录删除', 1761400000000000501, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:logininfo:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001045, '日志导出', 1761400000000000501, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:logininfo:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001050, '账户解锁', 1761400000000000501, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:logininfo:unlock', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
-- 在线用户按钮
INSERT IGNORE INTO sys_menu VALUES (1761400000000001046, '在线查询', 1761400000000000109, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:online:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001047, '批量强退', 1761400000000000109, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:online:batchLogout', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001048, '单条强退', 1761400000000000109, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:online:forceLogout', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
-- 代码生成按钮
INSERT IGNORE INTO sys_menu VALUES (1761400000000001055, '生成查询', 1761400000000000115, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'tool:gen:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001056, '生成修改', 1761400000000000115, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'tool:gen:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001057, '生成删除', 1761400000000000115, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'tool:gen:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001058, '导入代码', 1761400000000000115, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'tool:gen:import', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001059, '预览代码', 1761400000000000115, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'tool:gen:preview', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001060, '生成代码', 1761400000000000115, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'tool:gen:code', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
-- OSS相关按钮
INSERT IGNORE INTO sys_menu VALUES (1761400000000001600, '文件查询', 1761400000000000118, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:oss:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001601, '文件上传', 1761400000000000118, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:oss:upload', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001602, '文件下载', 1761400000000000118, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:oss:download', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001603, '文件删除', 1761400000000000118, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:oss:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001620, '配置列表', 1761400000000000118, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:ossConfig:list', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001621, '配置添加', 1761400000000000118, 6, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:ossConfig:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001622, '配置编辑', 1761400000000000118, 6, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:ossConfig:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001623, '配置删除', 1761400000000000118, 6, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:ossConfig:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
-- 客户端管理按钮
INSERT IGNORE INTO sys_menu VALUES (1761400000000001061, '客户端管理查询', 1761400000000000123, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:client:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001062, '客户端管理新增', 1761400000000000123, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:client:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001063, '客户端管理修改', 1761400000000000123, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:client:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001064, '客户端管理删除', 1761400000000000123, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:client:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001065, '客户端管理导出', 1761400000000000123, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:client:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
-- 测试菜单
INSERT IGNORE INTO sys_menu VALUES (1761400000000001500, '测试单表', 1761400000000000005, 1, 'demo', 'demo/demo/index', '', 'N', 'Y', 'C', '0', '0', 'demo:demo:list', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '测试单表菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001501, '测试单表查询', 1761400000000001500, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'demo:demo:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001502, '测试单表新增', 1761400000000001500, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'demo:demo:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001503, '测试单表修改', 1761400000000001500, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'demo:demo:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001504, '测试单表删除', 1761400000000001500, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'demo:demo:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001505, '测试单表导出', 1761400000000001500, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'demo:demo:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001506, '测试树表', 1761400000000000005, 1, 'tree', 'demo/tree/index', '', 'N', 'Y', 'C', '0', '0', 'demo:tree:list', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '测试树表菜单');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001507, '测试树表查询', 1761400000000001506, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'demo:tree:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001508, '测试树表新增', 1761400000000001506, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'demo:tree:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001509, '测试树表修改', 1761400000000001506, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'demo:tree:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001510, '测试树表删除', 1761400000000001506, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'demo:tree:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');
INSERT IGNORE INTO sys_menu VALUES (1761400000000001511, '测试树表导出', 1761400000000001506, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'demo:tree:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');

-- ============================================================
-- 7. 用户和角色关联表（用户N-1角色）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user_role
(
    user_id BIGINT(20) NOT NULL COMMENT '用户ID',
    role_id BIGINT(20) NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    KEY idx_sys_user_role_rid (role_id)
) ENGINE=InnoDB COMMENT = '用户和角色关联表';

INSERT IGNORE INTO sys_user_role VALUES (1761100000000000001, 1761300000000000001);
INSERT IGNORE INTO sys_user_role VALUES (1761100000000000003, 1761300000000000003);
INSERT IGNORE INTO sys_user_role VALUES (1761100000000000004, 1761300000000000004);

-- ============================================================
-- 8. 角色和菜单关联表（角色1-N菜单）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role_menu
(
    role_id BIGINT(20) NOT NULL COMMENT '角色ID',
    menu_id BIGINT(20) NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB COMMENT = '角色和菜单关联表';

-- 超级管理员不需要单独配置角色菜单（拥有所有权限），以下为测试角色的菜单关联
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000001);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000005);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000100);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000101);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000102);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000103);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000104);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000105);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000106);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000107);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000108);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000118);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000123);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000130);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000131);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000133);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000500);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000000501);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001001);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001002);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001003);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001004);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001005);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001006);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001007);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001008);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001009);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001010);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001011);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001012);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001013);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001014);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001015);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001016);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001017);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001018);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001019);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001020);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001021);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001022);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001023);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001024);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001025);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001026);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001027);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001028);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001029);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001030);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001031);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001032);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001033);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001034);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001035);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001036);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001037);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001038);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001039);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001040);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001041);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001042);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001043);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001044);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001045);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001050);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001061);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001062);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001063);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001064);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001065);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001500);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001501);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001502);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001503);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001504);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001505);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001506);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001507);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001508);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001509);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001510);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001511);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001600);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001601);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001602);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001603);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001620);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001621);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001622);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000003, 1761400000000001623);
-- 仅本人角色
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000004, 1761400000000000005);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000004, 1761400000000001500);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000004, 1761400000000001501);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000004, 1761400000000001502);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000004, 1761400000000001503);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000004, 1761400000000001504);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000004, 1761400000000001505);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000004, 1761400000000001506);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000004, 1761400000000001507);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000004, 1761400000000001508);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000004, 1761400000000001509);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000004, 1761400000000001510);
INSERT IGNORE INTO sys_role_menu VALUES (1761300000000000004, 1761400000000001511);

-- ============================================================
-- 9. 角色和部门关联表（角色1-N部门）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role_dept
(
    role_id BIGINT(20) NOT NULL COMMENT '角色ID',
    dept_id BIGINT(20) NOT NULL COMMENT '部门ID',
    PRIMARY KEY (role_id, dept_id)
) ENGINE=InnoDB COMMENT = '角色和部门关联表';

-- ============================================================
-- 10. 用户与岗位关联表（用户1-N岗位）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user_post
(
    user_id BIGINT(20) NOT NULL COMMENT '用户ID',
    post_id BIGINT(20) NOT NULL COMMENT '岗位ID',
    PRIMARY KEY (user_id, post_id)
) ENGINE=InnoDB COMMENT = '用户与岗位关联表';

INSERT IGNORE INTO sys_user_post VALUES (1761100000000000001, 1761200000000000001);

-- ============================================================
-- 11. 操作日志记录
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_oper_log
(
    oper_id        BIGINT(20)    NOT NULL                   COMMENT '日志主键',
    title          VARCHAR(50)   DEFAULT ''                 COMMENT '模块标题',
    business_type  INT(2)        DEFAULT 0                  COMMENT '业务类型（0其它 1新增 2修改 3删除）',
    method         VARCHAR(100)  DEFAULT ''                 COMMENT '方法名称',
    request_method VARCHAR(10)   DEFAULT ''                 COMMENT '请求方式',
    operator_type  INT(1)        DEFAULT 0                  COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
    oper_name      VARCHAR(50)   DEFAULT ''                 COMMENT '操作人员',
    user_id        BIGINT(20)    DEFAULT NULL               COMMENT '操作用户ID',
    dept_id        BIGINT(20)    DEFAULT NULL               COMMENT '操作部门ID',
    dept_name      VARCHAR(50)   DEFAULT ''                 COMMENT '部门名称',
    client_key     VARCHAR(32)   DEFAULT ''                 COMMENT '客户端',
    device_type    VARCHAR(32)   DEFAULT ''                 COMMENT '设备类型',
    browser        VARCHAR(50)   DEFAULT ''                 COMMENT '浏览器类型',
    os             VARCHAR(50)   DEFAULT ''                 COMMENT '操作系统',
    oper_url       VARCHAR(255)  DEFAULT ''                 COMMENT '请求URL',
    oper_ip        VARCHAR(128)  DEFAULT ''                 COMMENT '主机地址',
    oper_location  VARCHAR(255)  DEFAULT ''                 COMMENT '操作地点',
    oper_param     VARCHAR(4000) DEFAULT ''                 COMMENT '请求参数',
    json_result    VARCHAR(4000) DEFAULT ''                 COMMENT '返回参数',
    status         INT(1)        DEFAULT 0                  COMMENT '操作状态（0正常 1异常）',
    error_msg      VARCHAR(4000) DEFAULT ''                 COMMENT '错误消息',
    oper_time      DATETIME                                 COMMENT '操作时间',
    cost_time      BIGINT(20)    DEFAULT 0                  COMMENT '消耗时间',
    PRIMARY KEY (oper_id),
    KEY idx_sys_oper_log_bt (business_type),
    KEY idx_sys_oper_log_uid (user_id),
    KEY idx_sys_oper_log_s  (status),
    KEY idx_sys_oper_log_ot (oper_time)
) ENGINE=InnoDB COMMENT = '操作日志记录';

-- ============================================================
-- 12. 字典类型表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_dict_type
(
    dict_id     BIGINT(20)   NOT NULL                   COMMENT '字典主键',
    dict_name   VARCHAR(100) DEFAULT ''                 COMMENT '字典名称',
    dict_type   VARCHAR(100) DEFAULT ''                 COMMENT '字典类型',
    create_dept BIGINT(20)   DEFAULT NULL               COMMENT '创建部门',
    create_by   BIGINT(20)   DEFAULT NULL               COMMENT '创建者',
    create_time DATETIME                                COMMENT '创建时间',
    update_by   BIGINT(20)   DEFAULT NULL               COMMENT '更新者',
    update_time DATETIME                                COMMENT '更新时间',
    remark      VARCHAR(500) DEFAULT NULL               COMMENT '备注',
    PRIMARY KEY (dict_id),
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB COMMENT = '字典类型表';

INSERT IGNORE INTO sys_dict_type VALUES (1761500000000000001, '用户性别', 'sys_user_gender', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '用户性别列表');
INSERT IGNORE INTO sys_dict_type VALUES (1761500000000000002, '菜单状态', 'sys_show_hide', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '菜单状态列表');
INSERT IGNORE INTO sys_dict_type VALUES (1761500000000000003, '系统开关', 'sys_normal_disable', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '系统开关列表');
INSERT IGNORE INTO sys_dict_type VALUES (1761500000000000006, '系统是否', 'sys_yes_no', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '系统是否列表');
INSERT IGNORE INTO sys_dict_type VALUES (1761500000000000007, '通知类型', 'sys_notice_type', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '通知类型列表');
INSERT IGNORE INTO sys_dict_type VALUES (1761500000000000008, '通知状态', 'sys_notice_status', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '通知状态列表');
INSERT IGNORE INTO sys_dict_type VALUES (1761500000000000009, '操作类型', 'sys_oper_type', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '操作类型列表');
INSERT IGNORE INTO sys_dict_type VALUES (1761500000000000010, '系统状态', 'sys_common_status', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '登录状态列表');
INSERT IGNORE INTO sys_dict_type VALUES (1761500000000000011, '授权类型', 'sys_grant_type', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '认证授权类型');
INSERT IGNORE INTO sys_dict_type VALUES (1761500000000000012, '设备类型', 'sys_device_type', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '客户端设备类型');

-- ============================================================
-- 13. 字典数据表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_dict_data
(
    dict_code   BIGINT(20)   NOT NULL                   COMMENT '字典编码',
    dict_sort   INT(4)       DEFAULT 0                  COMMENT '字典排序',
    dict_label  VARCHAR(100) DEFAULT ''                 COMMENT '字典标签',
    dict_value  VARCHAR(100) DEFAULT ''                 COMMENT '字典键值',
    dict_type   VARCHAR(100) DEFAULT ''                 COMMENT '字典类型',
    css_class   VARCHAR(100) DEFAULT NULL               COMMENT '样式属性（其他样式扩展）',
    list_class  VARCHAR(100) DEFAULT NULL               COMMENT '表格回显样式',
    is_default  CHAR(1)      DEFAULT 'N'                COMMENT '是否默认（Y是 N否）',
    create_dept BIGINT(20)   DEFAULT NULL               COMMENT '创建部门',
    create_by   BIGINT(20)   DEFAULT NULL               COMMENT '创建者',
    create_time DATETIME                                COMMENT '创建时间',
    update_by   BIGINT(20)   DEFAULT NULL               COMMENT '更新者',
    update_time DATETIME                                COMMENT '更新时间',
    remark      VARCHAR(500) DEFAULT NULL               COMMENT '备注',
    PRIMARY KEY (dict_code),
    KEY idx_sys_dict_data_type (dict_type)
) ENGINE=InnoDB COMMENT = '字典数据表';

INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000001, 1, '男', '0', 'sys_user_gender', '', '', 'Y', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '性别男');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000002, 2, '女', '1', 'sys_user_gender', '', '', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '性别女');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000003, 3, '未知', '2', 'sys_user_gender', '', '', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '性别未知');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000004, 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '显示菜单');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000005, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '隐藏菜单');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000006, 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '正常状态');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000007, 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '停用状态');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000012, 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '系统默认是');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000013, 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '系统默认否');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000014, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '通知');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000015, 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '公告');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000016, 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '正常状态');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000017, 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '关闭状态');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000029, 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '其他操作');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000018, 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '新增操作');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000019, 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '修改操作');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000020, 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '删除操作');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000021, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '授权操作');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000022, 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '导出操作');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000023, 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '导入操作');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000024, 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '强退操作');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000025, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '生成操作');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000026, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '清空操作');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000027, 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '正常状态');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000028, 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '停用状态');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000030, 0, '密码认证', 'password', 'sys_grant_type', 'el-check-tag', 'default', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '密码认证');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000031, 0, '短信认证', 'sms', 'sys_grant_type', 'el-check-tag', 'default', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '短信认证');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000032, 0, '邮件认证', 'email', 'sys_grant_type', 'el-check-tag', 'default', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '邮件认证');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000033, 0, '小程序认证', 'xcx', 'sys_grant_type', 'el-check-tag', 'default', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '小程序认证');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000034, 0, '三方登录认证', 'social', 'sys_grant_type', 'el-check-tag', 'default', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '三方登录认证');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000035, 0, 'PC', 'pc', 'sys_device_type', '', 'default', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, 'PC');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000036, 0, '安卓', 'android', 'sys_device_type', '', 'default', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '安卓');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000037, 0, 'iOS', 'ios', 'sys_device_type', '', 'default', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, 'iOS');
INSERT IGNORE INTO sys_dict_data VALUES (1761600000000000038, 0, '小程序', 'xcx', 'sys_device_type', '', 'default', 'N', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '小程序');

-- ============================================================
-- 14. 参数配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_config
(
    config_id    BIGINT(20)   NOT NULL                   COMMENT '参数主键',
    config_name  VARCHAR(100) DEFAULT ''                 COMMENT '参数名称',
    config_key   VARCHAR(100) DEFAULT ''                 COMMENT '参数键名',
    config_value VARCHAR(500) DEFAULT ''                 COMMENT '参数键值',
    config_type  CHAR(1)      DEFAULT 'N'                COMMENT '系统内置（Y是 N否）',
    create_dept  BIGINT(20)   DEFAULT NULL               COMMENT '创建部门',
    create_by    BIGINT(20)   DEFAULT NULL               COMMENT '创建者',
    create_time  DATETIME                                COMMENT '创建时间',
    update_by    BIGINT(20)   DEFAULT NULL               COMMENT '更新者',
    update_time  DATETIME                                COMMENT '更新时间',
    remark       VARCHAR(500) DEFAULT NULL               COMMENT '备注',
    PRIMARY KEY (config_id)
) ENGINE=InnoDB COMMENT = '参数配置表';

INSERT IGNORE INTO sys_config VALUES (1761700000000000001, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '初始化密码 123456');
INSERT IGNORE INTO sys_config VALUES (1761700000000000002, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '是否开启注册用户功能（true开启，false关闭）');
INSERT IGNORE INTO sys_config VALUES (1761700000000000003, 'OSS预览列表资源开关', 'sys.oss.previewListResource', 'true', 'Y', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, 'true:开启, false:关闭');

-- ============================================================
-- 15. 系统访问记录（登录日志）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_login_info
(
    info_id        BIGINT(20)   NOT NULL                  COMMENT '访问ID',
    user_name      VARCHAR(50)  DEFAULT ''                COMMENT '用户账号',
    client_key     VARCHAR(32)  DEFAULT ''                COMMENT '客户端',
    device_type    VARCHAR(32)  DEFAULT ''                COMMENT '设备类型',
    ipaddr         VARCHAR(128) DEFAULT ''                COMMENT '登录IP地址',
    login_location VARCHAR(255) DEFAULT ''                COMMENT '登录地点',
    browser        VARCHAR(50)  DEFAULT ''                COMMENT '浏览器类型',
    os             VARCHAR(50)  DEFAULT ''                COMMENT '操作系统',
    status         CHAR(1)      DEFAULT '0'               COMMENT '登录状态（0正常 1异常）',
    msg            VARCHAR(255) DEFAULT ''                COMMENT '提示消息',
    login_time     DATETIME                               COMMENT '访问时间',
    PRIMARY KEY (info_id),
    KEY idx_sys_login_info_s  (status),
    KEY idx_sys_login_info_lt (login_time)
) ENGINE=InnoDB COMMENT = '系统访问记录';

-- ============================================================
-- 16. 通知公告表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_notice
(
    notice_id      BIGINT(20)   NOT NULL                   COMMENT '公告ID',
    notice_title   VARCHAR(50)  NOT NULL                   COMMENT '公告标题',
    notice_type    CHAR(1)      NOT NULL                   COMMENT '公告类型（1通知 2公告）',
    notice_content LONGBLOB     DEFAULT NULL               COMMENT '公告内容',
    status         CHAR(1)      DEFAULT '0'                COMMENT '公告状态（0正常 1关闭）',
    create_dept    BIGINT(20)   DEFAULT NULL               COMMENT '创建部门',
    create_by      BIGINT(20)   DEFAULT NULL               COMMENT '创建者',
    create_time    DATETIME                                COMMENT '创建时间',
    update_by      BIGINT(20)   DEFAULT NULL               COMMENT '更新者',
    update_time    DATETIME                                COMMENT '更新时间',
    remark         VARCHAR(255) DEFAULT NULL               COMMENT '备注',
    PRIMARY KEY (notice_id)
) ENGINE=InnoDB COMMENT = '通知公告表';

INSERT IGNORE INTO sys_notice VALUES (1761800000000000001, '温馨提醒：2018-07-01 新版本发布啦', '2', '新版本内容', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '管理员');
INSERT IGNORE INTO sys_notice VALUES (1761800000000000002, '维护通知：2018-07-01 系统凌晨维护', '1', '维护内容', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '管理员');

-- ============================================================
-- 17. 消息记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_message
(
    message_id    BIGINT(20)    NOT NULL                   COMMENT '消息ID',
    category      VARCHAR(20)   NOT NULL                   COMMENT '消息分组(system/notice/workflow)',
    type          VARCHAR(20)   NOT NULL                   COMMENT '消息类型',
    source        VARCHAR(20)   NOT NULL                   COMMENT '消息来源',
    title         VARCHAR(100)  DEFAULT ''                 COMMENT '标题',
    message       VARCHAR(500)  DEFAULT ''                 COMMENT '摘要消息',
    content       LONGTEXT                                 COMMENT '详细内容',
    data_json     LONGTEXT                                 COMMENT '扩展数据JSON',
    path          VARCHAR(500)  DEFAULT NULL               COMMENT '前端跳转路径',
    send_user_ids VARCHAR(2000) NOT NULL DEFAULT '0'       COMMENT '目标用户ID串，0表示全局',
    create_dept   BIGINT(20)    DEFAULT NULL               COMMENT '创建部门',
    create_by     BIGINT(20)    DEFAULT NULL               COMMENT '创建者',
    create_time   DATETIME                                 COMMENT '创建时间',
    update_by     BIGINT(20)    DEFAULT NULL               COMMENT '更新者',
    update_time   DATETIME                                 COMMENT '更新时间',
    PRIMARY KEY (message_id),
    KEY idx_sys_message_category_time (category, create_time)
) ENGINE=InnoDB COMMENT = '消息记录表';

-- ============================================================
-- 18. 代码生成业务表
-- ============================================================
CREATE TABLE IF NOT EXISTS gen_table
(
    table_id        BIGINT(20)   NOT NULL                   COMMENT '编号',
    data_name       VARCHAR(200) DEFAULT ''                 COMMENT '数据源名称',
    table_name      VARCHAR(200) DEFAULT ''                 COMMENT '表名称',
    table_comment   VARCHAR(500) DEFAULT ''                 COMMENT '表描述',
    class_name      VARCHAR(100) DEFAULT ''                 COMMENT '实体类名称',
    tpl_category    VARCHAR(200) DEFAULT 'crud'             COMMENT '使用的模板（crud单表操作 tree树表操作）',
    frontend_type   VARCHAR(50)  DEFAULT 'vue'              COMMENT '前端模板类型',
    package_name    VARCHAR(100)                            COMMENT '生成包路径',
    module_name     VARCHAR(30)                             COMMENT '生成模块名',
    business_name   VARCHAR(30)                             COMMENT '生成业务名',
    function_name   VARCHAR(50)                             COMMENT '生成功能名',
    function_author VARCHAR(50)                             COMMENT '生成功能作者',
    gen_type        CHAR(1)      DEFAULT '0'                COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
    gen_path        VARCHAR(200) DEFAULT '/'                COMMENT '生成路径（不填默认项目路径）',
    options         VARCHAR(1000)                           COMMENT '其它生成选项',
    create_dept     BIGINT(20)   DEFAULT NULL               COMMENT '创建部门',
    create_by       BIGINT(20)   DEFAULT NULL               COMMENT '创建者',
    create_time     DATETIME                                COMMENT '创建时间',
    update_by       BIGINT(20)   DEFAULT NULL               COMMENT '更新者',
    update_time     DATETIME                                COMMENT '更新时间',
    remark          VARCHAR(500) DEFAULT NULL               COMMENT '备注',
    PRIMARY KEY (table_id)
) ENGINE=InnoDB COMMENT = '代码生成业务表';

-- ============================================================
-- 19. 代码生成业务表字段
-- ============================================================
CREATE TABLE IF NOT EXISTS gen_table_column
(
    column_id      BIGINT(20)   NOT NULL                   COMMENT '编号',
    table_id       BIGINT(20)                              COMMENT '归属表编号',
    column_name    VARCHAR(200)                            COMMENT '列名称',
    column_comment VARCHAR(500)                            COMMENT '列描述',
    column_type    VARCHAR(100)                            COMMENT '列类型',
    java_type      VARCHAR(500)                            COMMENT 'JAVA类型',
    java_field     VARCHAR(200)                            COMMENT 'JAVA字段名',
    is_pk          CHAR(1)                                 COMMENT '是否主键（1是）',
    is_increment   CHAR(1)                                 COMMENT '是否自增（1是）',
    is_required    CHAR(1)                                 COMMENT '是否必填（1是）',
    is_insert      CHAR(1)                                 COMMENT '是否为插入字段（1是）',
    is_edit        CHAR(1)                                 COMMENT '是否编辑字段（1是）',
    is_list        CHAR(1)                                 COMMENT '是否列表字段（1是）',
    is_query       CHAR(1)                                 COMMENT '是否查询字段（1是）',
    query_type     VARCHAR(200) DEFAULT 'EQ'               COMMENT '查询方式',
    html_type      VARCHAR(200)                            COMMENT '显示类型',
    dict_type      VARCHAR(200) DEFAULT ''                 COMMENT '字典类型',
    sort           INT                                     COMMENT '排序',
    create_dept    BIGINT(20)   DEFAULT NULL               COMMENT '创建部门',
    create_by      BIGINT(20)   DEFAULT NULL               COMMENT '创建者',
    create_time    DATETIME                                COMMENT '创建时间',
    update_by      BIGINT(20)   DEFAULT NULL               COMMENT '更新者',
    update_time    DATETIME                                COMMENT '更新时间',
    PRIMARY KEY (column_id)
) ENGINE=InnoDB COMMENT = '代码生成业务表字段';

-- ============================================================
-- 20. OSS对象存储表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_oss
(
    oss_id        BIGINT(20)   NOT NULL                   COMMENT '对象存储主键',
    file_name     VARCHAR(255) NOT NULL DEFAULT ''        COMMENT '文件名',
    original_name VARCHAR(255) NOT NULL DEFAULT ''        COMMENT '原名',
    file_suffix   VARCHAR(10)  NOT NULL DEFAULT ''        COMMENT '文件后缀名',
    url           VARCHAR(500) NOT NULL                   COMMENT 'URL地址',
    ext1          TEXT                  DEFAULT NULL      COMMENT '扩展字段',
    create_dept   BIGINT(20)            DEFAULT NULL      COMMENT '创建部门',
    create_time   DATETIME              DEFAULT NULL      COMMENT '创建时间',
    create_by     BIGINT(20)            DEFAULT NULL      COMMENT '上传人',
    update_time   DATETIME              DEFAULT NULL      COMMENT '更新时间',
    update_by     BIGINT(20)            DEFAULT NULL      COMMENT '更新人',
    service       VARCHAR(20)  NOT NULL DEFAULT 'minio'   COMMENT '服务商',
    PRIMARY KEY (oss_id)
) ENGINE=InnoDB COMMENT = 'OSS对象存储表';

-- ============================================================
-- 21. OSS对象存储配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_oss_config
(
    oss_config_id BIGINT(20)  NOT NULL                  COMMENT '主键',
    config_key    VARCHAR(20) NOT NULL DEFAULT ''       COMMENT '配置key',
    access_key    VARCHAR(255)         DEFAULT ''       COMMENT 'accessKey',
    secret_key    VARCHAR(255)         DEFAULT ''       COMMENT '秘钥',
    bucket_name   VARCHAR(255)         DEFAULT ''       COMMENT '桶名称',
    prefix        VARCHAR(255)         DEFAULT ''       COMMENT '前缀',
    endpoint      VARCHAR(255)         DEFAULT ''       COMMENT '访问站点',
    domain_url    VARCHAR(255)         DEFAULT ''       COMMENT '自定义域名',
    is_https      CHAR(1)              DEFAULT 'N'      COMMENT '是否https（Y=是,N=否）',
    region        VARCHAR(255)         DEFAULT ''       COMMENT '域',
    access_policy CHAR(1)     NOT NULL DEFAULT '1'      COMMENT '桶权限类型(0=private 1=public 2=custom)',
    status        CHAR(1)              DEFAULT 'N'      COMMENT '是否默认（Y=是,N=否）',
    ext1          VARCHAR(255)         DEFAULT ''       COMMENT '扩展字段',
    create_dept   BIGINT(20)           DEFAULT NULL     COMMENT '创建部门',
    create_by     BIGINT(20)           DEFAULT NULL     COMMENT '创建者',
    create_time   DATETIME             DEFAULT NULL     COMMENT '创建时间',
    update_by     BIGINT(20)           DEFAULT NULL     COMMENT '更新者',
    update_time   DATETIME             DEFAULT NULL     COMMENT '更新时间',
    remark        VARCHAR(500)         DEFAULT NULL     COMMENT '备注',
    PRIMARY KEY (oss_config_id)
) ENGINE=InnoDB COMMENT = '对象存储配置表';

INSERT IGNORE INTO sys_oss_config VALUES (1761900000000000001, 'minio', 'ruoyi', 'ruoyi123', 'ruoyi', '', '127.0.0.1:9000', '', 'N', '', '1', 'Y', '', 1761000000000000103, 1761100000000000001, SYSDATE(), 1761100000000000001, SYSDATE(), NULL);
INSERT IGNORE INTO sys_oss_config VALUES (1761900000000000002, 'qiniu', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi', '', 's3-cn-north-1.qiniucs.com', '', 'N', '', '1', 'N', '', 1761000000000000103, 1761100000000000001, SYSDATE(), 1761100000000000001, SYSDATE(), NULL);
INSERT IGNORE INTO sys_oss_config VALUES (1761900000000000003, 'aliyun', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi', '', 'oss-cn-beijing.aliyuncs.com', '', 'N', '', '1', 'N', '', 1761000000000000103, 1761100000000000001, SYSDATE(), 1761100000000000001, SYSDATE(), NULL);
INSERT IGNORE INTO sys_oss_config VALUES (1761900000000000004, 'qcloud', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi-1240000000', '', 'cos.ap-beijing.myqcloud.com', '', 'N', 'ap-beijing', '1', 'N', '', 1761000000000000103, 1761100000000000001, SYSDATE(), 1761100000000000001, SYSDATE(), NULL);
INSERT IGNORE INTO sys_oss_config VALUES (1761900000000000005, 'image', 'ruoyi', 'ruoyi123', 'ruoyi', 'image', '127.0.0.1:9000', '', 'N', '', '1', 'N', '', 1761000000000000103, 1761100000000000001, SYSDATE(), 1761100000000000001, SYSDATE(), NULL);

-- ============================================================
-- 22. 系统授权表（客户端管理）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_client
(
    id              BIGINT(20)    NOT NULL            COMMENT 'id',
    client_id       VARCHAR(64)   DEFAULT NULL        COMMENT '客户端id',
    client_key      VARCHAR(32)   DEFAULT NULL        COMMENT '客户端key',
    client_secret   VARCHAR(255)  DEFAULT NULL        COMMENT '客户端秘钥',
    grant_type      VARCHAR(255)  DEFAULT NULL        COMMENT '授权类型',
    device_type     VARCHAR(32)   DEFAULT NULL        COMMENT '设备类型',
    access_path     VARCHAR(2000) DEFAULT NULL        COMMENT '允许访问路径',
    ip_whitelist    VARCHAR(1000) DEFAULT NULL        COMMENT 'IP白名单',
    active_timeout  INT(11)       DEFAULT 1800        COMMENT 'token活跃超时时间',
    timeout         INT(11)       DEFAULT 604800      COMMENT 'token固定超时',
    status          CHAR(1)       DEFAULT '0'         COMMENT '状态（0正常 1停用）',
    del_flag        CHAR(1)       DEFAULT '0'         COMMENT '删除标志（0代表存在 1代表删除）',
    create_dept     BIGINT(20)    DEFAULT NULL        COMMENT '创建部门',
    create_by       BIGINT(20)    DEFAULT NULL        COMMENT '创建者',
    create_time     DATETIME      DEFAULT NULL        COMMENT '创建时间',
    update_by       BIGINT(20)    DEFAULT NULL        COMMENT '更新者',
    update_time     DATETIME      DEFAULT NULL        COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT = '系统授权表';

INSERT IGNORE INTO sys_client VALUES (1762000000000000001, 'e5cd7e4891bf95d1d19206ce24a7b32e', 'pc', 'pc123', 'password,social', 'pc', NULL, NULL, 1800, 604800, '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), 1761100000000000001, SYSDATE());
INSERT IGNORE INTO sys_client VALUES (1762000000000000002, '428a8310cd442757ae699df5d894f051', 'app', 'app123', 'password,sms,social', 'android', '/app/**', NULL, 1800, 604800, '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), 1761100000000000001, SYSDATE());

-- ============================================================
-- 23. 测试单表
-- ============================================================
CREATE TABLE IF NOT EXISTS test_demo
(
    id          BIGINT(0)    NOT NULL COMMENT '主键',
    dept_id     BIGINT(0)    NULL DEFAULT NULL COMMENT '部门id',
    user_id     BIGINT(0)    NULL DEFAULT NULL COMMENT '用户id',
    order_num   INT(0)       NULL DEFAULT 0 COMMENT '排序号',
    test_key    VARCHAR(255) NULL DEFAULT NULL COMMENT 'key键',
    value       VARCHAR(255) NULL DEFAULT NULL COMMENT '值',
    version     INT(0)       NULL DEFAULT 0 COMMENT '版本',
    create_dept BIGINT(0)    NULL DEFAULT NULL COMMENT '创建部门',
    create_time DATETIME(0)  NULL DEFAULT NULL COMMENT '创建时间',
    create_by   BIGINT(0)    NULL DEFAULT NULL COMMENT '创建人',
    update_time DATETIME(0)  NULL DEFAULT NULL COMMENT '更新时间',
    update_by   BIGINT(0)    NULL DEFAULT NULL COMMENT '更新人',
    del_flag    INT(0)       NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id) USING BTREE
) ENGINE = InnoDB COMMENT = '测试单表';

INSERT IGNORE INTO test_demo VALUES (1762100000000000001, 1761000000000000102, 1761100000000000004, 1, '测试数据权限', '测试', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_demo VALUES (1762100000000000002, 1761000000000000102, 1761100000000000003, 2, '子节点1', '111', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_demo VALUES (1762100000000000003, 1761000000000000102, 1761100000000000003, 3, '子节点2', '222', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_demo VALUES (1762100000000000004, 1761000000000000108, 1761100000000000004, 4, '测试数据', 'demo', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_demo VALUES (1762100000000000005, 1761000000000000108, 1761100000000000003, 13, '子节点11', '1111', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_demo VALUES (1762100000000000006, 1761000000000000108, 1761100000000000003, 12, '子节点22', '2222', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_demo VALUES (1762100000000000007, 1761000000000000108, 1761100000000000003, 11, '子节点33', '3333', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_demo VALUES (1762100000000000008, 1761000000000000108, 1761100000000000003, 10, '子节点44', '4444', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_demo VALUES (1762100000000000009, 1761000000000000108, 1761100000000000003, 9, '子节点55', '5555', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_demo VALUES (1762100000000000010, 1761000000000000108, 1761100000000000003, 8, '子节点66', '6666', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_demo VALUES (1762100000000000011, 1761000000000000108, 1761100000000000003, 7, '子节点77', '7777', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_demo VALUES (1762100000000000012, 1761000000000000108, 1761100000000000003, 6, '子节点88', '8888', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_demo VALUES (1762100000000000013, 1761000000000000108, 1761100000000000003, 5, '子节点99', '9999', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);

-- ============================================================
-- 24. 测试树表
-- ============================================================
CREATE TABLE IF NOT EXISTS test_tree
(
    id          BIGINT(0)    NOT NULL COMMENT '主键',
    parent_id   BIGINT(0)    NULL DEFAULT 0 COMMENT '父id',
    dept_id     BIGINT(0)    NULL DEFAULT NULL COMMENT '部门id',
    user_id     BIGINT(0)    NULL DEFAULT NULL COMMENT '用户id',
    tree_name   VARCHAR(255) NULL DEFAULT NULL COMMENT '值',
    version     INT(0)       NULL DEFAULT 0 COMMENT '版本',
    create_dept BIGINT(0)    NULL DEFAULT NULL COMMENT '创建部门',
    create_time DATETIME(0)  NULL DEFAULT NULL COMMENT '创建时间',
    create_by   BIGINT(0)    NULL DEFAULT NULL COMMENT '创建人',
    update_time DATETIME(0)  NULL DEFAULT NULL COMMENT '更新时间',
    update_by   BIGINT(0)    NULL DEFAULT NULL COMMENT '更新人',
    del_flag    INT(0)       NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id) USING BTREE
) ENGINE = InnoDB COMMENT = '测试树表';

INSERT IGNORE INTO test_tree VALUES (1762200000000000001, 0, 1761000000000000102, 1761100000000000004, '测试数据权限', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_tree VALUES (1762200000000000002, 1762200000000000001, 1761000000000000102, 1761100000000000003, '子节点1', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_tree VALUES (1762200000000000003, 1762200000000000002, 1761000000000000102, 1761100000000000003, '子节点2', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_tree VALUES (1762200000000000004, 0, 1761000000000000108, 1761100000000000004, '测试树1', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_tree VALUES (1762200000000000005, 1762200000000000004, 1761000000000000108, 1761100000000000003, '子节点11', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_tree VALUES (1762200000000000006, 1762200000000000004, 1761000000000000108, 1761100000000000003, '子节点22', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_tree VALUES (1762200000000000007, 1762200000000000004, 1761000000000000108, 1761100000000000003, '子节点33', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_tree VALUES (1762200000000000008, 1762200000000000005, 1761000000000000108, 1761100000000000003, '子节点44', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_tree VALUES (1762200000000000009, 1762200000000000006, 1761000000000000108, 1761100000000000003, '子节点55', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_tree VALUES (1762200000000000010, 1762200000000000007, 1761000000000000108, 1761100000000000003, '子节点66', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_tree VALUES (1762200000000000011, 1762200000000000007, 1761000000000000108, 1761100000000000003, '子节点77', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_tree VALUES (1762200000000000012, 1762200000000000010, 1761000000000000108, 1761100000000000003, '子节点88', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);
INSERT IGNORE INTO test_tree VALUES (1762200000000000013, 1762200000000000010, 1761000000000000108, 1761100000000000003, '子节点99', 0, 1761000000000000103, SYSDATE(), 1761100000000000001, NULL, NULL, 0);

SET FOREIGN_KEY_CHECKS = 1;

-- 注意：本文件只定义 RuoYi 基础表；QuantPF quantpf_* 业务表请通过 db/migration/V1_*__*.sql 管理。

-- ============================================================
-- 文件内容准备完成：24 张 RuoYi 基础表定义与初始化数据（本文件未执行 SQL）。
-- 执行前请确认目标 schema 为 quantpf，并先完成 schema diff/备份。
-- 共包含 24 张 RuoYi 基础表；QuantPF quantpf_* 业务表不在本脚本范围内
-- 表清单:
--   1. sys_social       - 社会化关系表
--   2. sys_dept         - 部门表
--   3. sys_user         - 用户信息表
--   4. sys_post         - 岗位信息表
--   5. sys_role         - 角色信息表
--   6. sys_menu         - 菜单权限表
--   7. sys_user_role    - 用户角色关联表
--   8. sys_role_menu    - 角色菜单关联表
--   9. sys_role_dept    - 角色部门关联表
--  10. sys_user_post    - 用户岗位关联表
--  11. sys_oper_log     - 操作日志记录
--  12. sys_dict_type    - 字典类型表
--  13. sys_dict_data    - 字典数据表
--  14. sys_config       - 参数配置表
--  15. sys_login_info   - 登录日志表
--  16. sys_notice       - 通知公告表
--  17. sys_message      - 消息记录表
--  18. gen_table        - 代码生成业务表
--  19. gen_table_column - 代码生成业务表字段
--  20. sys_oss          - OSS对象存储表
--  21. sys_oss_config   - OSS配置表
--  22. sys_client       - 系统授权表
--  23. test_demo        - 测试单表
--  24. test_tree        - 测试树表
-- ============================================================

