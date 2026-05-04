# SpringBoot 自动配置原理

## 学习目标

- 理解 SpringBoot 自动配置的核心机制
- 掌握 @Conditional 条件注解的使用
- 能够手写简单的 Starter 自动配置

## 核心知识点

### 1. SpringBoot 自动配置的核心

SpringBoot 的自动配置基于**约定大于配置**的思想，核心流程：

1. `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`
2. `@EnableAutoConfiguration` 导入 `AutoConfigurationImportSelector`
3. `AutoConfigurationImportSelector` 读取 `META-INF/spring.factories` 中所有自动配置类
4. 通过 `@Conditional` 条件判断，满足条件的配置类生效
5. 配置类中定义的 Bean 被注册到 Spring 容器

### 2. @Conditional 条件注解

SpringBoot 使用条件注解控制配置类是否生效：

| 注解 | 条件 |
|------|------|
| `@ConditionalOnClass` | classpath 中存在指定类 |
| `@ConditionalOnMissingClass` | classpath 中不存在指定类 |
| `@ConditionalOnBean` | 容器中存在指定 Bean |
| `@ConditionalOnMissingBean` | 容器中不存在指定 Bean |
| `@ConditionalOnProperty` | 配置文件中存在指定属性 |
| `@ConditionalOnWebApplication` | 当前是 Web 应用 |

**示例**：MyBatis 自动配置类上的条件

```java
@Configuration
@ConditionalOnClass({ SqlSessionFactory.class, SqlSessionFactoryBean.class })
@ConditionalOnSingleCandidate(DataSource.class)
@EnableConfigurationProperties(MybatisProperties.class)
public class MybatisAutoConfiguration {
    // 当 classpath 中有 MyBatis 相关类，且只有一个 DataSource 时生效
}
```

### 3. 手写自动配置示例

假设我们要创建一个简单的打招呼 Starter：

**步骤 1：创建属性配置类**

```java
@ConfigurationProperties(prefix = "hello")
@Data
public class HelloProperties {
    private String prefix = "Hello";
    private String suffix = "!";
}
```

**步骤 2：创建服务类**

```java
public class HelloService {
    private HelloProperties properties;

    public HelloService(HelloProperties properties) {
        this.properties = properties;
    }

    public String sayHello(String name) {
        return properties.getPrefix() + " " + name + properties.getSuffix();
    }
}
```

**步骤 3：创建自动配置类**

```java
@Configuration
@EnableConfigurationProperties(HelloProperties.class)
@ConditionalOnClass(HelloService.class)
@ConditionalOnMissingBean(HelloService.class)
public class HelloAutoConfiguration {

    @Autowired
    private HelloProperties properties;

    @Bean
    public HelloService helloService() {
        return new HelloService(properties);
    }
}
```

**步骤 4：注册自动配置类**

在 `src/main/resources/META-INF/spring.factories` 中添加：

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.example.hello.HelloAutoConfiguration
```

**使用**：

```yaml
hello:
  prefix: "Hi"
  suffix: "~"
```

```java
@Autowired
private HelloService helloService;

// helloService.sayHello("SpringBoot") => "Hi SpringBoot~"
```

### 4. 自动配置报告

在 `application.yml` 中开启调试：

```yaml
debug: true
```

启动时控制台会输出：
- Positive matches：生效的自动配置类
- Negative matches：未生效的自动配置类及原因
- Exclusions：被排除的自动配置类

### 5. 排除自动配置

```java
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class TliasApplication { ... }
```

或配置文件中：

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

### 6. 自动配置与版本差异

| 特性 | SpringBoot 2.x | SpringBoot 3.x |
|------|---------------|----------------|
| 自动配置注册文件 | `META-INF/spring.factories` | `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` |
| 最低 JDK | JDK 8 | JDK 17 |
| Jakarta EE | `javax.*` 包 | `jakarta.*` 包 |

## 动手练习

### 练习 1：查看自动配置报告

在版本 A 项目的 `application.yml` 中设置 `debug: true`，启动项目后分析哪些自动配置类生效了。

### 练习 2：手写 Starter

按照上面的步骤，手写一个最简单的 Starter 模块，打包后引入另一个项目中测试。

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| 自动配置不生效 | spring.factories 路径或格式错误 | 确认路径是 `META-INF/spring.factories` |
| Bean 重复定义 | 自动配置和自定义配置冲突 | 使用 @ConditionalOnMissingBean |
| 属性注入失败 | @EnableConfigurationProperties 未加 | 在配置类上加该注解 |

## 本节小结

SpringBoot 自动配置的本质是"根据 classpath 和配置条件，自动注册 Bean"。理解 `@Conditional` 和 `spring.factories` 机制，你就能看懂任何 Starter 的工作原理，甚至手写自己的 Starter。

## 参考文档

- [SpringBoot 自动配置官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/auto-configuration-classes.html)
- [Creating Your Own Auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)

