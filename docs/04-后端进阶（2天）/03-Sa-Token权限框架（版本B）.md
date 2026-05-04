# Sa-Token 权限框架（版本 B 专属）

## 学习目标

- 理解 Sa-Token 的设计理念和使用方式
- 掌握登录认证、权限校验、角色判断的 API
- 了解 Sa-Token 与 Spring Security 的对比

## 核心知识点

### 1. 为什么选择 Sa-Token

Spring Security 功能强大但配置复杂，学习曲线陡峭。Sa-Token 是一个轻量级 Java 权限认证框架，设计简洁、API 优雅：

- 登录/注销/续期：一行代码搞定
- 权限认证 / 角色认证 / 会话治理：开箱即用
- 分布式会话共享：支持 Redis 集成

> 官方文档：[https://sa-token.cc](https://sa-token.cc)

### 2. 快速入门

**依赖**：

```xml
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot3-starter</artifactId>
    <version>1.37.0</version>
</dependency>
```

**配置**：

```yaml
sa-token:
  token-name: Authorization
  timeout: 2592000    # Token 有效期30天，单位秒
  activity-timeout: -1  # 临时有效期，-1代表不限制
  is-concurrent: true   # 是否允许同一账号多地登录
  is-share: false       # 是否共享Token
  token-style: uuid     # Token风格
```

### 3. 登录与注销

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/login")
    public Result login(@RequestBody LoginReqVO req) {
        // 1. 校验用户名密码
        AdminUserDO user = userService.authenticate(req.getUsername(), req.getPassword());

        // 2. 登录并创建 Token
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        return Result.success(token);
    }

    @PostMapping("/logout")
    public Result logout() {
        StpUtil.logout();
        return Result.success();
    }
}
```

### 4. 权限校验

```java
@RestController
@RequestMapping("/depts")
public class DeptController {

    @GetMapping
    @SaCheckPermission("system:dept:list")
    public Result list() { ... }

    @PostMapping
    @SaCheckPermission("system:dept:create")
    public Result save(@RequestBody Dept dept) { ... }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:dept:delete")
    public Result delete(@PathVariable Integer id) { ... }
}
```

**获取当前登录用户**：

```java
// 获取当前登录用户ID
Long userId = StpUtil.getLoginIdAsLong();

// 判断是否登录
boolean isLogin = StpUtil.isLogin();
```

### 5. 路由拦截器

```java
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/login", "/auth/register");
    }
}
```

### 6. Sa-Token 与 JWT 的关系

Sa-Token 默认使用 Session 机制（服务端存储会话）。如果需要 JWT 无状态方案：

```xml
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-jwt</artifactId>
    <version>1.37.0</version>
</dependency>
```

```yaml
sa-token:
  token-style: jwt
```

### 7. 与 Spring Security 对比

| 特性 | Spring Security | Sa-Token |
|------|----------------|----------|
| 学习成本 | 高 | 低 |
| 配置复杂度 | 复杂 | 极简 |
| 功能丰富度 | 非常丰富 | 常用功能齐全 |
| 扩展性 | 强 | 中等 |
| 适用场景 | 大型企业级 | 中小型项目、快速开发 |

## 动手练习

### 练习 1：Sa-Token 登录测试

1. 调用登录接口获取 Token
2. 在请求头中携带 `Authorization: Bearer xxx` 访问受保护接口
3. 调用注销接口后再次访问，观察是否被拦截

### 练习 2：权限注解测试

给不同用户分配不同权限，测试 `@SaCheckPermission` 的拦截效果。

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| 未能读取到有效Token | Token名称不匹配 | 检查前端请求头名称与 sa-token.token-name 配置 |
| 登录无效 | 未持久化登录态 | 确保调用了 StpUtil.login() |
| 权限校验不生效 | 拦截器未注册 | 检查 SaInterceptor 是否添加到 WebMvcConfigurer |

## 本节小结

Sa-Token 以极简的 API 设计降低了权限框架的学习门槛。版本 B 基于 yudao 框架，已集成 Sa-Token 实现完整的 RBAC（角色-权限）体系，比版本 A 的手写 Filter 更加健壮和可扩展。

## 参考文档

- [Sa-Token 官方文档](https://sa-token.cc/doc.html)

