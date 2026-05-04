# Maven 依赖管理

## 学习目标

学完本节后，你将能够：
- 像理解"外卖平台"一样理解 Maven 的坐标、仓库和生命周期
- 独立编写和读懂 `pom.xml` 文件中的每一项配置
- 解决项目中"jar 包冲突"这个最头疼的问题
- 理解父子模块项目是如何协作的

---

## 核心知识点

### 1. Maven 是什么——从"手动找 jar 包"到"自动点餐"

#### 1.1 没有 Maven 的时代

想象你正在做一个 Java 项目，需要用到很多第三方功能：
- 做 Web 接口 → 需要 Spring 框架的 jar 包
- 连接数据库 → 需要 MySQL 驱动的 jar 包
- 生成 getter/setter → 需要 Lombok 的 jar 包

**没有 Maven 时，你必须手动做这些事情：**
1. 打开浏览器，搜索 "spring-web jar 下载"
2. 找到官网，下载 `spring-web-5.3.x.jar`
3. 发现 Spring 还依赖了 `spring-core`、`spring-beans`...
4. 继续下载这些依赖的依赖...
5. 把所有 jar 包复制到项目的 `lib/` 文件夹
6. 在 IDEA 中一个一个手动添加引用

更糟糕的是：
- 同事电脑上 jar 包版本和你不一样 → 代码在他那里报错
- 项目要部署到服务器 → 又要手动复制一堆 jar 包
- 某个 jar 包升级了 → 手动替换，生怕漏了哪个依赖

**这就是 "依赖地狱"（Dependency Hell）。**

#### 1.2 Maven 的出现

Maven 就像**外卖平台 + 中央厨房**：

| 现实中的外卖平台 | Maven 中的对应概念 |
|---------------|------------------|
| 你在 App 上点菜（宫保鸡丁） | 你在 `pom.xml` 中写 `<dependency>`（我要 Spring Web） |
| 平台自动找到食材、调料 | Maven 自动找到这个 jar 包，以及它需要的所有 jar 包 |
| 平台从中央仓库配送到你 | Maven 从中央仓库下载到本地仓库 |
| 你收到完整的一份菜 | 你的项目自动拥有了所有需要的 jar 包 |

**Maven 的核心功能只有三个：**
1. **依赖管理**：自动下载、管理 jar 包及其传递依赖
2. **构建自动化**：一键完成编译 → 测试 → 打包 → 部署
3. **项目标准化**：所有 Maven 项目的目录结构都一样，换项目不用重新适应

---

### 2. 项目坐标（GAV）——每个 jar 包的"身份证号码"

在世界上，每个人有唯一的身份证号。在 Maven 世界中，每个 jar 包也有唯一的三元组坐标：

```xml
<!-- 就像身份证号有三段信息：省市区+出生日期+顺序码 -->
<!-- GAV 也有三段：组织 + 项目名 + 版本号 -->

<groupId>com.tlias</groupId>           <!-- Group：组织/公司域名倒写 -->
                                      <!-- 就像 "com.microsoft" 代表微软公司 -->
                                      <!-- 我们写 "com.tlias" 代表 Tlias 公司/项目 -->

<artifactId>tlias-backend</artifactId> <!-- Artifact：项目/模块名称 -->
                                      <!-- 就像 "身份证" 这个证件的名称 -->
                                      <!-- 你的项目叫 tlias-backend -->

<version>1.0.0</version>               <!-- Version：版本号 -->
                                      <!-- 软件会不断升级，版本号区分不同迭代 -->
                                      <!-- 1.0.0 = 第1个主版本，第0个次版本，第0个补丁 -->
```

**为什么域名要倒写？**
- 正常域名：`tlias.com`
- 倒写：`com.tlias`
- 原因：这样按字母排序时，同一个公司的项目会聚集在一起（所有 `com.tlias.*` 排在一起），方便管理。

**版本号的三段含义（语义化版本）：**
- `1.0.0`：第一位（主版本）—— 不兼容的大改动才升级
- `1.1.0`：第二位（次版本）—— 新增功能，但兼容旧版
- `1.0.1`：第三位（补丁版本）—— 修复 bug，不新增功能

---

### 3. pom.xml 核心配置——逐行拆解

`pom.xml`（Project Object Model）是 Maven 项目的核心配置文件，就像外卖订单——你告诉 Maven 你要什么，它给你配齐。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- 上面这行是 XML 文件的声明，就像每份文件开头的"密级"标签 -->
<!-- version="1.0" 表示 XML 规范版本，encoding="UTF-8" 表示文件编码 -->

<project xmlns="http://maven.apache.org/POM/4.0.0">
    <!-- <project> 是整个配置文件的根标签，所有配置都在里面 -->
    <!-- xmlns 是 XML 命名空间，告诉解析器按什么规则解读这个文件 -->

    <!-- ============================================ -->
    <!-- 第 1 部分：继承父项目 -->
    <!-- ============================================ -->
    <!-- 为什么要继承？
         就像你装修房子，如果有一个"精装套餐"包含了所有基础配置，
         你只需要在套餐基础上微调，而不是从零开始设计。
         Spring Boot 的父项目就是这个"精装套餐"。
    -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <!-- Spring Boot 团队开发的，org 代表非盈利开源组织 -->

        <artifactId>spring-boot-starter-parent</artifactId>
        <!-- starter-parent = 启动器父项目，管理了所有 Spring Boot 依赖的默认版本 -->

        <version>2.7.18</version>
        <!-- 版本 A 使用 2.7.x（稳定、成熟）
             版本 B 使用 3.2.x（新特性、需要 JDK17）
        -->
    </parent>

    <!-- ============================================ -->
    <!-- 第 2 部分：当前项目的基本信息 -->
    <!-- ============================================ -->
    <groupId>com.tlias</groupId>           <!-- 我们项目的组织名 -->
    <artifactId>tlias-backend</artifactId> <!-- 我们项目的模块名 -->
    <version>1.0.0</version>               <!-- 我们项目的版本号 -->
    <packaging>jar</packaging>             <!-- 打包方式：jar（Java 可运行包）-->
                                          <!-- 也可以是 war（Web 应用包，放入 Tomcat 运行）-->

    <!-- ============================================ -->
    <!-- 第 3 部分：属性配置（定义变量，后面引用）-->
    <!-- ============================================ -->
    <!-- 为什么要用属性？
         就像你在文档里写"本文中的'公司名'均指XXX科技有限公司"，
         后面只需要引用这个变量，修改时改一处即可。
    -->
    <properties>
        <!-- 告诉 Maven：这个项目用 Java 1.8 语法编译 -->
        <java.version>1.8</java.version>

        <!-- 自定义属性：MyBatis 的版本号 -->
        <!-- 在后面 <dependency> 中可以用 ${mybatis.version} 引用 -->
        <mybatis.version>2.3.1</mybatis.version>

        <!-- 强制使用 UTF-8 编码，避免中文乱码 -->
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <!-- ============================================ -->
    <!-- 第 4 部分：依赖管理（核心中的核心）-->
    <!-- ============================================ -->
    <!-- <dependencies> 就像你的购物清单，里面列了所有需要的 jar 包 -->
    <dependencies>

        <!-- 依赖 1：Spring Boot Web 启动器 -->
        <!-- 引入这一个，Maven 会自动帮你引入：
             - Spring 核心框架
             - Spring MVC（处理 Web 请求）
             - 嵌入式 Tomcat（内置 Web 服务器）
             - Jackson（JSON 处理）
             ... 等等几十个个 jar 包
             这就是 "starter"（启动器）的含义：引入一个，带进来一群。
        -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <!-- 注意：这里没有写 <version>！
                 因为父项目 spring-boot-starter-parent 已经规定好了 compatible 版本，
                 不需要我们手动指定，避免版本不兼容。
            -->
        </dependency>

        <!-- 依赖 2：MyBatis Spring Boot 启动器 -->
        <!-- MyBatis 是操作数据库的框架，
             mybatis-spring-boot-starter 让它和 Spring Boot 无缝集成 -->
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <!-- 这里写了版本号，因为我们前面在 <properties> 中定义了 -->
            <version>${mybatis.version}</version>
        </dependency>

        <!-- 依赖 3：MySQL 数据库驱动 -->
        <!-- 你的 Java 代码要连接 MySQL 数据库，就需要这个"翻译官" -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <!-- 运行时依赖：编译时不需要，运行时才需要 -->
            <scope>runtime</scope>
        </dependency>

    </dependencies>

    <!-- ============================================ -->
    <!-- 第 5 部分：构建配置（告诉 Maven 怎么打包）-->
    <!-- ============================================ -->
    <build>
        <plugins>
            <!-- Spring Boot Maven 插件 -->
            <!-- 作用：
                 1. 把项目打包成可执行的 fat jar（包含所有依赖的 jar 包）
                 2. 支持 mvn spring-boot:run 一键启动
                 3. 生成 build-info 信息
            -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

---

### 4. 依赖范围（Scope）——不同场景用不同的"套餐"

想象你去健身房，有不同的会员卡：

| 会员卡类型 | 能使用什么 | Maven Scope | 能做什么 | 典型场景 |
|-----------|-----------|-------------|---------|---------|
| 全日通卡 | 所有器械 | compile | 编译用 + 运行用 | 绝大多数依赖（默认） |
| 只洗澡卡 | 仅淋浴区 | provided | 编译用，运行不用 | servlet-api（服务器自带） |
| 体验卡 | 进门时出示 | runtime | 运行用，编译不用 | MySQL 驱动 |
| 私教课 | 单独购买 | test | 仅私教课可用 | JUnit 测试框架 |

```xml
<!-- compile（默认值，不写就是 compile） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- 编译时需要它（写代码时import），运行时也需要它 -->
</dependency>

<!-- provided：编译时需要，但运行时容器会提供 -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <scope>provided</scope>
    <!-- 写代码时需要 import javax.servlet.*，
         但部署到 Tomcat 后，Tomcat 自己带了 servlet-api，
         所以打包时不需要把这个 jar 打进去 -->
</dependency>

<!-- runtime：编译时不需要，运行时需要 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
    <!-- 写代码时你只写 JDBC 标准接口，不直接引用 MySQL 类，
         所以编译时不需要；但运行时必须加载 MySQL 驱动 -->
</dependency>

<!-- test：仅在测试代码中使用 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
    <!-- 只在 src/test/java 目录下的测试代码中使用，
         主代码(src/main/java)无法引用，打包也不会包含 -->
</dependency>
```

**记忆口诀：**
- `compile` = 全程跟随（最常用）
- `provided` = 服务器有，我不带（servlet、lombok）
- `runtime` = 暗箱操作，运行时才加载（数据库驱动）
- `test` = 仅测试用（JUnit）

---

### 5. 依赖冲突解决——"同名不同款"怎么办

#### 5.1 什么是依赖冲突

你的项目依赖了 A 和 B：
- A 依赖了 `slf4j-1.7.30`
- B 依赖了 `slf4j-2.0.0`

现在你的项目同时有 slf4j 的两个版本！Java 运行时只会加载一个，加载哪个？

这就是**依赖冲突**，就像你同时买了两张同一航班但不同座位的机票——必须选一个。

#### 5.2 Maven 的默认解决策略

**策略一：就近原则（路径最短优先）**
```
你的项目
  ├── A.jar → slf4j-1.7.30（距离 = 2）
  └── B.jar → slf4j-2.0.0（距离 = 2）
```
如果距离相同，看策略二。

**策略二：先声明优先**
在 `pom.xml` 中，先写的依赖胜出。

```xml
<dependencies>
    <!-- A 先声明，所以 A 带的 slf4j-1.7.30 胜出 -->
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>A</artifactId>
    </dependency>
    <!-- B 后声明，所以 B 带的 slf4j-2.0.0 被忽略 -->
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>B</artifactId>
    </dependency>
</dependencies>
```

#### 5.3 手动解决冲突

**方式一：排除（Exclusion）**
就像点菜时说"宫保鸡丁不要放花生"：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>some-lib</artifactId>
    <!-- 我不要 some-lib 自带的 slf4j-simple -->
    <exclusions>
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

**方式二：锁定版本（Dependency Management）**
就像公司统一规定"所有项目必须用 XX 品牌的电脑"：

```xml
<!-- 在父 POM 中统一声明版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <!-- 所有子模块的 guava 都用这个版本，不用自己写 version -->
            <version>31.1-jre</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 5.4 查看依赖树——冲突诊断工具

```bash
# 查看项目的完整依赖树，就像查看"这道菜用了哪些食材"
mvn dependency:tree

# 输出示例：
# com.tlias:tlias-backend:jar:1.0.0
# +- org.springframework.boot:spring-boot-starter-web:jar:2.7.18
# |  +- org.springframework.boot:spring-boot-starter:jar:2.7.18
# |  |  +- org.springframework.boot:spring-boot:jar:2.7.18
# |  |  |  \- org.springframework:spring-core:jar:5.3.31
# |  \- org.springframework:spring-web:jar:5.3.31
# +- mysql:mysql-connector-java:jar:8.0.33:runtime
# +- com.alibaba:druid:jar:1.2.20
```

**看到冲突时的特征：**
```
+- org.slf4j:slf4j-api:jar:1.7.36:compile
\- org.slf4j:slf4j-api:jar:2.0.7:compile (omitted for conflict)
                                    ^^^^^^^^^^^^^^^^^^^^^^^^
                                    这一行就是被忽略的版本
```

---

### 6. 常用 Maven 命令——你的"厨房操作台"

| 命令 | 作用 | 生活类比 |
|------|------|---------|
| `mvn compile` | 编译源代码 | 切菜、备料 |
| `mvn test` | 运行所有测试 | 试吃、品控 |
| `mvn package` | 编译 + 测试 + 打包 | 装盒 |
| `mvn clean` | 清理之前的编译结果 | 清理厨房台面 |
| `mvn install` | 打包并安装到本地仓库 | 做好菜放入冰箱，其他菜可以用 |
| `mvn spring-boot:run` | 直接运行 SpringBoot 项目 | 直接开吃 |

```bash
# 最常用的组合命令：
# 先清理 → 再打包 → 跳过测试（开发时省时间）
mvn clean package -DskipTests

# 参数解释：
# clean      = 删除 target/ 目录（上次编译的结果）
# package    = 编译 + 打包成 jar
# -DskipTests = 跳过测试（-D 表示设置系统属性）

# 开发调试时常用：
mvn spring-boot:run
# 启动 SpringBoot 项目，自动热部署（修改代码后自动重启）
```

---

### 7. 多模块项目——父子结构

想象你开了一家餐饮集团：
- **总部（父 POM）**：制定统一标准（JDK 版本、依赖版本），不直接做菜
- **中餐部（子模块）**：按总部标准做中餐
- **西餐部（子模块）**：按总部标准做西餐
- **甜品部（子模块）**：按总部标准做甜品

```
tlias-pro-backend/                    ← 总部（父项目）
├── pom.xml                          ← 父 POM：只定义标准，不写代码
│   ├── <modules>                    ← 声明有哪些子模块
│   ├── <dependencyManagement>       ← 统一锁定依赖版本
│   └── <pluginManagement>           ← 统一插件配置
│
├── tlias-pro-server/                ← 中餐部（启动入口）
│   └── pom.xml                      ← 子 POM：声明 <parent>，写自己的依赖
│
├── tlias-pro-framework/             ← 中央厨房（公共框架）
│   └── pom.xml                      ← 被其他模块引用
│
└── tlias-pro-module-system/         ← 西餐部（业务模块）
    └── pom.xml
```

**父 POM 的核心作用：**
```xml
<!-- 父 POM：tlias-pro-backend/pom.xml -->
<project>
    <groupId>com.tliaspro</groupId>
    <artifactId>tlias-pro-backend</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>  <!-- 注意：父项目打包方式是 pom，不是 jar！ -->

    <!-- 声明有哪些子模块 -->
    <modules>
        <module>tlias-pro-server</module>
        <module>tlias-pro-framework</module>
        <module>tlias-pro-module-system</module>
    </modules>

    <!-- 统一管理依赖版本 -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-boot-starter</artifactId>
                <version>3.5.5</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

**子 POM 的核心结构：**
```xml
<!-- 子 POM：tlias-pro-server/pom.xml -->
<project>
    <!-- 声明"我爸是谁" -->
    <parent>
        <groupId>com.tliaspro</groupId>
        <artifactId>tlias-pro-backend</artifactId>
        <version>1.0.0</version>
    </parent>

    <!-- 子模块的 artifactId，groupId 和 version 继承自父 POM -->
    <artifactId>tlias-pro-server</artifactId>

    <dependencies>
        <!-- 引用父 POM 管理版本的依赖，不用写 version -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <!-- 版本号在父 POM 的 dependencyManagement 中定义了 -->
        </dependency>
    </dependencies>
</project>
```

---

## 动手练习

### 练习 1：从零创建一个 Maven 项目

**目标**：不借助 IDEA 的模板，纯手写 pom.xml，理解每一行的含义。

**步骤**：

1. 创建项目目录：
```bash
mkdir my-first-maven
cd my-first-maven
```

2. 手动创建标准目录结构：
```
my-first-maven/
├── pom.xml              ← 项目配置文件
└── src/
    ├── main/
    │   ├── java/        ← 主代码目录（必须叫这个名字）
    │   └── resources/   ← 配置文件目录
    └── test/
        ├── java/        ← 测试代码目录
        └── resources/   ← 测试配置文件
```

3. 编写 `pom.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <!-- 继承 Spring Boot 父项目 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
    </parent>

    <!-- 当前项目坐标 -->
    <groupId>com.example</groupId>
    <artifactId>my-first-maven</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <!-- 添加 Web 依赖，让项目能处理 HTTP 请求 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>

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

4. 编写第一个 Java 类 `src/main/java/com/example/HelloController.java`：
```java
package com.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Maven!";
    }
}
```

5. 编写启动类 `src/main/java/com/example/MyApplication.java`：
```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

6. 运行项目：
```bash
mvn spring-boot:run
```

7. 浏览器访问 `http://localhost:8080/hello`，看到 `Hello, Maven!` 即成功。

---

### 练习 2：解决依赖冲突

**场景**：你的项目同时依赖了两个库，它们分别依赖了不同版本的 `slf4j`。

**步骤**：

1. 在 `pom.xml` 中添加两个故意冲突的依赖：
```xml
<dependencies>
    <!-- 这个依赖引入了 slf4j-api 1.7.x -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 这个依赖引入了 slf4j-api 2.0.x -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
</dependencies>
```

2. 查看依赖树：
```bash
mvn dependency:tree | findstr slf4j
```

3. 观察输出中是否有 `(omitted for conflict)` 标记。

4. 使用 `<exclusion>` 排除冲突：
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.9</version>
    <!-- 排除这个依赖自带的 log4j（如果有的话） -->
</dependency>
```

---

## 常见错误排查

### 阶段 1：环境配置问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `'mvn' 不是内部或外部命令` | 没有安装 Maven，或没有配置环境变量 | 1. 下载 Maven 解压到无中文路径<br>2. 配置 `MAVEN_HOME` 和 `Path` 环境变量<br>3. 重启命令行输入 `mvn -v` 验证 |
| 依赖下载速度极慢或超时 | 默认连接国外 Maven 中央仓库 | 在 `~/.m2/settings.xml` 中配置阿里云镜像（见下方配置） |

**阿里云镜像配置：**
```xml
<!-- 文件路径：C:\Users\你的用户名\.m2\settings.xml -->
<settings>
    <mirrors>
        <mirror>
            <id>aliyun</id>
            <name>阿里云公共仓库</name>
            <url>https://maven.aliyun.com/repository/public</url>
            <mirrorOf>central</mirrorOf>
        </mirror>
    </mirrors>
</settings>
```

### 阶段 2：编译期错误

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `找不到符号：类 Xxx` | 依赖未引入，或 jar 包下载不完整 | 1. 检查 `pom.xml` 中是否有该依赖<br>2. 删除 `~/.m2/repository` 对应目录，重新 `mvn clean install` |
| `编码 GBK 的不可映射字符` | 代码中有中文，但 Maven 用 GBK 编译 | 在 `pom.xml` 的 `<properties>` 中添加 `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>` |
| `java: 无效的目标发行版: 17` | IDEA 配置的 JDK 版本和 pom.xml 不一致 | 1. 检查 `pom.xml` 中的 `<java.version>`<br>2. 检查 IDEA → Project Structure → SDK 设置 |

### 阶段 3：运行时错误

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `ClassNotFoundException` | 运行时缺少某个 jar 包 | 检查该依赖的 scope 是否为 `test` 或 `provided`，改为 `compile` |
| `NoSuchMethodError` | 依赖冲突，运行时加载了错误版本 | 用 `mvn dependency:tree` 分析，排除冲突版本 |

### 阶段 4：IDEA 集成问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| IDEA 中 import 报错但编译通过 | IDEA 未正确识别 Maven 项目 | 右键 `pom.xml` → `Add as Maven Project` |
| 修改 pom.xml 后依赖没生效 | IDEA 没有自动重新导入 | 点击 IDEA 右侧 Maven 面板中的刷新按钮，或 `Ctrl+Shift+O` |

---

## 本节小结

```
┌─────────────────────────────────────────────────────────┐
│                    Maven 知识体系                          │
├─────────────────────────────────────────────────────────┤
│  是什么？  Java 项目的"外卖平台"——自动管理 jar 包和构建流程   │
├─────────────────────────────────────────────────────────┤
│  核心概念：                                               │
│    • GAV 坐标    = jar 包的"身份证号"（groupId + artifactId + version）│
│    • pom.xml     = "外卖订单"，告诉 Maven 你需要什么         │
│    • 仓库        = 中央仓库（全球共享）→ 本地仓库（你的冰箱）│
│    • 生命周期    = clean → compile → test → package → install│
├─────────────────────────────────────────────────────────┤
│  关键技能：                                               │
│    • 会写 pom.xml 的五大板块（parent、坐标、properties、dependencies、build）│
│    • 会用 scope 控制依赖范围（compile/provided/runtime/test）│
│    • 会用 mvn dependency:tree 诊断冲突                     │
│    • 会用 exclusion 排除冲突依赖                           │
├─────────────────────────────────────────────────────────┤
│  避坑指南：                                               │
│    • 配置阿里云镜像（加速下载）                            │
│    • 统一 UTF-8 编码（避免中文乱码）                       │
│    • 多模块项目父 POM 的 packaging 必须是 pom              │
└─────────────────────────────────────────────────────────┘
```

---

## 参考文档

- [Maven 官方入门指南](https://maven.apache.org/guides/getting-started/)
- [Maven 仓库搜索](https://mvnrepository.com/) — 查找任何 jar 包的坐标
- [阿里云 Maven 镜像配置](https://developer.aliyun.com/mvn/guide)
