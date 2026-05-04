# MySQL 设计与 SQL 操作

## 学习目标

学完本节后，你将能够：
- 理解数据库、表、字段之间的关系，像理解 Excel 工作簿一样理解数据库
- 按照规范设计出结构合理的数据库表
- 熟练编写增删改查 SQL，理解每种语句的执行逻辑
- 知道什么时候该加索引，什么时候不该加
- 使用 Navicat 或 DataGrip 可视化操作数据库

---

## 核心知识点

### 1. 数据库基础概念——从 Excel 说起

#### 1.1 什么是数据库

想象你有一份员工信息表，用 Excel 管理：

```
员工信息.xlsx
├── Sheet1（员工列表）
│   ├── 列：姓名、性别、部门、入职日期...
│   └── 行：张三、李四、王五...
├── Sheet2（部门列表）
│   ├── 列：部门名称、部门经理...
│   └── 行：研发部、市场部...
```

数据库本质上就是**超级加强版的 Excel**：
- Excel 文件 → 数据库（Database）
- Excel 的 Sheet → 数据表（Table）
- Excel 的列头 → 表字段/列（Column/Field）
- Excel 的行 → 表记录/行（Row/Record）

**但数据库比 Excel 强大得多：**
- 能同时被成千上万的人访问（并发）
- 能保证数据不丢失、不重复（事务）
- 能在几亿条记录中秒级查询（索引）
- 能自动检查数据的合法性（约束）

#### 1.2 关系型数据库的核心概念

```
数据库服务器（MySQL 进程）
    └── 数据库（Database）= 一个业务系统的数据仓库
            ├── 表（Table）= 一类数据的集合
            │       ├── 字段（Column）= 数据的属性
            │       └── 记录（Row）= 一条具体的数据
            ├── 表（Table）
            └── 表（Table）
```

**关系（Relationship）的含义：**

表与表之间通过某些字段建立关联：
```
员工表（emp）                          部门表（dept）
┌────┬────────┬─────────┐             ┌────┬────────┐
│ id │ name   │ dept_id │             │ id │ name   │
├────┼────────┼─────────┤             ├────┼────────┤
│ 1  │ 张三   │    1    │ ──────────→ │ 1  │ 研发部 │
│ 2  │ 李四   │    2    │ ──────────→ │ 2  │ 市场部 │
│ 3  │ 王五   │    1    │ ──────────→ │    │        │
└────┴────────┴─────────┘             └────┴────────┘
         ↑                                ↑
    外键（dept_id）                   主键（id）
```

- `dept_id` 是员工表的外键（Foreign Key），指向部门表的主键（Primary Key）
- 这种关联让数据不重复存储：员工表不用存"研发部"三个字，只存数字 1

---

### 2. 数据库设计规范——好的设计让后人受益

#### 2.1 命名规范

| 对象 | 规范 | 示例 | 反例 |
|------|------|------|------|
| 数据库名 | 小写，业务相关 | `tlias_db` | `TLIAS`（大写，Linux 区分大小写） |
| 表名 | 小写 + 下划线 | `emp`、`operate_log` | `EmpInfo`、`empInfo` |
| 字段名 | 小写 + 下划线 | `create_time`、`dept_id` | `createTime`、`DeptId` |
| 主键 | 统一叫 `id` | `id` | `user_id`、`pk_id` |
| 索引名 | `idx_表名_字段名` | `idx_emp_name` | `index1` |

**为什么强调小写 + 下划线？**
- Linux 系统默认区分大小写，`Emp` 和 `emp` 是两个不同的表
- 下划线可读性好：`create_time` 比 `createtime` 清晰
- 这是业界通用规范（Google、阿里巴巴都推荐）

#### 2.2 数据库设计的三大范式

范式（Normal Form）是数据库设计的理论指导，防止数据冗余和异常。

**第一范式（1NF）：字段原子性**

每个字段的值是不可再分的最小单元。

```
❌ 不符合 1NF：
员工表
┌────┬────────┬─────────────────────────────┐
│ id │ name   │ contact                     │
├────┼────────┼─────────────────────────────┤
│ 1  │ 张三   │ 13800138000, zhangsan@qq.com│  ← 一个字段存了两个信息！
└────┴────────┴─────────────────────────────┘

✅ 符合 1NF：
员工表
┌────┬────────┬─────────────┬─────────────────┐
│ id │ name   │ phone       │ email           │
├────┼────────┼─────────────┼─────────────────┤
│ 1  │ 张三   │ 13800138000 │ zhangsan@qq.com │
└────┴────────┴─────────────┴─────────────────┘
```

**第二范式（2NF）：非主键字段必须完全依赖主键**

适用于联合主键（多个字段组成主键）的情况。简单说：一张表只描述一件事。

```
❌ 不符合 2NF：
选课表（学号 + 课程号 = 联合主键）
┌────────┬────────┬────────┬──────────┐
│ 学号   │ 课程号 │ 成绩   │ 课程名称 │
├────────┼────────┼────────┼──────────┤
│ 202401 │ C001   │ 90     │ Java基础 │
│ 202401 │ C002   │ 85     │ MySQL    │
└────────┴────────┴────────┴──────────┘
         ↑                            ↑
    主键的一部分                    只依赖"课程号"，不依赖"学号"
    成绩依赖（学号+课程号）          这叫"部分依赖"，不符合 2NF

✅ 符合 2NF：拆成两张表
选课表                        课程表
┌────────┬────────┬────────┐  ┌────────┬──────────┐
│ 学号   │ 课程号 │ 成绩   │  │ 课程号 │ 课程名称 │
├────────┼────────┼────────┤  ├────────┼──────────┤
│ 202401 │ C001   │ 90     │  │ C001   │ Java基础 │
│ 202401 │ C002   │ 85     │  │ C002   │ MySQL    │
└────────┴────────┴────────┘  └────────┴──────────┘
```

**第三范式（3NF）：非主键字段之间不能相互依赖**

消除传递依赖。

```
❌ 不符合 3NF：
员工表
┌────┬────────┬─────────┬───────────┐
│ id │ name   │ dept_id │ dept_name │
├────┼────────┼─────────┼───────────┤
│ 1  │ 张三   │ 1       │ 研发部    │
│ 2  │ 李四   │ 1       │ 研发部    │  ← dept_name 重复存储！
└────┴────────┴─────────┴───────────┘

问题：如果研发部改名了，要改好多行！

dept_name 依赖 dept_id，dept_id 依赖 id（主键）
这叫"传递依赖"：id → dept_id → dept_name

✅ 符合 3NF：拆成两张表
员工表                        部门表
┌────┬────────┬─────────┐    ┌────┬───────────┐
│ id │ name   │ dept_id │    │ id │ dept_name │
├────┼────────┼─────────┤    ├────┼───────────┤
│ 1  │ 张三   │ 1       │ →  │ 1  │ 研发部    │
│ 2  │ 李四   │ 1       │ →  │ 2  │ 市场部    │
└────┴────────┴─────────┘    └────┴───────────┘
```

**实际项目中的平衡：**

三大范式不是绝对教条。有时候为了查询性能，会故意"反范式"设计——允许少量冗余，换取更快的查询速度。

```
例子：订单表存不存商品名称？

严格 3NF：订单表只存商品ID，查商品名时 JOIN 商品表
反范式：订单表直接存商品名称，查询时不需要 JOIN

为什么反范式？因为订单创建后，商品名称通常不会变，
而且订单查询极其频繁，JOIN 会影响性能。
这就是"以空间换时间"。
```

---

### 3. 数据类型选择——给每个字段分配合适的"容器"

选择数据类型就像选择容器：装水用杯子，装米用袋子。

| 类型 | 用途 | 存储范围 | 示例 |
|------|------|---------|------|
| `INT` | 整数 | -21亿 ~ +21亿 | 用户ID、年龄、数量 |
| `BIGINT` | 大整数 | 极大 | 分布式ID（雪花算法） |
| `VARCHAR(n)` | 变长字符串 | 0~65535字节 | 姓名、邮箱、地址 |
| `CHAR(n)` | 定长字符串 | 固定n字节 | 手机号(11)、身份证号(18) |
| `DECIMAL(m,d)` | 精确小数 | m位数字，d位小数 | 金额（不能浮点！） |
| `DATETIME` | 日期时间 | 1000-01-01 ~ 9999-12-31 | 创建时间、更新时间 |
| `DATE` | 日期 | 只有年月日 | 生日、入职日期 |
| `TINYINT` | 小整数 | -128~127 | 性别(1男2女)、状态(0禁用1启用) |
| `TEXT` | 长文本 | 最大64KB | 文章正文、描述 |
| `BLOB` | 二进制数据 | 最大64KB | 图片、文件（一般不存数据库） |

**关键选择原则：**

1. **金额必须用 DECIMAL，不能用 FLOAT/DOUBLE**
   ```
   FLOAT 是近似值！0.1 + 0.2 在计算机中不等于 0.3
   DECIMAL(10,2) 可以精确存储 99999999.99
   ```

2. **VARCHAR vs CHAR**
   - `VARCHAR(50)`：变长，存"张三"只占用 6 字节（UTF-8 下），省空间
   - `CHAR(11)`：定长，手机号固定 11 位，查询速度略快
   - 结论：长度变化大的用 VARCHAR，固定长度的用 CHAR

3. **DATETIME vs TIMESTAMP**
   - `DATETIME`：存"2024-01-15 10:30:00"，范围大，无时区概念
   - `TIMESTAMP`：也存日期时间，但会按服务器时区转换
   - 本课程统一用 `DATETIME`，简单直观

---

### 4. SQL 语句分类与详解

SQL 语句分为四大类，记住口诀：**"姑娘们舞"（DDL、DML、DQL、DCL）**

#### 4.1 DDL（Data Definition Language）— 数据定义语言

DDL 用于定义数据库结构：创建、修改、删除数据库和表。

```sql
-- ============================================
-- 1. 创建数据库
-- ============================================
-- IF NOT EXISTS：如果不存在才创建，避免报错
-- DEFAULT CHARACTER SET utf8mb4：使用 UTF-8 编码，支持 emoji 和中文
CREATE DATABASE IF NOT EXISTS tlias_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 切换到 tlias_db 数据库
USE tlias_db;

-- ============================================
-- 2. 创建部门表
-- ============================================
CREATE TABLE dept (
    -- id：主键，自增
    -- PRIMARY KEY = 主键约束：值唯一且不能为空
    -- AUTO_INCREMENT = 自增：插入时不用指定，自动+1
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    -- name：部门名称
    -- VARCHAR(50) = 最多50个字符
    -- NOT NULL = 不能为空（必须有值）
    name VARCHAR(50) NOT NULL COMMENT '部门名称',

    -- create_time：创建时间
    -- DEFAULT NOW() = 默认值为当前时间
    create_time DATETIME DEFAULT NOW() COMMENT '创建时间',

    -- update_time：更新时间
    update_time DATETIME DEFAULT NOW() COMMENT '修改时间'

) COMMENT = '部门表';  -- 表的注释，说明这张表是做什么的

-- ============================================
-- 3. 创建员工表
-- ============================================
CREATE TABLE emp (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    username VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名',
    -- UNIQUE = 唯一约束：不能有重复值
    -- 用户名不能重复，所以加 UNIQUE

    password VARCHAR(50) COMMENT '密码',

    name VARCHAR(50) COMMENT '姓名',

    gender TINYINT COMMENT '性别 1男 2女',
    -- TINYINT：小整数，只占1字节
    -- 用数字表示性别，比存"男"/"女"省空间

    image VARCHAR(300) COMMENT '头像URL',
    -- 存图片的网络地址，而不是图片本身

    job TINYINT COMMENT '职位 1班主任 2讲师 3学工主管 4教研主管 5咨询师',

    entrydate DATE COMMENT '入职日期',
    -- DATE 只存年月日，不含时分秒

    dept_id INT COMMENT '部门ID',

    create_time DATETIME DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME DEFAULT NOW() COMMENT '更新时间',

    -- 外键约束：dept_id 的值必须在 dept 表的 id 中存在
    FOREIGN KEY (dept_id) REFERENCES dept(id)

) COMMENT = '员工表';

-- ============================================
-- 4. 修改表结构（开发中经常需要）
-- ============================================
-- 添加字段
ALTER TABLE emp ADD COLUMN phone VARCHAR(20) COMMENT '手机号';

-- 修改字段类型
ALTER TABLE emp MODIFY COLUMN phone VARCHAR(30);

-- 删除字段
ALTER TABLE emp DROP COLUMN phone;

-- 删除表（危险操作！）
DROP TABLE IF EXISTS emp;
```

#### 4.2 DML（Data Manipulation Language）— 数据操作语言

DML 用于操作表中的数据：增删改。

```sql
-- ============================================
-- 1. 插入数据（INSERT）
-- ============================================
-- 插入单条
INSERT INTO dept(name) VALUES ('教研部');

-- 插入多条
INSERT INTO dept(name) VALUES
    ('学工部'),
    ('就业部'),
    ('市场部');

-- 插入员工（指定部分字段，未指定的用默认值）
INSERT INTO emp(username, password, name, gender, dept_id)
VALUES ('zhangsan', '123456', '张三', 1, 1);

-- ============================================
-- 2. 更新数据（UPDATE）
-- ============================================
-- ⚠️ 警告：UPDATE 必须加 WHERE，否则全表都会被修改！
UPDATE emp SET name = '张三三' WHERE id = 1;

-- 更新多个字段
UPDATE emp
SET name = '李四',
    gender = 2,
    update_time = NOW()
WHERE id = 2;

-- ❌ 危险！没有 WHERE，所有员工的密码都会变成 'abc'
-- UPDATE emp SET password = 'abc';

-- ============================================
-- 3. 删除数据（DELETE）
-- ============================================
-- ⚠️ 警告：DELETE 必须加 WHERE，否则全表数据都会被删除！
DELETE FROM emp WHERE id = 1;

-- ❌ 危险！没有 WHERE，所有数据都没了
-- DELETE FROM emp;

-- 清空表（重置自增ID）
TRUNCATE TABLE emp;
-- TRUNCATE 比 DELETE 快，因为它不记录日志，但无法回滚
```

#### 4.3 DQL（Data Query Language）— 数据查询语言

DQL 是 SQL 中最常用、最复杂的部分，用于查询数据。

```sql
-- ============================================
-- 基础查询
-- ============================================

-- 1. 查询所有字段（* 表示所有列）
SELECT * FROM emp;

-- 2. 查询指定字段
SELECT id, name, gender FROM emp;

-- 3. 给字段起别名（让结果更易读）
SELECT name AS '姓名', gender AS '性别' FROM emp;
-- AS 可以省略：SELECT name '姓名' FROM emp;

-- 4. 去重查询
SELECT DISTINCT dept_id FROM emp;  -- 有哪些部门有员工

-- 5. 条件查询（WHERE）
SELECT * FROM emp WHERE gender = 1;           -- 男员工
SELECT * FROM emp WHERE id > 5;                -- ID大于5
SELECT * FROM emp WHERE name = '张三';          -- 精确匹配
SELECT * FROM emp WHERE name LIKE '张%';        -- 姓张的（%匹配任意字符）
SELECT * FROM emp WHERE name LIKE '%三';        -- 名字以三结尾
SELECT * FROM emp WHERE name LIKE '%张%';       -- 名字包含"张"

-- 6. 多条件查询（AND / OR）
SELECT * FROM emp WHERE gender = 1 AND dept_id = 1;   -- 研发部男员工
SELECT * FROM emp WHERE dept_id = 1 OR dept_id = 2;   -- 研发部或市场部

-- 7. 范围查询
SELECT * FROM emp WHERE id BETWEEN 1 AND 10;          -- ID在1到10之间
SELECT * FROM emp WHERE dept_id IN (1, 2, 3);         -- 部门是1、2、3之一

-- 8. 空值判断
SELECT * FROM emp WHERE image IS NULL;                -- 没有头像的
SELECT * FROM emp WHERE image IS NOT NULL;            -- 有头像的

-- 9. 排序（ORDER BY）
SELECT * FROM emp ORDER BY entrydate DESC;            -- 按入职日期降序（新的在前）
SELECT * FROM emp ORDER BY entrydate ASC;             -- 升序（老的在前面）
SELECT * FROM emp ORDER BY dept_id ASC, entrydate DESC; -- 先按部门升序，再按日期降序

-- ============================================
-- 聚合查询（统计）
-- ============================================
-- COUNT：计数
SELECT COUNT(*) FROM emp;                             -- 员工总数
SELECT COUNT(*) FROM emp WHERE gender = 1;            -- 男员工数

-- SUM / AVG / MAX / MIN
SELECT SUM(salary) FROM emp;                          -- 工资总和
SELECT AVG(salary) FROM emp;                          -- 平均工资
SELECT MAX(entrydate) FROM emp;                       -- 最晚入职日期
SELECT MIN(entrydate) FROM emp;                       -- 最早入职日期

-- GROUP BY：分组统计
-- 查询每个部门的员工人数
SELECT dept_id, COUNT(*) AS emp_count
FROM emp
GROUP BY dept_id;

-- HAVING：对分组结果过滤（WHERE 不能用于聚合条件）
-- 查询员工数超过 5 人的部门
SELECT dept_id, COUNT(*) AS emp_count
FROM emp
GROUP BY dept_id
HAVING COUNT(*) > 5;

-- ============================================
-- 多表查询（JOIN）
-- ============================================

-- 1. 内连接（INNER JOIN）：只返回两表匹配的数据
-- 查询员工及其部门名称
SELECT
    e.id,
    e.name AS emp_name,
    d.name AS dept_name
FROM emp e                          -- e 是 emp 的别名
INNER JOIN dept d ON e.dept_id = d.id;  -- d 是 dept 的别名，连接条件是 dept_id = id

-- 结果：
-- ┌────┬──────────┬──────────┐
-- │ id │ emp_name │ dept_name│
-- ├────┼──────────┼──────────┤
-- │ 1  │ 张三     │ 研发部   │
-- │ 2  │ 李四     │ 市场部   │
-- └────┴──────────┴──────────┘
-- 注意：没有部门的员工（dept_id 为 NULL 或不存在）不会出现在结果中

-- 2. 左连接（LEFT JOIN）：返回左表全部，右表不匹配为 NULL
SELECT e.name, d.name AS dept_name
FROM emp e
LEFT JOIN dept d ON e.dept_id = d.id;

-- 结果：
-- ┌──────────┬──────────┐
-- │ name     │ dept_name│
-- ├──────────┼──────────┤
-- │ 张三     │ 研发部   │
-- │ 李四     │ NULL     │  ← 左连接会保留没有部门的员工
-- └──────────┴──────────┘

-- 3. 右连接（RIGHT JOIN）：和左连接相反
-- 实际中很少用，通常把表顺序换一下用 LEFT JOIN

-- 4. 子查询（一个查询的结果作为另一个查询的条件）
-- 查询"研发部"的所有员工
SELECT * FROM emp WHERE dept_id = (
    SELECT id FROM dept WHERE name = '研发部'
);

-- 查询有员工的部门
SELECT * FROM dept WHERE id IN (
    SELECT DISTINCT dept_id FROM emp WHERE dept_id IS NOT NULL
);
```

---

### 5. 索引——让查询飞起来的"目录"

#### 5.1 什么是索引

想象一本 1000 页的书，没有目录你要怎么找某一章？
- 没有目录：从第 1 页翻到第 1000 页，一页一页看
- 有目录：翻到目录页，找到对应页码，直接翻到那一页

索引就是数据库的"目录"。

```sql
-- 没有索引时：
SELECT * FROM emp WHERE name = '张三';
-- MySQL 必须扫描全表，一行一行比对 name 字段
-- 100 万条数据可能要几秒

-- 给 name 字段加索引后：
CREATE INDEX idx_emp_name ON emp(name);
-- MySQL 用类似二叉树的结构快速定位
-- 100 万条数据可能只要几毫秒
```

#### 5.2 索引的类型

| 类型 | 说明 | 使用场景 |
|------|------|---------|
| 主键索引 | 主键自动创建 | 每条记录的唯一标识 |
| 唯一索引 | 值不能重复 | 用户名、手机号、邮箱 |
| 普通索引 | 加速查询 | WHERE、ORDER BY、JOIN 的字段 |
| 组合索引 | 多个字段联合 | 经常一起查询的字段 |
| 全文索引 | 文本搜索 | 文章内容搜索 |

#### 5.3 创建索引

```sql
-- 普通索引
CREATE INDEX idx_emp_name ON emp(name);

-- 唯一索引
CREATE UNIQUE INDEX idx_emp_username ON emp(username);

-- 组合索引（最左前缀原则）
-- 如果你经常按"部门 + 职位"查询，建组合索引比两个单独索引更高效
CREATE INDEX idx_emp_dept_job ON emp(dept_id, job);
-- 查询条件必须包含 dept_id 才能用到这个索引
-- WHERE dept_id = 1 AND job = 2  → 用到索引
-- WHERE job = 2                  → 用不到索引（缺少 dept_id）

-- 在 CREATE TABLE 时就定义索引
CREATE TABLE emp (
    id INT PRIMARY KEY AUTO_INCREMENT,  -- 主键索引（自动创建）
    username VARCHAR(20) UNIQUE,         -- 唯一索引
    name VARCHAR(50),
    dept_id INT,
    INDEX idx_name (name),               -- 普通索引
    INDEX idx_dept_id (dept_id)          -- 普通索引
);
```

#### 5.4 索引的使用原则

```
✅ 应该加索引的情况：
   • WHERE 条件中的字段
   • ORDER BY 排序的字段
   • JOIN ON 连接的字段
   • 区分度高的字段（如身份证号）

❌ 不应该加索引的情况：
   • 频繁更新的字段（索引也需要维护，更新会变慢）
   • 区分度低的字段（如性别只有男/女，加索引意义不大）
   • 小表（数据量少于 1000 行，全表扫描更快）
   • 很少查询的字段

⚠️ 索引不是越多越好：
   • 每个索引都占用磁盘空间
   • 插入、更新、删除时，MySQL 要维护索引，会变慢
   • 一般一张表不超过 5 个索引
```

#### 5.5 查看查询是否使用了索引

```sql
-- 在 SELECT 前加 EXPLAIN，查看执行计划
EXPLAIN SELECT * FROM emp WHERE name = '张三';

-- 关注结果中的 key 字段：
-- key = idx_emp_name  → 使用了索引，很好！
-- key = NULL          → 没有使用索引，考虑优化！
```

---

### 6. 版本 A 核心表结构详解

```sql
-- ============================================
-- 部门表（dept）
-- ============================================
-- 存储公司的部门信息，结构最简单
CREATE TABLE dept (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '部门名称',
    create_time DATETIME DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME DEFAULT NOW() COMMENT '更新时间'
) COMMENT = '部门表';

-- ============================================
-- 员工表（emp）
-- ============================================
-- 存储员工基本信息，是系统的核心表
CREATE TABLE emp (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名（登录用）',
    password VARCHAR(50) COMMENT '密码（后续用 JWT 后不再需要）',
    name VARCHAR(50) COMMENT '真实姓名',
    gender TINYINT COMMENT '性别：1男 2女',
    image VARCHAR(300) COMMENT '头像图片URL',
    job TINYINT COMMENT '职位：1班主任 2讲师 3学工主管 4教研主管 5咨询师',
    entrydate DATE COMMENT '入职日期',
    dept_id INT COMMENT '所属部门ID（外键）',
    create_time DATETIME DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME DEFAULT NOW() COMMENT '更新时间',
    FOREIGN KEY (dept_id) REFERENCES dept(id)
) COMMENT = '员工表';

-- ============================================
-- 工作经历表（emp_expr）
-- ============================================
-- 一个员工有多段工作经历（一对多关系）
-- 所以单独建一张表，通过 emp_id 关联到员工表
CREATE TABLE emp_expr (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id INT NOT NULL COMMENT '所属员工ID',
    begin DATE COMMENT '开始日期',
    end DATE COMMENT '结束日期',
    company VARCHAR(100) COMMENT '公司名称',
    job VARCHAR(50) COMMENT '职位',
    FOREIGN KEY (emp_id) REFERENCES emp(id)
) COMMENT = '工作经历表';

-- ============================================
-- 操作日志表（operate_log）
-- ============================================
-- 记录用户的操作行为，用于审计和问题排查
CREATE TABLE operate_log (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    operate_emp_id INT COMMENT '操作人ID',
    operate_time DATETIME DEFAULT NOW() COMMENT '操作时间',
    class_name VARCHAR(200) COMMENT '操作的类名',
    method_name VARCHAR(100) COMMENT '操作的方法名',
    method_params VARCHAR(2000) COMMENT '方法参数',
    return_value VARCHAR(2000) COMMENT '返回值',
    cost_time BIGINT COMMENT '执行耗时（毫秒）'
) COMMENT = '操作日志表';
```

---

## 动手练习

### 练习 1：创建数据库和表

**目标**：在 MySQL 中创建完整的数据库和表结构。

**步骤**：

1. 打开 Navicat 或 DataGrip，连接到 MySQL
2. 新建查询，执行以下 SQL：

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS tlias_db DEFAULT CHARACTER SET utf8mb4;
USE tlias_db;

-- 创建部门表
CREATE TABLE dept (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '部门名称',
    create_time DATETIME DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME DEFAULT NOW() COMMENT '更新时间'
) COMMENT = '部门表';

-- 创建员工表
CREATE TABLE emp (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(50) COMMENT '密码',
    name VARCHAR(50) COMMENT '姓名',
    gender TINYINT COMMENT '性别 1男 2女',
    image VARCHAR(300) COMMENT '头像URL',
    job TINYINT COMMENT '职位',
    entrydate DATE COMMENT '入职日期',
    dept_id INT COMMENT '部门ID',
    create_time DATETIME DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME DEFAULT NOW() COMMENT '更新时间',
    FOREIGN KEY (dept_id) REFERENCES dept(id)
) COMMENT = '员工表';
```

3. 插入测试数据：

```sql
-- 插入部门
INSERT INTO dept(name) VALUES
    ('教研部'), ('学工部'), ('就业部'), ('市场部');

-- 插入员工
INSERT INTO emp(username, password, name, gender, job, entrydate, dept_id) VALUES
    ('zhangsan', '123456', '张三', 1, 2, '2020-03-15', 1),
    ('lisi', '123456', '李四', 2, 1, '2021-06-20', 2),
    ('wangwu', '123456', '王五', 1, 3, '2019-01-10', 1),
    ('zhaoliu', '123456', '赵六', 2, 2, '2022-09-01', 3),
    ('sunqi', '123456', '孙七', 1, 4, '2018-05-18', 1);
```

### 练习 2：SQL 查询练习

在查询编辑器中执行以下 SQL，验证结果：

```sql
-- 练习 1：查询所有男员工，按入职日期降序排列
SELECT * FROM emp WHERE gender = 1 ORDER BY entrydate DESC;

-- 练习 2：查询每个部门的员工人数
SELECT d.name AS dept_name, COUNT(*) AS emp_count
FROM emp e
LEFT JOIN dept d ON e.dept_id = d.id
GROUP BY e.dept_id, d.name;

-- 练习 3：查询入职超过 1 年的员工及其所属部门名称
SELECT e.name, d.name AS dept_name, e.entrydate
FROM emp e
INNER JOIN dept d ON e.dept_id = d.id
WHERE e.entrydate <= DATE_SUB(NOW(), INTERVAL 1 YEAR);

-- 练习 4：查询姓"张"的员工
SELECT * FROM emp WHERE name LIKE '张%';

-- 练习 5：查询没有分配部门的员工
SELECT * FROM emp WHERE dept_id IS NULL;
```

---

## 常见错误排查

### 阶段 1：连接与权限问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Can't connect to MySQL server` | MySQL 服务没启动 | 1. Windows：服务管理器找到 MySQL 启动<br>2. Mac/Linux：`sudo systemctl start mysql` |
| `Access denied for user 'root'@'localhost'` | 用户名或密码错误 | 检查连接配置中的用户名和密码 |
| `Unknown database 'tlias_db'` | 数据库不存在 | 先执行 `CREATE DATABASE tlias_db` |

### 阶段 2：表结构问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Table 'tlias_db.emp' doesn't exist` | 表未创建或表名拼写错误 | 1. 检查表名大小写（Linux 区分大小写）<br>2. 先执行 CREATE TABLE |
| `Duplicate entry 'zhangsan' for key 'username'` | 唯一约束冲突 | 检查要插入的数据是否已存在 |
| `Cannot add foreign key constraint` | 外键类型不匹配或数据冲突 | 1. 确保关联字段类型一致<br>2. 确保被引用的数据已存在 |
| `Data too long for column 'name'` | 数据长度超过字段限制 | 缩短数据或修改字段类型为更大的 VARCHAR |

### 阶段 3：SQL 语法问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `You have an error in your SQL syntax` | SQL 语法错误 | 1. 检查关键字拼写<br>2. 检查引号、括号是否配对<br>3. 检查中文标点（如中文逗号） |
| `Column 'name' in field list is ambiguous` | 多表查询时字段名冲突 | 给字段加表别名前缀：`e.name` |
| `Every derived table must have its own alias` | 子查询没起别名 | 给子查询结果起别名：`SELECT * FROM (...) AS t` |

### 阶段 4：性能问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 查询极慢 | 没有索引，全表扫描 | 用 `EXPLAIN` 分析，在 WHERE 字段上加索引 |
| `Lock wait timeout exceeded` | 事务未提交导致锁等待 | 检查是否有未关闭的事务，提交或回滚 |
| 插入变慢 | 索引过多 | 减少不必要的索引，或批量插入 |

---

## 本节小结

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        MySQL 数据库核心知识                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  基本概念：                                                               │
│    数据库 = 超级 Excel                                                   │
│    表 = Sheet，字段 = 列，记录 = 行                                       │
│    主键 = 唯一标识，外键 = 表之间的关联                                    │
│                                                                          │
│  设计规范：                                                               │
│    • 命名：小写 + 下划线（emp、create_time）                              │
│    • 1NF：字段不可再分                                                    │
│    • 2NF：非主键完全依赖主键（一张表一件事）                               │
│    • 3NF：消除传递依赖（减少冗余）                                         │
│                                                                          │
│  数据类型：                                                               │
│    • INT/BIGINT：整数 ID                                                 │
│    • VARCHAR/CHAR：字符串                                                │
│    • DECIMAL：金额（不能用 FLOAT！）                                       │
│    • DATETIME：日期时间                                                  │
│    • TINYINT：状态、性别                                                 │
│                                                                          │
│  SQL 分类：                                                               │
│    • DDL：CREATE / ALTER / DROP（定义结构）                               │
│    • DML：INSERT / UPDATE / DELETE（操作数据）                            │
│    • DQL：SELECT（查询数据）——最常用                                      │
│                                                                          │
│  索引：                                                                   │
│    • 作用：加速查询，类似书的目录                                         │
│    • 原则：WHERE/ORDER BY/JOIN 字段加索引，小表和不常查的字段不加           │
│    • 查看：EXPLAIN SELECT ...                                            │
│                                                                          │
│  避坑指南：                                                               │
│    • UPDATE/DELETE 必须加 WHERE！                                        │
│    • 金额用 DECIMAL，不用 FLOAT                                          │
│    • 数据库字符集用 utf8mb4（支持 emoji）                                 │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 参考文档

- [MySQL 8.0 官方文档](https://dev.mysql.com/doc/refman/8.0/en/)
- [MySQL 索引优化](https://dev.mysql.com/doc/refman/8.0/en/optimization-indexes.html)
- [SQL 必知必会（书籍）](https://book.douban.com/subject/35167240/)
