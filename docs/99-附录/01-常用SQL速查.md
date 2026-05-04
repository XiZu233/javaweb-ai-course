# 附录一：常用 SQL 速查手册

> 适用版本：Version A（MyBatis + XML）/ Version B（MyBatis-Plus）
> 数据库：MySQL 8.0
> 目标：零基础快速掌握 SQL 核心语法，能独立完成 Tlias 人事系统的数据库操作

---

## 一、概述与目标

本文档是 Tlias 人事管理系统开发过程中最常用的 SQL 语法速查手册，涵盖以下学习目标：

1. **掌握 DDL 语句**：能够独立创建数据库、数据表，理解字段类型和约束的含义
2. **熟练编写 DML/DQL**：完成增删改查操作，特别是带条件查询、多表关联、分页查询
3. **理解索引与事务**：知道何时建索引、如何查看执行计划、事务的基本控制
4. **具备问题排查能力**：遇到 SQL 报错时，能快速定位是语法错误、权限问题还是数据问题

---

## 二、SQL 分类总览

SQL 语句按功能分为四大类，用一张表格快速建立全局认识：

| 分类 | 全称 | 作用 | 典型语句 |
|------|------|------|---------|
| DDL | Data Definition Language | 定义数据库结构（建库、建表、改表） | CREATE、ALTER、DROP、TRUNCATE |
| DML | Data Manipulation Language | 操作表中的数据 | INSERT、UPDATE、DELETE |
| DQL | Data Query Language | 查询数据 | SELECT |
| DCL | Data Control Language | 控制用户权限 | GRANT、REVOKE |

```
+------------------+
|   SQL 四大分类   |
+------------------+
| DDL  -> 建库建表 |
| DML  -> 增删改   |
| DQL  -> 查询     |
| DCL  -> 权限     |
+------------------+
```

---

## 三、DDL：数据定义语言

### 3.1 什么是 DDL？为什么要学？

DDL（Data Definition Language）用于定义数据库的"骨架"——数据库本身、数据表、字段、索引等结构。在 Tlias 项目中，你需要用 DDL 创建 `dept`（部门表）、`emp`（员工表）等基础表。理解 DDL 是后端开发的第一步，因为表结构设计直接影响后续代码编写的难易程度。

### 3.2 数据库操作

```sql
-- 1. 创建数据库（推荐写法，避免重复创建报错）
-- IF NOT EXISTS：如果数据库已存在则跳过，不会报错
-- CHARACTER SET utf8mb4：使用 utf8mb4 字符集，支持中文和 emoji
-- COLLATE utf8mb4_unicode_ci：排序规则，ci 表示不区分大小写
CREATE DATABASE IF NOT EXISTS tlias_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. 查看当前有哪些数据库
SHOW DATABASES;

-- 3. 切换到指定数据库（后续操作都在这个库中进行）
USE tlias_db;

-- 4. 查看当前正在使用的数据库
SELECT DATABASE();

-- 5. 删除数据库（危险操作！生产环境禁用）
-- IF EXISTS：如果数据库不存在则跳过，不会报错
DROP DATABASE IF EXISTS tlias_db;
```

### 3.3 数据表操作

#### 3.3.1 创建表（CREATE TABLE）

```sql
-- 创建部门表（Tlias 项目核心表之一）
-- 每个字段的含义：
--   id: 主键，自增，唯一标识一条记录
--   name: 部门名称，不允许为空
--   create_time: 创建时间，默认当前时间
--   update_time: 更新时间，记录变更时自动更新
CREATE TABLE dept (
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID，主键自增',
    name        VARCHAR(50) NOT NULL COMMENT '部门名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 创建员工表（含外键关联）
-- FOREIGN KEY：外键约束，保证 dept_id 的值必须在 dept 表中存在
-- ON DELETE SET NULL：删除部门时，该部门员工的 dept_id 自动设为 NULL
CREATE TABLE emp (
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '员工ID',
    username    VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名，唯一',
    password    VARCHAR(100) DEFAULT '123456' COMMENT '密码',
    name        VARCHAR(50) NOT NULL COMMENT '姓名',
    gender      TINYINT DEFAULT 1 COMMENT '性别：1男 2女',
    image       VARCHAR(200) COMMENT '头像URL',
    job         TINYINT DEFAULT 1 COMMENT '职位：1班主任 2讲师 3学工主管 4教研主管 5咨询师 6其他',
    entrydate   DATE COMMENT '入职日期',
    dept_id     INT COMMENT '所属部门ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    -- 外键约束：关联到 dept 表的 id 字段
    FOREIGN KEY (dept_id) REFERENCES dept(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';
```

**常用字段类型速查表**：

| 类型 | 说明 | 示例 | 适用场景 |
|------|------|------|---------|
| INT | 整数，4字节 | `id INT` | 主键ID、计数 |
| BIGINT | 大整数，8字节 | `user_id BIGINT` | 用户ID（数据量大时） |
| TINYINT | 小整数，1字节 | `gender TINYINT` | 状态码（0/1）、性别（1/2） |
| VARCHAR(n) | 变长字符串 | `name VARCHAR(50)` | 姓名、标题、URL |
| TEXT | 长文本 | `content TEXT` | 文章内容、日志详情 |
| DATETIME | 日期时间 | `create_time DATETIME` | 创建时间、更新时间 |
| DATE | 日期（无时间） | `entrydate DATE` | 生日、入职日期 |
| DECIMAL(m,d) | 精确小数 | `salary DECIMAL(10,2)` | 金额、价格 |

**常用约束速查表**：

| 约束 | 关键字 | 作用 |
|------|--------|------|
| 主键 | PRIMARY KEY | 唯一标识每条记录，自动创建索引 |
| 自增 | AUTO_INCREMENT | 整数主键自动+1，无需手动赋值 |
| 非空 | NOT NULL | 该字段必须有值，不能为 NULL |
| 唯一 | UNIQUE | 字段值不能重复（如用户名、手机号） |
| 默认值 | DEFAULT | 不赋值时的默认值 |
| 外键 | FOREIGN KEY | 关联另一张表的主键，保证数据一致性 |

#### 3.3.2 修改表（ALTER TABLE）

```sql
-- 1. 添加新字段：给员工表增加手机号字段
ALTER TABLE emp ADD COLUMN phone VARCHAR(20) COMMENT '手机号';

-- 2. 修改字段类型：将用户名长度从50改为100
ALTER TABLE emp MODIFY COLUMN username VARCHAR(100) NOT NULL UNIQUE;

-- 3. 修改字段名：将 image 改名为 avatar
ALTER TABLE emp CHANGE COLUMN image avatar VARCHAR(200) COMMENT '头像';

-- 4. 删除字段：删除手机号字段
ALTER TABLE emp DROP COLUMN phone;

-- 5. 添加索引：给 dept_id 添加普通索引，加速关联查询
ALTER TABLE emp ADD INDEX idx_dept_id (dept_id);

-- 6. 删除索引
ALTER TABLE emp DROP INDEX idx_dept_id;

-- 7. 修改表名：将 emp 改名为 employee
ALTER TABLE emp RENAME TO employee;
```

#### 3.3.3 删除表（DROP / TRUNCATE）

```sql
-- DROP TABLE：删除整个表，包括表结构和所有数据，不可恢复
DROP TABLE IF EXISTS emp;

-- TRUNCATE TABLE：清空表中所有数据，但保留表结构，比 DELETE 快
-- 注意：TRUNCATE 会重置自增ID，且无法回滚
TRUNCATE TABLE emp;
```

| 操作 | 删除结构 | 删除数据 | 可回滚 | 自增ID | 速度 |
|------|---------|---------|--------|--------|------|
| DROP | 是 | 是 | 否 | - | 最快 |
| TRUNCATE | 否 | 是 | 否 | 重置 | 快 |
| DELETE（无WHERE） | 否 | 是 | 是 | 不重置 | 慢 |

### 3.4 查看表结构

```sql
-- 查看当前数据库所有表
SHOW TABLES;

-- 查看表结构（字段信息）
DESC dept;
-- 或
DESCRIBE dept;

-- 查看建表语句（完整SQL）
SHOW CREATE TABLE dept;

-- 查看表索引信息
SHOW INDEX FROM emp;
```

---

## 四、DML：数据操作语言

### 4.1 什么是 DML？为什么要学？

DML（Data Manipulation Language）用于对表中的数据进行增、删、改操作。在 Tlias 项目中，新增员工、修改部门信息、删除离职员工等操作最终都会转化为 DML 语句。MyBatis 和 MyBatis-Plus 本质上就是将 Java 对象映射为 DML 语句。

### 4.2 插入数据（INSERT）

```sql
-- 1. 插入单条数据：指定字段（推荐，清晰且安全）
INSERT INTO dept (name) VALUES ('学工部');

-- 2. 插入单条数据：所有字段（必须按表结构顺序，容易出错）
INSERT INTO dept VALUES (NULL, '教研部', NOW(), NOW());

-- 3. 插入多条数据（批量插入，性能更好）
INSERT INTO dept (name) VALUES
    ('咨询部'),
    ('就业部'),
    ('人事部');

-- 4. 插入员工数据（含日期字段）
INSERT INTO emp (username, password, name, gender, job, entrydate, dept_id)
VALUES ('zhangsan', '123456', '张三', 1, 1, '2021-03-15', 1);

-- 5. 从另一张表查询并插入（数据迁移常用）
INSERT INTO emp_backup (id, username, name)
SELECT id, username, name FROM emp WHERE entrydate < '2020-01-01';
```

### 4.3 更新数据（UPDATE）

```sql
-- 1. 更新单条记录（必须带 WHERE，否则全表更新！）
UPDATE emp SET name = '张三丰' WHERE id = 1;

-- 2. 更新多个字段
UPDATE emp
SET name = '李四四',
    gender = 2,
    update_time = NOW()
WHERE id = 2;

-- 3. 批量更新：将学工部所有员工的职位改为班主任
UPDATE emp SET job = 1 WHERE dept_id = 1;

-- 4. 使用表达式更新：给所有入职超过3年的员工密码追加后缀（仅演示）
UPDATE emp SET password = CONCAT(password, '_old')
WHERE entrydate <= DATE_SUB(CURDATE(), INTERVAL 3 YEAR);
```

> **重要警告**：UPDATE 和 DELETE 必须带 WHERE 条件！忘记 WHERE 会导致全表数据被修改/删除，生产环境可能造成重大事故。在 IDEA 中执行 UPDATE/DELETE 时，如果没有 WHERE，数据库工具通常会提示确认。

### 4.4 删除数据（DELETE）

```sql
-- 1. 删除单条记录
DELETE FROM emp WHERE id = 10;

-- 2. 批量删除：删除离职超过1年的员工
DELETE FROM emp WHERE entrydate <= DATE_SUB(CURDATE(), INTERVAL 1 YEAR);

-- 3. 按条件删除：删除部门ID为空的员工（未分配部门）
DELETE FROM emp WHERE dept_id IS NULL;

-- 4. 清空全表（可回滚，但速度比 TRUNCATE 慢）
DELETE FROM emp;
```

---

## 五、DQL：数据查询语言

### 5.1 什么是 DQL？为什么要学？

DQL（Data Query Language）即 SELECT 语句，是 SQL 中使用频率最高的语句。Tlias 项目中的员工列表查询、部门统计、分页展示等功能都依赖 SELECT。掌握 SELECT 的完整语法，是后端开发的核心能力。

### 5.2 SELECT 基础语法

```sql
-- 完整语法结构（执行顺序从上到下）
SELECT          -- 第5步：选择要显示的列
    列名/表达式/函数
FROM            -- 第1步：确定从哪张表查
WHERE           -- 第2步：过滤行条件
GROUP BY        -- 第3步：按列分组
HAVING          -- 第4步：过滤分组后的结果
ORDER BY        -- 第6步：排序
LIMIT           -- 第7步：分页限制
```

### 5.3 基础查询

```sql
-- 1. 查询所有列（开发调试时用，生产环境不推荐，性能差）
SELECT * FROM emp;

-- 2. 查询指定列（推荐，减少数据传输）
SELECT id, username, name, gender FROM emp;

-- 3. 给列起别名（让结果更易读）
SELECT
    id AS 员工ID,
    name AS 姓名,
    gender AS 性别
FROM emp;

-- 4. 去重查询：查询所有不同的职位
SELECT DISTINCT job FROM emp;

-- 5. 常量列：给每行结果添加一个固定值列
SELECT id, name, '正式员工' AS 员工类型 FROM emp;
```

### 5.4 条件查询（WHERE）

```sql
-- 1. 比较运算符
SELECT * FROM emp WHERE gender = 1;           -- 等于
SELECT * FROM emp WHERE id > 5;               -- 大于
SELECT * FROM emp WHERE entrydate >= '2022-01-01'; -- 大于等于
SELECT * FROM emp WHERE dept_id != 1;         -- 不等于（也可用 <>）

-- 2. 逻辑运算符
SELECT * FROM emp WHERE gender = 1 AND job = 1;     -- 并且
SELECT * FROM emp WHERE dept_id = 1 OR dept_id = 2; -- 或者
SELECT * FROM emp WHERE NOT gender = 1;             -- 取反

-- 3. 范围查询
SELECT * FROM emp WHERE id BETWEEN 1 AND 5;   -- id 在 1~5 之间（包含边界）
SELECT * FROM emp WHERE job IN (1, 2, 3);     -- job 是 1 或 2 或 3

-- 4. 模糊查询（LIKE）
-- % 代表任意多个字符，_ 代表单个字符
SELECT * FROM emp WHERE name LIKE '张%';      -- 姓张的所有人
SELECT * FROM emp WHERE name LIKE '%三';      -- 名字以三结尾
SELECT * FROM emp WHERE name LIKE '李_';      -- 姓李且名字只有两个字

-- 5. 空值判断
SELECT * FROM emp WHERE dept_id IS NULL;      -- 未分配部门的员工
SELECT * FROM emp WHERE image IS NOT NULL;    -- 有头像的员工
```

### 5.5 排序（ORDER BY）

```sql
-- 1. 单字段升序（默认 ASC，可省略）
SELECT * FROM emp ORDER BY entrydate ASC;

-- 2. 单字段降序
SELECT * FROM emp ORDER BY entrydate DESC;

-- 3. 多字段排序：先按部门升序，同部门内按入职日期降序
SELECT * FROM emp ORDER BY dept_id ASC, entrydate DESC;

-- 4. 按表达式排序：按入职时长从长到短
SELECT *, DATEDIFF(CURDATE(), entrydate) AS 工作天数
FROM emp
ORDER BY 工作天数 DESC;
```

### 5.6 分页查询（LIMIT）

```sql
-- LIMIT 语法：LIMIT 起始位置, 每页条数
-- 注意：起始位置从 0 开始

-- 第1页，每页5条（显示第 1~5 条）
SELECT * FROM emp LIMIT 0, 5;
-- 或 MySQL 8.0 新语法
SELECT * FROM emp LIMIT 5 OFFSET 0;

-- 第2页，每页5条（显示第 6~10 条）
SELECT * FROM emp LIMIT 5, 5;

-- 第3页，每页5条（显示第 11~15 条）
SELECT * FROM emp LIMIT 10, 5;

-- 通用公式：第 page 页，每页 pageSize 条
-- LIMIT (page - 1) * pageSize, pageSize
```

### 5.7 聚合函数与分组（GROUP BY / HAVING）

```sql
-- 1. 统计员工总数
SELECT COUNT(*) FROM emp;

-- 2. 统计有部门的员工数（COUNT 忽略 NULL）
SELECT COUNT(dept_id) FROM emp;

-- 3. 统计最早和最晚的入职日期
SELECT MIN(entrydate) AS 最早入职, MAX(entrydate) AS 最晚入职 FROM emp;

-- 4. 按部门分组统计人数
SELECT
    dept_id,
    COUNT(*) AS 人数,
    AVG(DATEDIFF(CURDATE(), entrydate)) AS 平均工作天数
FROM emp
WHERE dept_id IS NOT NULL    -- WHERE 在分组前过滤
GROUP BY dept_id
HAVING COUNT(*) >= 2;        -- HAVING 在分组后过滤

-- 5. 分组统计每个职位的男女比例
SELECT
    job,
    gender,
    COUNT(*) AS 人数
FROM emp
GROUP BY job, gender;
```

| 聚合函数 | 作用 | 示例 |
|---------|------|------|
| COUNT(*) | 统计行数 | `COUNT(*)` |
| COUNT(列) | 统计非 NULL 值 | `COUNT(dept_id)` |
| SUM(列) | 求和 | `SUM(salary)` |
| AVG(列) | 平均值 | `AVG(salary)` |
| MAX(列) | 最大值 | `MAX(entrydate)` |
| MIN(列) | 最小值 | `MIN(entrydate)` |

> **WHERE vs HAVING 的区别**：
> - WHERE 在分组前过滤原始行，不能使用聚合函数
> - HAVING 在分组后过滤分组结果，可以使用聚合函数

### 5.8 多表关联查询（JOIN）

```sql
-- 员工表和部门表的关系：
-- emp.dept_id  ->  dept.id
--
--    emp 表              dept 表
--  +--------+---------+  +----+--------+
--  | id     | dept_id |  | id | name   |
--  +--------+---------+  +----+--------+
--  | 1      | 1       |  | 1  | 学工部 |
--  | 2      | 2       |  | 2  | 教研部 |
--  | 3      | NULL    |  | 3  | 咨询部 |
--  +--------+---------+  +----+--------+

-- 1. 内连接（INNER JOIN）：只返回两表匹配的数据
-- 结果中不会包含 dept_id 为 NULL 的员工
SELECT
    e.id,
    e.name AS 员工姓名,
    d.name AS 部门名称
FROM emp e
INNER JOIN dept d ON e.dept_id = d.id;

-- 2. 左连接（LEFT JOIN）：返回左表所有数据，右表不匹配则补 NULL
-- 结果中包含未分配部门的员工（部门名称为 NULL）
SELECT
    e.id,
    e.name AS 员工姓名,
    IFNULL(d.name, '未分配') AS 部门名称
FROM emp e
LEFT JOIN dept d ON e.dept_id = d.id;

-- 3. 右连接（RIGHT JOIN）：返回右表所有数据，左表不匹配则补 NULL
-- 结果中包含没有员工的部门
SELECT
    d.name AS 部门名称,
    e.name AS 员工姓名
FROM emp e
RIGHT JOIN dept d ON e.dept_id = d.id;

-- 4. 三表关联：员工 + 部门 + 工作经历
SELECT
    e.name AS 员工,
    d.name AS 部门,
    ee.company AS 曾任职公司,
    ee.job AS 曾任职位
FROM emp e
LEFT JOIN dept d ON e.dept_id = d.id
LEFT JOIN emp_expr ee ON e.id = ee.emp_id;
```

| JOIN 类型 | 结果特点 | 使用场景 |
|-----------|---------|---------|
| INNER JOIN | 只返回匹配的行 | 必须有关联数据的查询 |
| LEFT JOIN | 左表全保留，右表不匹配补 NULL | 显示所有员工及其部门（含未分配） |
| RIGHT JOIN | 右表全保留，左表不匹配补 NULL | 显示所有部门及其员工（含空部门） |
| CROSS JOIN | 笛卡尔积，两表行数相乘 | 极少使用 |

### 5.9 子查询

```sql
-- 1. 单行子查询：查询"教研部"的所有员工
SELECT * FROM emp
WHERE dept_id = (SELECT id FROM dept WHERE name = '教研部');

-- 2. 多行子查询：查询"学工部"和"教研部"的所有员工
SELECT * FROM emp
WHERE dept_id IN (SELECT id FROM dept WHERE name IN ('学工部', '教研部'));

-- 3. 相关子查询：查询每个部门中入职最早的员工
SELECT * FROM emp e1
WHERE entrydate = (
    SELECT MIN(entrydate) FROM emp e2 WHERE e2.dept_id = e1.dept_id
);

-- 4. EXISTS 子查询：查询有员工的部门
SELECT * FROM dept d
WHERE EXISTS (SELECT 1 FROM emp e WHERE e.dept_id = d.id);
```

---

## 六、常用函数速查

### 6.1 字符串函数

```sql
-- CONCAT：字符串拼接
SELECT CONCAT(name, ' - ', job) FROM emp;

-- LENGTH：字符串字节长度（UTF-8下中文占3字节）
SELECT LENGTH('张三');  -- 结果：6

-- CHAR_LENGTH：字符串字符数
SELECT CHAR_LENGTH('张三');  -- 结果：2

-- SUBSTRING：截取子串（从第1个字符开始，取2个字符）
SELECT SUBSTRING(name, 1, 1) AS 姓氏 FROM emp;  -- 提取姓

-- REPLACE：替换字符串
SELECT REPLACE(phone, '138', '139') FROM emp;

-- UPPER / LOWER：大小写转换
SELECT UPPER('abc');  -- ABC
SELECT LOWER('ABC');  -- abc

-- TRIM：去除首尾空格
SELECT TRIM('  hello  ');  -- 'hello'
```

### 6.2 日期函数

```sql
-- NOW() / CURRENT_TIMESTAMP：当前日期时间
SELECT NOW();

-- CURDATE()：当前日期（不含时间）
SELECT CURDATE();

-- CURTIME()：当前时间（不含日期）
SELECT CURTIME();

-- YEAR / MONTH / DAY：提取年月日
SELECT YEAR(entrydate), MONTH(entrydate), DAY(entrydate) FROM emp;

-- DATEDIFF：计算两个日期相差的天数
SELECT DATEDIFF(CURDATE(), entrydate) AS 入职天数 FROM emp;

-- DATE_ADD / DATE_SUB：日期加减
SELECT DATE_ADD(CURDATE(), INTERVAL 7 DAY);   -- 7天后
SELECT DATE_SUB(CURDATE(), INTERVAL 1 MONTH); -- 1个月前

-- DATE_FORMAT：格式化日期
SELECT DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s');  -- 2024-01-15 14:30:00
SELECT DATE_FORMAT(NOW(), '%Y年%m月%d日');         -- 2024年01月15日

-- STR_TO_DATE：字符串转日期
SELECT STR_TO_DATE('2024-01-15', '%Y-%m-%d');
```

### 6.3 数学函数

```sql
-- ROUND：四舍五入
SELECT ROUND(3.14159, 2);  -- 3.14

-- CEIL / FLOOR：向上/向下取整
SELECT CEIL(3.2);   -- 4
SELECT FLOOR(3.8);  -- 3

-- ABS：绝对值
SELECT ABS(-10);  -- 10

-- MOD：取余
SELECT MOD(10, 3);  -- 1

-- RAND：随机数（0~1之间）
SELECT RAND();
SELECT FLOOR(RAND() * 100);  -- 0~99 的随机整数
```

### 6.4 条件函数

```sql
-- IF：条件判断
SELECT IF(gender = 1, '男', '女') AS 性别 FROM emp;

-- IFNULL：如果为 NULL 则返回指定值
SELECT IFNULL(d.name, '未分配') AS 部门 FROM emp e LEFT JOIN dept d ON e.dept_id = d.id;

-- CASE WHEN：多条件判断
SELECT
    name,
    CASE
        WHEN job = 1 THEN '班主任'
        WHEN job = 2 THEN '讲师'
        WHEN job = 3 THEN '学工主管'
        WHEN job = 4 THEN '教研主管'
        WHEN job = 5 THEN '咨询师'
        ELSE '其他'
    END AS 职位名称
FROM emp;
```

---

## 七、索引

### 7.1 什么是索引？为什么要用？

索引（Index）是数据库中一种特殊的数据结构，类似于书籍的目录。没有索引时，查询需要全表扫描（逐行比对）；有索引时，数据库可以快速定位到目标数据。Tlias 项目中，员工表的 `username`（唯一索引）、`dept_id`（普通索引）都应该建立索引。

```
无索引查询：全表扫描
  第1行 -> 不匹配
  第2行 -> 不匹配
  ...
  第10000行 -> 匹配（耗时！）

有索引查询：B+树快速定位
  索引树查找 -> 直接定位到数据页（快速！）
```

### 7.2 索引操作

```sql
-- 1. 创建普通索引
CREATE INDEX idx_emp_name ON emp(name);

-- 2. 创建唯一索引（索引列值不能重复）
CREATE UNIQUE INDEX idx_emp_username ON emp(username);

-- 3. 创建联合索引（最左前缀原则）
-- 查询条件同时用到 dept_id 和 job 时，此索引生效
CREATE INDEX idx_dept_job ON emp(dept_id, job);

-- 4. 查看表的索引
SHOW INDEX FROM emp;

-- 5. 删除索引
DROP INDEX idx_emp_name ON emp;

-- 6. 查看查询是否使用了索引（执行计划）
EXPLAIN SELECT * FROM emp WHERE name = '张三';
```

### 7.3 最左前缀原则

联合索引 `(dept_id, job)` 的生效情况：

| 查询条件 | 是否使用索引 | 说明 |
|---------|------------|------|
| `WHERE dept_id = 1` | 是 | 使用第1列 |
| `WHERE dept_id = 1 AND job = 2` | 是 | 使用第1、2列 |
| `WHERE job = 2` | 否 | 跳过了第1列 |
| `WHERE dept_id = 1 OR job = 2` | 否 | OR 条件导致索引失效 |

---

## 八、事务

### 8.1 什么是事务？为什么要用？

事务（Transaction）是一组 SQL 操作的集合，这些操作要么全部成功，要么全部失败。Tlias 项目中，删除员工时同时删除其工作经历，这两个操作应该放在同一个事务中，避免出现"员工删除了但工作经历还在"的数据不一致问题。

```
事务的 ACID 特性：
+-----------+------------------------------------------+
| A - 原子性 | 要么全部成功，要么全部回滚               |
| C - 一致性 | 事务前后数据保持完整约束                 |
| I - 隔离性 | 多个事务互不干扰                         |
| D - 持久性 | 事务提交后数据永久保存                   |
+-----------+------------------------------------------+
```

### 8.2 事务控制

```sql
-- 1. 开启事务
START TRANSACTION;
-- 或
BEGIN;

-- 2. 执行一组操作
DELETE FROM emp_expr WHERE emp_id = 5;  -- 先删工作经历
DELETE FROM emp WHERE id = 5;            -- 再删员工

-- 3. 提交事务（所有操作永久生效）
COMMIT;

-- 4. 如果中间出错，回滚事务（所有操作撤销）
ROLLBACK;
```

### 8.3 SpringBoot 中的事务（Java 代码）

```java
// 在 Service 方法上添加 @Transactional 注解
// 方法执行成功自动 COMMIT，抛出异常自动 ROLLBACK
@Transactional
public void deleteEmp(Integer id) {
    empExprMapper.deleteByEmpId(id);  // 先删工作经历
    empMapper.deleteById(id);          // 再删员工
}
```

---

## 九、用户与权限（DCL）

```sql
-- 1. 创建用户
CREATE USER 'tlias_user'@'localhost' IDENTIFIED BY 'password123';

-- 2. 授予权限（对 tlias_db 库的所有表拥有全部权限）
GRANT ALL PRIVILEGES ON tlias_db.* TO 'tlias_user'@'localhost';

-- 3. 授予特定权限（只读）
GRANT SELECT ON tlias_db.* TO 'readonly_user'@'%';

-- 4. 刷新权限（使权限变更立即生效）
FLUSH PRIVILEGES;

-- 5. 查看用户权限
SHOW GRANTS FOR 'tlias_user'@'localhost';

-- 6. 撤销权限
REVOKE DELETE ON tlias_db.* FROM 'tlias_user'@'localhost';

-- 7. 删除用户
DROP USER 'tlias_user'@'localhost';
```

| 权限 | 说明 |
|------|------|
| ALL PRIVILEGES | 所有权限 |
| SELECT | 查询数据 |
| INSERT | 插入数据 |
| UPDATE | 更新数据 |
| DELETE | 删除数据 |
| CREATE | 创建表/库 |
| DROP | 删除表/库 |

---

## 十、Tlias 项目完整初始化脚本

```sql
-- ============================================
-- Tlias 人事管理系统 - 数据库初始化脚本
-- 版本：A/B 通用
-- 数据库：MySQL 8.0
-- 字符集：utf8mb4（支持中文和 emoji）
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS tlias_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE tlias_db;

-- 部门表
DROP TABLE IF EXISTS dept;
CREATE TABLE dept (
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    name        VARCHAR(50) NOT NULL COMMENT '部门名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

INSERT INTO dept (id, name) VALUES
(1, '学工部'), (2, '教研部'), (3, '咨询部'), (4, '就业部'), (5, '人事部');

-- 员工表
DROP TABLE IF EXISTS emp;
CREATE TABLE emp (
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '员工ID',
    username    VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(100) DEFAULT '123456' COMMENT '密码',
    name        VARCHAR(50) NOT NULL COMMENT '姓名',
    gender      TINYINT DEFAULT 1 COMMENT '性别：1男 2女',
    image       VARCHAR(200) COMMENT '头像URL',
    job         TINYINT DEFAULT 1 COMMENT '职位：1班主任 2讲师 3学工主管 4教研主管 5咨询师 6其他',
    entrydate   DATE COMMENT '入职日期',
    dept_id     INT COMMENT '所属部门ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (dept_id) REFERENCES dept(id) ON DELETE SET NULL
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

-- 员工工作经历表
DROP TABLE IF EXISTS emp_expr;
CREATE TABLE emp_expr (
    id      INT PRIMARY KEY AUTO_INCREMENT COMMENT '经历ID',
    emp_id  INT NOT NULL COMMENT '员工ID',
    begin   DATE NOT NULL COMMENT '开始时间',
    end     DATE COMMENT '结束时间',
    company VARCHAR(100) NOT NULL COMMENT '公司名称',
    job     VARCHAR(50) COMMENT '职位',
    FOREIGN KEY (emp_id) REFERENCES emp(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工工作经历表';

INSERT INTO emp_expr (emp_id, begin, end, company, job) VALUES
(2, '2019-07-01', '2021-02-28', 'ABC教育', '助教'),
(3, '2020-03-01', '2021-05-30', 'XYZ科技', '初级讲师'),
(4, '2021-06-01', '2022-12-31', '123培训', '咨询师'),
(6, '2022-02-01', '2023-01-31', 'DEF学校', '班主任');

-- 操作日志表
DROP TABLE IF EXISTS operate_log;
CREATE TABLE operate_log (
    id              INT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    operate_emp_id  INT COMMENT '操作人ID',
    operate_time    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    class_name      VARCHAR(200) COMMENT '类名',
    method_name     VARCHAR(100) COMMENT '方法名',
    method_params   TEXT COMMENT '方法参数',
    return_value    TEXT COMMENT '返回值',
    cost_time       BIGINT COMMENT '耗时(ms)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

---

## 十一、常见错误排查

| 错误现象 | 可能原因 | 解决方案 |
|---------|---------|---------|
| `Access denied for user` | 用户名/密码错误或权限不足 | 检查连接配置；执行 `GRANT` 授权 |
| `Table doesn't exist` | 表名拼写错误或未切换数据库 | 检查表名；先执行 `USE 数据库名` |
| `Duplicate entry` | 唯一约束冲突 | 检查重复数据；或改用 `INSERT IGNORE` |
| `Cannot add or update a child row` | 外键约束失败 | 插入数据时确保外键值在父表中存在 |
| `Lock wait timeout` | 事务未提交导致锁等待 | 检查是否有未提交的事务，执行 `ROLLBACK` |
| `Unknown column` | 列名拼写错误 | 用 `DESC 表名` 查看正确列名 |
| `You have an error in your SQL syntax` | SQL 语法错误 | 检查关键字拼写、逗号、引号是否成对 |
| `Data too long for column` | 数据长度超过字段定义 | 用 `ALTER TABLE` 增大字段长度 |
| `Out of range value` | 数值超出字段范围 | 检查 INT/BIGINT 范围；或用更大的类型 |

---

## 十二、SQL 速查表（Quick Reference）

```
+----------------------------------------------------------+
|                    SQL 速查卡                              |
+----------------------------------------------------------+
| 建库：CREATE DATABASE db CHARACTER SET utf8mb4;         |
| 建表：CREATE TABLE t (id INT PRIMARY KEY AUTO_INCREMENT);|
| 插入：INSERT INTO t (a,b) VALUES (1,2), (3,4);           |
| 更新：UPDATE t SET a=1 WHERE id=5;                       |
| 删除：DELETE FROM t WHERE id=5;                          |
| 查询：SELECT * FROM t WHERE ... ORDER BY ... LIMIT 0,10; |
| 关联：SELECT * FROM a LEFT JOIN b ON a.id = b.a_id;      |
| 分组：SELECT dept_id, COUNT(*) FROM emp GROUP BY dept_id;|
| 索引：CREATE INDEX idx ON t(col);                        |
| 事务：BEGIN; ... COMMIT;/ROLLBACK;                       |
+----------------------------------------------------------+

分页公式：
+----------------------------------------------------------+
| 第 page 页，每页 pageSize 条                             |
| LIMIT (page - 1) * pageSize, pageSize                    |
|                                                          |
| 例：第3页，每页10条 -> LIMIT 20, 10                      |
+----------------------------------------------------------+
```

---

## 十三、参考文档

1. [MySQL 8.0 官方文档 - SQL 语句](https://dev.mysql.com/doc/refman/8.0/en/sql-statements.html)
2. [MySQL 8.0 官方文档 - 函数与操作符](https://dev.mysql.com/doc/refman/8.0/en/functions.html)
3. [W3Schools - SQL 教程](https://www.w3schools.com/sql/)
4. [SQLBolt - 交互式 SQL 学习](https://sqlbolt.com/)
5. Tlias 项目课程文档：`docs/02-后端基础（4天）/04-MySQL设计与SQL操作.md`
