# JWT 认证与拦截器

## 学习目标

- 理解 HTTP 无状态特性带来的身份识别难题，掌握 JWT 作为无状态认证方案的核心思想
- 掌握 JWT 的三段式结构（Header.Payload.Signature），理解 Base64 编码与 HMAC 签名的作用
- 能够使用 jjwt 库完成 JWT 的生成、解析、校验完整流程
- 实现登录接口签发 JWT，并在拦截器中完成后续请求的 Token 校验
- 理解 Version A（jjwt 0.9.1 + JDK8）与 Version B（jjwt 0.12.3 + JDK17）的 API 差异

---

## 核心知识点

### 1. 为什么需要登录认证

#### 1.1 HTTP 是无状态协议

HTTP 协议本身不会记住任何请求之间的关联。就像你去银行办业务，每次窗口工作人员都把你当成第一次来的陌生人，你需要反复出示身份证。登录认证就是为了解决这个问题：用户第一次"出示身份证"（登录）后，后续每次请求都携带一个"临时通行证"，服务器看到通行证就知道你是谁。

```
无认证的场景：
  客户端          服务器
    |  请求1: 查看余额   |
    | ----------------> |
    |  响应: 请先登录    |
    | <---------------- |
    |  请求2: 查看余额   |
    | ----------------> |
    |  响应: 请先登录    |   <-- 服务器"失忆"了
    | <---------------- |

有认证的场景：
  客户端          服务器
    |  登录请求         |
    | ----------------> |
    |  响应: Token=xxx  |
    | <---------------- |
    |  请求: 查看余额    |
    |  Header: Token=xxx|
    | ----------------> |
    |  响应: 余额1000元  |   <-- 服务器认出你了
    | <---------------- |
```

#### 1.2 Session vs Token 的对比

| 对比维度 | Session 方式 | Token 方式（JWT） |
|---------|-------------|------------------|
| 存储位置 | 服务器内存/Redis | 客户端（localStorage） |
| 服务器状态 | 有状态（需存 Session） | 无状态（不存会话信息） |
| 分布式支持 | 需共享 Session（如 Redis） | 天然支持，任意节点可校验 |
| 性能开销 | 每次查 Session 表 | 本地解析签名即可 |
| 跨域支持 | Cookie 默认受限 | Header 传递，天然跨域 |
| 适用场景 | 传统单体应用 | 前后端分离、微服务、移动端 |

**真实场景类比**：
- Session 像"存包柜"——你把包存在服务器（存包柜），拿到一个号码牌（SessionID），每次凭号码牌取包。服务器必须记住每个柜子里放了什么。
- JWT 像"演唱会门票"——门票上印了你的座位信息（Payload），还有防伪水印（Signature）。检票员不用查系统，看一眼门票就知道你是不是真票、坐哪。

---

### 2. JWT 是什么

JWT（JSON Web Token）是一个开放标准（RFC 7519），用于在各方之间安全地传输信息。它由三部分组成，用点号 `.` 连接：

```
  Header          Payload        Signature
    |               |               |
  eyJhbG...    eyJ1c2Vy...    SflKxw...
    |               |               |
    +---------------+---------------+
                    |
            完整 JWT Token
```

#### 2.1 Header（头部）

Header 是一个 JSON 对象，描述令牌的类型和签名算法：

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

- `alg`：签名算法，常见有 HS256（HMAC + SHA-256）、RS256（RSA + SHA-256）
- `typ`：令牌类型，固定为 JWT

这个 JSON 会被 **Base64Url 编码** 成字符串，成为 JWT 的第一部分。

#### 2.2 Payload（载荷）

Payload 存放"声明"（claims），也就是你要传递的数据：

```json
{
  "empId": 1,
  "username": "admin",
  "exp": 1715500000
}
```

Payload 也经过 Base64Url 编码，成为 JWT 的第二部分。

**⚠️ 重要提醒**：Base64 编码不是加密！任何人都能解码看到 Payload 内容。所以 JWT 里**不能放密码、身份证号等敏感信息**。

#### 2.3 Signature（签名）

签名用于验证消息没有被篡改。生成方式：

```
HMACSHA256(
  base64Url(header) + "." + base64Url(payload),
  secret
)
```

- 将 Header 和 Payload 的 Base64 字符串用点号连接
- 用密钥（secret）通过 HMAC-SHA256 算法生成签名
- 签名是防篡改的核心：如果你改了 Payload 里的 empId，签名就不匹配，校验会失败

**类比理解**：签名就像信封上的火漆印章。信封内容（Payload）是公开的，但火漆印章（Signature）需要专用印章（secret）才能盖出来。收件人收到后，用同样的印章比对，就知道信封有没有被拆开过。

---

### 3. jjwt 依赖引入

jjwt 是 Java 中最流行的 JWT 库。两个版本的依赖不同：

#### Version A（SpringBoot 2.7 + JDK8）—— jjwt 0.9.1

```xml
<!-- pom.xml 中添加 jjwt 依赖 -->
<dependency>
    <!-- 组ID：开发这个库的组织 -->
    <groupId>io.jsonwebtoken</groupId>
    <!--  artifactID：库的名称 -->
    <artifactId>jjwt</artifactId>
    <!-- 版本号：0.9.1 是 JDK8 兼容的经典版本 -->
    <version>0.9.1</version>
</dependency>
```

#### Version B（SpringBoot 3.2 + JDK17）—— jjwt 0.12.3

```xml
<!-- pom.xml 中需要引入三个模块 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <!-- runtime 表示编译时不需要，运行时才需要 -->
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

**为什么 Version B 要拆成三个包？**
jjwt 0.12.x 采用了模块化设计：`jjwt-api` 只包含接口（编译依赖），`jjwt-impl` 是具体实现，`jjwt-jackson` 负责 JSON 序列化。这样可以让你的项目只依赖接口，减少编译时耦合。

---

### 4. JWT 工具类实现

#### Version A：jjwt 0.9.1 风格

```java
package com.tlias.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * JWT 工具类 - Version A（jjwt 0.9.1 + JDK8）
 * 负责生成和解析 JWT Token
 */
@Component
public class JwtUtils {

    // ========================================
    // 第1步：从配置文件读取密钥和过期时间
    // ========================================

    // @Value 注解将 application.yml 中的配置注入到字段
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private Long expire;

    // static 字段无法直接用 @Value 注入（因为 static 属于类，@Value 属于实例）
    // 所以用实例字段接收，再通过 @PostConstruct 中转赋值给 static 字段
    private static String STATIC_SECRET;
    private static Long STATIC_EXPIRE;

    /**
     * @PostConstruct 注解的方法会在对象构造完成、依赖注入完成后执行
     * 这里将实例字段的值复制给 static 字段，供静态方法使用
     */
    @PostConstruct
    public void init() {
        STATIC_SECRET = secret;      // 将实例密钥转给静态变量
        STATIC_EXPIRE = expire;      // 将实例过期时间转给静态变量
    }

    // ========================================
    // 第2步：生成 JWT Token
    // ========================================

    /**
     * 根据员工ID和用户名生成 JWT Token
     *
     * @param empId    员工ID，存入 Payload
     * @param username 用户名，存入 Payload
     * @return 生成的 JWT 字符串
     */
    public static String generateToken(Integer empId, String username) {
        // 计算过期时间点：当前时间 + 有效期（毫秒）
        Date expiration = new Date(System.currentTimeMillis() + STATIC_EXPIRE);

        // Jwts.builder() 开始构建 JWT
        return Jwts.builder()
                // 设置 Payload 中的自定义声明（claim）
                .claim("empId", empId)           // 存入员工ID
                .claim("username", username)     // 存入用户名
                // 设置签发时间
                .setIssuedAt(new Date())
                // 设置过期时间
                .setExpiration(expiration)
                // 设置签名算法和密钥（HS256 + secret）
                .signWith(SignatureAlgorithm.HS256, STATIC_SECRET)
                // 压缩并生成最终字符串
                .compact();
    }

    // ========================================
    // 第3步：解析 JWT Token
    // ========================================

    /**
     * 解析 Token，获取 Claims（Payload 中的所有声明）
     *
     * @param token JWT 字符串
     * @return Claims 对象，包含所有 Payload 数据
     * @throws Exception 如果 Token 过期、签名错误或被篡改，会抛出异常
     */
    public static Claims parseToken(String token) throws Exception {
        // Jwts.parser() 创建解析器
        // setSigningKey 设置用于验证签名的密钥
        // parseClaimsJws 解析 Token，同时验证签名和过期时间
        // getBody 获取 Payload 部分
        return Jwts.parser()
                .setSigningKey(STATIC_SECRET)
                .parseClaimsJws(token)
                .getBody();
    }

    // ========================================
    // 第4步：便捷方法——获取用户信息
    // ========================================

    /**
     * 从 Token 中提取员工ID
     */
    public static Integer getEmpId(String token) throws Exception {
        Claims claims = parseToken(token);
        // get() 返回 Object，需要强制转换为 Integer
        return (Integer) claims.get("empId");
    }

    /**
     * 从 Token 中提取用户名
     */
    public static String getUsername(String token) throws Exception {
        Claims claims = parseToken(token);
        return (String) claims.get("username");
    }
}
```

#### Version B：jjwt 0.12.3 风格（JDK17）

```java
package com.tlias.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类 - Version B（jjwt 0.12.3 + JDK17）
 * 0.12.x 版本 API 有较大变化，使用了更安全的 SecretKey 类型
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private Long expire;

    private static SecretKey STATIC_KEY;    // 0.12.x 使用 SecretKey 替代 String
    private static Long STATIC_EXPIRE;

    @PostConstruct
    public void init() {
        // 0.12.x 要求密钥必须是 SecretKey 类型
        // Keys.hmacShaKeyFor 将字符串转为安全的密钥对象
        STATIC_KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        STATIC_EXPIRE = expire;
    }

    /**
     * 生成 JWT Token - 0.12.x 风格
     */
    public static String generateToken(Long empId, String username) {
        Date expiration = new Date(System.currentTimeMillis() + STATIC_EXPIRE);

        return Jwts.builder()
                // 0.12.x 使用 claim() 方法链式添加声明
                .claim("empId", empId)
                .claim("username", username)
                // subject 是标准声明，表示令牌主题（通常是用户标识）
                .subject(String.valueOf(empId))
                // 签发时间
                .issuedAt(new Date())
                // 过期时间
                .expiration(expiration)
                // 0.12.x 使用 signWith(SecretKey) 替代 signWith(Algorithm, String)
                .signWith(STATIC_KEY)
                .compact();
    }

    /**
     * 解析 JWT Token - 0.12.x 风格
     */
    public static Claims parseToken(String token) throws Exception {
        // 0.12.x 使用 parser() 返回 JwtParserBuilder
        // verifyWith 替代了旧的 setSigningKey
        // parseSignedClaims 替代了旧的 parseClaimsJws
        return Jwts.parser()
                .verifyWith(STATIC_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Long getEmpId(String token) throws Exception {
        Claims claims = parseToken(token);
        return claims.get("empId", Long.class);  // 0.12.x 支持直接指定类型
    }
}
```

#### 配置文件 application.yml

```yaml
# ========================================
# JWT 配置
# ========================================
jwt:
  # 密钥：用于签名和验证，必须足够长且保密
  # 生产环境建议使用 256 位（32字节）以上的随机字符串
  secret: tlias-secret-key-2024-java-web-training-course
  # 过期时间：12小时，单位毫秒
  # 43200000 = 12 * 60 * 60 * 1000
  expire: 43200000
```

---

### 5. 登录接口签发 JWT

```java
package com.tlias.controller;

import com.tlias.pojo.Emp;
import com.tlias.pojo.Result;
import com.tlias.service.EmpService;
import com.tlias.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 登录控制器
 * 处理用户登录请求，验证身份后签发 JWT
 */
@Slf4j
@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private EmpService empService;

    /**
     * 员工登录接口
     *
     * @param emp 请求体中的用户名和密码
     * @return 登录成功返回 JWT Token，失败返回错误信息
     */
    @PostMapping
    public Result login(@RequestBody Emp emp) {
        // 记录日志，方便排查问题
        log.info("员工登录请求，用户名：{}", emp.getUsername());

        // ========================================
        // 第1步：验证用户名和密码
        // ========================================
        Emp loginEmp = empService.login(emp);
        if (loginEmp == null) {
            log.warn("登录失败：用户名或密码错误，用户名：{}", emp.getUsername());
            return Result.error("用户名或密码错误");
        }

        // ========================================
        // 第2步：登录成功，生成 JWT
        // ========================================
        String jwtToken = JwtUtils.generateToken(loginEmp.getId(), loginEmp.getUsername());
        log.info("登录成功，员工ID：{}，已签发 JWT", loginEmp.getId());

        // 返回 Token 给前端
        return Result.success(jwtToken);
    }
}
```

---

### 6. 前端存储与使用 Token

```javascript
// ========================================
// 登录成功后存储 Token
// ========================================

// 假设这是登录接口的响应处理
function handleLogin(response) {
    // response.data 就是服务器返回的 JWT 字符串
    const token = response.data;

    // 存入浏览器的 localStorage，关闭浏览器后仍然保留
    localStorage.setItem('token', token);

    // 也可以存入 sessionStorage（关闭标签页就消失）
    // sessionStorage.setItem('token', token);
}

// ========================================
// 后续请求自动携带 Token
// ========================================

// 使用 Axios 拦截器，在每个请求发送前自动加上 Token
axios.interceptors.request.use(
    // config 是本次请求的配置对象
    function(config) {
        // 从 localStorage 读取 Token
        const token = localStorage.getItem('token');

        // 如果 Token 存在，添加到请求头
        if (token) {
            // 后端通过 "token" 这个 Header 名称获取 JWT
            config.headers['token'] = token;
        }

        // 必须返回 config，请求才会继续发送
        return config;
    },
    // 请求发送前的错误处理
    function(error) {
        return Promise.reject(error);
    }
);
```

---

### 7. 拦截器校验 JWT 完整流程

```
请求完整流程图：

  浏览器
    |
    |  ① POST /login
    |     {username: "admin", password: "123456"}
    v
  +------------------+
  |   LoginController |
  |   - 验证用户名密码  |
  |   - 生成 JWT      |
  +------------------+
    |
    |  ② 返回 JWT Token
    v
  浏览器存储 Token (localStorage)
    |
    |  ③ GET /emps
    |     Header: token=xxx.yyy.zzz
    v
  +------------------+
  |   TokenInterceptor|  <-- 拦截器
  |   - 从 Header 取 token
  |   - 调用 JwtUtils.parseToken()
  |   - 校验签名和过期时间
  +------------------+
    |
    |  ④ Token 有效？
    |     是 → 放行，进入 Controller
    |     否 → 返回 NOT_LOGIN
    v
  +------------------+
  |   EmpController   |
  |   - 执行业务逻辑   |
  +------------------+
```

#### 拦截器实现代码

```java
package com.tlias.interceptor;

import com.alibaba.fastjson.JSON;
import com.tlias.pojo.Result;
import com.tlias.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 登录校验拦截器
 * 拦截所有请求，验证 Token 有效性
 */
@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    /**
     * preHandle 在 Controller 方法执行前调用
     * 返回 true 表示放行，返回 false 表示拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // ========================================
        // 第1步：获取请求路径
        // ========================================
        String url = request.getRequestURI();
        log.info("请求路径：{}", url);

        // ========================================
        // 第2步：登录请求直接放行
        // ========================================
        // 用户还没登录，不可能携带有效 Token
        if (url.contains("/login")) {
            log.info("登录请求，直接放行");
            return true;  // true = 放行
        }

        // ========================================
        // 第3步：从请求头获取 Token
        // ========================================
        // 前端通过 Header 的 "token" 字段传递 JWT
        String token = request.getHeader("token");

        // 判断 Token 是否为空或空白
        if (token == null || token.trim().isEmpty()) {
            log.warn("Token 为空，拒绝访问：{}", url);
            // 返回 JSON 格式的错误信息
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(Result.error("NOT_LOGIN")));
            return false;  // false = 拦截
        }

        // ========================================
        // 第4步：解析并校验 Token
        // ========================================
        try {
            // parseToken 会同时做三件事：
            // 1. 解析 Base64 的 Header 和 Payload
            // 2. 用密钥重新计算签名，与 Token 中的签名比对（防篡改）
            // 3. 检查 exp（过期时间）是否超过当前时间
            JwtUtils.parseToken(token);

            log.info("Token 校验通过，放行：{}", url);
            return true;  // 校验通过，放行

        } catch (Exception e) {
            // 可能抛出的异常：
            // - ExpiredJwtException: Token 已过期
            // - SignatureException: 签名不匹配（密钥错误或 Token 被篡改）
            // - MalformedJwtException: Token 格式错误
            log.error("Token 校验失败：{}，原因：{}", url, e.getMessage());

            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(Result.error("NOT_LOGIN")));
            return false;  // 校验失败，拦截
        }
    }
}
```

#### 注册拦截器

```java
package com.tlias.config;

import com.tlias.interceptor.TokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 * 用于注册拦截器、跨域配置等
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TokenInterceptor tokenInterceptor;

    /**
     * 添加拦截器到 Spring MVC 拦截器链
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                // 添加自定义拦截器
                .addInterceptor(tokenInterceptor)
                // 拦截所有路径
                .addPathPatterns("/**")
                // 排除登录接口（不登录怎么拿 Token？）
                .excludePathPatterns("/login")
                // 也可以排除静态资源
                .excludePathPatterns("/uploads/**");
    }
}
```

---

### 8. Version A 与 Version B 的 jjwt API 差异总结

| 功能 | jjwt 0.9.1（Version A / JDK8） | jjwt 0.12.3（Version B / JDK17） |
|------|-------------------------------|----------------------------------|
| 依赖包 | 单个 `jjwt` | 三个模块：`jjwt-api` + `jjwt-impl` + `jjwt-jackson` |
| 签名方式 | `.signWith(Algorithm, String)` | `.signWith(SecretKey)` |
| 密钥类型 | `String`（直接传字符串） | `SecretKey`（需 `Keys.hmacShaKeyFor()` 转换） |
| 解析器设置密钥 | `.setSigningKey(String)` | `.verifyWith(SecretKey)` |
| 解析方法 | `.parseClaimsJws(token)` | `.parseSignedClaims(token)` |
| 获取 Payload | `.getBody()` | `.getPayload()` |
| 设置过期时间 | `.setExpiration(Date)` | `.expiration(Date)` |
| 设置签发时间 | `.setIssuedAt(Date)` | `.issuedAt(Date)` |
| 获取 Claim | `(Integer) claims.get("key")` | `claims.get("key", Integer.class)` |

---

### 9. JWT 安全注意事项

1. **密钥必须保密**：`secret` 泄露后，任何人都能伪造 Token。生产环境应使用随机生成的长字符串，不要硬编码在代码中。
2. **Payload 不加密**：Base64 只是编码，不是加密。不要在 Payload 中存放密码、身份证号等敏感信息。
3. **设置合理过期时间**：过期时间太长，Token 被盗后风险窗口大；太短，用户频繁需要重新登录。一般 12-24 小时。
4. **JWT 无法主动作废**：一旦签发，在过期前一直有效。如果需要"登出后立即失效"功能，需要配合 Redis 黑名单或改用 Session 方案。
5. **使用 HTTPS**：Token 在 Header 中传输，如果走 HTTP 会被中间人截获。生产环境必须使用 HTTPS。

---

## 动手练习

### 练习 1：JWT 解析调试

**目标**：直观理解 JWT 的三段结构。

**步骤**：
1. 启动项目，调用登录接口获取 JWT Token
2. 将 Token 复制到 [https://jwt.io](https://jwt.io) 的解码区域
3. 观察右侧自动解析出的 Header、Payload 内容
4. 尝试修改 Payload 中的某个字符（比如把 empId 从 1 改成 2）
5. 观察下方的 Signature Verified 变为 Invalid Signature

**思考问题**：为什么修改 Payload 后签名就失效了？

### 练习 2：Token 过期体验

**目标**：感受 Token 过期后的行为。

**步骤**：
1. 将 `application.yml` 中的 `jwt.expire` 临时改为 `60000`（1分钟）
2. 重启项目，登录获取 Token
3. 立即访问一个受保护的接口（如 `/emps`），应该成功
4. 等待 1 分钟后再次访问，观察返回 `NOT_LOGIN`
5. 查看后端日志，应该看到 `ExpiredJwtException`
6. 实验完成后记得把过期时间改回 12 小时

### 练习 3：jjwt 版本迁移对比

**目标**：理解两个版本 API 的差异。

**步骤**：
1. 在 Version A 项目中使用 jjwt 0.9.1 的 API 写一个生成 Token 的方法
2. 查看生成的 Token 结构（用 jwt.io 解码）
3. 对比 Version B 中 jjwt 0.12.3 的写法差异
4. 列出至少 3 个 API 变化点

---

## 常见错误排查

### 依赖配置问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 编译期 | `ClassNotFoundException: io.jsonwebtoken.Jwts` | pom.xml 中 jjwt 依赖未添加或版本号写错 | 检查 pom.xml，确认依赖已添加且 Maven 已重新导入 |
| 编译期 | `cannot find symbol: method parser()` | 使用了 0.12.x 的 API 但引入的是 0.9.1 | 对照版本差异表，使用对应版本的 API |
| 编译期 | `cannot find symbol: method signWith(SignatureAlgorithm, String)` | 0.12.x 移除了这个方法 | 改用 `.signWith(SecretKey)` |
| 运行期 | `NoClassDefFoundError: jjwt-impl` | Version B 缺少 jjwt-impl 或 scope 不是 runtime | 确保引入 jjwt-impl 且 scope 为 runtime |

### 参数请求问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 请求期 | 后端收到 token 为 null | 前端 Header 名称与后端不一致 | 确认前端用 `config.headers['token']`，后端用 `request.getHeader("token")` |
| 请求期 | 前端收到 200 但数据为空 | 拦截器返回 NOT_LOGIN 但 Content-Type 不对 | 设置 `response.setContentType("application/json;charset=UTF-8")` |
| 请求期 | 登录接口也被拦截 | 拦截器排除了 `/login` 但请求路径是 `/login/` | 检查路径匹配，或使用 `.excludePathPatterns("/login/**")` |

### 代码逻辑问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 运行期 | `@Value` 注入为 null | 工具类没有加 `@Component`，Spring 没有管理它 | 给 JwtUtils 加 `@Component` 注解 |
| 运行期 | static 字段还是 null | `@PostConstruct` 方法没执行，或类未被 Spring 扫描 | 确认类在 Spring 扫描路径下，且使用了 `@Component` |
| 运行期 | Token 生成成功但解析失败 | 生成和解析使用的 secret 不一致 | 确保生成和解析都读取同一个配置值 |
| 运行期 | 解析抛出 `ExpiredJwtException` | Token 已过期 | 重新登录获取新 Token，或延长过期时间 |

### 性能安全问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 安全 | Token 被截获后可随意使用 | 使用 HTTP 传输，未加密 | 生产环境强制使用 HTTPS |
| 安全 | Payload 中能看到用户密码 | 误将敏感信息放入 Payload | Payload 只放用户ID、用户名等非敏感信息 |
| 安全 | 密钥太短被暴力破解 | secret 只有几个字符 | 使用至少 256 位（32字节）的随机字符串 |
| 性能 | 每次请求都解析 JWT 开销大 | 解析涉及 Base64 解码和 HMAC 计算 | 在高并发场景下，考虑配合 Redis 缓存解析结果 |

---

## 本节小结

```
+-----------------------------------------------------------+
|                     JWT 认证与拦截器                        |
+-----------------------------------------------------------+
|                                                           |
|   +----------------+     +----------------+              |
|   |   为什么用 JWT  |     |  不用 Session  |              |
|   |   - 无状态      |     |  - 分布式麻烦  |              |
|   |   - 天然跨域    |     |  - 服务器存会话 |              |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   |   JWT 结构      |     |  Header        |              |
|   |   xxxx.yyyy.zzzz| --> |  Payload       |              |
|   |                 |     |  Signature     |              |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   |   jjwt 工具类   |     |  generateToken |              |
|   |   Version A/B   | --> |  parseToken    |              |
|   |   API 有差异    |     |  getEmpId      |              |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   |   登录接口      | --> |  验证密码       |              |
|   |   签发 Token    |     |  生成 JWT      |              |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   |   拦截器        | --> |  取 Header     |              |
|   |   校验 Token    |     |  parseToken()  |              |
|   |   放行/拦截     |     |  异常=拦截     |              |
|   +----------------+     +----------------+              |
|                                                           |
+-----------------------------------------------------------+
```

---

## 参考文档

- [JWT 官方介绍](https://jwt.io/introduction)
- [JWT.io 在线调试工具](https://jwt.io)
- [jjwt GitHub 仓库](https://github.com/jwtk/jjwt)
- [jjwt 0.12.x 迁移指南](https://github.com/jwtk/jjwt#jjwt-0120-new-features)
- [RFC 7519 - JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519)
