# JDBC 到 MyBatis 原理

## 学习目标

- 理解 JDBC 的执行流程和局限性
- 掌握 MyBatis 的基本配置和使用方式
- 理解 MyBatis 的 SQL 映射原理
- 理解连接池的作用

## 核心知识点

### 1. JDBC 基础

JDBC（Java Database Connectivity）是 Java 访问数据库的标准 API：

```java
// JDBC 完整流程
public void jdbcDemo() throws Exception {
    // 1. 注册驱动
    Class.forName("com.mysql.cj.jdbc.Driver");

    // 2. 获取连接
    Connection conn = DriverManager.getConnection(
        "jdbc:mysql://localhost:3306/tlias_db", "root", "123456");

    // 3. 创建 Statement
    String sql = "SELECT * FROM emp WHERE id = ?";
    PreparedStatement pstmt = conn.prepareStatement(sql);
    pstmt.setInt(1, 1);

    // 4. 执行查询
    ResultSet rs = pstmt.executeQuery();

    // 5. 处理结果
    while (rs.next()) {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        System.out.println(id + ": " + name);
    }

    // 6. 释放资源（倒序关闭）
    rs.close();
    pstmt.close();
    conn.close();
}
```

**JDBC 的痛点**：
- 代码重复：注册驱动、获取连接、释放资源每次都写
- SQL 硬编码：Java 代码中夹杂 SQL 字符串
- 结果集映射：手动将 ResultSet 转换为 Java 对象
- 连接管理：频繁创建/关闭连接效率低

### 2. MyBatis 简介

MyBatis 是一款优秀的持久层框架，解决了 JDBC 的大部分痛点：
- SQL 与 Java 代码分离（XML 或注解）
- 自动结果集映射
- 支持动态 SQL
- 插件扩展机制（分页、性能监控）

### 3. MyBatis 快速入门

**依赖**：
```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.3.1</version>
</dependency>
```

**配置**：
```yaml
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.tlias.pojo
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

**Mapper 接口（注解方式）**：
```java
@Mapper
public interface DeptMapper {
    @Select("select * from dept")
    List<Dept> list();

    @Delete("delete from dept where id = #{id}")
    void deleteById(Integer id);

    @Insert("insert into dept(name) values(#{name})")
    void insert(Dept dept);
}
```

**Mapper XML（动态 SQL）**：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.tlias.mapper.EmpMapper">

    <select id="list" resultType="com.tlias.pojo.Emp">
        select * from emp
        <where>
            <if test="name != null and name != ''">
                and name like concat('%', #{name}, '%')
            </if>
            <if test="gender != null">
                and gender = #{gender}
            </if>
        </where>
    </select>

</mapper>
```

### 4. MyBatis 动态 SQL 标签

| 标签 | 作用 |
|------|------|
| `<if>` | 条件判断 |
| `<where>` | 自动处理 WHERE 和 AND/OR |
| `<set>` | 自动处理 SET 和逗号 |
| `<foreach>` | 循环遍历（用于 IN 批量操作） |
| `<choose>` | 多选一（类似 switch） |
| `<trim>` | 自定义前缀/后缀处理 |

### 5. 连接池

数据库连接是昂贵的资源，频繁创建/销毁连接会严重影响性能。**连接池**提前创建一批连接，使用时从池中获取，用完归还，避免了连接的频繁创建。

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
```

| 参数 | 说明 |
|------|------|
| initial-size | 初始连接数 |
| min-idle | 最小空闲连接数 |
| max-active | 最大活跃连接数 |
| max-wait | 获取连接的最大等待时间 |

## 动手练习

### 练习 1：手写 JDBC

不使用 MyBatis，纯 JDBC 实现：
1. 查询所有部门
2. 根据 ID 查询员工
3. 体会 JDBC 的代码重复问题

### 练习 2：MyBatis 动态 SQL

使用 `<foreach>` 实现批量删除：
```java
void deleteByIds(List<Integer> ids);
```

对应的 XML 动态 SQL 应该怎么写？

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| Invalid bound statement | Mapper XML 未加载或 namespace 错误 | 检查 mapper-locations 和 namespace |
| Parameter 'xxx' not found | 参数名错误或映射问题 | 使用 @Param 注解指定参数名 |
| There is no getter for property | 实体类缺少 getter/setter | 添加 Lombok 的 @Data 或手写 getter/setter |

## 本节小结

JDBC 是数据库访问的底层 API，MyBatis 在其之上提供了 SQL 映射和动态 SQL 能力，大幅提升了开发效率。理解连接池的作用，能够帮助你写出高性能的数据库应用。

## 参考文档

- [MyBatis 官方文档](https://mybatis.org/mybatis-3/zh/index.html)
- [MyBatis-Spring-Boot-Starter](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
