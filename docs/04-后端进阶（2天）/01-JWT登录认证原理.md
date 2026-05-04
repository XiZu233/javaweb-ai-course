# JWT 登录认证原理

## 学习目标

- 理解 Session 和 Token 两种认证方式的区别
- 掌握 JWT 的组成和生成/解析流程
- 能够实现基于 JWT 的登录接口

## 核心知识点

### 1. 为什么需要登录认证

HTTP 是无状态协议，服务器无法识别两次请求是否来自同一用户。登录认证就是为了解决这个问题：用户登录后，后续请求携带凭证，服务器验证凭证确认用户身份。

### 2. Session vs Token

**Session 方式**：
- 登录成功后，服务器创建 Session 对象存储用户信息
- 通过 Cookie 将 SessionID 返回给浏览器
- 后续请求自动携带 Cookie，服务器根据 SessionID 查找用户信息
- 缺点：服务器需要存储 Session，分布式环境下需要共享 Session

**Token 方式（JWT）**：
- 登录成功后，服务器生成 Token 字符串返回给前端
- 前端将 Token 存储在 localStorage
- 后续请求在请求头中携带 Token
- 服务器验证 Token 的有效性，从中解析用户信息
- 优点：服务器无状态，天然支持分布式

### 3. JWT 结构

JWT（JSON Web Token）由三部分组成，用点号分隔：

```
xxxxx.yyyyy.zzzzz
  ↑      ↑      ↑
Header  Payload  Signature
```

**Header**：声明类型和签名算法
```json
{ "alg": "HS256", "typ": "JWT" }
```

**Payload**：存放声明（如用户ID、用户名、过期时间）
```json
{ "empId": 1, "username": "admin", "exp": 1715500000 }
```

**Signature**：对前两部分进行签名，防止篡改
```
HMACSHA256(base64Url(header) + "." + base64Url(payload), secret)
```

### 4. JWT 工具类实现

```java
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private Long expire;

    private static String STATIC_SECRET;
    private static Long STATIC_EXPIRE;

    @PostConstruct
    public void init() {
        STATIC_SECRET = secret;
        STATIC_EXPIRE = expire;
    }

    // 生成 Token
    public static String generateToken(Integer empId, String username) {
        return JWT.create()
                .withClaim("empId", empId)
                .withClaim("username", username)
                .withExpiresAt(new Date(System.currentTimeMillis() + STATIC_EXPIRE))
                .sign(Algorithm.HMAC256(STATIC_SECRET));
    }

    // 解析 Token
    public static DecodedJWT parseToken(String token) {
        return JWT.require(Algorithm.HMAC256(STATIC_SECRET))
                .build()
                .verify(token);
    }

    // 获取用户ID
    public static Integer getEmpId(String token) {
        return parseToken(token).getClaim("empId").asInt();
    }
}
```

**配置**（application.yml）：
```yaml
jwt:
  secret: your-secret-key-here
  expire: 43200000  # 12小时，单位毫秒
```

### 5. 登录接口

```java
@Slf4j
@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private EmpMapper empMapper;

    @PostMapping
    public Result login(@RequestBody Emp emp) {
        log.info("员工登录：{}", emp.getUsername());

        // 1. 根据用户名查询用户
        Emp e = empMapper.getByUsername(emp.getUsername());
        if (e == null || !e.getPassword().equals(emp.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        // 2. 生成 JWT Token
        String jwt = JwtUtils.generateToken(e.getId(), e.getUsername());
        return Result.success(jwt);
    }
}
```

### 6. 前端存储与使用 Token

```javascript
// 登录成功后存储 Token
localStorage.setItem('token', response.data)

// 后续请求携带 Token
request.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers['token'] = token
    }
    return config
})
```

### 7. JWT 注意事项

- **不要存放敏感信息**在 Payload 中（Payload 是 Base64 编码，可解码）
- **设置合理的过期时间**，过期后需要重新登录
- **密钥 secret 必须保密**，泄露后任何人都可以伪造 Token
- JWT 一旦签发无法主动作废（如需登出功能，需要配合黑名单）

## 动手练习

### 练习 1：JWT 解析调试

1. 登录获取 JWT Token
2. 将 Token 复制到 [jwt.io](https://jwt.io) 查看 Header、Payload 内容
3. 修改 Payload 中的某个字符，观察验证是否失败

### 练习 2：Token 过期处理

将 jwt.expire 设置为 60000（1分钟），登录后等待 1 分钟再发起请求，观察是否返回 NOT_LOGIN。

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| The Token has expired | Token 过期 | 重新登录获取新 Token |
| The verification failed | 密钥不匹配或 Token 被篡改 | 检查 secret 配置，重新登录 |
| 无法注入 @Value 到 static | static 字段不属于实例 | 用 @PostConstruct 中转赋值 |

## 本节小结

JWT 是现代前后端分离项目的标准认证方案。它通过签名保证不可篡改，通过过期时间控制有效期，服务端无需存储会话信息，天然适合分布式和微服务架构。

## 参考文档

- [JWT 官方介绍](https://jwt.io/introduction)
- [java-jwt GitHub](https://github.com/auth0/java-jwt)
