# 附录 A：常用 SQL 脚本

## 版本 A 数据库初始化脚本

```sql
-- Tlias人事管理系统 - 数据库初始化脚本
-- 版本A：传统实训版
-- 数据库：MySQL 8.0

CREATE DATABASE IF NOT EXISTS tlias_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tlias_db;

-- =============================
-- 1. 部门表
-- =============================
DROP TABLE IF EXISTS dept;
CREATE TABLE dept (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    name VARCHAR(50) NOT NULL COMMENT '部门名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

INSERT INTO dept (id, name) VALUES
(1, '学工部'),
(2, '教研部'),
(3, '咨询部'),
(4, '就业部'),
(5, '人事部');

-- =============================
-- 2. 员工表
-- =============================
DROP TABLE IF EXISTS emp;
CREATE TABLE emp (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '员工ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) DEFAULT '123456' COMMENT '密码',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender TINYINT DEFAULT 1 COMMENT '性别：1男 2女',
    image VARCHAR(200) COMMENT '头像URL',
    job TINYINT DEFAULT 1 COMMENT '职位：1班主任 2讲师 3学工主管 4教研主管 5咨询师 6其他',
    entrydate DATE COMMENT '入职日期',
    dept_id INT COMMENT '所属部门ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (dept_id) REFERENCES dept(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

INSERT INTO emp (id, username, password, name, gender, image, job, entrydate, dept_id) VALUES
(1, 'admin', '123456', '管理员', 1, '', 4, '2020-01-01', 2),
(2, 'zhangsan', '123456', '张三', 1, '', 1, '2021-03-15', 1),
(3, 'lisi', '123456', '李四', 2, '', 2, '2021-06-20', 2),
(4, 'wangwu', '123456', '王五', 1, '', 3, '2022-01-10', 3),
(5, 'zhaoliu', '123456', '赵六', 2, '', 5, '2022-09-01', 4),
(6, 'qianqi', '123456', '钱七', 1, '', 1, '2023-02-14', 1),
(7, 'sunba', '123456', '孙八', 2, '', 2, '2023-07-01', 2),
(8, 'zhoujiu', '123456', '周九', 1, '', 6, '2024-01-15', 5);

-- =============================
-- 3. 员工工作经历表
-- =============================
DROP TABLE IF EXISTS emp_expr;
CREATE TABLE emp_expr (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '经历ID',
    emp_id INT NOT NULL COMMENT '员工ID',
    begin DATE NOT NULL COMMENT '开始时间',
    end DATE COMMENT '结束时间',
    company VARCHAR(100) NOT NULL COMMENT '公司名称',
    job VARCHAR(50) COMMENT '职位',
    FOREIGN KEY (emp_id) REFERENCES emp(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工工作经历表';

INSERT INTO emp_expr (emp_id, begin, end, company, job) VALUES
(2, '2019-07-01', '2021-02-28', 'ABC教育', '助教'),
(3, '2020-03-01', '2021-05-30', 'XYZ科技', '初级讲师'),
(4, '2021-06-01', '2022-12-31', '123培训', '咨询师'),
(6, '2022-02-01', '2023-01-31', 'DEF学校', '班主任');

-- =============================
-- 4. 操作日志表
-- =============================
DROP TABLE IF EXISTS operate_log;
CREATE TABLE operate_log (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    operate_emp_id INT COMMENT '操作人ID',
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    class_name VARCHAR(200) COMMENT '类名',
    method_name VARCHAR(100) COMMENT '方法名',
    method_params TEXT COMMENT '方法参数',
    return_value TEXT COMMENT '返回值',
    cost_time BIGINT COMMENT '耗时(ms)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

## 常用查询语句

```sql
-- 查询所有员工及其部门
SELECT e.*, d.name AS dept_name
FROM emp e LEFT JOIN dept d ON e.dept_id = d.id;

-- 查询每个部门的员工人数
SELECT d.name, COUNT(e.id) AS emp_count
FROM dept d LEFT JOIN emp e ON d.id = e.dept_id
GROUP BY d.id;

-- 查询入职超过 2 年的员工
SELECT * FROM emp WHERE entrydate <= DATE_SUB(CURDATE(), INTERVAL 2 YEAR);
```

