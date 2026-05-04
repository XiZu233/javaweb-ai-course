-- Tlias Pro 智能人事管理平台 - 数据库初始化脚本
-- 版本B：AI增强实战版
-- 基于yudao-boot-mini精简版改造

CREATE DATABASE IF NOT EXISTS tlias_pro_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tlias_pro_db;

-- =============================
-- 1. 部门表
-- =============================
DROP TABLE IF EXISTS dept;
CREATE TABLE dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    name VARCHAR(50) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0停用 1正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除：0否 1是'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

INSERT INTO dept (id, name, parent_id, sort) VALUES
(1, '学工部', 0, 1),
(2, '教研部', 0, 2),
(3, '咨询部', 0, 3),
(4, '就业部', 0, 4),
(5, '人事部', 0, 5);

-- =============================
-- 2. 员工表
-- =============================
DROP TABLE IF EXISTS emp;
CREATE TABLE emp (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '员工ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) DEFAULT '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO' COMMENT '密码（BCrypt加密）',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender TINYINT DEFAULT 1 COMMENT '性别：1男 2女',
    image VARCHAR(200) COMMENT '头像URL',
    job TINYINT DEFAULT 1 COMMENT '职位：1班主任 2讲师 3学工主管 4教研主管 5咨询师 6其他',
    entrydate DATE COMMENT '入职日期',
    dept_id BIGINT COMMENT '所属部门ID',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT DEFAULT 1 COMMENT '状态：0离职 1在职',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    FOREIGN KEY (dept_id) REFERENCES dept(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

INSERT INTO emp (id, username, name, gender, job, entrydate, dept_id, phone, email) VALUES
(1, 'admin', '管理员', 1, 4, '2020-01-01', 2, '13800000001', 'admin@tlias.com'),
(2, 'zhangsan', '张三', 1, 1, '2021-03-15', 1, '13800000002', 'zhangsan@tlias.com'),
(3, 'lisi', '李四', 2, 2, '2021-06-20', 2, '13800000003', 'lisi@tlias.com'),
(4, 'wangwu', '王五', 1, 3, '2022-01-10', 3, '13800000004', 'wangwu@tlias.com'),
(5, 'zhaoliu', '赵六', 2, 5, '2022-09-01', 4, '13800000005', 'zhaoliu@tlias.com');

-- =============================
-- 3. 操作日志表
-- =============================
DROP TABLE IF EXISTS operate_log;
CREATE TABLE operate_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    operate_emp_id BIGINT COMMENT '操作人ID',
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    class_name VARCHAR(200) COMMENT '类名',
    method_name VARCHAR(100) COMMENT '方法名',
    method_params TEXT COMMENT '方法参数',
    return_value TEXT COMMENT '返回值',
    cost_time BIGINT COMMENT '耗时(ms)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- =============================
-- 4. AI知识库文档表（RAG用）
-- =============================
DROP TABLE IF EXISTS ai_document;
CREATE TABLE ai_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    content TEXT COMMENT '文档内容',
    source VARCHAR(100) COMMENT '来源',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库文档表';

INSERT INTO ai_document (title, content, source) VALUES
('员工年假制度', '根据公司《员工手册》第三章规定：\n\n1. 年假天数：入职满1年享受5天年假，满10年享受10天，满20年享受15天。\n2. 请假流程：需提前3个工作日通过OA系统提交申请，经直属上级审批。\n3. 年假不可跨年累计，当年未休完按作废处理。\n4. 离职时未休年假按日工资折算。', '员工手册'),
('报销流程说明', '费用报销流程：\n\n1. 填写报销单，附发票原件。\n2. 部门负责人审批（单笔超5000元需总监审批）。\n3. 财务部审核票据合规性。\n4. 审批通过后7个工作日内打款。\n\n注意事项：差旅费需附行程单，招待费需注明事由及参与人员。', '财务制度'),
('加班管理规定', '加班管理：\n\n1. 工作日加班：18:00后开始计算，需提前在OA提交加班申请。\n2. 加班餐补：超过20:00可享受30元餐补。\n3. 调休：加班时长可1:1兑换调休，需在3个月内使用。\n4. 周末加班：需提前一周申请，优先安排调休。', '人事制度');