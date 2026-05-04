# MyBatis-Plus 快速开发（版本 B 专属）

## 学习目标

- 理解 MyBatis-Plus 与原生 MyBatis 的区别
- 掌握 BaseMapper 提供的通用 CRUD 方法
- 熟练使用 Wrapper 条件构造器编写复杂查询
- 掌握分页插件和代码生成器的使用

## 核心知识点

### 1. MyBatis-Plus 是什么

MyBatis-Plus（简称 MP）是 MyBatis 的增强工具，在 MyBatis 基础上只做增强不做改变：
- 提供通用 CRUD，无需手写简单 SQL
- 支持 Lambda 条件构造，类型安全
- 内置分页插件、代码生成器、性能分析插件

> 官方文档：[https://baomidou.com](https://baomidou.com)

### 2. 快速入门

**依赖**（SpringBoot3 + JDK17 环境）：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.5</version>
</dependency>
```

**实体类**：

```java
@Data
@TableName("emp")
public class Emp {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String password;
    private String name;
    private Integer gender;
    private String image;
    private Integer job;
    private LocalDate entrydate;
    private Integer deptId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

**Mapper 接口**：

```java
@Mapper
public interface EmpMapper extends BaseMapper<Emp> {
    // 无需编写任何方法，BaseMapper 已提供 CRUD
}
```

### 3. BaseMapper 通用 CRUD

BaseMapper 提供了以下常用方法：

```java
// 插入
int insert(T entity);              // 插入一条记录

// 删除
int deleteById(Serializable id);
int deleteByMap(Map<String, Object> columnMap);
int delete(Wrapper<T> queryWrapper);

// 更新
int updateById(T entity);
int update(T entity, Wrapper<T> updateWrapper);

// 查询
T selectById(Serializable id);
List<T> selectBatchIds(Collection<? extends Serializable> idList);
List<T> selectByMap(Map<String, Object> columnMap);
T selectOne(Wrapper<T> queryWrapper);
Integer selectCount(Wrapper<T> queryWrapper);
List<T> selectList(Wrapper<T> queryWrapper);
List<Map<String, Object>> selectMaps(Wrapper<T> queryWrapper);
```

使用示例：

```java
@Service
public class EmpServiceImpl extends ServiceImpl<EmpMapper, Emp> implements EmpService {

    public void demo() {
        // 根据ID查询
        Emp emp = baseMapper.selectById(1);

        // 新增
        Emp newEmp = new Emp();
        newEmp.setName("李四");
        newEmp.setGender(1);
        baseMapper.insert(newEmp);

        // 根据ID更新（只更新非空字段）
        Emp update = new Emp();
        update.setId(1);
        update.setName("张三改名");
        baseMapper.updateById(update);

        // 根据ID删除
        baseMapper.deleteById(1);
    }
}
```

### 4. Wrapper 条件构造器

Wrapper 用于构造复杂的 WHERE 条件：

```java
// QueryWrapper：普通字段字符串（不推荐，易写错字段名）
QueryWrapper<Emp> qw = new QueryWrapper<>();
qw.eq("gender", 1)
  .like("name", "张")
  .ge("entrydate", "2020-01-01")
  .orderByDesc("entrydate");
List<Emp> list = empMapper.selectList(qw);

// LambdaQueryWrapper：Lambda 表达式（推荐，类型安全）
LambdaQueryWrapper<Emp> lqw = new LambdaQueryWrapper<>();
lqw.eq(Emp::getGender, 1)
   .like(Emp::getName, "张")
   .ge(Emp::getEntrydate, LocalDate.of(2020, 1, 1));
List<Emp> list = empMapper.selectList(lqw);
```

常用条件方法：

| 方法 | 说明 | 对应 SQL |
|------|------|---------|
| eq | 等于 | = |
| ne | 不等于 | <> |
| gt | 大于 | > |
| ge | 大于等于 | >= |
| lt | 小于 | < |
| le | 小于等于 | <= |
| like / likeRight / likeLeft | 模糊查询 | LIKE |
| in | IN 查询 | IN |
| between | 范围查询 | BETWEEN |
| isNull / isNotNull | NULL 判断 | IS NULL |
| orderByDesc / orderByAsc | 排序 | ORDER BY |

### 5. 分页插件

**配置分页插件**：

```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

**使用分页**：

```java
// 创建分页对象：当前页1，每页10条
Page<Emp> page = new Page<>(1, 10);

// 条件查询加分页
LambdaQueryWrapper<Emp> lqw = new LambdaQueryWrapper<>();
lqw.eq(Emp::getGender, 1);

Page<Emp> resultPage = empMapper.selectPage(page, lqw);

// 获取结果
long total = resultPage.getTotal();      // 总记录数
List<Emp> records = resultPage.getRecords(); // 当前页数据
```

### 6. 自动填充（create_time / update_time）

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

实体类字段上加 `@TableField(fill = FieldFill.INSERT)` 或 `FieldFill.INSERT_UPDATE`。

### 7. 逻辑删除

```java
// 实体类
@TableLogic
@TableField("is_deleted")
private Integer deleted;

// application.yml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

配置后，`deleteById` 自动变成 `UPDATE ... SET deleted=1`。

### 8. 与原生 MyBatis 对比

| 特性 | 原生 MyBatis | MyBatis-Plus |
|------|-------------|--------------|
| 简单 CRUD | 需手写 SQL | BaseMapper 内置 |
| 动态 SQL | XML `<if>` | Wrapper 代码构造 |
| 分页 | PageHelper 插件 | 内置分页插件 |
| 代码生成 | 无 | AutoGenerator |
| 学习成本 | 较低 | 低（先学 MyBatis 再学 MP） |

## 动手练习

### 练习 1：BaseMapper CRUD

使用 MyBatis-Plus 完成：
1. 新增一个员工
2. 根据 ID 查询
3. 查询所有女员工
4. 分页查询入职日期在 2020 年之后的员工

### 练习 2：Wrapper 复杂查询

实现：查询 "教研部" 下入职超过 2 年且姓 "张" 的员工，按入职日期降序排列。

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| Invalid bound statement | Mapper 未扫描到 | 检查 @Mapper 或 @MapperScan |
| Property 'sqlSessionFactory' not found | 多数据源或配置冲突 | 检查 MybatisPlusConfig 是否正确 |
| 分页不生效 | 分页插件未注册 | 确认 PaginationInnerInterceptor 已加入 |
| 自动填充不生效 | Handler 未注册为 Bean | 加 @Component 注解 |

## 本节小结

MyBatis-Plus 在原生 MyBatis 之上提供了通用的 CRUD 和强大的条件构造器，大幅减少了样板代码。版本 B 采用 MP 作为数据层框架，配合 SpringBoot3 和 JDK17，是当下企业的主流技术选型。

## 参考文档

- [MyBatis-Plus 官方文档](https://baomidou.com/getting-started/)
- [MyBatis-Plus GitHub](https://github.com/baomidou/mybatis-plus)

