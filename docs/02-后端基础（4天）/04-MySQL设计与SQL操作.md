# MySQL 设计与 SQL 操作

## 学习目标

- 掌握数据库设计的基本规范和范式
- 熟练运用常用 SQL 语句进行增删改查
- 理解索引的作用及使用场景
- 能够使用 Navicat 或 DataGrip 管理数据库

## 核心知识点

### 1. 数据库设计规范

**命名规范**：
- 表名：小写 + 下划线，如 `emp`、`dept`、`operate_log`
- 字段名：小写 + 下划线，如 `create_time`、`dept_id`
- 主键：统一命名为 `id`，类型 `INT` 或 `BIGINT`，自增
- 时间字段：`create_time`、`update_time`，类型 `DATETIME`

**三大范式（简要）**：
- 第一范式：字段原子性，不可再分
- 第二范式：非主键字段必须完全依赖主键
- 第三范式：非主键字段之间不能相互依赖

> 实际项目中常做适当反范式设计，以空间换时间，提升查询性能。

### 2. 数据类型选择

| 类型 | 用途 | 示例 |
|------|------|------|
| INT | 整数，主键、外键 | id, age |
| BIGINT | 大整数，分布式ID | 雪花ID |
| VARCHAR(n) | 可变长度字符串 | name, email |
| CHAR(n) | 固定长度字符串 | 手机号、身份证号 |
| DECIMAL(m,d) | 精确小数，金额计算 | salary |
| DATETIME | 日期时间 | create_time |
| TINYINT | 布尔/状态 | gender(1男2女) |
| TEXT | 长文本 | description |

### 3. 常用 SQL 语句

**DDL（数据定义）**：

```sql
-- 创建数据库
CREATE DATABASE tlias_db DEFAULT CHARACTER SET utf8mb4;

-- 创建部门表
CREATE TABLE dept (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '部门名称',
    create_time DATETIME DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME DEFAULT NOW() COMMENT '修改时间'
) COMMENT '部门表';

-- 创建员工表
CREATE TABLE emp (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(50) COMMENT '密码',
    name VARCHAR(50) COMMENT '姓名',
    gender TINYINT COMMENT '性别 1男 2女',
    image VARCHAR(300) COMMENT '头像URL',
    job TINYINT COMMENT '职位 1班主任 2讲师 3学工主管 4教研主管 5咨询师',
    entrydate DATE COMMENT '入职日期',
    dept_id INT COMMENT '部门ID',
    create_time DATETIME DEFAULT NOW(),
    update_time DATETIME DEFAULT NOW(),
    FOREIGN KEY (dept_id) REFERENCES dept(id)
) COMMENT '员工表';
```

**DML（数据操作）**：

```sql
-- 插入数据
INSERT INTO dept(name) VALUES ('教研部'), ('学工部'), ('就业部');

-- 更新数据
UPDATE emp SET name = '张三' WHERE id = 1;

-- 删除数据
DELETE FROM emp WHERE id = 1;

-- 查询数据
SELECT * FROM emp WHERE dept_id = 1;
SELECT id, name, gender FROM emp WHERE gender = 1 ORDER BY entrydate DESC;
```

**DQL（多表查询）**：

```sql
-- 内连接：只返回两表匹配的数据
SELECT e.id, e.name, d.name AS dept_name
FROM emp e INNER JOIN dept d ON e.dept_id = d.id;

-- 左连接：返回左表全部，右表不匹配为NULL
SELECT e.id, e.name, d.name AS dept_name
FROM emp e LEFT JOIN dept d ON e.dept_id = d.id;

-- 聚合查询
SELECT dept_id, COUNT(*) AS emp_count, AVG(salary) AS avg_salary
FROM emp GROUP BY dept_id HAVING COUNT(*) > 5;
```

### 4. 索引

索引是帮助数据库高效获取数据的数据结构，类似书的目录。

```sql
-- 创建索引
CREATE INDEX idx_emp_name ON emp(name);

-- 组合索引（最左前缀原则）
CREATE INDEX idx_emp_dept_job ON emp(dept_id, job);
```

**索引使用原则**：
- WHERE 条件、ORDER BY、JOIN ON 的字段考虑加索引
- 频繁更新的字段不建议加索引
- 索引不是越多越好，写操作会变慢
- 优先使用覆盖索引（查询字段都在索引中）

### 5. 版本 A 核心表结构

```sql
-- 部门表
dept(id, name, create_time, update_time)

-- 员工表
emp(id, username, password, name, gender, image, job, entrydate, dept_id, create_time, update_time)

-- 工作经历表
emp_expr(id, emp_id, begin, end, company, job)

-- 操作日志表
operate_log(id, operate_emp_id, operate_time, class_name, method_name, method_params, return_value, cost_time)
```

## 动手练习

### 练习 1：创建数据库和表

1. 在 MySQL 中创建 `tlias_db` 数据库
2. 执行上述 `dept` 和 `emp` 建表语句
3. 插入至少 5 条部门和 10 条员工数据

### 练习 2：SQL 查询练习

1. 查询所有男员工，按入职日期降序排列
2. 查询每个部门的员工人数
3. 查询入职超过 1 年的员工及其所属部门名称

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| Unknown database | 数据库不存在 | 先执行 CREATE DATABASE |
| Table doesn't exist | 表未创建或表名错误 | 检查表名大小写（Linux区分） |
| Duplicate entry | 唯一约束冲突 | 检查主键或UNIQUE字段是否重复 |
| Cannot add foreign key | 外键类型不匹配或数据冲突 | 确保关联字段类型一致，且被引用数据存在 |
| Lock wait timeout | 事务未提交导致锁等待 | 检查是否有未关闭的事务 |

## 本节小结

数据库设计是后端开发的基础。掌握命名规范、正确选择数据类型、熟练编写 SQL、合理使用索引，是每一位后端工程师的必修课。

## 参考文档

- [MySQL 官方文档](https://dev.mysql.com/doc/)
- [MySQL 索引原理](https://dev.mysql.com/doc/refman/8.0/en/optimization-indexes.html)

