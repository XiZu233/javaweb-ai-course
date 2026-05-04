# SpringBoot 分层架构与 IOC/DI

## 学习目标

学完本节后，你将能够：
- 理解为什么代码要分成 Controller、Service、Mapper 三层，而不是写在一个文件里
- 像理解"公司组织架构"一样理解 IOC（控制反转）和 DI（依赖注入）
- 熟练使用 SpringBoot 的核心注解完成组件注册和注入
- 理解 SpringBoot "零配置"启动背后的自动配置原理

---

## 核心知识点

### 1. 三层架构——为什么代码要分层

#### 1.1 一个反面教材

假设你是一个刚入行的程序员，老板让你做一个"查询用户信息"的功能。你可能这样写：

```java
// ❌ 错误示范：把所有代码堆在一个类里
@RestController
public class UserController {

    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Integer id) {
        // 第 1 步：接收 HTTP 请求（Controller 的职责）

        // 第 2 步：直接写 SQL 查询（Mapper 的职责）
        String sql = "SELECT * FROM user WHERE id = " + id;
        Connection conn = DriverManager.getConnection("jdbc:mysql://...", "root", "123");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        // 第 3 步：处理业务逻辑（Service 的职责）
        User user = new User();
        if (rs.next()) {
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            // ... 还要判断年龄是否合法、格式化日期 ...
        }

        // 第 4 步：返回响应（Controller 的职责）
        return user;
    }
}
```

**这个写法有什么问题？**
1. **代码混乱**：一个方法里同时处理 HTTP、数据库、业务逻辑，像厨房、卧室、厕所都在一个房间
2. **无法复用**：如果另一个接口也要查询用户信息，你要复制粘贴这段 SQL 代码
3. **难以测试**：想单独测试 SQL 写得对不对？不行，必须启动整个 Web 服务
4. **难以维护**：三个月后你自己都看不懂这段代码
5. **安全问题**：SQL 直接拼接字符串，有 SQL 注入风险

#### 1.2 三层架构的诞生

聪明的工程师们发现，可以把代码按职责分成三层，就像公司有三个部门：

```
┌─────────────────────────────────────────────────────────────┐
│                      用户请求（HTTP）                         │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  Controller 控制层  ←→  前台接待（礼宾部）                     │
│  • 接收请求、校验参数                                         │
│  • 调用 Service 处理业务                                      │
│  • 封装响应结果返回给前端                                     │
│  注解：@RestController                                        │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  Service 业务层  ←→  业务经理（处理具体业务逻辑）              │
│  • 处理核心业务逻辑                                           │
│  • 数据校验、权限判断                                         │
│  • 调用 Mapper 获取数据                                       │
│  • 事务管理（要么全成功，要么全回滚）                          │
│  注解：@Service                                               │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  Mapper/DAO 数据层  ←→  仓库管理员（只和数据库打交道）         │
│  • 执行 SQL 查询                                              │
│  • 数据库表 和 Java 对象的映射                                │
│  • 不涉及任何业务判断                                         │
│  注解：@Mapper                                                │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      数据库（MySQL）                          │
└─────────────────────────────────────────────────────────────┘
```

**生活类比——去餐厅吃饭：**
- **Controller（服务员）**：你点菜说"要一份宫保鸡丁"，服务员记下来，但不炒菜
- **Service（厨师）**：厨师决定怎么做、放多少料、炒多久，但不自己去地里摘菜
- **Mapper（采购员）**：去菜市场买鸡肉和花生，但不决定这道菜怎么做

**分层的好处：**
1. **职责清晰**：每层只干自己的事，像流水线一样
2. **易于复用**：Service 的方法可以被多个 Controller 调用
3. **易于测试**：可以单独测试 Mapper 的 SQL 是否正确
4. **易于替换**：有一天不用 MySQL 改用 Oracle 了，只改 Mapper 层
5. **团队协作**：三个人可以分别写三层，互不影响

---

### 2. IOC 与 DI——从"自己造"到"别人给"

#### 2.1 传统方式：自己 new 对象

想象你要组装一台电脑：

```java
// 传统方式：自己买零件、自己组装
public class UserService {

    // 我自己创建 UserMapper 对象
    private UserMapper userMapper = new UserMapperImpl();

    // 如果 UserMapper 又依赖了 SqlSessionFactory...
    // 如果 SqlSessionFactory 又依赖了 DataSource...
    // 如果 DataSource 又依赖了数据库连接池配置...
    // 你会发现：为了创建一个对象，你要先创建它依赖的所有对象！
    // 这就是"对象之间的紧耦合"
}
```

**问题：**
- `UserService` 和 `UserMapperImpl` 绑死了，换实现类要改代码
- 对象的创建顺序很难管理（A 依赖 B，B 依赖 C，C 依赖 D...）
- 每个对象都是"单例"还是"多例"？自己管理很麻烦
- 对象创建后怎么销毁？内存泄漏怎么办？

#### 2.2 IOC：控制反转

**IOC（Inversion of Control，控制反转）**的核心思想只有一句话：

> **"你不要自己创建对象，让 Spring 容器帮你创建和管理。"**

就像装修房子：
- **传统方式**：你自己买水泥、买砖、买电线、自己施工（累死）
- **IOC 方式**：你告诉装修公司"我要简约风格"，装修公司搞定一切（你只关注"住"这件事）

在代码中：
```java
// IOC 方式：声明"我需要什么"，Spring 给你注入
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;  // Spring 会自动创建并注入进来

    // 你不需要关心 userMapper 是怎么创建的
    // 你不需要关心它依赖了什么
    // 你只需要用它
}
```

#### 2.3 DI：依赖注入

**DI（Dependency Injection，依赖注入）**是 IOC 的一种实现方式。

"注入"这个词很形象——就像打针，把药物（依赖对象）注射（注入）到身体（你的类）里。

Spring 有三种注入方式：

**方式一：字段注入（Field Injection）——最常用**
```java
@Service
public class UserService {
    @Autowired           // Spring 自动把 UserMapper 的实例注入到这个字段
    private UserMapper userMapper;
}
```
优点：代码最简洁
缺点：无法给字段加 `final`，单元测试时难以 mock

**方式二：构造器注入（Constructor Injection）——官方推荐**
```java
@Service
public class UserService {

    private final UserMapper userMapper;

    // Spring 会自动调用这个构造方法，传入 UserMapper 实例
    @Autowired  // Spring 4.3+ 可以省略这个注解
    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
}
```
优点：
- 可以加 `final`，保证对象不会被修改
- 依赖一目了然（看构造方法就知道这个类需要什么）
- 单元测试时可以手动传入 mock 对象

**方式三：Setter 注入（Setter Injection）**
```java
@Service
public class UserService {
    private UserMapper userMapper;

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
}
```
优点：可选依赖，不设置也能正常运行
缺点：对象可能在 setter 调用前被使用，导致 NullPointerException

**我们的课程统一使用字段注入（最简洁），但你要知道构造器注入是更规范的做法。**

#### 2.4 IOC 容器的工作流程

```
┌──────────────────────────────────────────────────────────────┐
│                   Spring IOC 容器启动过程                       │
├──────────────────────────────────────────────────────────────┤
│  第 1 步：扫描                                                   │
│    Spring 扫描 @SpringBootApplication 所在包及其子包               │
│    找到所有带 @Component、@Service、@Controller、@Mapper 的类      │
│                                                                │
│  第 2 步：注册                                                   │
│    把这些类"登记"到容器中，称为 Bean（豆荚）                       │
│    每个 Bean 有一个名字（默认是类名首字母小写）                     │
│    例如：UserService → userService                               │
│                                                                │
│  第 3 步：创建                                                   │
│    根据 @Scope 决定创建方式：                                     │
│    • singleton（默认）：只创建一个，大家共用                       │
│    • prototype：每次需要都创建新的                                │
│                                                                │
│  第 4 步：注入                                                   │
│    遍历所有 Bean，找到 @Autowired 标记的字段                       │
│    从容器中查找对应类型的 Bean，注入进去                           │
│                                                                │
│  第 5 步：初始化                                                 │
│    调用 @PostConstruct 标记的方法（如初始化缓存、建立连接等）       │
│                                                                │
│  第 6 步：就绪                                                   │
│    容器启动完成，开始接收 HTTP 请求                               │
│                                                                │
│  第 7 步：销毁（应用关闭时）                                      │
│    调用 @PreDestroy 标记的方法（如关闭连接、释放资源）              │
└──────────────────────────────────────────────────────────────┘
```

---

### 3. 常用注解——SpringBoot 的"标签系统"

#### 3.1 组件类注解

| 注解 | 含义 | 使用位置 | 类比 |
|------|------|---------|------|
| `@RestController` | 控制层组件 | Controller 类 | 餐厅服务员 |
| `@Service` | 业务层组件 | Service 类 | 厨师 |
| `@Mapper` | MyBatis 映射器 | Mapper 接口 | 采购员 |
| `@Repository` | 数据访问组件 | DAO 类 | 仓库管理员 |
| `@Component` | 通用组件 | 工具类、配置类 | 杂工 |
| `@Configuration` | 配置类 | 配置类 | 装修设计师 |

**@RestController 详解：**
```java
// @RestController = @Controller + @ResponseBody

// @Controller 表示"这是一个控制器类，处理 HTTP 请求"
// @ResponseBody 表示"返回的内容直接作为响应体，不要去找模板页面"

@RestController
@RequestMapping("/users")   // 基础路径：这个类的所有接口都以 /users 开头
public class UserController {

    @GetMapping             // 完整路径 = /users + "" = /users
    public List<User> list() { ... }

    @GetMapping("/{id}")    // 完整路径 = /users + /{id} = /users/1
    public User getById(@PathVariable Integer id) { ... }
}
```

#### 3.2 依赖注入注解

| 注解 | 注入方式 | 来源 | 推荐使用场景 |
|------|---------|------|------------|
| `@Autowired` | 按类型注入 | Spring 提供 | 最常用，Spring 项目首选 |
| `@Resource` | 按名称注入 | JDK 标准 | 需要按名称匹配时使用 |

```java
// @Autowired 按类型注入
@Autowired
private UserMapper userMapper;  // Spring 找类型为 UserMapper 的 Bean

// 如果有多个同类型的 Bean，配合 @Qualifier 指定名称
@Autowired
@Qualifier("userMapperImpl2")   // 指定要注入名字叫 userMapperImpl2 的 Bean
private UserMapper userMapper;

// @Resource 按名称注入（JDK 标准，不依赖 Spring）
@Resource(name = "userMapperImpl2")
private UserMapper userMapper;
```

#### 3.3 生命周期注解

```java
@Service
public class DataInitializer {

    // 构造方法
    public DataInitializer() {
        System.out.println("1. 构造方法：对象被创建");
    }

    @PostConstruct
    public void init() {
        // 容器创建 Bean 后、使用前调用
        System.out.println("2. 初始化：加载缓存、预热数据...");
    }

    @PreDestroy
    public void destroy() {
        // 容器关闭前调用
        System.out.println("3. 销毁：关闭连接、释放资源...");
    }
}
```

**执行顺序：**
```
构造方法 → @PostConstruct → Bean 使用 → @PreDestroy → 对象销毁
```

---

### 4. 分层解耦完整示例——从请求到数据库的完整链路

我们来实现一个"用户管理"的完整功能，看看三层如何协作：

#### 4.1 Controller 层——"前台接待"

```java
package com.tlias.controller;

import com.tlias.pojo.Result;
import com.tlias.pojo.User;
import com.tlias.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UserController：用户管理控制器
 * 职责：接收前端请求，调用 Service，返回统一响应
 * 规则：
 *   1. 不做业务判断（交给 Service）
 *   2. 不直接操作数据库（交给 Mapper）
 *   3. 只负责：接收参数 → 调用 Service → 封装返回结果
 */
@RestController                  // 声明为 REST 控制器，返回 JSON
@RequestMapping("/users")        // 基础路径，所有接口以 /users 开头
public class UserController {

    @Autowired                   // Spring 自动注入 UserService 实例
    private UserService userService;

    /**
     * 查询所有用户
     * GET /users
     * 对应前端：点击"用户列表"按钮
     */
    @GetMapping
    public Result list() {
        // 1. 调用 Service 获取数据（不自己做）
        List<User> userList = userService.list();

        // 2. 封装成统一响应返回
        // Result.success() 是我们定义的包装类，结构：{code:1, msg:"success", data:[...]}
        return Result.success(userList);
    }

    /**
     * 根据 ID 查询用户
     * GET /users/1
     * @PathVariable：从 URL 路径中提取 {id} 的值
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        User user = userService.getById(id);
        return Result.success(user);
    }

    /**
     * 新增用户
     * POST /users
     * @RequestBody：把请求体的 JSON 转成 User 对象
     */
    @PostMapping
    public Result save(@RequestBody User user) {
        userService.save(user);
        // 新增成功不需要返回数据，直接返回成功标记
        return Result.success();
    }

    /**
     * 删除用户
     * DELETE /users/1
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        userService.delete(id);
        return Result.success();
    }
}
```

#### 4.2 Service 层——"业务经理"

```java
package com.tlias.service;

import com.tlias.mapper.UserMapper;
import com.tlias.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UserService：用户业务逻辑处理
 * 职责：处理业务规则、数据校验、事务管理
 * 规则：
 *   1. 不接收 HTTP 请求（那是 Controller 的事）
 *   2. 不直接写 SQL（那是 Mapper 的事）
 *   3. 只做：业务判断 → 数据校验 → 调用 Mapper → 返回结果
 */
@Service                         // 声明为业务层组件，Spring 会管理它的生命周期
public class UserService {

    @Autowired                   // 注入 Mapper，让 Service 能操作数据库
    private UserMapper userMapper;

    /**
     * 查询所有用户
     * 简单查询，直接透传给 Mapper
     */
    public List<User> list() {
        return userMapper.list();
    }

    /**
     * 根据 ID 查询
     */
    public User getById(Integer id) {
        return userMapper.getById(id);
    }

    /**
     * 新增用户
     * @Transactional：声明式事务
     * 作用：方法内的所有数据库操作要么全部成功，要么全部回滚
     * 场景：如果新增用户后还要同步初始化用户配置，两者必须一起成功
     */
    @Transactional
    public void save(User user) {
        // 1. 业务校验（Service 的核心价值之一）
        if (user.getAge() != null && user.getAge() < 18) {
            // 未成年不允许注册
            throw new RuntimeException("年龄必须大于等于18岁");
        }

        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }

        // 2. 可以在这里加更多业务逻辑
        // 例如：检查用户名是否已存在
        // 例如：给新用户设置默认头像
        // 例如：发送欢迎短信

        // 3. 调用 Mapper 写入数据库
        userMapper.insert(user);
    }

    /**
     * 删除用户
     */
    public void delete(Integer id) {
        userMapper.deleteById(id);
    }
}
```

#### 4.3 Mapper 层——"仓库管理员"

```java
package com.tlias.mapper;

import com.tlias.pojo.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * UserMapper：用户数据访问层
 * 职责：只负责 SQL 执行和数据映射
 * 规则：
 *   1. 不做业务判断
 *   2. 不做参数校验
 *   3. 只写 SQL 和简单的结果映射
 */
@Mapper                          // MyBatis 扫描到这个接口，会自动生成实现类
public interface UserMapper {

    /**
     * 查询所有用户
     * @Select：直接在注解中写 SQL（简单查询推荐）
     */
    @Select("SELECT id, name, age, gender FROM user")
    List<User> list();

    /**
     * 根据 ID 查询
     * #{id}：预编译参数，防止 SQL 注入
     */
    @Select("SELECT id, name, age, gender FROM user WHERE id = #{id}")
    User getById(Integer id);

    /**
     * 新增用户
     * @Options(useGeneratedKeys = true, keyProperty = "id")：
     *   让数据库自动生成主键，并回填到 user.id 字段
     */
    @Insert("INSERT INTO user(name, age, gender) VALUES(#{name}, #{age}, #{gender})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);

    /**
     * 根据 ID 删除
     */
    @Delete("DELETE FROM user WHERE id = #{id}")
    void deleteById(Integer id);
}
```

#### 4.4 统一响应类——Result

```java
package com.tlias.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果类
 * 前端收到的永远是这个结构，方便统一处理
 * {
 *   "code": 1,           // 1=成功，0=失败
 *   "msg": "success",    // 提示信息
 *   "data": { ... }      // 业务数据（成功时有，失败时可能为 null）
 * }
 */
@Data                            // Lombok：自动生成 getter、setter、toString
@NoArgsConstructor               // Lombok：生成无参构造方法
@AllArgsConstructor              // Lombok：生成全参构造方法
public class Result {
    private Integer code;        // 状态码：1 成功，0 失败
    private String msg;          // 提示信息
    private Object data;         // 业务数据（可以是任意类型）

    /**
     * 快速创建成功响应（无数据）
     * 用于新增、删除、更新成功后，只需要告诉前端"成功了"
     */
    public static Result success() {
        return new Result(1, "success", null);
    }

    /**
     * 快速创建成功响应（带数据）
     * 用于查询成功，需要把数据传给前端
     */
    public static Result success(Object data) {
        return new Result(1, "success", data);
    }

    /**
     * 快速创建失败响应
     * 用于参数错误、权限不足等业务异常
     */
    public static Result error(String msg) {
        return new Result(0, msg, null);
    }
}
```

---

### 5. SpringBoot 自动配置原理——"约定大于配置"

#### 5.1 为什么 SpringBoot 能"零配置"启动

传统 Spring 项目需要大量 XML 配置：
```xml
<!-- 传统 Spring 的配置文件（application-context.xml） -->
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/>
    <property name="url" value="jdbc:mysql://localhost:3306/tlias"/>
    <property name="username" value="root"/>
    <property name="password" value="123456"/>
</bean>

<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
    <property name="dataSource" ref="dataSource"/>
</bean>
<!-- ... 还有几十个 bean 要配置 ... -->
```

SpringBoot 的出现让这一切都消失了。它凭什么能做到？

#### 5.2 自动配置的三板斧

**第一板斧：@SpringBootApplication**

```java
@SpringBootApplication  // ← 这一个注解，等于下面三个
public class TliasApplication {
    public static void main(String[] args) {
        SpringApplication.run(TliasApplication.class, args);
    }
}

// @SpringBootApplication 实际上是：
// @Configuration          → 表示这是一个配置类
// @EnableAutoConfiguration → 开启自动配置（核心！）
// @ComponentScan          → 扫描当前包及子包下的所有组件
```

**第二板斧：@EnableAutoConfiguration 的魔法**

SpringBoot 启动时会做一件事：**查看你的 classpath 里有哪些 jar 包**。

```
SpringBoot 扫描 classpath：
  发现 spring-boot-starter-web.jar → 自动配置 Tomcat + Spring MVC
  发现 mybatis-spring-boot-starter.jar → 自动配置 MyBatis + SqlSessionFactory
 发现 mysql-connector-java.jar → 自动配置 DataSource（连接池）
  发现 druid.jar → 自动配置 Druid 连接池
```

每一个 `xxx-spring-boot-starter` 里面都包含一个 `META-INF/spring.factories` 文件，里面列出了"如果检测到某条件，就自动配置某 Bean"。

**第三板斧：application.yml 微调**

自动配置有默认值，但你可以覆盖：

```yaml
# application.yml —— 告诉 SpringBoot："我知道你能自动配置，但我有些特殊要求"

# 自动配置的 DataSource 默认连接哪里？你自己指定
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/tlias
    username: root
    password: 123456

# 自动配置的 Tomcat 默认端口是 8080，你想改？
server:
  port: 8080

# 自动配置的 Jackson 日期格式默认是时间戳，你想改？
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
```

#### 5.3 自动配置的执行流程

```
┌──────────────────────────────────────────────────────────────┐
│              SpringBoot 自动配置流程（简化版）                  │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  1. 启动类 main() 调用 SpringApplication.run()                │
│                    ↓                                         │
│  2. 创建 Spring 容器                                          │
│                    ↓                                         │
│  3. 读取所有 starter jar 包中的 META-INF/spring.factories     │
│                    ↓                                         │
│  4. 每个自动配置类上都有 @ConditionalOnClass                  │
│     → "如果 classpath 里有 Xxx.class，我才生效"               │
│                    ↓                                         │
│  5. 生效的自动配置类创建默认 Bean                             │
│     → 如：Tomcat、DispatcherServlet、DataSource...            │
│                    ↓                                         │
│  6. 读取 application.yml，用你配置的值覆盖默认值               │
│                    ↓                                         │
│  7. 扫描 @ComponentScan 范围内的组件                          │
│                    ↓                                         │
│  8. 完成依赖注入，容器启动完毕                                 │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**一句话总结：SpringBoot 的"零配置"不是真的没有配置，而是"配置被提前写好了，你只需要改你想改的部分"。**

---

## 动手练习

### 练习 1：创建完整的三层项目

**目标**：从零搭建一个"部门管理"接口，体验完整的三层协作流程。

**步骤**：

1. 在 `version-a/tlias-backend` 中创建以下文件：

```java
// 1. pojo/Dept.java —— 数据对象
package com.tlias.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Dept {
    private Integer id;
    private String name;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

```java
// 2. mapper/DeptMapper.java —— 数据层
package com.tlias.mapper;

import com.tlias.pojo.Dept;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DeptMapper {

    @Select("SELECT id, name, create_time, update_time FROM dept")
    List<Dept> list();

    @Delete("DELETE FROM dept WHERE id = #{id}")
    void deleteById(Integer id);

    @Insert("INSERT INTO dept(name, create_time, update_time) VALUES(#{name}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Dept dept);
}
```

```java
// 3. service/DeptService.java —— 业务层
package com.tlias.service;

import com.tlias.mapper.DeptMapper;
import com.tlias.pojo.Dept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DeptService {

    @Autowired
    private DeptMapper deptMapper;

    public List<Dept> list() {
        return deptMapper.list();
    }

    public void delete(Integer id) {
        deptMapper.deleteById(id);
    }

    public void add(Dept dept) {
        deptMapper.insert(dept);
    }
}
```

```java
// 4. controller/DeptController.java —— 控制层
package com.tlias.controller;

import com.tlias.pojo.Dept;
import com.tlias.pojo.Result;
import com.tlias.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/depts")
public class DeptController {

    @Autowired
    private DeptService deptService;

    @GetMapping
    public Result list() {
        List<Dept> deptList = deptService.list();
        return Result.success(deptList);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        deptService.delete(id);
        return Result.success();
    }

    @PostMapping
    public Result add(@RequestBody Dept dept) {
        deptService.add(dept);
        return Result.success();
    }
}
```

2. 启动项目，用 Postman 或浏览器测试：
   - `GET http://localhost:8080/depts` → 查询部门列表
   - `POST http://localhost:8080/depts` + JSON body → 新增部门
   - `DELETE http://localhost:8080/depts/1` → 删除部门

### 练习 2：理解 IOC 的多实现切换

**目标**：体会"面向接口编程" + "依赖注入"的好处。

**步骤**：

1. 定义接口：
```java
public interface MessageService {
    void send(String message);
}
```

2. 创建两个实现：
```java
@Service
@Primary  // 默认使用这个实现
public class EmailService implements MessageService {
    public void send(String message) {
        System.out.println("发送邮件：" + message);
    }
}

@Service
public class SmsService implements MessageService {
    public void send(String message) {
        System.out.println("发送短信：" + message);
    }
}
```

3. 在 Controller 中注入接口：
```java
@RestController
public class MessageController {

    @Autowired
    private MessageService messageService;  // Spring 会自动注入 @Primary 标记的实现

    @GetMapping("/send")
    public void send() {
        messageService.send("Hello IOC!");  // 输出：发送邮件：Hello IOC!
    }
}
```

4. 尝试切换实现：去掉 `EmailService` 的 `@Primary`，加到 `SmsService` 上，重启后观察输出变化。

---

## 常见错误排查

### 阶段 1：项目启动失败

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Failed to configure a DataSource` | 引入了 mybatis 依赖但没有配置数据库连接 | 在 `application.yml` 中配置 `spring.datasource` 信息 |
| `Consider defining a bean of type 'XxxMapper'` | Mapper 接口没有被扫描到 | 1. 检查是否有 `@Mapper` 注解<br>2. 检查 Mapper 是否在启动类同包或子包下 |
| `Field xxxMapper in com.tlias.service.XxxService required a bean of type` | Service 注入 Mapper 失败 | 检查 Mapper 是否有 `@Mapper`，或启动类是否有 `@MapperScan` |
| `Port 8080 was already in use` | 8080 端口被其他程序占用 | `application.yml` 中改 `server.port=8081` |

### 阶段 2：接口访问异常

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `404 Not Found` | URL 路径错误或请求方法不匹配 | 1. 检查 `@RequestMapping` 路径<br>2. 检查用的注解是 `@GetMapping` 还是 `@PostMapping` |
| `400 Bad Request` | 参数绑定失败 | 1. 检查 JSON 字段名是否和对象属性名一致<br>2. 检查是否有 `@RequestBody`（POST 请求需要） |
| `405 Method Not Allowed` | 请求方法和接口定义不匹配 | 用 GET 调了 POST 接口，或反之 |
| `Whitelabel Error Page` | SpringBoot 默认错误页，表示后端抛异常 | 查看 IDEA 控制台异常堆栈 |

### 阶段 3：数据层问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Table 'tlias.xxx' doesn't exist` | 数据库表不存在 | 执行初始化 SQL 脚本创建表 |
| `Unknown column 'xxx' in 'field list'` | 实体类属性和数据库字段不匹配 | 检查 `pojo` 类属性名和数据库字段名 |
| `Data too long for column 'xxx'` | 插入数据长度超过字段限制 | 缩短数据长度或修改数据库字段类型 |

### 阶段 4：依赖注入问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `No qualifying bean of type 'XxxService' available` | Spring 容器里没有这个类型的 Bean | 1. 检查是否有 `@Service`<br>2. 检查类是否在扫描范围内 |
| `Could not autowire. No beans of 'XxxMapper' type found.` | IDEA 的误报，实际运行正常 | IDEA 右键 `pom.xml` → Maven → Reload Project |
| `Circular dependency` | A 依赖 B，B 依赖 A | 重构代码打破循环，或构造器注入改字段注入 |

---

## 本节小结

```
┌─────────────────────────────────────────────────────────────┐
│                SpringBoot 分层架构 + IOC/DI                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  三层架构：                                                   │
│    Controller（控制层）= 服务员：接收请求、返回结果               │
│    Service（业务层）   = 厨师：业务逻辑、数据校验、事务管理        │
│    Mapper（数据层）    = 采购员：执行 SQL、数据库映射              │
│                                                              │
│  IOC/DI：                                                    │
│    IOC = 控制反转：你不要自己 new 对象，Spring 容器帮你管理       │
│    DI  = 依赖注入：Spring 自动把依赖的对象"注射"到你的类中        │
│    @Autowired = 按类型自动注入                                 │
│                                                              │
│  核心注解：                                                   │
│    @RestController = 控制器（处理 HTTP）                       │
│    @Service        = 业务组件                                  │
│    @Mapper         = MyBatis 映射器                            │
│    @Autowired      = 自动注入                                  │
│    @Transactional  = 事务管理                                  │
│                                                              │
│  自动配置：                                                   │
│    @SpringBootApplication = 开启自动配置 + 组件扫描             │
│    classpath 检测到 starter → 自动配置对应组件                  │
│    application.yml → 覆盖自动配置的默认值                       │
│                                                              │
│  黄金法则：                                                   │
│    Controller 不直接操作数据库！                                │
│    Service 不处理 HTTP 请求！                                   │
│    Mapper 不写业务逻辑！                                        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 参考文档

- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring IOC 容器详解](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans)
- [MyBatis-Spring-Boot-Starter 文档](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
