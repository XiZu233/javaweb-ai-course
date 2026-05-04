# SpringBoot 分层架构与 IOC/DI

## 学习目标

- 理解三层架构（Controller/Service/Mapper）的职责划分
- 掌握 IOC（控制反转）和 DI（依赖注入）的核心概念
- 能够使用 SpringBoot 注解完成组件注册和注入
- 理解 SpringBoot 自动配置的原理

## 核心知识点

### 1. 三层架构

```
┌─────────────────────────────────────┐
│         Controller 控制层            │  ← 接收请求，返回响应
│    @RestController / @Controller     │
├─────────────────────────────────────┤
│          Service 业务层              │  ← 业务逻辑处理
│         @Service                     │
├─────────────────────────────────────┤
│          Mapper/DAO 数据层           │  ← 数据库操作
│         @Mapper / @Repository        │
└─────────────────────────────────────┘
```

**职责划分**：
- **Controller**：接收 HTTP 请求，参数校验，调用 Service，返回统一响应
- **Service**：处理业务逻辑，事务管理，调用 Mapper
- **Mapper**：执行 SQL，映射数据库表与 Java 对象

### 2. IOC 与 DI

**IOC（Inversion of Control，控制反转）**：
传统方式中，对象 A 需要使用对象 B 时，A 自己创建 B。IOC 将这个控制权交给 Spring 容器，由容器统一管理对象的创建和生命周期。

**DI（Dependency Injection，依赖注入）**：
IOC 的实现方式之一。Spring 容器在创建对象时，自动将其依赖的其他对象注入进来。

```java
// 传统方式：自己 new
public class UserService {
    private UserMapper userMapper = new UserMapperImpl(); // 紧耦合
}

// Spring 方式：依赖注入
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper; // 由 Spring 容器注入，松耦合
}
```

### 3. 常用注解

```java
// 控制层
@RestController              // = @Controller + @ResponseBody
@RequestMapping("/users")    // 基础路径

// 业务层
@Service                     // 标记为业务组件

// 数据层
@Mapper                      // MyBatis 映射器
@Repository                  // Spring 数据访问组件

// 通用组件
@Component                   // 通用组件
@Configuration               // 配置类

// 依赖注入
@Autowired                   // 按类型注入（Spring 推荐）
@Resource                    // 按名称注入（JDK 标准）

// Bean 生命周期
@PostConstruct               // 初始化方法
@PreDestroy                  // 销毁方法

// 作用域
@Scope("singleton")          // 单例（默认）
@Scope("prototype")          // 每次请求创建新实例
```

### 4. 分层解耦示例

```java
// Controller：只负责接收请求和返回结果
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public Result list() {
        return Result.success(userService.list());
    }

    @PostMapping
    public Result save(@RequestBody User user) {
        userService.save(user);
        return Result.success();
    }
}

// Service：只负责业务逻辑
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public List<User> list() {
        return userMapper.list();
    }

    @Transactional
    public void save(User user) {
        // 业务校验
        if (user.getAge() < 18) {
            throw new BusinessException("年龄必须大于18岁");
        }
        userMapper.insert(user);
    }
}

// Mapper：只负责数据库操作
@Mapper
public interface UserMapper {
    @Select("select * from user")
    List<User> list();

    @Insert("insert into user(name, age) values(#{name}, #{age})")
    void insert(User user);
}
```

### 5. SpringBoot 自动配置原理

SpringBoot 的自动配置基于**约定大于配置**的思想：

1. **@SpringBootApplication** = @Configuration + @EnableAutoConfiguration + @ComponentScan
2. **@EnableAutoConfiguration** 根据 classpath 中的依赖自动配置 Bean
3. 检测到 `spring-boot-starter-web` → 自动配置 Tomcat 和 Spring MVC
4. 检测到 `mybatis-spring-boot-starter` → 自动配置 MyBatis

可以通过 `spring.main.banner-mode=off` 关闭启动 Banner，或通过 `debug=true` 查看自动配置报告。

## 动手练习

### 练习 1：创建分层项目

1. 创建 Controller、Service、Mapper 三层
2. 实现一个简单的用户查询接口
3. 验证依赖注入是否成功

### 练习 2：理解 IOC

1. 创建一个接口 `MessageService` 和两个实现类 `EmailService`、`SmsService`
2. 在 Controller 中注入 `MessageService`
3. 使用 `@Primary` 或 `@Qualifier` 切换实现

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| Field injection is not recommended | 使用 @Autowired 注入字段 | 改为构造器注入（更推荐） |
| No qualifying bean of type | 存在多个同类型 Bean | 使用 @Primary 或 @Qualifier |
| Circular dependency | A 依赖 B，B 依赖 A | 重构代码，或使用 @Lazy |
| Component scan 未生效 | 类不在启动类同包或子包下 | 移动类位置或配置 @ComponentScan |

## 本节小结

三层架构 + IOC/DI 是 SpringBoot 的核心设计思想。**Controller 不直接操作数据库，Service 不处理 HTTP 请求**，这种职责分离让代码更易维护、更易测试。

## 参考文档

- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring IOC 详解](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans)
