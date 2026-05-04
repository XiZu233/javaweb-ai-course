# Sa-Token 权限控制

## 学习目标

- 理解 Sa-Token 的设计哲学，对比 Spring Security 和手写 JWT 方案的优势
- 掌握 Sa-Token 的核心 API：登录、注销、校验、获取当前用户
- 能够使用 Sa-Token 实现角色认证和权限认证（RBAC 模型）
- 掌握 Sa-Token 与 SpringBoot 3 的集成方式，包括路由拦截器配置
- 理解 Sa-Token 的会话治理机制（登录设备数控制、登录状态查询）

---

## 核心知识点

### 1. 为什么选择 Sa-Token

#### 1.1 权限框架的演进路线

Java 生态中，权限认证框架经历了这样的演进：

```
手写 Session（繁琐）
    |
    v
Spring Security（功能强大但配置复杂）
    |
    v
Shiro（相对简单，但更新缓慢）
    |
    v
Sa-Token（极简设计，一行代码搞定）
```

**真实场景类比**：
- **Spring Security** 像一台专业单反相机——功能极其丰富，但你要学会光圈、快门、ISO、对焦模式，学习曲线陡峭。
- **Sa-Token** 像一台智能手机——打开就能拍，但也能调出专业模式满足进阶需求。

#### 1.2 Sa-Token 的核心优势

| 特性 | Spring Security | 手写 JWT + 拦截器 | Sa-Token |
|------|----------------|------------------|----------|
| 学习成本 | 极高（需要理解过滤器链、认证管理器等概念） | 中等（需要理解 JWT、拦截器） | 极低（API 极简） |
| 配置复杂度 | 复杂（大量配置类） | 中等（需要手写工具类和拦截器） | 极简（依赖 + 配置即可） |
| 登录/注销 | 需要配置多个组件 | 需要手写 Token 生成和清理 | `StpUtil.login(id)` 一行搞定 |
| 权限校验 | 需要配置方法安全 | 需要手写注解和切面 | `@SaCheckPermission` 注解搞定 |
| 分布式会话 | 需额外配置 Redis | 天然无状态（但无法主动登出） | 内置 Redis 集成 |
| 登录设备控制 | 复杂实现 | 需自行实现 | 内置 `is-concurrent` 配置 |
| 踢人下线 | 复杂实现 | 难以实现 | `StpUtil.logout(id)` 一行搞定 |

**Version A 使用 JWT + 拦截器**：适合理解底层原理，但功能有限（无法主动作废 Token、无法统计在线人数）。
**Version B 使用 Sa-Token**：开箱即用，功能完善，适合生产环境。

---

### 2. Sa-Token 与 SpringBoot 3 集成

#### 2.1 引入依赖

```xml
<!-- pom.xml 中添加 Sa-Token 依赖 -->
<!-- 注意：Version B 使用 SpringBoot 3，所以要用 sa-token-spring-boot3-starter -->
<dependency>
    <!-- Sa-Token 官方组织ID -->
    <groupId>cn.dev33</groupId>
    <!-- SpringBoot 3 专用 Starter -->
    <artifactId>sa-token-spring-boot3-starter</artifactId>
    <!-- 版本号，建议使用最新稳定版 -->
    <version>1.37.0</version>
</dependency>
```

**版本对应关系**：

| SpringBoot 版本 | Sa-Token Starter | JDK 版本 |
|----------------|------------------|---------|
| SpringBoot 2.x | `sa-token-spring-boot-starter` | JDK 8+ |
| SpringBoot 3.x | `sa-token-spring-boot3-starter` | JDK 17+ |

如果用错了 Starter，会导致类找不到或自动配置不生效。

#### 2.2 配置文件

```yaml
# application.yml 中配置 Sa-Token
sa-token:
  # Token 名称，前端请求头中用这个名称传递 Token
  # 前端需要：headers['Authorization'] = 'Bearer xxx'
  token-name: Authorization

  # Token 有效期，单位：秒
  # 2592000 = 30 天
  timeout: 2592000

  # 临时有效期（指定时间内无操作就视为过期）
  # -1 代表不限制，只要总时间没超就一直有效
  activity-timeout: -1

  # 是否允许同一账号多地登录
  # true = 允许在多个设备同时登录
  # false = 新登录会踢掉旧登录
  is-concurrent: true

  # 在多人登录同一账号时，是否共用同一个 Token
  # true = 共用（所有设备同一个 Token）
  # false = 不共用（每个设备不同 Token）
  is-share: false

  # Token 风格
  # uuid = 32位随机字符串
  # simple-uuid = 简化版 UUID
  # random-32 = 32位随机字符串
  # jwt = JWT 格式（需要额外引入 sa-token-jwt 依赖）
  token-style: uuid

  # 是否尝试从 Cookie 中读取 Token
  # 前后端分离项目通常关闭，只从 Header 读取
  is-read-cookie: false

  # 是否尝试从请求参数中读取 Token
  is-read-body: false
```

**配置项速查表**：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `token-name` | satoken | Token 在请求头/参数中的名称 |
| `timeout` | 2592000 | Token 有效期（秒），30天 |
| `activity-timeout` | -1 | 临时有效期，-1=不限制 |
| `is-concurrent` | true | 是否允许同一账号多地登录 |
| `is-share` | true | 是否共享 Token |
| `token-style` | uuid | Token 生成风格 |
| `is-read-cookie` | true | 是否从 Cookie 读取 Token |
| `is-read-header` | true | 是否从 Header 读取 Token |

---

### 3. StpUtil API 详解

`StpUtil` 是 Sa-Token 最核心的工具类，几乎所有操作都通过它完成。

#### 3.1 登录与注销

```java
package com.tlias.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tlias.pojo.Result;
import com.tlias.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 处理登录、注销等认证相关请求
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 登录接口
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回 Token
     */
    @PostMapping("/login")
    public Result login(@RequestParam String username,
                        @RequestParam String password) {

        // ========================================
        // 第1步：验证用户名和密码
        // ========================================
        User user = userService.authenticate(username, password);
        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        // ========================================
        // 第2步：调用 Sa-Token 登录
        // ========================================
        // StpUtil.login(userId) 是 Sa-Token 最核心的方法
        // 它会自动完成以下事情：
        //   1. 生成一个 Token
        //   2. 将 Token 和 userId 的映射存入 Session（内存或 Redis）
        //   3. 将 Token 写入当前请求的响应中（Header 或 Cookie）
        StpUtil.login(user.getId());

        // ========================================
        // 第3步：获取 Token 返回给前端
        // ========================================
        // StpUtil.getTokenValue() 获取刚才生成的 Token
        String token = StpUtil.getTokenValue();

        return Result.success(token);
    }

    /**
     * 注销接口
     * 调用后当前 Token 失效，需要重新登录
     */
    @PostMapping("/logout")
    public Result logout() {
        // StpUtil.logout() 注销当前会话
        // 会从 Session 中删除 Token 映射，使当前 Token 失效
        StpUtil.logout();
        return Result.success();
    }

    /**
     * 查询当前登录状态
     */
    @GetMapping("/isLogin")
    public Result isLogin() {
        // StpUtil.isLogin() 判断当前请求是否已登录
        boolean login = StpUtil.isLogin();
        return Result.success(login);
    }
}
```

#### 3.2 获取当前登录用户信息

```java
/**
 * 获取当前登录用户信息的常用方法
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户详情
     */
    @GetMapping("/current")
    public Result getCurrentUser() {
        // ========================================
        // StpUtil 获取登录信息的常用方法
        // ========================================

        // 获取当前登录用户的 ID（loginId）
        // 返回值类型是 Object，通常需要强转
        Object loginId = StpUtil.getLoginId();

        // 获取当前登录用户 ID 并转为 Long
        Long userId = StpUtil.getLoginIdAsLong();

        // 获取当前登录用户 ID 并转为 String
        String userIdStr = StpUtil.getLoginIdAsString();

        // 获取当前会话的 Token 值
        String token = StpUtil.getTokenValue();

        // 获取当前会话的剩余有效期（秒）
        long tokenTimeout = StpUtil.getTokenTimeout();

        // 获取当前会话的登录设备类型
        String loginDevice = StpUtil.getLoginDevice();

        // 查询数据库获取用户完整信息
        User user = userService.getById(userId);

        return Result.success(user);
    }
}
```

#### 3.3 StpUtil 常用方法速查表

| 方法 | 说明 | 示例 |
|------|------|------|
| `StpUtil.login(Object id)` | 登录，创建会话 | `StpUtil.login(10001)` |
| `StpUtil.logout()` | 当前账号注销 | `StpUtil.logout()` |
| `StpUtil.logout(Object id)` | 指定账号注销（踢人下线） | `StpUtil.logout(10001)` |
| `StpUtil.isLogin()` | 判断是否已登录 | `boolean b = StpUtil.isLogin()` |
| `StpUtil.checkLogin()` | 检查登录（未登录抛异常） | `StpUtil.checkLogin()` |
| `StpUtil.getLoginId()` | 获取当前登录 ID | `Object id = StpUtil.getLoginId()` |
| `StpUtil.getLoginIdAsLong()` | 获取当前登录 ID（转 Long） | `Long id = StpUtil.getLoginIdAsLong()` |
| `StpUtil.getLoginIdAsString()` | 获取当前登录 ID（转 String） | `String id = StpUtil.getLoginIdAsString()` |
| `StpUtil.getTokenValue()` | 获取当前 Token 值 | `String token = StpUtil.getTokenValue()` |
| `StpUtil.getTokenTimeout()` | 获取 Token 剩余有效期 | `long timeout = StpUtil.getTokenTimeout()` |
| `StpUtil.kickout(Object id)` | 将指定账号踢下线（不注销） | `StpUtil.kickout(10001)` |
| `StpUtil.hasRole(String role)` | 判断是否拥有指定角色 | `boolean b = StpUtil.hasRole("admin")` |
| `StpUtil.hasPermission(String perm)` | 判断是否拥有指定权限 | `boolean b = StpUtil.hasPermission("user:add")` |

---

### 4. 角色认证与权限认证

Sa-Token 支持 RBAC（Role-Based Access Control，基于角色的访问控制）模型：

```
用户（User）
  |
  |-- 拥有角色（Role）：admin、editor、viewer
  |       |
  |       |-- admin 拥有权限：user:add、user:delete、user:update、user:list
  |       |-- editor 拥有权限：user:update、user:list
  |       |-- viewer 拥有权限：user:list
```

#### 4.1 登录时设置角色和权限

```java
/**
 * 登录时设置用户的角色和权限
 * 这些信息会存储在 Sa-Token 的 Session 中
 */
@PostMapping("/login")
public Result login(@RequestParam String username,
                    @RequestParam String password) {

    User user = userService.authenticate(username, password);
    if (user == null) {
        return Result.error("用户名或密码错误");
    }

    // 登录
    StpUtil.login(user.getId());

    // ========================================
    // 设置当前用户的角色列表
    // ========================================
    // 从数据库查询该用户的角色
    List<String> roleList = userService.getRoles(user.getId());
    // StpUtil.getSession() 获取当前会话的 Session
    // set() 方法将数据存入 Session
    StpUtil.getSession().set("role-list", roleList);

    // ========================================
    // 设置当前用户的权限列表
    // ========================================
    List<String> permissionList = userService.getPermissions(user.getId());
    StpUtil.getSession().set("permission-list", permissionList);

    String token = StpUtil.getTokenValue();
    return Result.success(token);
}
```

#### 4.2 实现权限数据接口

Sa-Token 需要知道当前用户有哪些角色和权限，需要实现 `StpInterface` 接口：

```java
package com.tlias.config;

import cn.dev33.satoken.stp.StpInterface;
import com.tlias.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义权限验证接口扩展
 * Sa-Token 通过此接口获取当前用户的角色列表和权限列表
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private UserService userService;

    /**
     * 返回指定账号 ID 所拥有的权限码列表
     *
     * @param loginId  账号 ID（就是 StpUtil.login(id) 时传入的 id）
     * @param loginType 账号类型（多账号体系时使用，一般默认）
     * @return 权限码列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 将 loginId 转为 Long 类型
        Long userId = Long.valueOf(loginId.toString());

        // 从数据库查询该用户的权限列表
        // 权限码格式建议：模块:操作，如 system:user:add
        return userService.getPermissions(userId);
    }

    /**
     * 返回指定账号 ID 所拥有的角色码列表
     *
     * @param loginId   账号 ID
     * @param loginType 账号类型
     * @return 角色码列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());

        // 从数据库查询该用户的角色列表
        return userService.getRoles(userId);
    }
}
```

#### 4.3 使用注解进行权限校验

```java
package com.tlias.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.tlias.pojo.Result;
import org.springframework.web.bind.annotation.*;

/**
 * 部门管理控制器
 * 演示如何使用 Sa-Token 注解进行权限控制
 */
@RestController
@RequestMapping("/depts")
public class DeptController {

    /**
     * 查询部门列表
     * @SaCheckPermission 注解：只有拥有指定权限的用户才能访问
     * "system:dept:list" 是权限码，对应数据库中的权限记录
     */
    @GetMapping
    @SaCheckPermission("system:dept:list")
    public Result list() {
        // 查询部门列表逻辑
        return Result.success(deptService.list());
    }

    /**
     * 新增部门
     */
    @PostMapping
    @SaCheckPermission("system:dept:create")
    public Result save(@RequestBody Dept dept) {
        deptService.save(dept);
        return Result.success();
    }

    /**
     * 修改部门
     */
    @PutMapping
    @SaCheckPermission("system:dept:update")
    public Result update(@RequestBody Dept dept) {
        deptService.update(dept);
        return Result.success();
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("system:dept:delete")
    public Result delete(@PathVariable Integer id) {
        deptService.delete(id);
        return Result.success();
    }

    /**
     * 管理员专属接口
     * @SaCheckRole 注解：只有拥有指定角色的用户才能访问
     */
    @GetMapping("/admin/stats")
    @SaCheckRole("admin")
    public Result adminStats() {
        // 只有 admin 角色能访问的统计接口
        return Result.success(statsService.getDeptStats());
    }

    /**
     * 同时拥有多个权限才能访问
     * mode = SaMode.AND 表示必须同时拥有所有列出的权限
     */
    @PostMapping("/batch")
    @SaCheckPermission(value = {"system:dept:create", "system:dept:update"}, mode = SaMode.AND)
    public Result batchOperation(@RequestBody List<Dept> depts) {
        // 需要同时拥有创建和修改权限
        return Result.success();
    }
}
```

#### 4.4 权限注解详解

| 注解 | 作用 | 示例 |
|------|------|------|
| `@SaCheckLogin` | 校验是否登录 | `@SaCheckLogin` |
| `@SaCheckRole("admin")` | 校验是否有指定角色 | `@SaCheckRole("admin")` |
| `@SaCheckRole(value = {"admin", "super"}, mode = SaMode.OR)` | 校验是否有任意一个角色 | 满足一个即可 |
| `@SaCheckPermission("user:add")` | 校验是否有指定权限 | `@SaCheckPermission("user:add")` |
| `@SaCheckPermission(value = {"a", "b"}, mode = SaMode.AND)` | 校验是否同时拥有多个权限 | 必须全部满足 |
| `@SaCheckPermission(value = {"a", "b"}, mode = SaMode.OR)` | 校验是否有任意一个权限 | 满足一个即可（默认） |

---

### 5. 路由拦截器配置

除了注解方式，Sa-Token 还可以通过拦截器进行统一的登录校验。

```java
package com.tlias.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器配置
 * 注册 Sa-Token 的路由拦截器，实现统一登录校验
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // ========================================
        // 方式1：简单注册（只校验登录）
        // ========================================
        // new SaInterceptor(handle -> StpUtil.checkLogin())
        // 创建一个 SaInterceptor，传入一个 Lambda 表达式
        // 当请求被拦截时，执行 StpUtil.checkLogin()
        // 如果未登录，会抛出 NotLoginException

        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                // 拦截所有路径
                .addPathPatterns("/**")
                // 排除登录相关接口
                .excludePathPatterns("/auth/login")
                .excludePathPatterns("/auth/register")
                // 排除 Swagger / Knife4j 文档
                .excludePathPatterns("/doc.html", "/webjars/**", "/swagger-resources/**");

        // ========================================
        // 方式2：复杂路由匹配（推荐）
        // ========================================
        // 使用 SaRouter 进行更灵活的路由匹配和校验
        // 下面的代码注释掉了，作为进阶参考
        /*
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 使用 SaRouter 进行路由匹配
            SaRouter
                    // 匹配所有路径
                    .match("/**")
                    // 排除登录接口
                    .notMatch("/auth/login")
                    .notMatch("/auth/register")
                    // 排除静态资源
                    .notMatch("/uploads/**")
                    // 匹配到的路径执行校验：检查登录
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
        */
    }
}
```

**`SaInterceptor` 的工作原理**：
1. 请求到达时，拦截器先执行
2. 调用你传入的 Lambda 表达式（`handle -> StpUtil.checkLogin()`）
3. `StpUtil.checkLogin()` 检查当前请求是否已登录
4. 如果未登录，抛出 `NotLoginException`
5. 你可以配置全局异常处理器捕获这个异常，返回统一的错误信息

---

### 6. 全局异常处理

Sa-Token 在校验失败时会抛出异常，需要配置全局异常处理器：

```java
package com.tlias.exception;

import cn.dev33.satoken.exception.*;
import com.tlias.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 捕获 Sa-Token 抛出的各种异常，返回统一的错误信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获未登录异常
     * 当用户未登录或 Token 过期时抛出
     */
    @ExceptionHandler(NotLoginException.class)
    public Result handleNotLoginException(NotLoginException e) {
        log.warn("用户未登录：{}", e.getMessage());
        return Result.error("NOT_LOGIN");
    }

    /**
     * 捕获无角色异常
     * 当用户没有所需角色时抛出
     */
    @ExceptionHandler(NotRoleException.class)
    public Result handleNotRoleException(NotRoleException e) {
        log.warn("用户无角色权限：{}", e.getMessage());
        return Result.error("无角色权限：" + e.getRole());
    }

    /**
     * 捕获无权限异常
     * 当用户没有所需权限时抛出
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result handleNotPermissionException(NotPermissionException e) {
        log.warn("用户无操作权限：{}", e.getMessage());
        return Result.error("无操作权限：" + e.getPermission());
    }

    /**
     * 捕获其他所有异常
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return Result.error("系统繁忙，请稍后重试");
    }
}
```

---

### 7. Sa-Token 与 JWT 的关系

Sa-Token 默认使用 Session 机制（服务端存储会话信息）。如果你需要 JWT 的无状态特性，可以集成 `sa-token-jwt`：

```xml
<!-- 引入 sa-token-jwt 扩展 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-jwt</artifactId>
    <version>1.37.0</version>
</dependency>
```

```yaml
# 配置 Token 风格为 JWT
sa-token:
  token-style: jwt
  # JWT 签名密钥（必须设置，用于签名和验证）
  jwt-secret-key: your-secret-key-here-must-be-at-least-32-characters
```

**使用 JWT 模式的注意事项**：
- JWT 模式下 Token 包含用户信息，服务端不存储会话
- 无法使用踢人下线功能（因为服务端不知道有哪些 Token）
- 无法统计在线人数
- 一般不建议在 Sa-Token 中使用 JWT 模式，除非有特殊需求

---

## 动手练习

### 练习 1：Sa-Token 登录测试

**目标**：完整体验 Sa-Token 的登录、访问、注销流程。

**步骤**：
1. 确保项目已引入 `sa-token-spring-boot3-starter` 依赖
2. 调用 `POST /auth/login?username=admin&password=123456` 登录
3. 观察响应头中是否有 `Authorization` 字段（Sa-Token 会自动写入）
4. 在后续请求中携带 Header：`Authorization: Bearer xxx`（或直接 `Authorization: xxx`，取决于配置）
5. 调用 `GET /auth/isLogin`，应该返回 `true`
6. 调用 `POST /auth/logout` 注销
7. 再次调用 `GET /auth/isLogin`，应该返回 `false`

### 练习 2：权限注解测试

**目标**：验证 `@SaCheckPermission` 的拦截效果。

**步骤**：
1. 给两个用户分配不同权限：
   - 用户 A：拥有 `system:dept:list` 权限
   - 用户 B：拥有 `system:dept:list` 和 `system:dept:create` 权限
2. 分别用两个用户登录，获取各自的 Token
3. 用户 A 调用 `GET /depts`（查询），应该成功
4. 用户 A 调用 `POST /depts`（新增），应该返回无权限错误
5. 用户 B 调用 `POST /depts`，应该成功
6. 观察后端日志中的异常信息

### 练习 3：踢人下线测试

**目标**：体验 Sa-Token 的会话治理能力。

**步骤**：
1. 配置 `is-concurrent: false`（不允许同一账号多地登录）
2. 用浏览器 A 登录账号 admin，获取 Token A
3. 用浏览器 B 登录同一个账号 admin，获取 Token B
4. 在浏览器 A 中调用任意接口，应该返回未登录
5. 恢复 `is-concurrent: true`
6. 用 admin 账号登录两次，获取两个 Token
7. 调用 `POST /auth/kickout?userId=xxx`（需要实现这个接口，调用 `StpUtil.kickout(userId)`）
8. 两个浏览器的 Token 都应该失效

---

## 常见错误排查

### 依赖配置问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 编译期 | `ClassNotFoundException: cn.dev33.satoken.stp.StpUtil` | 依赖未引入或版本错误 | 检查 pom.xml，确认引入了 `sa-token-spring-boot3-starter` |
| 编译期 | `ClassNotFoundException: jakarta.servlet.*` | SpringBoot 3 项目用了 `sa-token-spring-boot-starter` | 改为 `sa-token-spring-boot3-starter` |
| 启动期 | Sa-Token 配置不生效 | yml 缩进错误或配置项写错 | 检查 `sa-token:` 下的缩进，参考官方文档 |
| 启动期 | `StpInterfaceImpl` 未生效 | 没有实现 `StpInterface` 接口或没加 `@Component` | 确认实现了接口并加了 `@Component` |

### 参数请求问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 请求期 | 未能读取到有效 Token | Token 名称不匹配 | 检查前端请求头名称与 `sa-token.token-name` 配置是否一致 |
| 请求期 | 登录后访问接口仍提示未登录 | Token 未正确传递 | 确认前端在请求头中携带了 Token |
| 请求期 | 权限校验不生效 | 拦截器未注册 | 检查 `SaTokenConfigure` 是否实现了 `WebMvcConfigurer` 并注册了 `SaInterceptor` |
| 请求期 | `@SaCheckPermission` 注解无效 | 没有配置 AOP 或拦截器 | 确认已注册 `SaInterceptor`，且注解所在类被 Spring 管理 |

### 代码逻辑问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 运行期 | `StpUtil.login()` 后 `getTokenValue()` 为 null | 登录后没有正确获取 Token | `StpUtil.login()` 后立即调用 `StpUtil.getTokenValue()` |
| 运行期 | `getPermissionList` 没有被调用 | `StpInterfaceImpl` 没有被 Spring 扫描 | 确认类在 Spring 扫描路径下，且加了 `@Component` |
| 运行期 | 权限校验总是失败 | 权限码字符串不匹配 | 检查数据库中的权限码与注解中的字符串是否完全一致（区分大小写） |
| 运行期 | 角色校验不生效 | `getRoleList` 返回空列表 | 检查数据库查询逻辑，确认角色数据正确 |

### 性能安全问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 性能 | 每次请求都查数据库获取权限 | `StpInterface` 的方法被频繁调用 | 在 `StpInterfaceImpl` 中加缓存（如 Caffeine） |
| 性能 | Token 有效期太长 | `timeout` 配置过大 | 根据业务需求设置合理的过期时间 |
| 安全 | Token 泄露后被滥用 | 没有限制登录设备数 | 设置 `is-concurrent: false` 或配合 Redis 管理会话 |
| 安全 | 权限注解被绕过 | 某些路径没有被拦截器覆盖 | 检查 `addPathPatterns` 和 `excludePathPatterns` 配置 |

---

## 本节小结

```
+-----------------------------------------------------------+
|                   Sa-Token 权限控制                        |
+-----------------------------------------------------------+
|                                                           |
|   +----------------+     +----------------+              |
|   |   为什么选择    |     |  极简 API      |              |
|   |   Sa-Token     | --> |  功能完善      |              |
|   |                |     |  分布式支持    |              |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   |   核心依赖      | --> |  sa-token-spring-boot3-starter |
|   |   配置文件      |     |  application.yml            |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   |   StpUtil API   |     |  login()      |              |
|   |   一行代码搞定  | --> |  logout()     |              |
|   |                 |     |  isLogin()    |              |
|   |                 |     |  getLoginId() |              |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   |   权限控制      | --> |  @SaCheckRole        |       |
|   |   RBAC 模型     |     |  @SaCheckPermission  |       |
|   |                 |     |  StpInterfaceImpl    |       |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   |   拦截器配置    | --> |  SaInterceptor       |       |
|   |   统一登录校验  |     |  addInterceptors()   |       |
|   +----------------+     +----------------+              |
|                                                           |
+-----------------------------------------------------------+
```

---

## 参考文档

- [Sa-Token 官方文档](https://sa-token.cc/doc.html)
- [Sa-Token 官方文档 - 登录认证](https://sa-token.cc/doc.html#/use/login-auth)
- [Sa-Token 官方文档 - 权限认证](https://sa-token.cc/doc.html#/use/jur-auth)
- [Sa-Token 官方文档 - 路由拦截式鉴权](https://sa-token.cc/doc.html#/use/route-check)
- [Sa-Token GitHub 仓库](https://github.com/dromara/sa-token)
