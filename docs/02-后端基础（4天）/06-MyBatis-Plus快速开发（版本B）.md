# MyBatis-Plus 快速开发（版本 B 专属）

## 学习目标

学完本节后，你将能够：
- 理解 MyBatis-Plus 和原生 MyBatis 的关系，就像理解"自动挡"和"手动挡"的区别
- 不写一行 SQL，完成常见的增删改查操作
- 使用 Lambda 条件构造器写出类型安全的查询代码
- 实现分页查询和自动填充字段（create_time / update_time）

---

## 核心知识点

### 1. MyBatis-Plus 是什么——MyBatis 的"自动挡"

#### 1.1 从 MyBatis 到 MyBatis-Plus

上一节我们学习了 MyBatis，它让我们从繁琐的 JDBC 中解放出来。但 MyBatis 仍然有一个痛点：

**简单 CRUD 也要写 SQL。**

```java
// 原生 MyBatis：每个方法都要写 SQL
@Mapper
public interface EmpMapper {
    @Select("SELECT * FROM emp WHERE id = #{id}")
    Emp selectById(Integer id);

    @Insert("INSERT INTO emp(name, gender) VALUES(#{name}, #{gender})")
    void insert(Emp emp);

    @Update("UPDATE emp SET name = #{name} WHERE id = #{id}")
    void updateById(Emp emp);

    @Delete("DELETE FROM emp WHERE id = #{id}")
    void deleteById(Integer id);
}
```

这些 SQL 太简单了，几乎每张学表都要写一遍。能不能让框架自动生成？

**MyBatis-Plus（简称 MP）** 就是做这个的：

```java
// MyBatis-Plus：继承 BaseMapper，方法全有了
@Mapper
public interface EmpMapper extends BaseMapper<Emp> {
    // 什么都不用写！BaseMapper 已提供 CRUD
}
```

#### 1.2 MP 的定位

| 类比 | 说明 |
|------|------|
| MyBatis | 手动挡汽车——灵活、可控，但要自己操作每个步骤 |
| MyBatis-Plus | 自动挡汽车——简单操作就能完成大部分任务，复杂场景切手动 |

**MP 的设计哲学**：
- **只做增强，不做改变**：你仍然可以用原生 MyBatis 的所有功能
- **无侵入**：引入 MP 不会影响现有代码
- **损耗小**：自动生成 SQL 的性能和手写几乎一样

#### 1.3 MP 的核心功能

```
┌─────────────────────────────────────────────────────────────┐
│                   MyBatis-Plus 功能地图                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. 通用 CRUD（BaseMapper）                                  │
│     → 不用写 SQL，继承接口即可获得增删改查                     │
│                                                              │
│  2. 条件构造器（Wrapper）                                     │
│     → 用 Java 代码拼接 WHERE 条件，不用写 XML                 │
│                                                              │
│  3. 分页插件（Pagination）                                    │
│     → 内置分页，不用引入 PageHelper                           │
│                                                              │
│  4. 代码生成器（AutoGenerator）                               │
│     → 根据数据库表自动生成 Entity、Mapper、Service、Controller │
│                                                              │
│  5. 自动填充（MetaObjectHandler）                             │
│     → 自动设置 create_time、update_time                       │
│                                                              │
│  6. 逻辑删除                                                  │
│     → delete 变成 UPDATE ... SET deleted = 1                  │
│                                                              │
│  7. 乐观锁                                                    │
│     → 防止并发更新覆盖（版本号机制）                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

### 2. 快速入门——三步上手

#### 2.1 第一步：引入依赖

版本 B 使用 SpringBoot 3 + JDK17，MP 版本要匹配：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.5</version>
</dependency>
```

#### 2.2 第二步：配置实体类

```java
package com.tliaspro.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工实体类
 * @TableName("emp")：指定对应的数据库表名
 * 如果类名和表名一致（忽略大小写），可以省略
 */
@Data
@TableName("emp")
public class Emp {

    /**
     * @TableId：标记主键字段
     * type = IdType.AUTO：数据库自增（让 MySQL 自动生成 ID）
     * 其他类型：
     *   IdType.NONE：无策略
     *   IdType.INPUT：手动输入
     *   IdType.ASSIGN_ID：雪花算法（Long 类型）
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String username;
    private String password;
    private String name;
    private Integer gender;
    private String image;
    private Integer job;
    private LocalDate entrydate;

    /**
     * dept_id 是数据库字段名
     * deptId 是 Java 属性名（驼峰命名）
     * MP 默认开启 map-underscore-to-camel-case，自动转换
     */
    private Integer deptId;

    /**
     * @TableField(fill = FieldFill.INSERT)
     * 插入时自动填充（需要配合 MetaObjectHandler 使用）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * @TableField(fill = FieldFill.INSERT_UPDATE)
     * 插入和更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

#### 2.3 第三步：Mapper 接口继承 BaseMapper

```java
package com.tliaspro.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tliaspro.pojo.Emp;
import org.apache.ibatis.annotations.Mapper;

/**
 * EmpMapper：只需要继承 BaseMapper<Emp>，CRUD 方法全都有了
 * BaseMapper<Emp> 中的泛型 <Emp> 告诉 MP：这个 Mapper 操作的是 emp 表
 */
@Mapper
public interface EmpMapper extends BaseMapper<Emp> {
    // 你可以在这里写自定义方法（复杂查询）
    // MP 不会覆盖，会和 BaseMapper 的方法共存
}
```

**BaseMapper 自动提供的方法：**

```java
// 插入
int insert(T entity);                    // 插入一条记录

// 删除
int deleteById(Serializable id);         // 根据 ID 删除
int deleteByMap(Map<String, Object> columnMap);  // 根据 Map 条件删除
int delete(Wrapper<T> queryWrapper);     // 根据条件构造器删除

// 更新
int updateById(T entity);                // 根据 ID 更新（只更新非空字段）
int update(T entity, Wrapper<T> updateWrapper);  // 根据条件更新

// 查询
T selectById(Serializable id);           // 根据 ID 查询
List<T> selectBatchIds(Collection<? extends Serializable> idList);  // 批量查询
List<T> selectByMap(Map<String, Object> columnMap);  // 根据 Map 查询
T selectOne(Wrapper<T> queryWrapper);    // 查询一条（多条会报错）
Integer selectCount(Wrapper<T> queryWrapper);  // 查询总数
List<T> selectList(Wrapper<T> queryWrapper);   // 条件查询列表
List<Map<String, Object>> selectMaps(Wrapper<T> queryWrapper);  // 返回 Map 列表
```

---

### 3. 使用 BaseMapper——不写 SQL 的 CRUD

#### 3.1 Service 层中使用 BaseMapper

版本 B 中，Service 层通常继承 `ServiceImpl`，它内部已经封装了 BaseMapper：

```java
package com.tliaspro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tliaspro.mapper.EmpMapper;
import com.tliaspro.pojo.Emp;
import com.tliaspro.service.EmpService;
import org.springframework.stereotype.Service;

/**
 * ServiceImpl<M, T>：
 *   M = Mapper 接口类型（EmpMapper）
 *   T = 实体类类型（Emp）
 * 继承后，this.baseMapper 就是 EmpMapper 实例
 * 同时获得 IService 的所有方法（save、remove、update、get、list 等）
 */
@Service
public class EmpServiceImpl extends ServiceImpl<EmpMapper, Emp> implements EmpService {

    public void demo() {
        // ========== 插入 ==========
        Emp newEmp = new Emp();
        newEmp.setName("李四");
        newEmp.setGender(1);
        newEmp.setDeptId(1);
        // insert 方法会自动生成：INSERT INTO emp(name, gender, dept_id) VALUES(?, ?, ?)
        baseMapper.insert(newEmp);
        // 插入后，newEmp.getId() 会自动回填生成的主键值

        // ========== 查询 ==========
        // 根据 ID 查询
        Emp emp = baseMapper.selectById(1);

        // 查询所有
        List<Emp> allEmps = baseMapper.selectList(null);
        // 传 null 表示没有条件，查询全部

        // ========== 更新 ==========
        Emp update = new Emp();
        update.setId(1);
        update.setName("张三改名");
        // updateById 会自动生成：UPDATE emp SET name = ? WHERE id = ?
        // 只更新非空字段！如果 gender 为 null，不会更新 gender 字段
        baseMapper.updateById(update);

        // ========== 删除 ==========
        baseMapper.deleteById(1);
    }
}
```

#### 3.2 IService 接口的便捷方法

继承 `ServiceImpl` 后，还可以直接用 `this` 调用更多便捷方法：

```java
@Service
public class EmpServiceImpl extends ServiceImpl<EmpMapper, Emp> implements EmpService {

    public void demo2() {
        // save = insert
        Emp emp = new Emp();
        emp.setName("王五");
        this.save(emp);   // 和 baseMapper.insert(emp) 等价

        // saveOrUpdate：如果 ID 存在则更新，不存在则插入
        this.saveOrUpdate(emp);

        // removeById = deleteById
        this.removeById(1);

        // getById = selectById
        Emp e = this.getById(1);

        // list = selectList(null)
        List<Emp> list = this.list();

        // count = selectCount(null)
        long total = this.count();

        // saveBatch：批量插入
        this.saveBatch(Arrays.asList(emp1, emp2, emp3));
    }
}
```

---

### 4. Wrapper 条件构造器——用 Java 代码写 WHERE

#### 4.1 为什么需要 Wrapper

假设你要查询"性别为男、姓名包含'张'、入职日期在 2020 年之后"的员工：

```java
// 原生 MyBatis：要写 XML
<select id="list" resultType="Emp">
    SELECT * FROM emp
    <where>
        <if test="gender != null">
            AND gender = #{gender}
        </if>
        <if test="name != null">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="begin != null">
            AND entrydate >= #{begin}
        </if>
    </where>
</select>
```

```java
// MyBatis-Plus：用 Java 代码直接构造
LambdaQueryWrapper<Emp> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Emp::getGender, 1)
       .like(Emp::getName, "张")
       .ge(Emp::getEntrydate, LocalDate.of(2020, 1, 1));
List<Emp> list = empMapper.selectList(wrapper);
```

**Wrapper 的优势：**
- **不用写 XML**：条件在 Java 代码中动态组装
- **类型安全**：`Emp::getName` 是编译时检查的，字段名写错直接编译报错
- **IDE 自动补全**：写 `Emp::` 后 IDE 会提示所有属性

#### 4.2 QueryWrapper vs LambdaQueryWrapper

| 类型 | 写法 | 优点 | 缺点 |
|------|------|------|------|
| QueryWrapper | `eq("name", "张三")` | 简单直接 | 字段名是字符串，写错不报错 |
| LambdaQueryWrapper | `eq(Emp::getName, "张三")` | 类型安全、IDE 提示 | 写法稍长 |

**推荐：永远使用 LambdaQueryWrapper，避免字段名拼写错误。**

#### 4.3 常用条件方法

```java
LambdaQueryWrapper<Emp> wrapper = new LambdaQueryWrapper<>();

// ========== 比较运算 ==========
wrapper.eq(Emp::getGender, 1);           // gender = 1（等于）
wrapper.ne(Emp::getGender, 1);           // gender <> 1（不等于）
wrapper.gt(Emp::getId, 100);             // id > 100（大于）
wrapper.ge(Emp::getEntrydate, date);     // entrydate >= date（大于等于）
wrapper.lt(Emp::getId, 100);             // id < 100（小于）
wrapper.le(Emp::getEntrydate, date);     // entrydate <= date（小于等于）

// ========== 模糊查询 ==========
wrapper.like(Emp::getName, "张");        // name LIKE '%张%'
wrapper.likeRight(Emp::getName, "张");   // name LIKE '张%'
wrapper.likeLeft(Emp::getName, "三");    // name LIKE '%三'

// ========== IN / BETWEEN ==========
wrapper.in(Emp::getDeptId, Arrays.asList(1, 2, 3));  // dept_id IN (1, 2, 3)
wrapper.between(Emp::getEntrydate, begin, end);      // entrydate BETWEEN begin AND end

// ========== NULL 判断 ==========
wrapper.isNull(Emp::getImage);           // image IS NULL
wrapper.isNotNull(Emp::getImage);        // image IS NOT NULL

// ========== 排序 ==========
wrapper.orderByDesc(Emp::getEntrydate);  // ORDER BY entrydate DESC
wrapper.orderByAsc(Emp::getId);          // ORDER BY id ASC

// ========== 组合条件 ==========
// AND 连接（默认）
wrapper.eq(Emp::getGender, 1)
       .like(Emp::getName, "张");        // gender = 1 AND name LIKE '%张%'

// OR 连接
wrapper.eq(Emp::getGender, 1)
       .or()
       .eq(Emp::getDeptId, 2);           // gender = 1 OR dept_id = 2

// 嵌套条件
wrapper.and(w -> w.eq(Emp::getGender, 1).like(Emp::getName, "张"))
       .or()
       .eq(Emp::getDeptId, 2);
// (gender = 1 AND name LIKE '%张%') OR dept_id = 2
```

#### 4.4 完整查询示例

```java
@Service
public class EmpServiceImpl extends ServiceImpl<EmpMapper, Emp> implements EmpService {

    public List<Emp> queryEmps(EmpQueryDTO dto) {
        LambdaQueryWrapper<Emp> wrapper = new LambdaQueryWrapper<>();

        // 动态条件：只有当参数不为空时才添加条件
        if (StringUtils.isNotBlank(dto.getName())) {
            wrapper.like(Emp::getName, dto.getName());
        }
        if (dto.getGender() != null) {
            wrapper.eq(Emp::getGender, dto.getGender());
        }
        if (dto.getBegin() != null && dto.getEnd() != null) {
            wrapper.between(Emp::getEntrydate, dto.getBegin(), dto.getEnd());
        }
        if (dto.getDeptId() != null) {
            wrapper.eq(Emp::getDeptId, dto.getDeptId());
        }

        // 排序
        wrapper.orderByDesc(Emp::getEntrydate);

        return baseMapper.selectList(wrapper);
    }
}
```

---

### 5. 分页插件——不用写 LIMIT

#### 5.1 配置分页插件

```java
package com.tliaspro.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 添加分页插件
     * DbType.MYSQL：指定数据库类型为 MySQL
     * MP 会根据数据库类型生成对应的分页 SQL（MySQL 用 LIMIT，Oracle 用 ROWNUM）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

#### 5.2 使用分页

```java
@Service
public class EmpServiceImpl extends ServiceImpl<EmpMapper, Emp> implements EmpService {

    public Page<Emp> queryEmpPage(int pageNum, int pageSize, EmpQueryDTO dto) {
        // 第 1 步：创建分页对象
        // pageNum = 当前页码（从 1 开始）
        // pageSize = 每页显示条数
        Page<Emp> page = new Page<>(pageNum, pageSize);

        // 第 2 步：构造查询条件
        LambdaQueryWrapper<Emp> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(dto.getName())) {
            wrapper.like(Emp::getName, dto.getName());
        }
        if (dto.getGender() != null) {
            wrapper.eq(Emp::getGender, dto.getGender());
        }

        // 第 3 步：执行分页查询
        // MP 会自动在 SQL 后面添加 LIMIT 子句
        Page<Emp> resultPage = baseMapper.selectPage(page, wrapper);

        // 第 4 步：获取结果
        long total = resultPage.getTotal();        // 总记录数
        long pages = resultPage.getPages();        // 总页数
        List<Emp> records = resultPage.getRecords();  // 当前页数据

        System.out.println("总记录数：" + total);
        System.out.println("总页数：" + pages);
        System.out.println("当前页数据：" + records);

        return resultPage;
    }
}
```

**MP 自动生成的分页 SQL：**

```sql
-- 查询当前页数据
SELECT * FROM emp WHERE gender = 1 LIMIT 0, 10;
-- LIMIT 0, 10 表示从第 0 条开始，取 10 条（第 1 页）

-- 查询总记录数
SELECT COUNT(*) FROM emp WHERE gender = 1;
```

**返回给前端的格式：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 86,           // 总记录数
    "pages": 9,            // 总页数
    "current": 1,          // 当前页
    "size": 10,            // 每页大小
    "records": [           // 当前页数据
      { "id": 1, "name": "张三" },
      { "id": 2, "name": "李四" }
    ]
  }
}
```

---

### 6. 自动填充——不再手动设置时间

#### 6.1 问题：每次都要手动设置 create_time / update_time

```java
// ❌ 没有自动填充时
public void saveEmp(Emp emp) {
    emp.setCreateTime(LocalDateTime.now());   // 每次都要写
    emp.setUpdateTime(LocalDateTime.now());   // 每次都要写
    baseMapper.insert(emp);
}

public void updateEmp(Emp emp) {
    emp.setUpdateTime(LocalDateTime.now());   // 每次都要写
    baseMapper.updateById(emp);
}
```

#### 6.2 解决方案：MetaObjectHandler

```java
package com.tliaspro.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自动填充处理器
 * 实现 MetaObjectHandler 接口，在插入和更新时自动填充字段
 */
@Slf4j
@Component  // 必须加 @Component，让 Spring 管理
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("开始插入填充...");

        // strictInsertFill：严格填充（如果字段已有值，不覆盖）
        // 参数：metaObject, 字段名, 字段类型, 填充值
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("开始更新填充...");
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

实体类字段上添加注解：

```java
public class Emp {
    @TableField(fill = FieldFill.INSERT)         // 只在插入时填充
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)  // 插入和更新时都填充
    private LocalDateTime updateTime;
}
```

**效果：**
- `insert(emp)` → 自动设置 `createTime` 和 `updateTime`
- `updateById(emp)` → 自动设置 `updateTime`
- 你再也不用手动写 `LocalDateTime.now()`

---

### 7. 逻辑删除——不是真删除

#### 7.1 什么是逻辑删除

**物理删除**：`DELETE FROM emp WHERE id = 1` —— 数据真的没了，无法恢复
**逻辑删除**：`UPDATE emp SET is_deleted = 1 WHERE id = 1` —— 数据还在，只是标记为"已删除"

逻辑删除的好处：
- 误删可以恢复
- 保留数据用于审计
- 关联数据不会悬空

#### 7.2 MP 逻辑删除配置

```java
// 实体类中添加逻辑删除字段
public class Emp {
    // ... 其他字段 ...

    @TableLogic                              // 标记为逻辑删除字段
    @TableField("is_deleted")
    private Integer deleted;
}
```

```yaml
# application.yml 配置
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted       # 逻辑删除字段名
      logic-delete-value: 1             # 删除时的值
      logic-not-delete-value: 0         # 未删除时的值
```

**配置后的效果：**

```java
// 调用 deleteById
empMapper.deleteById(1);

// MP 实际执行的 SQL：
// UPDATE emp SET is_deleted = 1 WHERE id = 1 AND is_deleted = 0

// 调用 selectList
empMapper.selectList(null);

// MP 实际执行的 SQL：
// SELECT * FROM emp WHERE is_deleted = 0
// 自动加上 WHERE is_deleted = 0，过滤掉已删除的数据
```

---

### 8. 原生 MyBatis vs MyBatis-Plus 对比

| 特性 | 原生 MyBatis | MyBatis-Plus |
|------|-------------|--------------|
| 简单 CRUD | 需手写 SQL 或注解 | BaseMapper 内置，无需手写 |
| 动态 SQL | XML `<if>` 标签 | Wrapper 代码构造 |
| 分页 | 需引入 PageHelper 插件 | 内置分页插件 |
| 代码生成 | 无 | AutoGenerator 自动生成 |
| 自动填充 | 手动设置 | MetaObjectHandler 自动填充 |
| 逻辑删除 | 手动实现 | 注解配置即可 |
| 学习成本 | 较低 | 低（先学 MyBatis 再学 MP） |
| 灵活性 | 高（完全控制 SQL） | 高（复杂 SQL 仍可用 XML） |

**结论：**
- 简单场景用 MP（90% 的 CRUD）
- 复杂 SQL 仍然可以用原生 MyBatis 的 XML
- 两者可以共存，不冲突

---

## 动手练习

### 练习 1：BaseMapper CRUD

**目标**：使用 MyBatis-Plus 完成员工管理的 CRUD。

**步骤**：

1. 确保 `EmpMapper` 继承了 `BaseMapper<Emp>`
2. 在 Service 中编写测试方法：

```java
@Service
public class EmpServiceImpl extends ServiceImpl<EmpMapper, Emp> implements EmpService {

    public void testCrud() {
        // 1. 新增员工
        Emp emp = new Emp();
        emp.setName("测试员工");
        emp.setGender(1);
        emp.setDeptId(1);
        emp.setEntrydate(LocalDate.now());
        this.save(emp);
        System.out.println("新增成功，ID = " + emp.getId());

        // 2. 根据 ID 查询
        Emp found = this.getById(emp.getId());
        System.out.println("查询结果：" + found);

        // 3. 查询所有女员工
        LambdaQueryWrapper<Emp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Emp::getGender, 2);
        List<Emp> femaleEmps = this.list(wrapper);
        System.out.println("女员工数量：" + femaleEmps.size());

        // 4. 修改员工
        emp.setName("测试员工改名");
        this.updateById(emp);

        // 5. 删除员工
        this.removeById(emp.getId());
    }
}
```

3. 在 Controller 中加一个测试接口，调用 `testCrud()`

### 练习 2：Wrapper 复杂查询

**目标**：实现一个复杂条件查询。

**需求**：查询"研发部"下入职超过 2 年且姓"张"的员工，按入职日期降序排列。

```java
public List<Emp> complexQuery() {
    LambdaQueryWrapper<Emp> wrapper = new LambdaQueryWrapper<>();

    // 部门 = 研发部（假设 ID 是 1）
    wrapper.eq(Emp::getDeptId, 1);

    // 姓"张"
    wrapper.likeRight(Emp::getName, "张");

    // 入职超过 2 年
    wrapper.le(Emp::getEntrydate, LocalDate.now().minusYears(2));

    // 按入职日期降序
    wrapper.orderByDesc(Emp::getEntrydate);

    return baseMapper.selectList(wrapper);
}
```

### 练习 3：分页查询

**目标**：实现带条件的分页查询。

```java
public Page<Emp> pageQuery(int pageNum, int pageSize, String name, Integer gender) {
    Page<Emp> page = new Page<>(pageNum, pageSize);

    LambdaQueryWrapper<Emp> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.isNotBlank(name)) {
        wrapper.like(Emp::getName, name);
    }
    if (gender != null) {
        wrapper.eq(Emp::getGender, gender);
    }

    return baseMapper.selectPage(page, wrapper);
}
```

---

## 常见错误排查

### 阶段 1：配置问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Invalid bound statement` | Mapper 未扫描到 | 检查 `@Mapper` 注解或启动类的 `@MapperScan` |
| `Property 'sqlSessionFactory' not found` | 多数据源或配置冲突 | 检查 `MybatisPlusConfig` 是否正确 |
| `Failed to process import candidates` | 依赖版本不兼容 | SpringBoot 3 必须用 MP 3.5.5+ |

### 阶段 2：使用问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 分页不生效 | 分页插件未注册 | 确认 `PaginationInnerInterceptor` 已加入 `MybatisPlusInterceptor` |
| 自动填充不生效 | Handler 未注册为 Bean | 加 `@Component` 注解 |
| `updateById` 没有更新某些字段 | 字段值为 null | MP 的 `updateById` 默认忽略 null 字段 |
| 逻辑删除后还能查到 | 配置未生效 | 检查 `logic-delete-field` 和实体类字段名是否一致 |

### 阶段 3：Wrapper 问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 条件不生效 | 字段名写错 | 使用 `LambdaQueryWrapper`，避免字符串字段名 |
| 生成的 SQL 不对 | 条件组合错误 | 检查 `and()` 和 `or()` 的使用 |
| `selectOne` 报错 | 查询结果有多条 | 改用 `selectList` 或确保条件唯一 |

### 阶段 4：兼容性问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `java.lang.NoClassDefFoundError` | 依赖冲突 | 检查 pom.xml 中是否有多个 MP 版本 |
| 和原生 MyBatis XML 冲突 | namespace 重复 | 确保 XML 的 namespace 和 MP Mapper 一致 |

---

## 本节小结

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    MyBatis-Plus 核心知识                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  MP = MyBatis 的增强工具，只做增强不做改变                                │
│  类比：MyBatis 是手动挡，MP 是自动挡，复杂场景可以切手动                     │
│                                                                          │
│  核心使用步骤：                                                            │
│    1. 引入 mybatis-plus-boot-starter 依赖                                 │
│    2. 实体类加 @TableName、@TableId、@TableField 注解                      │
│    3. Mapper 继承 BaseMapper<T>                                            │
│    4. Service 继承 ServiceImpl<Mapper, T>                                  │
│                                                                          │
│  BaseMapper 内置方法：                                                     │
│    insert、deleteById、updateById、selectById、selectList...               │
│                                                                          │
│  Wrapper 条件构造器：                                                      │
│    LambdaQueryWrapper<T>：类型安全，IDE 自动提示                           │
│    eq、ne、gt、ge、lt、le、like、between、in、orderByDesc...               │
│                                                                          │
│  分页插件：                                                                │
│    1. 配置 MybatisPlusInterceptor，添加 PaginationInnerInterceptor         │
│    2. new Page<>(pageNum, pageSize)                                        │
│    3. baseMapper.selectPage(page, wrapper)                                 │
│                                                                          │
│  自动填充：                                                                │
│    1. 实体类字段加 @TableField(fill = FieldFill.INSERT)                    │
│    2. 实现 MetaObjectHandler，加 @Component                                │
│                                                                          │
│  逻辑删除：                                                                │
│    1. 实体类字段加 @TableLogic                                             │
│    2. application.yml 配置 logic-delete-value                              │
│                                                                          │
│  避坑指南：                                                                │
│    • SpringBoot 3 必须用 MP 3.5.5+                                        │
│    • 分页插件必须注册才能生效                                               │
│    • 复杂 SQL 仍然可以用原生 MyBatis XML                                    │
│    • 推荐用 LambdaQueryWrapper，字段名不会写错                             │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 参考文档

- [MyBatis-Plus 官方文档](https://baomidou.com/getting-started/)
- [MyBatis-Plus GitHub](https://github.com/baomidou/mybatis-plus)
- [MyBatis-Plus 3.5.x 版本说明](https://baomidou.com/pages/779a6e/)
