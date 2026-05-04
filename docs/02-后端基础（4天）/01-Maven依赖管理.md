# Maven 依赖管理

## 学习目标

- 理解 Maven 的坐标、仓库和生命周期概念
- 掌握 pom.xml 的基本配置
- 能够管理项目依赖和解决依赖冲突
- 理解继承和聚合的项目结构

## 核心知识点

### 1. Maven 是什么

Maven 是 Java 项目的构建和依赖管理工具，核心功能：
- **依赖管理**：自动下载和管理 jar 包
- **构建自动化**：编译、测试、打包、部署一键完成
- **项目结构标准化**：统一的项目目录结构

### 2. 项目坐标（GAV）

每个 Maven 项目通过三个坐标唯一标识：

```xml
<groupId>com.tlias</groupId>      <!-- 组织/公司域名倒写 -->
<artifactId>tlias-backend</artifactId>  <!-- 项目名 -->
<version>1.0.0</version>            <!-- 版本号 -->
```

### 3. pom.xml 核心配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <!-- 继承 Spring Boot 父项目 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
    </parent>

    <groupId>com.tlias</groupId>
    <artifactId>tlias-backend</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <!-- 属性配置 -->
    <properties>
        <java.version>1.8</java.version>
        <mybatis.version>2.3.1</mybatis.version>
    </properties>

    <!-- 依赖管理 -->
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>${mybatis.version}</version>
        </dependency>
    </dependencies>

    <!-- 构建配置 -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 4. 依赖范围（Scope）

| Scope | 说明 | 典型场景 |
|-------|------|---------|
| compile | 默认，编译和运行都需要 | 绝大多数依赖 |
| provided | 编译需要，运行时由容器提供 | servlet-api、lombok |
| runtime | 运行需要，编译不需要 | JDBC驱动 |
| test | 仅测试需要 | JUnit、Mockito |
| system | 使用本地 jar | 不推荐 |

### 5. 依赖冲突解决

当同一个 jar 包存在多个版本时，Maven 采用"**就近原则**"和"**先声明优先**"：

```xml
<!-- 方式一：排除冲突依赖 -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>some-lib</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 方式二：锁定版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>31.1-jre</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 6. 常用 Maven 命令

```bash
# 编译
mvn compile

# 测试
mvn test

# 打包（跳过测试）
mvn package -DskipTests

# 安装到本地仓库
mvn install

# 清理
mvn clean

# 组合命令
mvn clean package -DskipTests

# 运行 SpringBoot 项目
mvn spring-boot:run
```

### 7. 多模块项目

```
tlias-pro-backend/
├── pom.xml                 <!-- 父 POM -->
├── tlias-pro-server/
│   └── pom.xml             <!-- 启动入口 -->
├── tlias-pro-framework/
│   └── pom.xml             <!-- 公共框架 -->
└── tlias-pro-module-system/
    └── pom.xml             <!-- 业务模块 -->
```

父 POM 中声明 `<modules>`，子 POM 中声明 `<parent>`，实现依赖版本统一管理。

## 动手练习

### 练习 1：创建 Maven 项目

1. 在 IDEA 中创建 Maven 项目
2. 添加 SpringBoot Web 依赖
3. 编写一个简单的 HelloController
4. 使用 `mvn spring-boot:run` 启动

### 练习 2：解决依赖冲突

1. 添加两个依赖，它们间接依赖不同版本的 slf4j
2. 使用 `mvn dependency:tree` 查看依赖树
3. 使用 `<exclusion>` 排除冲突版本

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| 依赖下载失败 | 网络问题或仓库配置错误 | 配置阿里云镜像或检查网络 |
| 找不到符号 | 依赖未引入或版本不对 | 检查 pom.xml 中的依赖配置 |
| 依赖冲突 | 多个版本共存 | 使用 `dependency:tree` 分析，排除冲突 |
| 编码问题 | 未指定 UTF-8 | 在 properties 中添加 `project.build.sourceEncoding=UTF-8` |

## 本节小结

Maven 是 Java 项目的基石工具。掌握 GAV 坐标、依赖管理和生命周期，你就能高效地管理项目构建和第三方库。

## 参考文档

- [Maven 官方文档](https://maven.apache.org/guides/)
- [Maven 仓库](https://mvnrepository.com/)
