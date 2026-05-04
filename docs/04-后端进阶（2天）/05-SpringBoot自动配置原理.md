# SpringBoot 自动配置原理

## 学习目标

- 理解 `@SpringBootApplication` 注解的组成，掌握三大元注解各自的作用
- 掌握自动配置的核心流程：从 `spring.factories` / `AutoConfiguration.imports` 到条件注解判断
- 能够熟练使用 `@Conditional` 系列注解控制 Bean 的注册条件
- 理解 SpringBoot "约定大于配置" 的设计哲学
- 能够手写一个简单的 Starter，理解自定义自动配置的完整流程

---

## 核心知识点

### 1. 为什么需要自动配置

#### 1.1 传统 Spring 项目的配置之痛

在没有 SpringBoot 之前，开发一个 Web 项目需要写大量配置：

```xml
<!-- 传统 Spring 项目的 applicationContext.xml -->
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/>
    <property name="url" value="jdbc:mysql://localhost:3306/test"/>
    <property name="username" value="root"/>
    <property name="password" value="123456"/>
</bean>

<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
    <property name="dataSource" ref="dataSource"/>
    <property name="mapperLocations" value="classpath:mapper/*.xml"/>
</bean>

<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
    <property name="basePackage" value="com.example.mapper"/>
</bean>

<!-- 还有事务管理器、视图解析器、组件扫描... -->
```

**真实场景类比**：传统 Spring 配置就像自己组装电脑——你需要知道 CPU 怎么插、内存怎么装、硬盘怎么接、线怎么理。SpringBoot 自动配置就像买一台品牌整机——开机即用，但高手也可以打开机箱自己升级。

#### 1.2 SpringBoot 的解决思路

SpringBoot 提出了**"约定大于配置"**（Convention Over Configuration）的理念：

```
传统方式：
  你要什么 → 你配置什么 → Spring 帮你创建

SpringBoot 方式：
  你引入依赖 → SpringBoot 猜测你要什么 → 自动帮你配置好
```

比如：
- 你在 pom.xml 中引入了 `spring-boot-starter-web` → SpringBoot 自动配置 Tomcat、Spring MVC、Jackson
- 你在 pom.xml 中引入了 `mybatis-spring-boot-starter` → SpringBoot 自动配置 SqlSessionFactory、MapperScanner
- 你在 `application.yml` 中写了 `spring.datasource.url` → SpringBoot 自动创建 DataSource

---

### 2. @SpringBootApplication 拆解

`@SpringBootApplication` 是一个组合注解，等于三个注解的合体：

```java
// 这是 @SpringBootApplication 的简化定义
@SpringBootConfiguration      // ①
@EnableAutoConfiguration      // ②
@ComponentScan                // ③
public @interface SpringBootApplication {
}
```

#### 2.1 @SpringBootConfiguration

```java
@Configuration
public @interface SpringBootConfiguration {
}
```

- 本质上就是 `@Configuration`，表示这是一个配置类
- 作用：允许在启动类中定义 `@Bean` 方法

```java
@SpringBootApplication
public class TliasApplication {

    // 因为启动类有 @Configuration，所以可以直接定义 Bean
    @Bean
    public MyService myService() {
        return new MyService();
    }

    public static void main(String[] args) {
        SpringApplication.run(TliasApplication.class, args);
    }
}
```

#### 2.2 @ComponentScan

- 开启组件扫描，自动发现并注册 Spring Bean
- 默认扫描当前包及其子包下的 `@Component`、`@Service`、`@Controller`、`@Repository` 等

```
项目结构：
  com.tlias
    ├── TliasApplication.java      ← 启动类在这里
    ├── config/
    │     └── WebConfig.java       ← 会被扫描到（子包）
    ├── controller/
    │     └── EmpController.java   ← 会被扫描到（子包）
    └── service/
          └── EmpService.java      ← 会被扫描到（子包）

注意：如果类在 com.tlias 的父包或兄弟包中，不会被扫描到！
```

#### 2.3 @EnableAutoConfiguration（核心）

这是自动配置的入口注解，它导入了一个选择器：

```java
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {
}
```

`AutoConfigurationImportSelector` 的核心逻辑：

```
+-----------------------------------------------------------+
|              @EnableAutoConfiguration 执行流程              |
+-----------------------------------------------------------+
|                                                           |
|   ① Spring 启动                                            |
|     |                                                     |
|   ② 解析 @EnableAutoConfiguration                          |
|     |                                                     |
|   ③ 调用 AutoConfigurationImportSelector                   |
|     |                                                     |
|   ④ 读取自动配置注册文件                                     |
|     |  SpringBoot 2.x: META-INF/spring.factories           |
|     |  SpringBoot 3.x: META-INF/spring/org.springframework.|
|     |                  boot.autoconfigure.AutoConfiguration|
|     |                  .imports                            |
|     |                                                     |
|   ⑤ 获取所有自动配置类全限定名列表                            |
|     |  如：org.mybatis.spring.boot.autoconfigure.          |
|     |       MybatisAutoConfiguration                       |
|     |                                                     |
|   ⑥ 逐个解析配置类上的 @Conditional 条件注解                 |
|     |  如：@ConditionalOnClass(DataSource.class)           |
|     |      → classpath 中有 DataSource 才生效              |
|     |                                                     |
|   ⑦ 条件满足的配置类被注册到 Spring 容器                      |
|     |  配置类中的 @Bean 方法被执行，Bean 被创建               |
|     |                                                     |
|   ⑧ 自动配置完成                                           |
|                                                           |
+-----------------------------------------------------------+
```

---

### 3. 自动配置注册文件

#### 3.1 SpringBoot 2.x：spring.factories

在 SpringBoot 2.x 中，所有自动配置类注册在 `META-INF/spring.factories` 文件中：

```properties
# META-INF/spring.factories
# 这个文件位于每个 Starter 的 jar 包中

# 自动配置的键是固定的：EnableAutoConfiguration
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
  org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration
```

**文件格式说明**：
- 键：`org.springframework.boot.autoconfigure.EnableAutoConfiguration`
- 值：逗号分隔的自动配置类全限定名
- 每行末尾的 `\` 是续行符（properties 文件多行写法）

#### 3.2 SpringBoot 3.x：AutoConfiguration.imports

SpringBoot 3.x 改变了注册方式，使用新的文件：

```
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
# 注意：这是文件路径，不是 properties 键值对格式
# 每行一个自动配置类全限定名

org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration
```

**为什么 SpringBoot 3 要改？**
- `spring.factories` 是 properties 格式，所有类型的配置混在一起
- 新的 `.imports` 文件更纯粹，只放自动配置类
- 支持更灵活的导入机制（如 `@AutoConfiguration` 注解）

#### 3.3 版本差异对比

| 特性 | SpringBoot 2.x | SpringBoot 3.x |
|------|---------------|----------------|
| 自动配置注册文件 | `META-INF/spring.factories` | `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` |
| 文件格式 | properties 键值对 | 纯文本，每行一个类名 |
| 最低 JDK | JDK 8 | JDK 17 |
| Jakarta EE 包名 | `javax.*` | `jakarta.*` |
| 自动配置类注解 | `@Configuration` | `@AutoConfiguration`（新注解，可指定 before/after） |

---

### 4. @Conditional 条件注解

自动配置不是一股脑把所有配置都加载，而是通过条件注解判断"当前环境是否需要这个配置"。

#### 4.1 常用条件注解速查表

| 注解 | 条件 | 示例场景 |
|------|------|---------|
| `@ConditionalOnClass` | classpath 中存在指定类 | 有 `DataSource.class` 才配置数据源 |
| `@ConditionalOnMissingClass` | classpath 中不存在指定类 | 没有 `Tomcat.class` 才配置 Jetty |
| `@ConditionalOnBean` | 容器中存在指定 Bean | 有 `DataSource` Bean 才配置 `SqlSessionFactory` |
| `@ConditionalOnMissingBean` | 容器中不存在指定 Bean | 没有自定义的 `ObjectMapper` 才配置默认的 |
| `@ConditionalOnProperty` | 配置文件中存在指定属性 | `spring.datasource.url` 存在才配置数据源 |
| `@ConditionalOnWebApplication` | 当前是 Web 应用 | 只在 Web 环境下配置 `DispatcherServlet` |
| `@ConditionalOnExpression` | SpEL 表达式为 true | 复杂条件判断 |
| `@ConditionalOnJava` | 指定 Java 版本 | JDK 17+ 才启用某些功能 |

#### 4.2 条件注解工作原理

```java
/**
 * @ConditionalOnClass 的简化理解
 * 实际源码更复杂，这里展示核心逻辑
 */
public class OnClassCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // 获取 @ConditionalOnClass 中指定的类名
        String[] classNames = getClassNames(metadata);

        for (String className : classNames) {
            try {
                // 尝试加载类
                Class.forName(className);
                // 加载成功 → 条件满足
                return true;
            } catch (ClassNotFoundException e) {
                // 加载失败 → 条件不满足
                return false;
            }
        }
        return true;
    }
}
```

#### 4.3 MyBatis 自动配置类分析

以 MyBatis 的自动配置类为例，看条件注解如何配合使用：

```java
package org.mybatis.spring.boot.autoconfigure;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * MyBatis 自动配置类
 * 展示了多个条件注解的组合使用
 */
@Configuration
// 条件1：classpath 中必须有 SqlSessionFactory 和 SqlSessionFactoryBean
// 说明：只有引入了 mybatis-spring 依赖，这个配置才生效
@ConditionalOnClass({ SqlSessionFactory.class, SqlSessionFactoryBean.class })
// 条件2：容器中必须有且只有一个 DataSource Bean
// 说明：需要先配置好数据源，才能配置 SqlSessionFactory
@ConditionalOnSingleCandidate(DataSource.class)
// 启用 MybatisProperties 配置属性绑定
// 可以将 application.yml 中的 mybatis.* 配置绑定到对象
@EnableConfigurationProperties(MybatisProperties.class)
public class MybatisAutoConfiguration {

    private final MybatisProperties properties;
    private final DataSource dataSource;

    // 通过构造器注入 DataSource
    public MybatisAutoConfiguration(MybatisProperties properties,
                                     DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    /**
     * 创建 SqlSessionFactory Bean
     * @ConditionalOnMissingBean：如果用户自己定义了 SqlSessionFactory，就用用户的
     * 这体现了"约定大于配置，但允许覆盖"
     */
    @Bean
    @ConditionalOnMissingBean  // 条件3：容器中没有 SqlSessionFactory 时才创建
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        // 使用自动注入的 DataSource
        factory.setDataSource(dataSource);
        // 使用配置文件中的 mapper 路径
        factory.setMapperLocations(properties.resolveMapperLocations());
        // ... 其他配置
        return factory.getObject();
    }

    /**
     * 创建 MapperScannerConfigurer
     * 自动扫描 @Mapper 接口
     */
    @Bean
    @ConditionalOnMissingBean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer scanner = new MapperScannerConfigurer();
        // 扫描路径从配置中读取
        scanner.setBasePackage(properties.getBasePackage());
        return scanner;
    }
}
```

**条件注解组合分析**：

```
MybatisAutoConfiguration 生效的条件：
  ① @ConditionalOnClass
     → classpath 中有 SqlSessionFactory.class
     → 即引入了 mybatis 依赖

  ② @ConditionalOnSingleCandidate(DataSource.class)
     → Spring 容器中有一个 DataSource Bean
     → 即配置了数据源（如 spring.datasource.url）

  ③ @ConditionalOnMissingBean(SqlSessionFactory.class)
     → 用户没有自定义 SqlSessionFactory
     → 如果用户自己定义了，就用用户的

三个条件同时满足 → MybatisAutoConfiguration 生效 → 自动创建 SqlSessionFactory
```

---

### 5. 手写自动配置示例

通过手写一个最简单的 Starter，理解自动配置的完整流程。

#### 5.1 场景：创建一个打招呼 Starter

功能：引入依赖后，可以直接注入 `HelloService`，通过配置自定义问候语。

#### 5.2 项目结构

```
hello-spring-boot-starter/
├── pom.xml
└── src/main/java/com/example/hello/
    ├── HelloProperties.java          # 属性配置类
    ├── HelloService.java             # 核心服务类
    ├── HelloAutoConfiguration.java   # 自动配置类
    └── src/main/resources/
        └── META-INF/
            └── spring/
                └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

#### 5.3 步骤 1：创建属性配置类

```java
package com.example.hello;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Hello 属性配置类
 * 将 application.yml 中 hello.* 的配置绑定到这个对象
 *
 * @ConfigurationProperties(prefix = "hello") 表示：
 *   hello.prefix → 绑定到 prefix 字段
 *   hello.suffix → 绑定到 suffix 字段
 */
@ConfigurationProperties(prefix = "hello")
public class HelloProperties {

    // 问候前缀，默认 "Hello"
    private String prefix = "Hello";

    // 问候后缀，默认 "!"
    private String suffix = "!";

    // ========== Getter / Setter ==========
    // 必须提供 Getter 和 Setter，否则属性绑定不生效

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}
```

#### 5.4 步骤 2：创建服务类

```java
package com.example.hello;

/**
 * Hello 服务类
 * 提供打招呼功能
 */
public class HelloService {

    // 持有配置属性
    private final HelloProperties properties;

    /**
     * 构造器注入属性
     */
    public HelloService(HelloProperties properties) {
        this.properties = properties;
    }

    /**
     * 打招呼方法
     * 格式：prefix + name + suffix
     *
     * @param name 名字
     * @return 问候语
     */
    public String sayHello(String name) {
        return properties.getPrefix() + " " + name + properties.getSuffix();
    }
}
```

#### 5.5 步骤 3：创建自动配置类

```java
package com.example.hello;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hello 自动配置类
 * 当条件满足时，自动创建 HelloService Bean
 */
@Configuration
// 启用 HelloProperties 的属性绑定
@EnableConfigurationProperties(HelloProperties.class)
// 条件1：classpath 中有 HelloService 类（确保依赖正确引入）
@ConditionalOnClass(HelloService.class)
// 条件2：容器中没有 HelloService Bean 时才创建（允许用户覆盖）
@ConditionalOnMissingBean(HelloService.class)
public class HelloAutoConfiguration {

    // 持有配置属性（通过构造器注入）
    private final HelloProperties properties;

    public HelloAutoConfiguration(HelloProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建 HelloService Bean
     */
    @Bean
    public HelloService helloService() {
        // 将配置属性传入服务类
        return new HelloService(properties);
    }
}
```

#### 5.6 步骤 4：注册自动配置类

**SpringBoot 2.x 方式**（`META-INF/spring.factories`）：

```properties
# src/main/resources/META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.example.hello.HelloAutoConfiguration
```

**SpringBoot 3.x 方式**（`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`）：

```
# src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.example.hello.HelloAutoConfiguration
```

#### 5.7 步骤 5：打包并在其他项目使用

```xml
<!-- hello-spring-boot-starter 的 pom.xml -->
<project>
    <groupId>com.example</groupId>
    <artifactId>hello-spring-boot-starter</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <!-- 自动配置需要 Spring Boot 的自动配置支持 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <!-- 配置属性提示支持（可选，用于 IDE 自动提示） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

在其他项目中引入：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>hello-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

```yaml
# application.yml 中配置
hello:
  prefix: "Hi"
  suffix: "~"
```

```java
@Autowired
private HelloService helloService;

// helloService.sayHello("SpringBoot") → "Hi SpringBoot~"
```

---

### 6. 自动配置报告

SpringBoot 提供了自动配置报告功能，可以查看哪些自动配置类生效了、哪些没有。

#### 6.1 开启调试模式

```yaml
# application.yml
debug: true
```

#### 6.2 启动日志分析

开启后，启动控制台会输出三部分信息：

```
=========================
AUTO-CONFIGURATION REPORT
=========================

Positive matches:（生效的自动配置）
-----------------
   DataSourceAutoConfiguration matched:
      - @ConditionalOnClass found required classes 'DataSource', 'PoolDataSource'
      - @ConditionalOnProperty 'spring.datasource.url' found

   MybatisAutoConfiguration matched:
      - @ConditionalOnClass found required classes 'SqlSessionFactory'
      - @ConditionalOnSingleCandidate DataSource found

Negative matches:（未生效的自动配置及原因）
-----------------
   RabbitAutoConfiguration did not match:
      - @ConditionalOnClass did not find required class 'com.rabbitmq.client.Channel'
      （说明：没有引入 RabbitMQ 依赖）

   RedisAutoConfiguration did not match:
      - @ConditionalOnClass did not find required class 'org.springframework.data.redis.core.RedisOperations'
      （说明：没有引入 Redis 依赖）

Exclusions:（被排除的自动配置）
-----------
    None
```

#### 6.3 通过 Actuator 查看

引入 `spring-boot-starter-actuator` 依赖后，可以访问：

```
GET /actuator/conditions
```

返回 JSON 格式的自动配置报告。

---

### 7. 排除自动配置

如果某个自动配置类不需要，可以排除它。

#### 7.1 启动类排除

```java
// 排除数据源自动配置（比如使用非 JDBC 数据源时）
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
public class TliasApplication {
    public static void main(String[] args) {
        SpringApplication.run(TliasApplication.class, args);
    }
}
```

#### 7.2 配置文件排除

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
```

---

## 动手练习

### 练习 1：查看自动配置报告

**目标**：直观感受 SpringBoot 自动配置了哪些内容。

**步骤**：
1. 在 Version A 或 Version B 项目的 `application.yml` 中添加 `debug: true`
2. 启动项目，观察控制台输出的 `AUTO-CONFIGURATION REPORT`
3. 找到 `Positive matches`，列出至少 5 个生效的自动配置类
4. 找到 `Negative matches`，分析为什么没有生效（如缺少什么依赖）
5. 实验完成后删除 `debug: true`（生产环境不要开启）

### 练习 2：手写 Starter

**目标**：完整体验自定义自动配置的开发流程。

**步骤**：
1. 创建一个新 Maven 模块 `my-starter`
2. 按照上面的 5 个步骤，创建一个简单的 Starter（可以是 HelloService 或其他功能）
3. 执行 `mvn install` 将 Starter 安装到本地 Maven 仓库
4. 在实训项目中引入这个 Starter 依赖
5. 在 `application.yml` 中配置属性
6. 编写测试类，验证 Starter 功能正常

### 练习 3：条件注解实验

**目标**：理解 `@ConditionalOnProperty` 的作用。

**步骤**：
1. 创建一个简单的配置类：

```java
@Configuration
@ConditionalOnProperty(prefix = "myfeature", name = "enabled", havingValue = "true")
public class MyFeatureConfiguration {
    @Bean
    public String myFeature() {
        return "MyFeature is enabled!";
    }
}
```

2. 在 `application.yml` 中设置 `myfeature.enabled: true`，启动项目，验证 Bean 存在
3. 改为 `myfeature.enabled: false`，启动项目，验证 Bean 不存在
4. 删除 `myfeature.enabled` 配置，启动项目，验证 Bean 不存在（默认不匹配）

---

## 常见错误排查

### 依赖配置问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 编译期 | `spring.factories` 找不到 | 文件路径或名称写错 | 确认路径是 `META-INF/spring.factories`（2.x）或 `META-INF/spring/...AutoConfiguration.imports`（3.x） |
| 编译期 | 自动配置类没有被加载 | 注册文件中类名写错 | 检查全限定名是否正确，包括包名和类名 |
| 启动期 | 自动配置不生效 | Starter 没有被引入 | 检查 pom.xml 中是否引入了自定义 Starter |

### 参数请求问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 运行期 | 属性注入失败 | `@EnableConfigurationProperties` 未加 | 在自动配置类上加 `@EnableConfigurationProperties(XxxProperties.class)` |
| 运行期 | 配置项没有提示 | 缺少 `spring-boot-configuration-processor` | 添加该依赖并重新编译 |
| 运行期 | 配置值没有生效 | 前缀写错或字段没有 Setter | 检查 `@ConfigurationProperties(prefix = "xxx")` 和字段的 Setter |

### 代码逻辑问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 运行期 | Bean 重复定义 | 自动配置和自定义配置冲突 | 自动配置类上加 `@ConditionalOnMissingBean` |
| 运行期 | 自动配置类没有执行 | 条件注解判断为 false | 检查条件注解要求是否满足（如 classpath 中是否有指定类） |
| 运行期 | 自定义 Starter 的 Bean 为 null | 包扫描路径不对 | 确认 Starter 的包名在启动类的扫描范围内，或检查自动配置是否生效 |

### 性能安全问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 性能 | 启动速度变慢 | 自动配置类太多，条件判断耗时 | 排除不需要的自动配置，减少 Starter 依赖 |
| 性能 | `debug: true` 导致日志过多 | 生产环境开启了调试模式 | 生产环境务必关闭 `debug: true` |
| 安全 | 自动配置暴露了不该暴露的端点 | Actuator 配置不当 | 限制 Actuator 端点的访问权限 |
| 安全 | Starter 中硬编码了密钥 | 开发时图方便写了死值 | 使用 `@Value` 或 `@ConfigurationProperties` 从配置读取 |

---

## 本节小结

```
+-----------------------------------------------------------+
|              SpringBoot 自动配置原理                        |
+-----------------------------------------------------------+
|                                                           |
|   +----------------+     +----------------+              |
|   |  为什么自动配置 |     |  约定大于配置   |              |
|   |  减少样板代码   | --> |  开箱即用      |              |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   | @SpringBootApplication |                           |
|   |  = @Configuration      |                           |
|   |  + @EnableAutoConfiguration |                       |
|   |  + @ComponentScan        |                          |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   | 自动配置注册文件 | --> |  2.x: spring.factories     |
|   |                  |     |  3.x: AutoConfiguration.imports |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   | @Conditional 条件 | --> |  OnClass       |           |
|   | 判断是否生效      |     |  OnMissingBean |           |
|   |                   |     |  OnProperty    |           |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   | 手写 Starter    | --> |  Properties    |           |
|   | 自定义自动配置   |     |  Service       |           |
|   |                   |     |  AutoConfiguration |       |
|   |                   |     |  注册文件       |           |
|   +----------------+     +----------------+              |
|                                                           |
+-----------------------------------------------------------+
```

---

## 参考文档

- [SpringBoot 自动配置官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/auto-configuration-classes.html)
- [Creating Your Own Auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)
- [SpringBoot 3.x 自动配置迁移指南](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide#auto-configuration-files)
- [Spring Factories 加载机制源码](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration.locating-auto-configuration-candidates)
