# 条件搜索与动态 SQL

## 学习目标

学完本节后，你将能够：

- 理解为什么需要动态 SQL（拼接 SQL 的痛苦）
- 掌握 MyBatis 全部 7 个动态标签：`<if>` / `<where>` / `<set>` / `<foreach>` / `<choose>` / `<trim>` / `<sql>+<include>`
- 能写出"任意条件组合"的搜索接口
- 会用 `<foreach>` 实现批量操作（批量删除、批量插入）
- 知道动态 SQL 的常见坑及调试方法

---

## 核心知识点

### 1. 为什么需要动态 SQL——"硬编码 SQL"是噩梦

#### 1.1 真实场景

员工管理页面有一个搜索框，用户可以输入：

- 姓名（可选）
- 性别（可选）
- 入职日期范围（可选）

```
搜索框：
  ┌──────────────────────────────────────────────────────────┐
  │ 姓名：[      ]   性别：[全部▼]   入职：[    ] ~ [    ]    │
  │                                                [搜索]     │
  └──────────────────────────────────────────────────────────┘
```

**用户的输入有 N 种组合**：

| 场景 | name | gender | begin | end |
|------|------|--------|-------|-----|
| 什么都不填 | null | null | null | null |
| 只搜姓名 | "张" | null | null | null |
| 只搜性别 | null | 1 | null | null |
| 姓名 + 性别 | "张" | 1 | null | null |
| 全部都填 | "张" | 1 | 2024-01-01 | 2024-12-31 |
| ……（共 16 种组合） | ... | ... | ... | ... |

#### 1.2 笨办法：写 16 个 SQL

```java
// ❌ 噩梦写法
if (name != null && gender == null && begin == null && end == null) {
    sql = "SELECT * FROM emp WHERE name LIKE ?";
}
else if (name == null && gender != null && begin == null && end == null) {
    sql = "SELECT * FROM emp WHERE gender = ?";
}
else if (name != null && gender != null && begin == null && end == null) {
    sql = "SELECT * FROM emp WHERE name LIKE ? AND gender = ?";
}
// ……再写 13 个分支……
```

**4 个条件 → 2⁴ = 16 种组合，10 个条件 → 1024 种组合，根本写不过来。**

#### 1.3 中间办法：拼字符串

```java
// ⚠️ 能跑但容易出 bug
StringBuilder sb = new StringBuilder("SELECT * FROM emp WHERE 1=1");
List<Object> params = new ArrayList<>();
if (name != null) {
    sb.append(" AND name LIKE ?");
    params.add("%" + name + "%");
}
if (gender != null) {
    sb.append(" AND gender = ?");
    params.add(gender);
}
// ……
```

**问题：**

- 容易拼错（漏空格、AND 接连）
- SQL 散落在 Java 代码里，难以阅读和维护
- 容易引入 SQL 注入风险（如果不用 PreparedStatement）

#### 1.4 终极方案：动态 SQL 标签

MyBatis 提供一组"积木式"的标签，让 SQL 像搭积木一样组装：

```xml
<select id="list" resultType="Emp">
    SELECT * FROM emp
    <where>
        <if test="name != null">AND name LIKE CONCAT('%', #{name}, '%')</if>
        <if test="gender != null">AND gender = #{gender}</if>
        <if test="begin != null">AND entrydate &gt;= #{begin}</if>
        <if test="end != null">AND entrydate &lt;= #{end}</if>
    </where>
</select>
```

**好处：**

- SQL 集中在 XML 里，易读
- MyBatis 自动处理 WHERE/AND/逗号等细节
- 一份 SQL 应对 16 种组合

---

### 2. `<if>` 标签——"如果……才拼接"

#### 2.1 基本用法

```xml
<select id="list" resultType="Emp">
    SELECT * FROM emp
    WHERE 1 = 1
    <if test="name != null and name != ''">
        AND name LIKE CONCAT('%', #{name}, '%')
    </if>
    <if test="gender != null">
        AND gender = #{gender}
    </if>
</select>
```

**`test` 属性写的是 OGNL 表达式：**

| 表达式 | 含义 |
|--------|------|
| `name != null` | 不为 null |
| `name != null and name != ''` | 不为 null 且不为空字符串 |
| `gender != null` | gender 不为 null（注意：Integer 类型不能用 `!= ''`） |
| `age > 18` | age 大于 18 |
| `status == 1` | 状态为 1 |
| `list != null and list.size() > 0` | 集合不为空 |

#### 2.2 为什么要写 `WHERE 1 = 1`

**问题：** 如果所有 `<if>` 都不成立，SQL 变成：

```sql
SELECT * FROM emp WHERE
```

**报语法错误！** 加上 `1 = 1` 可以保证 WHERE 后永远有内容：

```sql
SELECT * FROM emp WHERE 1 = 1  -- 没有任何条件时也能跑
```

但这种写法**不优雅**，更好的做法是用下面的 `<where>` 标签。

---

### 3. `<where>` 标签——"智能 WHERE"

#### 3.1 解决两个痛点

```xml
<select id="list" resultType="Emp">
    SELECT * FROM emp
    <where>
        <if test="name != null">AND name LIKE CONCAT('%', #{name}, '%')</if>
        <if test="gender != null">AND gender = #{gender}</if>
    </where>
</select>
```

**`<where>` 帮我们做了三件事：**

1. 自动拼上 `WHERE` 关键字
2. 自动去掉**第一个多余的 `AND` 或 `OR`**
3. 如果所有 `<if>` 都不成立，**不生成 WHERE**（避免空 WHERE 报错）

#### 3.2 三种情况对比

```
┌───────────────────────────────────────────────────────────────────┐
│ 情况 1：什么都不传                                                  │
│   生成：SELECT * FROM emp                                          │
│   说明：自动省略了 WHERE                                            │
├───────────────────────────────────────────────────────────────────┤
│ 情况 2：只传 name="张"                                              │
│   生成：SELECT * FROM emp WHERE name LIKE CONCAT('%', '张', '%')   │
│   说明：自动加上 WHERE，并去掉了第一个 AND                           │
├───────────────────────────────────────────────────────────────────┤
│ 情况 3：传 name="张" 和 gender=1                                    │
│   生成：SELECT * FROM emp                                          │
│         WHERE name LIKE CONCAT('%','张','%')                       │
│         AND gender = 1                                             │
│   说明：第一个 AND 被去掉，第二个 AND 保留                           │
└───────────────────────────────────────────────────────────────────┘
```

#### 3.3 完整示例（Tlias 员工搜索）

```xml
<select id="list" resultType="com.tlias.pojo.Emp">
    SELECT
        e.id, e.username, e.name, e.gender,
        e.entrydate, e.dept_id,
        d.name AS dept_name
    FROM emp e
    LEFT JOIN dept d ON e.dept_id = d.id
    <where>
        <if test="name != null and name != ''">
            AND e.name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="gender != null">
            AND e.gender = #{gender}
        </if>
        <if test="begin != null">
            AND e.entrydate &gt;= #{begin}
        </if>
        <if test="end != null">
            AND e.entrydate &lt;= #{end}
        </if>
    </where>
    ORDER BY e.update_time DESC
</select>
```

---

### 4. `<set>` 标签——"智能 SET"

#### 4.1 update 语句的痛点

```xml
<!-- ❌ 错误写法：尾部多余的逗号会报语法错 -->
<update id="update">
    UPDATE emp
    SET
        <if test="name != null">name = #{name},</if>
        <if test="gender != null">gender = #{gender},</if>
        <if test="image != null">image = #{image},</if>
    WHERE id = #{id}
</update>
```

**如果只传了 `name`，生成的 SQL 是：**

```sql
UPDATE emp SET name = '张三', WHERE id = 1
                          ↑ 多了个逗号，语法错！
```

#### 4.2 用 `<set>` 解决

```xml
<update id="update">
    UPDATE emp
    <set>
        <if test="username != null">username = #{username},</if>
        <if test="name != null">name = #{name},</if>
        <if test="gender != null">gender = #{gender},</if>
        <if test="image != null">image = #{image},</if>
        <if test="job != null">job = #{job},</if>
        <if test="entrydate != null">entrydate = #{entrydate},</if>
        <if test="deptId != null">dept_id = #{deptId},</if>
        update_time = NOW()      <!-- 这一行永远会拼接，所以末尾不带逗号 -->
    </set>
    WHERE id = #{id}
</update>
```

**`<set>` 帮我们做了两件事：**

1. 自动拼上 `SET` 关键字
2. 自动去掉**最后一个多余的逗号**

#### 4.3 调用示例

```java
Emp emp = new Emp();
emp.setId(1);
emp.setName("张三");
emp.setGender(1);
// 其他字段都是 null
empMapper.update(emp);
```

**生成的 SQL：**

```sql
UPDATE emp
SET name = '张三', gender = 1, update_time = NOW()
WHERE id = 1
```

只更新传入的字段，其他字段保持原值（**部分更新**）。这就是经典的"修改员工"接口实现。

---

### 5. `<foreach>` 标签——"循环遍历集合"

#### 5.1 应用场景：批量删除

需求：前端传一个 id 列表 `[1, 2, 3]`，后端要执行：

```sql
DELETE FROM emp WHERE id IN (1, 2, 3)
```

#### 5.2 写法

```xml
<delete id="deleteByIds">
    DELETE FROM emp
    WHERE id IN
    <foreach collection="ids" item="id" separator="," open="(" close=")">
        #{id}
    </foreach>
</delete>
```

#### 5.3 属性详解

| 属性 | 含义 | 例子 |
|------|------|------|
| `collection` | 集合参数名（在 Java 中传入的变量名或 `@Param` 名） | `ids` |
| `item` | 遍历时每个元素的临时变量名 | `id` |
| `separator` | 元素之间的分隔符 | `,` |
| `open` | 整个循环的开头 | `(` |
| `close` | 整个循环的结尾 | `)` |
| `index` | 当前元素的索引（List 时）或 key（Map 时）（可选） | `i` |

#### 5.4 生成的 SQL

```java
List<Integer> ids = Arrays.asList(1, 2, 3);
empMapper.deleteByIds(ids);
```

**生成：**

```sql
DELETE FROM emp WHERE id IN ( 1 , 2 , 3 )
```

#### 5.5 高级用法：批量插入

```xml
<insert id="insertBatch">
    INSERT INTO emp (name, gender, entrydate)
    VALUES
    <foreach collection="empList" item="emp" separator=",">
        (#{emp.name}, #{emp.gender}, #{emp.entrydate})
    </foreach>
</insert>
```

**调用：**

```java
List<Emp> empList = ...;
empMapper.insertBatch(empList);
```

**生成：**

```sql
INSERT INTO emp (name, gender, entrydate) VALUES
    ('张三', 1, '2020-01-01'),
    ('李四', 2, '2021-03-15'),
    ('王五', 1, '2022-07-08')
```

**比循环单条插入快几十倍！**

#### 5.6 Mapper 接口的对应写法

```java
@Mapper
public interface EmpMapper {
    // ① 单参数集合：参数名要与 collection 一致
    void deleteByIds(@Param("ids") List<Integer> ids);

    // ② 多参数：通过 @Param 区分
    void insertBatch(@Param("empList") List<Emp> empList);
}
```

---

### 6. `<choose> / <when> / <otherwise>` 标签——"if/else if/else"

#### 6.1 应用场景

需求：员工排序方式：

- 如果传了 `sortBy`，按指定字段排序
- 如果没传 `sortBy`，按 update_time 倒序

```xml
<select id="list" resultType="Emp">
    SELECT * FROM emp
    ORDER BY
    <choose>
        <when test="sortBy == 'name'">name ASC</when>
        <when test="sortBy == 'entrydate'">entrydate DESC</when>
        <otherwise>update_time DESC</otherwise>
    </choose>
</select>
```

**等价于 Java 代码：**

```java
if (sortBy.equals("name")) {
    sql += "ORDER BY name ASC";
} else if (sortBy.equals("entrydate")) {
    sql += "ORDER BY entrydate DESC";
} else {
    sql += "ORDER BY update_time DESC";
}
```

#### 6.2 关键特性

- `<when>` 从上到下依次判断，**只有第一个为真的会被选中**
- `<otherwise>` 是兜底（相当于 else）
- 所有 `<when>` 都不成立时才会进入 `<otherwise>`

---

### 7. `<trim>` 标签——"通用裁剪工具"

#### 7.1 `<where>` 和 `<set>` 都是 `<trim>` 的封装

```xml
<!-- 等价于 <where> -->
<trim prefix="WHERE" prefixOverrides="AND |OR ">
    ...
</trim>

<!-- 等价于 <set> -->
<trim prefix="SET" suffixOverrides=",">
    ...
</trim>
```

#### 7.2 属性详解

| 属性 | 含义 |
|------|------|
| `prefix` | 在拼接结果前加什么 |
| `suffix` | 在拼接结果后加什么 |
| `prefixOverrides` | 去掉拼接结果开头的什么内容（用 `|` 分隔多个） |
| `suffixOverrides` | 去掉拼接结果末尾的什么内容 |

#### 7.3 实战场景：MySQL 的 `INSERT ... ON DUPLICATE KEY UPDATE`

需求：根据传入的字段动态更新

```xml
<insert id="upsert">
    INSERT INTO emp (id, name, gender)
    VALUES (#{id}, #{name}, #{gender})
    ON DUPLICATE KEY UPDATE
    <trim suffixOverrides=",">
        <if test="name != null">name = #{name},</if>
        <if test="gender != null">gender = #{gender},</if>
    </trim>
</insert>
```

---

### 8. `<sql>` + `<include>` 标签——"代码复用"

#### 8.1 应用场景：消除重复字段列表

很多查询都需要列出"完整字段列表"：

```xml
<select id="list" resultType="Emp">
    SELECT id, username, name, gender, entrydate, dept_id
    FROM emp
</select>

<select id="getById" resultType="Emp">
    SELECT id, username, name, gender, entrydate, dept_id
    FROM emp WHERE id = #{id}
</select>
```

**重复了！** 提取成可复用片段：

#### 8.2 写法

```xml
<!-- 定义片段 -->
<sql id="empColumns">
    id, username, name, gender, entrydate, dept_id
</sql>

<!-- 在 SELECT 中引用 -->
<select id="list" resultType="Emp">
    SELECT <include refid="empColumns"/>
    FROM emp
</select>

<select id="getById" resultType="Emp">
    SELECT <include refid="empColumns"/>
    FROM emp WHERE id = #{id}
</select>
```

字段一改改一处，所有引用处自动同步。

---

### 9. 完整实战示例：Tlias 员工搜索

#### 9.1 需求

```
┌──────────────────────────────────────────────────────────────────┐
│  员工搜索功能                                                     │
├──────────────────────────────────────────────────────────────────┤
│  ① 按姓名模糊搜索（可空）                                          │
│  ② 按性别筛选（可空）                                              │
│  ③ 按入职日期范围筛选（可空）                                      │
│  ④ 按部门筛选（可空）                                              │
│  ⑤ 联表查询出部门名称                                              │
│  ⑥ 默认按更新时间倒序                                              │
│  ⑦ 支持自定义排序字段                                              │
└──────────────────────────────────────────────────────────────────┘
```

#### 9.2 Mapper 接口

```java
@Mapper
public interface EmpMapper {

    /**
     * 多条件搜索员工
     */
    List<Emp> list(@Param("name") String name,
                   @Param("gender") Integer gender,
                   @Param("begin") LocalDate begin,
                   @Param("end") LocalDate end,
                   @Param("deptId") Integer deptId,
                   @Param("sortBy") String sortBy);

    /**
     * 批量删除
     */
    void deleteByIds(@Param("ids") List<Integer> ids);

    /**
     * 部分字段更新
     */
    void update(Emp emp);
}
```

#### 9.3 完整 EmpMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.tlias.mapper.EmpMapper">

    <!-- ============= 公共字段片段 ============= -->
    <sql id="empColumns">
        e.id, e.username, e.name, e.gender, e.image,
        e.job, e.entrydate, e.dept_id, e.create_time, e.update_time
    </sql>

    <!-- ============= 多条件搜索 ============= -->
    <select id="list" resultType="com.tlias.pojo.Emp">
        SELECT
            <include refid="empColumns"/>,
            d.name AS dept_name
        FROM emp e
        LEFT JOIN dept d ON e.dept_id = d.id
        <where>
            <if test="name != null and name != ''">
                AND e.name LIKE CONCAT('%', #{name}, '%')
            </if>
            <if test="gender != null">
                AND e.gender = #{gender}
            </if>
            <if test="begin != null">
                AND e.entrydate &gt;= #{begin}
            </if>
            <if test="end != null">
                AND e.entrydate &lt;= #{end}
            </if>
            <if test="deptId != null">
                AND e.dept_id = #{deptId}
            </if>
        </where>
        ORDER BY
        <choose>
            <when test="sortBy == 'name'">e.name ASC</when>
            <when test="sortBy == 'entrydate'">e.entrydate DESC</when>
            <otherwise>e.update_time DESC</otherwise>
        </choose>
    </select>

    <!-- ============= 批量删除 ============= -->
    <delete id="deleteByIds">
        DELETE FROM emp
        WHERE id IN
        <foreach collection="ids" item="id" separator="," open="(" close=")">
            #{id}
        </foreach>
    </delete>

    <!-- ============= 部分字段更新 ============= -->
    <update id="update">
        UPDATE emp
        <set>
            <if test="username != null">username = #{username},</if>
            <if test="name != null">name = #{name},</if>
            <if test="gender != null">gender = #{gender},</if>
            <if test="image != null">image = #{image},</if>
            <if test="job != null">job = #{job},</if>
            <if test="entrydate != null">entrydate = #{entrydate},</if>
            <if test="deptId != null">dept_id = #{deptId},</if>
            update_time = NOW()
        </set>
        WHERE id = #{id}
    </update>

</mapper>
```

#### 9.4 Service 层

```java
@Service
public class EmpServiceImpl implements EmpService {

    @Autowired
    private EmpMapper empMapper;

    @Override
    public PageInfo<Emp> page(Integer page, Integer pageSize,
                              String name, Integer gender,
                              LocalDate begin, LocalDate end,
                              Integer deptId, String sortBy) {
        PageHelper.startPage(page, pageSize);
        List<Emp> list = empMapper.list(name, gender, begin, end, deptId, sortBy);
        return new PageInfo<>(list);
    }

    @Override
    public void deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        empMapper.deleteByIds(ids);
    }

    @Override
    public void update(Emp emp) {
        empMapper.update(emp);
    }
}
```

#### 9.5 Controller 层

```java
@RestController
@RequestMapping("/emps")
public class EmpController {

    @Autowired
    private EmpService empService;

    @GetMapping
    public Result page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer gender,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
            @RequestParam(required = false) Integer deptId,
            @RequestParam(required = false) String sortBy) {

        PageInfo<Emp> pageInfo = empService.page(page, pageSize, name, gender,
                                                  begin, end, deptId, sortBy);
        return Result.success(pageInfo);
    }

    /**
     * 批量删除
     * DELETE /emps/1,2,3
     */
    @DeleteMapping("/{ids}")
    public Result delete(@PathVariable List<Integer> ids) {
        empService.deleteByIds(ids);
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody Emp emp) {
        empService.update(emp);
        return Result.success();
    }
}
```

---

### 10. 前端搜索表单集成

#### 10.1 Vue 3 + Element Plus 实现

```vue
<script setup>
import { ref, reactive } from 'vue';
import { getEmpList, deleteEmps } from '@/api/emp';

const queryForm = reactive({
    page: 1,
    pageSize: 10,
    name: '',
    gender: null,
    dateRange: [],
    deptId: null
});

const tableData = ref([]);
const total = ref(0);
const selectedIds = ref([]);

// 搜索
const onSearch = async () => {
    queryForm.page = 1;
    const params = {
        ...queryForm,
        begin: queryForm.dateRange?.[0],
        end: queryForm.dateRange?.[1]
    };
    delete params.dateRange;

    const res = await getEmpList(params);
    tableData.value = res.list;
    total.value = res.total;
};

// 重置
const onReset = () => {
    queryForm.name = '';
    queryForm.gender = null;
    queryForm.dateRange = [];
    queryForm.deptId = null;
    onSearch();
};

// 批量删除
const onBatchDelete = async () => {
    if (selectedIds.value.length === 0) {
        ElMessage.warning('请先勾选要删除的员工');
        return;
    }
    await ElMessageBox.confirm('确定删除选中员工？', '提示', { type: 'warning' });
    await deleteEmps(selectedIds.value);
    onSearch();
};
</script>

<template>
    <!-- 搜索栏 -->
    <el-form :model="queryForm" inline>
        <el-form-item label="姓名">
            <el-input v-model="queryForm.name" placeholder="模糊搜索" />
        </el-form-item>
        <el-form-item label="性别">
            <el-select v-model="queryForm.gender" placeholder="请选择" clearable>
                <el-option label="男" :value="1" />
                <el-option label="女" :value="2" />
            </el-select>
        </el-form-item>
        <el-form-item label="入职日期">
            <el-date-picker
                v-model="queryForm.dateRange"
                type="daterange"
                value-format="YYYY-MM-DD"
            />
        </el-form-item>
        <el-form-item>
            <el-button type="primary" @click="onSearch">搜索</el-button>
            <el-button @click="onReset">重置</el-button>
            <el-button type="danger" @click="onBatchDelete">批量删除</el-button>
        </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" @selection-change="(rows) => selectedIds = rows.map(r => r.id)">
        <el-table-column type="selection" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="entrydate" label="入职日期" />
        <el-table-column prop="deptName" label="部门" />
    </el-table>
</template>
```

#### 10.2 API 模块

```javascript
// api/emp.js
import request from '@/utils/request';

export const getEmpList = (params) => request.get('/emps', { params });

export const deleteEmps = (ids) => request.delete(`/emps/${ids.join(',')}`);
```

---

## 动手练习

### 练习 1：观察生成的 SQL

开启 MyBatis SQL 日志，观察不同条件下生成的 SQL：

```yaml
# application.yml
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

**观察：**

1. 调用 `GET /emps` → 应该看到不带 WHERE 的 SQL
2. 调用 `GET /emps?name=张` → 应该看到 `WHERE e.name LIKE ...`
3. 调用 `GET /emps?gender=1&begin=2024-01-01` → 应该看到 WHERE + AND

### 练习 2：实现批量删除

实现 `DELETE /emps/{ids}` 接口，前端传 `1,2,3`，后端把它们一次性删除。

**关键提示：**

```java
@DeleteMapping("/{ids}")
public Result delete(@PathVariable List<Integer> ids) {
    // Spring 会自动把 "1,2,3" 转成 List<Integer>
    empService.deleteByIds(ids);
    return Result.success();
}
```

### 练习 3：实现部分字段更新

测试 `<set>` 标签：

1. 调用 `PUT /emps`，body: `{"id":1, "name":"张三"}` → 只更新 name
2. 调用 `PUT /emps`，body: `{"id":1, "gender":2}` → 只更新 gender
3. 数据库中其他字段应保持原值

---

## 常见错误排查

### 阶段 1：依赖与配置问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Mapped Statements collection does not contain value` | XML 没被加载 | 检查 application.yml 中的 `mybatis.mapper-locations: classpath:mapper/*.xml` |
| XML 文件编辑后不生效 | IDE 没拷贝 XML 到 target 目录 | maven 配置 `<resource>` 包含 XML，或重启项目 |
| `BindingException: Invalid bound statement (not found)` | namespace 与接口全限定名不一致 | XML 的 `namespace` 必须等于 Mapper 接口的全路径 |

### 阶段 2：参数问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Parameter 'xxx' not found` | 参数名不匹配或缺少 @Param | 多参数必须加 `@Param` 注解 |
| `<foreach>` 报错"can't find collection" | collection 属性写错 | 单参数 List 用 `list`，单参数数组用 `array`，加 @Param 后用注解名 |
| 传入空集合报错 SQL 语法 | `IN ()` 是无效 SQL | Service 层先判断 `ids.isEmpty()` 再调用 |

### 阶段 3：SQL 拼接问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `near 'WHERE'`错误 | 所有 if 都不成立但写了 `WHERE` | 用 `<where>` 标签自动处理 |
| `SET ,name = ?,` 多余逗号 | 没用 `<set>` | 用 `<set>` 标签自动去掉末尾逗号 |
| `<` `>` 报 XML 解析错误 | XML 特殊字符未转义 | 用 `&lt;` `&gt;`，或用 `<![CDATA[ ... ]]>` 包裹 |
| `LIKE '%张%'` 字段值找不到 | `LIKE` 写法错误 | 用 `LIKE CONCAT('%', #{name}, '%')` |

### 阶段 4：业务逻辑问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 空字符串被误判为有值 | 只判了 `!= null` | 字符串判断写 `xxx != null and xxx != ''` |
| Integer 类型判断 `!= ''` 报错 | OGNL 表达式不支持数字与字符串比较 | Integer 只判 `!= null` |
| 模糊查询包含 `%` 字符时异常 | 没转义用户输入 | 后端转义：`name.replace("%","\\%")` |
| 排序字段被注入 | `${sortBy}` 直接拼接 | 用 `<choose>` 限定可用字段名 |

---

## 本节小结

```
┌────────────────────────────────────────────────────────────────────┐
│                  动态 SQL 与条件搜索                                │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  动态 SQL 解决什么问题：                                              │
│    多个搜索条件可有可无 → 一份 SQL 应对所有组合                       │
│                                                                     │
│  七大核心标签：                                                       │
│    ① <if>            → 条件判断                                     │
│    ② <where>         → 智能 WHERE：去掉首个 AND/OR                  │
│    ③ <set>           → 智能 SET：去掉末尾逗号                       │
│    ④ <foreach>       → 循环遍历：批量插入/删除                       │
│    ⑤ <choose>        → if/else if/else                             │
│    ⑥ <trim>          → 通用裁剪：where/set 的底层                   │
│    ⑦ <sql>+<include> → 代码复用：抽取公共片段                        │
│                                                                     │
│  核心套路：                                                          │
│    搜索 → <where> + <if>                                            │
│    更新 → <set>   + <if>                                            │
│    批量 → <foreach>                                                  │
│                                                                     │
│  避坑要点：                                                          │
│    • 字符串判断要 != null AND != ''                                  │
│    • XML 中 < > & 要转义                                             │
│    • LIKE 用 CONCAT('%', #{x}, '%')                                  │
│    • 多参数要用 @Param 注解                                          │
│    • 空集合传给 <foreach> 会报错，Service 先判空                      │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

---

## 参考文档

- [MyBatis 动态 SQL 官方文档](https://mybatis.org/mybatis-3/zh/dynamic-sql.html)
- [OGNL 表达式语法](https://commons.apache.org/proper/commons-ognl/language-guide.html)
- [MyBatis-Plus 条件构造器](https://baomidou.com/pages/10c804/)（Version B 用 Wrapper 替代动态 SQL）
