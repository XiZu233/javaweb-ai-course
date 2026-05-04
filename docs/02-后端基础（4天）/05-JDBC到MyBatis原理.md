# JDBC 到 MyBatis 原理

## 学习目标

学完本节后，你将能够：
- 理解 JDBC 的执行流程，体会"手写"数据库操作的痛苦
- 掌握 MyBatis 的核心配置和使用方式
- 理解 MyBatis 如何把 SQL 和 Java 代码解耦
- 理解连接池的作用，以及为什么必须用它

---

## 核心知识点

### 1. JDBC 基础——Java 访问数据库的"原生方式"

#### 1.1 JDBC 是什么

JDBC（Java Database Connectivity）是 Java 语言访问数据库的**标准 API**。所有 Java 框架（MyBatis、Hibernate、Spring Data JPA）底层最终都是通过 JDBC 与数据库通信的。

可以把 JDBC 理解为**数据库的 USB 接口规范**：
- JDBC 是规范（接口）
- MySQL 驱动（`mysql-connector-java.jar`）是具体实现
- 你的 Java 代码调用 JDBC 接口，驱动负责把请求翻译成 MySQL 能懂的协议

#### 1.2 JDBC 完整代码示例

下面是一个完整的 JDBC 查询示例，每一行都有详细注释：

```java
package com.tlias;

import java.sql.*;  // JDBC 的核心包

public class JdbcDemo {

    public static void main(String[] args) {
        // 定义连接参数
        String url = "jdbc:mysql://localhost:3306/tlias_db?useSSL=false";
        String username = "root";
        String password = "123456";

        // 声明资源变量（必须在 finally 中关闭）
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // ============================================
            // 第 1 步：加载数据库驱动
            // ============================================
            // Class.forName 会加载 mysql-connector-java.jar 中的 Driver 类
            // 这个类在加载时会自动向 DriverManager 注册自己
            // 注意：MySQL 8.0 的驱动类名是 com.mysql.cj.jdbc.Driver
            //      MySQL 5.x 的驱动类名是 com.mysql.jdbc.Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // ============================================
            // 第 2 步：获取数据库连接
            // ============================================
            // DriverManager 是 JDBC 的驱动管理器
            // getConnection 会根据 URL 找到对应的驱动，建立 TCP 连接
            // 这个操作很耗时！需要经过：TCP 三次握手 → MySQL 认证 → 分配线程
            conn = DriverManager.getConnection(url, username, password);

            // ============================================
            // 第 3 步：创建 PreparedStatement
            // ============================================
            // Statement 是"语句"，用于执行 SQL
            // 为什么用 PreparedStatement 而不是 Statement？
            // 1. 预编译：SQL 只解析一次，参数变化时不需要重新解析
            // 2. 防注入：参数自动转义，防止 SQL 注入攻击
            // 3. 可读性好：? 占位符让 SQL 更清晰

            String sql = "SELECT id, name, gender FROM emp WHERE id = ?";
            pstmt = conn.prepareStatement(sql);

            // 设置参数（第 1 个 ? 的值是 1）
            pstmt.setInt(1, 1);
            // 如果有多个 ?，依次 setXxx(2, value), setXxx(3, value)...

            // ============================================
            // 第 4 步：执行 SQL
            // ============================================
            // executeQuery() 用于 SELECT 查询，返回 ResultSet
            // executeUpdate() 用于 INSERT/UPDATE/DELETE，返回影响的行数
            rs = pstmt.executeQuery();

            // ============================================
            // 第 5 步：处理结果集
            // ============================================
            // ResultSet 是一个"游标"，初始指向第一行之前
            // next() 移动到下一行，如果有数据返回 true
            while (rs.next()) {
                // getXxx("列名") 或 getXxx(列索引) 获取值
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int gender = rs.getInt("gender");

                System.out.println("id=" + id + ", name=" + name + ", gender=" + gender);
            }

        } catch (ClassNotFoundException e) {
            // 驱动类找不到：没引入 mysql-connector-java.jar
            System.err.println("数据库驱动加载失败：" + e.getMessage());
        } catch (SQLException e) {
            // SQL 执行出错：语法错误、连接断开、权限不足等
            System.err.println("数据库操作失败：" + e.getMessage());
        } finally {
            // ============================================
            // 第 6 步：释放资源（倒序关闭）
            // ============================================
            // 为什么要倒序关闭？
            // ResultSet 依赖 PreparedStatement，PreparedStatement 依赖 Connection
            // 如果先关了 Connection，再关 ResultSet 会报错
            // 而且资源必须关闭，否则会导致内存泄漏和数据库连接耗尽！

            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
```

#### 1.3 JDBC 的六大痛点

写完上面的代码，你会发现 JDBC 有六个让人抓狂的问题：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        JDBC 的六大痛点                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  痛点 1：代码重复                                                         │
│    每次操作数据库都要写：加载驱动 → 获取连接 → 关闭资源                    │
│    真正和业务相关的代码（SQL + 结果处理）只占 20%                          │
│    就像每次写信都要重新造一支笔                                           │
│                                                                          │
│  痛点 2：SQL 硬编码                                                        │
│    SQL 字符串直接写在 Java 代码里                                         │
│    想改个 SQL 要重新编译、打包、部署整个项目                               │
│    SQL 和 Java 代码混在一起，难以维护                                      │
│                                                                          │
│  痛点 3：结果集映射繁琐                                                    │
│    手动把 ResultSet 的每一列映射到对象的每一个属性                          │
│    表有 20 个字段就要写 20 行 getXxx() 和 setXxx()                         │
│    字段名改了，Java 代码也要跟着改                                         │
│                                                                          │
│  痛点 4：连接管理低效                                                      │
│    每次操作都创建新连接，用完就关                                          │
│    创建 TCP 连接很耗时（几十毫秒）                                         │
│    高并发时，数据库连接数很快耗尽                                          │
│                                                                          │
│  痛点 5：异常处理啰嗦                                                      │
│    每个操作都要 try-catch-finally                                         │
│    关闭资源又要 try-catch，代码像"夹心饼干"                                │
│                                                                          │
│  痛点 6：没有分页支持                                                      │
│    想实现分页查询？自己写 LIMIT 和 COUNT                                   │
│    不同数据库的分页语法还不一样（MySQL 用 LIMIT，Oracle 用 ROWNUM）         │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**这些痛点催生了一个问题：能不能有一个框架，自动处理这些繁琐的工作，让我只关注 SQL 本身？**

答案是：**MyBatis**。

---

### 2. MyBatis 简介——SQL 映射框架

#### 2.1 MyBatis 是什么

MyBatis 是一款优秀的**持久层框架**，它的核心职责是：

> **把 JDBC 的繁琐工作自动化，让你只写 SQL，剩下的事情框架来做。**

MyBatis 解决了 JDBC 的哪些痛点：

| JDBC 痛点 | MyBatis 解决方案 |
|----------|-----------------|
| 代码重复（驱动、连接、关闭） | 配置文件统一管理，自动处理 |
| SQL 硬编码在 Java 中 | SQL 写在 XML 或注解中，与 Java 分离 |
| 手动结果集映射 | 自动映射（字段名和属性名对应） |
| 连接管理低效 | 与连接池（Druid/HikariCP）集成 |
| 异常处理啰嗦 | 统一转换为 RuntimeException |
| 没有分页 | 集成 PageHelper 插件 |

#### 2.2 MyBatis 在架构中的位置

```
┌─────────────────────────────────────────┐
│           Controller 控制层              │
│      "接收请求，调用 Service"             │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│           Service 业务层                 │
│      "处理业务逻辑，调用 Mapper"          │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│           Mapper 接口                    │
│      "定义方法，关联 SQL"                 │
│      "@Mapper 注解标记"                  │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         MyBatis 框架层                   │
│      "解析 SQL、执行 JDBC、映射结果"       │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         JDBC + 数据库驱动                 │
│      "与 MySQL 通信"                     │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│            MySQL 数据库                   │
└─────────────────────────────────────────┘
```

**关键理解**：
- 你写的 Mapper 接口只有方法定义，没有实现类
- MyBatis 通过动态代理，在运行时为接口生成实现类
- 实现类里就是 JDBC 代码（连接、执行、关闭），但你看不到，框架帮你做了

---

### 3. MyBatis 快速入门

#### 3.1 引入依赖

在 `pom.xml` 中添加 MyBatis-Spring-Boot 启动器：

```xml
<dependencies>
    <!-- MyBatis Spring Boot 启动器 -->
    <!-- 引入这一个，自动包含 mybatis、mybatis-spring、spring-jdbc -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>2.3.1</version>
    </dependency>

    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

#### 3.2 配置文件（application.yml）

```yaml
# ============================================
# 数据源配置：告诉 SpringBoot 怎么连接数据库
# ============================================
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/tlias_db?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8
    username: root
    password: 123456

# ============================================
# MyBatis 配置
# ============================================
mybatis:
  # mapper-locations: Mapper XML 文件的位置
  # classpath:mapper/*.xml = src/main/resources/mapper/ 目录下的所有 .xml 文件
  mapper-locations: classpath:mapper/*.xml

  # type-aliases-package: 实体类的包路径
  # 配置后，XML 中写 resultType="Emp" 即可，不用写全限定名
  type-aliases-package: com.tlias.pojo

  configuration:
    # map-underscore-to-camel-case: 自动驼峰映射
    # 数据库字段 create_time → Java 属性 createTime
    # 这样就不用手动写 resultMap 映射了
    map-underscore-to-camel-case: true

    # log-impl: 打印 SQL 日志到控制台
    # 开发时开启，方便调试；生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

#### 3.3 Mapper 接口（注解方式）——简单 SQL 推荐

对于简单的 CRUD，直接在方法上写注解：

```java
package com.tlias.mapper;

import com.tlias.pojo.Dept;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * DeptMapper：部门数据访问接口
 * @Mapper 注解告诉 MyBatis："这个接口我需要生成实现类"
 * SpringBoot 启动时会扫描所有 @Mapper 接口，为它们创建代理对象
 */
@Mapper
public interface DeptMapper {

    /**
     * 查询所有部门
     * @Select 注解中的 SQL 会被 MyBatis 执行
     * 返回类型 List<Dept> 告诉 MyBatis：把结果映射成 Dept 对象列表
     */
    @Select("SELECT id, name, create_time, update_time FROM dept")
    List<Dept> list();

    /**
     * 根据 ID 查询
     * #{id} 是占位符，MyBatis 会自动处理为预编译参数
     * 效果和 JDBC 的 pstmt.setInt(1, id) 一样，防 SQL 注入
     */
    @Select("SELECT id, name, create_time, update_time FROM dept WHERE id = #{id}")
    Dept getById(Integer id);

    /**
     * 删除部门
     */
    @Delete("DELETE FROM dept WHERE id = #{id}")
    void deleteById(Integer id);

    /**
     * 新增部门
     * @Options 配置选项：
     *   useGeneratedKeys = true：使用数据库自动生成的主键
     *   keyProperty = "id"：把生成的主键值回填到 dept 对象的 id 属性
     * 这样插入后，dept.getId() 就能拿到新生成的 ID
     */
    @Insert("INSERT INTO dept(name, create_time, update_time) VALUES(#{name}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Dept dept);

    /**
     * 更新部门
     * #{name} 会从传入的 Dept 对象中获取 name 属性的值
     */
    @Update("UPDATE dept SET name = #{name}, update_time = NOW() WHERE id = #{id}")
    void update(Dept dept);
}
```

#### 3.4 Mapper XML（动态 SQL）——复杂 SQL 推荐

当 SQL 比较复杂（有动态条件、多表连接），用 XML 更清晰：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- MyBatis XML 文件的标准头部 -->
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<!--
    mapper 标签的 namespace 必须是对应 Mapper 接口的全限定名
    这样 MyBatis 才能把 XML 中的 SQL 和接口中的方法关联起来
-->
<mapper namespace="com.tlias.mapper.EmpMapper">

    <!--
        resultMap：自定义结果映射
        当字段名和属性名不一致，或需要映射复杂对象时，需要定义 resultMap
        id="empResultMap"：给这个映射起个名字，后面引用
        type="Emp"：映射到 Emp 类（因为配置了 type-aliases-package，不用写全限定名）
    -->
    <resultMap id="empResultMap" type="Emp">
        <!-- id 标签用于主键字段 -->
        <id property="id" column="id"/>

        <!-- result 标签用于普通字段 -->
        <result property="name" column="name"/>
        <result property="gender" column="gender"/>
        <result property="entrydate" column="entrydate"/>

        <!--
            数据库字段叫 dept_id，但 Java 属性叫 deptId（驼峰命名）
            因为我们配置了 map-underscore-to-camel-case: true
            所以这一行其实可以省略，MyBatis 会自动映射
            这里写出来是为了展示 resultMap 的用法
        -->
        <result property="deptId" column="dept_id"/>

        <!--
            映射部门名称（emp 表中没有这个字段，来自 JOIN 查询）
            查询时给 dept.name 起了别名 dept_name
        -->
        <result property="deptName" column="dept_name"/>
    </resultMap>

    <!--
        select 标签定义查询语句
        id="list"：对应 Mapper 接口中的 list() 方法
        resultMap="empResultMap"：使用上面定义的映射规则
    -->
    <select id="list" resultMap="empResultMap">
        SELECT
            e.id, e.name, e.gender, e.entrydate,
            e.dept_id, d.name AS dept_name
        FROM emp e
        LEFT JOIN dept d ON e.dept_id = d.id
        <where>
            <!--
                <if> 是动态 SQL 标签：只有当 test 条件为 true 时，才包含这段 SQL
                test="name != null and name != ''"：判断传入的参数 name 不为空
                注意：这里的 name 是传入的参数名，不是数据库字段名
            -->
            <if test="name != null and name != ''">
                AND e.name LIKE CONCAT('%', #{name}, '%')
                -- CONCAT('%', #{name}, '%') 实现模糊查询
                -- 如果 name = '张'，最终 SQL 变成：AND e.name LIKE '%张%'
            </if>

            <if test="gender != null">
                AND e.gender = #{gender}
            </if>

            <if test="begin != null and end != null">
                AND e.entrydate BETWEEN #{begin} AND #{end}
            </if>
        </where>
        <!-- <where> 标签很聪明：
             如果里面没有任何条件，它不会生成 WHERE 关键字
             如果第一个条件有 AND，它会自动去掉 AND
        -->

        ORDER BY e.entrydate DESC
    </select>

</mapper>
```

对应的 Mapper 接口：

```java
@Mapper
public interface EmpMapper {
    // 不需要注解，MyBatis 会自动找到同名的 XML 配置
    List<Emp> list(
        @Param("name") String name,
        @Param("gender") Integer gender,
        @Param("begin") LocalDate begin,
        @Param("end") LocalDate end
    );
}
```

**@Param 注解说明**：
- 当方法有多个参数时，MyBatis 需要用 `@Param` 给参数命名
- XML 中的 `#{name}` 对应 `@Param("name")`
- 如果只有一个参数，可以省略 `@Param`

---

### 4. MyBatis 动态 SQL 标签详解

动态 SQL 是 MyBatis 最强大的特性之一，让你根据条件动态组装 SQL。

| 标签 | 作用 | 使用场景 |
|------|------|---------|
| `<if>` | 条件判断 | 根据参数是否有值决定是否添加条件 |
| `<where>` | 智能 WHERE | 自动处理 WHERE 关键字和多余的 AND/OR |
| `<set>` | 智能 SET | 动态 UPDATE 时自动处理 SET 和逗号 |
| `<foreach>` | 循环遍历 | 批量操作（IN 查询、批量插入） |
| `<choose>` | 多选一 | 类似 Java 的 switch |
| `<trim>` | 自定义修剪 | 更灵活的 WHERE/SET 替代方案 |

#### 4.1 <if> 和 <where>——条件查询

```xml
<select id="list" resultType="Emp">
    SELECT * FROM emp
    <where>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="gender != null">
            AND gender = #{gender}
        </if>
    </where>
</select>
```

**生成的 SQL 根据参数动态变化：**

| 传入参数 | 生成的 SQL |
|---------|-----------|
| name="张", gender=1 | `SELECT * FROM emp WHERE name LIKE '%张%' AND gender = 1` |
| name="张", gender=null | `SELECT * FROM emp WHERE name LIKE '%张%'` |
| name=null, gender=null | `SELECT * FROM emp`（<where> 自动去掉 WHERE） |

#### 4.2 <set>——动态更新

```xml
<update id="update">
    UPDATE emp
    <set>
        <if test="name != null and name != ''">
            name = #{name},
        </if>
        <if test="gender != null">
            gender = #{gender},
        </if>
        <if test="image != null">
            image = #{image},
        </if>
        update_time = NOW()
    </set>
    WHERE id = #{id}
</update>
```

**`<set>` 的智能之处：**
- 自动添加 `SET` 关键字
- 自动去掉最后一个逗号
- 如果没有条件，不会生成 SET（避免语法错误）

#### 4.3 <foreach>——批量操作

```xml
<!-- 批量删除：DELETE FROM emp WHERE id IN (1, 2, 3) -->
<delete id="deleteByIds">
    DELETE FROM emp WHERE id IN
    <!--
        collection="ids"：传入的参数名
        item="id"：每次循环的变量名
        separator=","：元素之间用逗号分隔
        open="("：循环开始前添加左括号
        close=")"：循环结束后添加右括号
    -->
    <foreach collection="ids" item="id" separator="," open="(" close=")">
        #{id}
    </foreach>
</delete>
```

对应的 Mapper 接口：
```java
void deleteByIds(@Param("ids") List<Integer> ids);
```

调用：`empMapper.deleteByIds(Arrays.asList(1, 2, 3))`
生成 SQL：`DELETE FROM emp WHERE id IN (1, 2, 3)`

---

### 5. 连接池——数据库连接的"共享充电宝"

#### 5.1 为什么需要连接池

**问题：频繁创建连接太慢了！**

```
没有连接池时（每次请求都新建连接）：
  请求 1：创建连接（50ms）→ 执行 SQL（5ms）→ 关闭连接（10ms）= 65ms
  请求 2：创建连接（50ms）→ 执行 SQL（5ms）→ 关闭连接（10ms）= 65ms
  请求 3：创建连接（50ms）→ 执行 SQL（5ms）→ 关闭连接（10ms）= 65ms

有连接池时（连接复用）：
  启动时：创建 5 个连接（50ms × 5 = 250ms，一次性）
  请求 1：获取已有连接（1ms）→ 执行 SQL（5ms）→ 归还连接（1ms）= 7ms
  请求 2：获取已有连接（1ms）→ 执行 SQL（5ms）→ 归还连接（1ms）= 7ms
  请求 3：获取已有连接（1ms）→ 执行 SQL（5ms）→ 归还连接（1ms）= 7ms
```

**连接池的核心思想**：
- 预先创建一批连接放在"池子"里
- 需要时从池子里取一个
- 用完归还到池子，不关闭
- 池子里的连接一直保持，避免了频繁创建/销毁的开销

#### 5.2 Druid 连接池配置

本课程使用阿里巴巴的 Druid 连接池（国内最常用）：

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource    # 指定使用 Druid 连接池
    druid:
      # 初始连接数：应用启动时创建 5 个连接
      initial-size: 5

      # 最小空闲连接数：池子里最少保持 5 个连接
      # 如果空闲连接少于 5，Druid 会自动创建新的
      min-idle: 5

      # 最大活跃连接数：同时最多 20 个连接在被使用
      # 如果第 21 个请求来了，它会排队等待
      max-active: 20

      # 获取连接的最大等待时间（毫秒）
      # 如果等待 60 秒还没拿到连接，抛出异常
      max-wait: 60000

      # 连接在池中最小生存时间
      # 连接空闲超过 10 分钟，可能被回收
      min-evictable-idle-time-millis: 600000

      # 检测连接的 SQL（验证连接是否有效）
      validation-query: SELECT 1

      # 申请连接时检测是否有效（防止拿到死连接）
      test-while-idle: true
```

#### 5.3 连接池的关键参数理解

```
┌──────────────────────────────────────────────────────────────┐
│                      Druid 连接池示意                          │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│   连接池（最大 20 个连接）                                     │
│   ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐                       │
│   │Conn│ │Conn│ │Conn│ │Conn│ │Conn│  ← 初始 5 个连接        │
│   │ 1  │ │ 2  │ │ 3  │ │ 4  │ │ 5  │                       │
│   └────┘ └────┘ └────┘ └────┘ └────┘                       │
│     ↑      ↑                                       ↑         │
│   空闲   空闲                                   被使用        │
│                                                              │
│   当所有 20 个连接都被占用：                                  │
│   • 第 21 个请求排队等待（最多等 max-wait 毫秒）              │
│   • 如果超时还没拿到，报错"连接池耗尽"                        │
│                                                              │
│   当连接空闲太久：                                            │
│   • Druid 会检测连接是否还可用                                │
│   • 如果不可用，关闭并创建新连接替换                           │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**连接池常见参数设置建议：**

| 参数 | 开发环境 | 生产环境 | 说明 |
|------|---------|---------|------|
| initial-size | 5 | 10-20 | 根据应用启动速度调整 |
| min-idle | 5 | 10-20 | 保持足够的空闲连接 |
| max-active | 20 | 100-200 | 根据并发量调整，不超过数据库最大连接数 |
| max-wait | 60000 | 60000 | 等待 1 分钟 |

---

## 动手练习

### 练习 1：手写 JDBC（体会痛点）

**目标**：不用 MyBatis，纯 JDBC 实现一个查询，体会代码的重复和繁琐。

**要求**：
1. 查询 `tlias_db.dept` 表的所有数据
2. 把结果封装成 `List<Dept>`
3. 必须正确关闭所有资源（Connection、Statement、ResultSet）

**参考代码**：
```java
public List<Dept> listAllDepts() {
    List<Dept> deptList = new ArrayList<>();
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
        Class.forName("com.mysql.cj.jdbc.Driver");
        conn = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/tlias_db", "root", "123456");
        pstmt = conn.prepareStatement("SELECT * FROM dept");
        rs = pstmt.executeQuery();

        while (rs.next()) {
            Dept dept = new Dept();
            dept.setId(rs.getInt("id"));
            dept.setName(rs.getString("name"));
            dept.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
            dept.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
            deptList.add(dept);
        }
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        // 关闭资源的代码...（又写一遍）
    }
    return deptList;
}
```

完成后，对比一下：如果用 MyBatis 的 `@Select("SELECT * FROM dept")`，只需要一行。

### 练习 2：MyBatis 动态 SQL——条件查询

**目标**：实现一个员工列表查询接口，支持按姓名、性别、入职日期范围筛选。

**步骤**：

1. 在 `EmpMapper.java` 中定义方法：
```java
@Mapper
public interface EmpMapper {
    List<Emp> list(
        @Param("name") String name,
        @Param("gender") Integer gender,
        @Param("begin") LocalDate begin,
        @Param("end") LocalDate end
    );
}
```

2. 在 `src/main/resources/mapper/EmpMapper.xml` 中写动态 SQL：
```xml
<select id="list" resultType="Emp">
    SELECT * FROM emp
    <where>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="gender != null">
            AND gender = #{gender}
        </if>
        <if test="begin != null and end != null">
            AND entrydate BETWEEN #{begin} AND #{end}
        </if>
    </where>
    ORDER BY entrydate DESC
</select>
```

3. 在 Controller 中调用：
```java
@GetMapping("/emps")
public Result list(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Integer gender,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
    List<Emp> list = empMapper.list(name, gender, begin, end);
    return Result.success(list);
}
```

4. 用浏览器测试：
   - `GET /emps` → 查询所有
   - `GET /emps?name=张` → 按姓名筛选
   - `GET /emps?gender=1` → 按性别筛选
   - `GET /emps?begin=2020-01-01&end=2021-12-31` → 按日期范围筛选
   - `GET /emps?name=张&gender=1` → 组合条件

### 练习 3：MyBatis 批量删除

**目标**：实现批量删除员工功能。

1. Mapper 接口：
```java
void deleteByIds(@Param("ids") List<Integer> ids);
```

2. XML：
```xml
<delete id="deleteByIds">
    DELETE FROM emp WHERE id IN
    <foreach collection="ids" item="id" separator="," open="(" close=")">
        #{id}
    </foreach>
</delete>
```

3. 测试：`empMapper.deleteByIds(Arrays.asList(1, 2, 3))`

---

## 常见错误排查

### 阶段 1：配置问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Invalid bound statement (not found)` | Mapper XML 未加载或 namespace 错误 | 1. 检查 `mapper-locations` 配置<br>2. 检查 XML 中 namespace 是否等于接口全限定名<br>3. 检查 XML 文件名是否在 mapper-locations 范围内 |
| `Failed to parse mapping resource` | XML 语法错误 | 检查 XML 标签是否配对、特殊字符是否转义 |
| `Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required` | MyBatis 配置不完整 | 检查是否引入了 mybatis-spring-boot-starter |

### 阶段 2：SQL 执行问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Parameter 'xxx' not found` | 参数名错误或映射问题 | 1. 检查 `@Param` 注解名称和 XML 中 `#{xxx}` 是否一致<br>2. 多个参数时必须加 `@Param` |
| `There is no getter for property named 'xxx'` | 实体类缺少 getter | 添加 Lombok 的 `@Data` 或手写 getter/setter |
| `Column 'xxx' not found` | 查询结果中有字段，但实体类没有对应属性 | 检查 SQL 中的列名和实体类属性名是否对应 |
| `Duplicate entry 'xxx' for key 'PRIMARY'` | 主键冲突 | 检查插入时是否指定了已存在的 ID |

### 阶段 3：连接池问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Connection pool exhausted` | 连接池连接数不足 | 增大 `max-active`，或检查是否有连接未归还 |
| `Connection reset` | 数据库连接超时断开 | 检查 `validation-query` 和 `test-while-idle` 配置 |
| 应用启动极慢 | 连接池初始化连接太多 | 减小 `initial-size` |

### 阶段 4：动态 SQL 问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 动态条件不生效 | `test` 表达式写错 | 检查参数名是否一致，注意 `and` / `or` 的用法 |
| 生成的 SQL 语法错误 | `<where>` 或 `<set>` 用法不对 | 检查是否遗漏了标签闭合 |
| 批量操作报错 | `<foreach>` 参数传递错误 | 检查 `collection` 属性名和 `@Param` 是否一致 |

---

## 本节小结

```
┌─────────────────────────────────────────────────────────────────────────┐
│                   JDBC → MyBatis → 连接池                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  JDBC：Java 访问数据库的原生 API                                           │
│    流程：加载驱动 → 获取连接 → 创建 Statement → 执行 → 处理结果 → 关闭资源   │
│    痛点：代码重复、SQL 硬编码、结果映射繁琐、连接管理低效                     │
│                                                                          │
│  MyBatis：SQL 映射框架                                                     │
│    解决：自动化 JDBC 的繁琐工作，让你只关注 SQL                             │
│    两种方式：                                                             │
│      • 注解方式：@Select / @Insert / @Update / @Delete（简单 SQL）         │
│      • XML 方式：动态 SQL（复杂查询）                                      │
│    核心配置：                                                             │
│      • mapper-locations：XML 文件位置                                      │
│      • type-aliases-package：实体类包路径                                  │
│      • map-underscore-to-camel-case：自动驼峰映射                         │
│                                                                          │
│  动态 SQL 标签：                                                           │
│    • <if>：条件判断                                                       │
│    • <where>：智能 WHERE（自动处理关键字和 AND）                           │
│    • <set>：智能 SET（自动处理逗号）                                       │
│    • <foreach>：批量操作（IN 查询、批量删除）                              │
│                                                                          │
│  连接池（Druid）：                                                         │
│    作用：预先创建连接，复用连接，避免频繁创建/销毁的开销                     │
│    核心参数：initial-size / min-idle / max-active / max-wait               │
│                                                                          │
│  避坑指南：                                                               │
│    • Mapper XML 的 namespace 必须等于接口全限定名                          │
│    • 多参数时必须用 @Param 注解                                            │
│    • UPDATE/DELETE 动态 SQL 用 <set> 标签                                 │
│    • 生产环境配置合理的连接池参数                                          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 参考文档

- [MyBatis 3 官方中文文档](https://mybatis.org/mybatis-3/zh/index.html)
- [MyBatis-Spring-Boot-Starter 文档](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
- [Druid 连接池官方文档](https://github.com/alibaba/druid/wiki)
