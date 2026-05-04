# Filter 与 Interceptor

## 学习目标

- 理解 Filter（Servlet 规范）和 Interceptor（Spring MVC）的本质区别与各自定位
- 掌握 Filter 和 Interceptor 的执行顺序，能够画出完整的请求处理链路图
- 能够独立编写 Filter 实现 JWT 校验，编写 Interceptor 实现权限校验
- 理解实际项目中 Filter 和 Interceptor 如何组合使用，各自负责什么
- 掌握 Filter 和 Interceptor 的注册方式及常见配置陷阱

---

## 核心知识点

### 1. 为什么需要请求拦截机制

在 Web 应用中，很多功能是"横切"在所有请求之上的——比如登录校验、权限检查、日志记录、耗时统计。如果每个 Controller 方法里都写一遍校验代码，会造成严重的代码重复。

**真实场景类比**：想象一个办公大楼，每层楼（每个 Controller）都有一些共同的安保需求：
- **大门安检（Filter）**：检查每个人是否带了门禁卡，不管你去哪层楼
- **楼层门禁（Interceptor）**：检查你是否有权限进入特定楼层，需要查系统
- **办公室签到（AOP）**：记录你进了哪个办公室、待了多久

Filter 和 Interceptor 就是 Web 应用中的"大门安检"和"楼层门禁"。

---

### 2. Filter 与 Interceptor 的本质区别

#### 2.1 所属规范不同

| 对比维度 | Filter（过滤器） | Interceptor（拦截器） |
|---------|----------------|---------------------|
| **所属规范** | Servlet 规范（Java EE / Jakarta EE） | Spring MVC 框架 |
| **执行时机** | Servlet 容器接收到请求后，进入 Servlet 之前 | DispatcherServlet 分发到 Controller 前后 |
| **依赖容器** | 依赖 Web 容器（Tomcat / Jetty） | 依赖 Spring 容器 |
| **获取 Spring Bean** | ❌ 不能直接获取（除非特殊处理） | ✅ 可以直接 `@Autowired` 注入 |
| **使用范围** | 所有 Java Web 应用（包括非 Spring 项目） | 仅 Spring / SpringBoot 项目 |
| **操作范围** | 可以修改请求和响应（如编码、压缩） | 可以操作 Controller 方法参数和返回值 |
| **执行次数** | 每个请求只执行一次 | 每个 Controller 方法执行前后各一次 |

#### 2.2 执行顺序图解

```
客户端请求
    |
    v
+------------------------------------------+
|  ① Filter1.doFilter() 开始               |  <-- Filter 链（可多个）
|     |                                    |
|     v                                    |
|  ② Filter2.doFilter() 开始               |
|     |                                    |
|     v                                    |
|  ③ DispatcherServlet（Spring 入口）       |
|     |                                    |
|     v                                    |
|  ④ Interceptor1.preHandle()              |  <-- true 才继续
|     |                                    |
|     v                                    |
|  ⑤ Interceptor2.preHandle()              |
|     |                                    |
|     v                                    |
|  ⑥ Controller 方法执行                    |  <-- 你的业务代码
|     |                                    |
|     v                                    |
|  ⑦ Interceptor2.postHandle()             |  <-- Controller 执行后，视图渲染前
|     |                                    |
|     v                                    |
|  ⑧ Interceptor1.postHandle()             |
|     |                                    |
|     v                                    |
|  ⑨ 视图渲染（如果有）                      |
|     |                                    |
|     v                                    |
|  ⑩ Interceptor2.afterCompletion()        |  <-- 无论是否异常都会执行
|     |                                    |
|     v                                    |
|  ⑪ Interceptor1.afterCompletion()        |
|     |                                    |
|     v                                    |
|  ⑫ Filter2.doFilter() 结束               |
|     |                                    |
|     v                                    |
|  ⑬ Filter1.doFilter() 结束               |
+------------------------------------------+
    |
    v
  响应返回客户端
```

**关键记忆点**：
- Filter 包裹在整个 Spring MVC 之外，像"洋葱的最外层"
- Interceptor 包裹在 Controller 之外，像"洋葱的中间层"
- 请求时：先 Filter 后 Interceptor
- 响应时：先 Interceptor 后 Filter（后进先出）

#### 2.3 类比理解

| 角色 | 类比 | 职责 |
|------|------|------|
| Filter | 小区大门保安 | 检查所有人是否带了门禁卡，不区分访客目的 |
| Interceptor | 楼栋管家 | 知道你要去哪家，能查系统看你有没有权限 |
| Controller | 住户 | 真正处理业务的人 |

Filter 不知道 Spring 的存在，它只认识 `ServletRequest` 和 `ServletResponse`。Interceptor 生在 Spring 里，可以注入 Service、Mapper 做任何数据库查询。

---

### 3. Filter 实现登录校验

Filter 是 Servlet 规范的一部分，实现 `javax.servlet.Filter`（JDK8）或 `jakarta.servlet.Filter`（JDK17）接口。

#### 3.1 使用 `@WebFilter` 注解方式（Version A）

```java
package com.tlias.filter;

import com.alibaba.fastjson.JSON;
import com.tlias.pojo.Result;
import com.tlias.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Token 校验过滤器 - Version A（JDK8 / javax.servlet）
 * 拦截所有请求，验证 JWT Token 的有效性
 *
 * @WebFilter 是 Servlet 规范提供的注解，告诉容器这个类是过滤器
 * urlPatterns = "/*" 表示拦截所有 URL
 */
@Slf4j
@WebFilter(urlPatterns = "/*")
public class TokenFilter implements Filter {

    /**
     * 初始化方法，Filter 创建时执行一次
     * 可以在这里做一些初始化配置
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("TokenFilter 初始化完成");
    }

    /**
     * 核心过滤方法，每个请求都会经过这里
     *
     * @param req   请求对象（ServletRequest 是通用类型，实际是 HttpServletRequest）
     * @param res   响应对象
     * @param chain FilterChain 过滤器链，调用 chain.doFilter() 表示放行到下一个过滤器或 Servlet
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // ========================================
        // 第1步：类型转换（ServletRequest -> HttpServletRequest）
        // ========================================
        // ServletRequest 是通用接口，HTTP 请求需要转成 HttpServletRequest 才能获取 Header
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // ========================================
        // 第2步：获取请求路径
        // ========================================
        // getRequestURI() 返回如 "/emps"、"/login" 这样的路径
        String url = request.getRequestURI();
        log.info("Filter 拦截到请求：{}", url);

        // ========================================
        // 第3步：登录请求直接放行
        // ========================================
        // 用户还没登录，不可能有 Token，所以登录接口必须放行
        // contains("login") 可以匹配 /login、/login.html 等
        if (url.contains("login")) {
            log.info("登录请求，Filter 放行");
            // chain.doFilter() 是放行的关键：继续执行下一个 Filter 或目标 Servlet
            chain.doFilter(req, res);
            return;  // 放行后必须 return，否则代码会继续往下执行
        }

        // ========================================
        // 第4步：从请求头获取 Token
        // ========================================
        // 前端通过 "token" 这个 Header 字段传递 JWT
        String token = request.getHeader("token");

        // StringUtils.hasLength() 是 Spring 的工具方法，检查字符串是否非空
        if (!StringUtils.hasLength(token)) {
            log.warn("Token 为空，请求被拒绝：{}", url);

            // 设置响应内容类型为 JSON，防止前端解析错误
            response.setContentType("application/json;charset=UTF-8");
            // 返回 NOT_LOGIN 错误信息
            response.getWriter().write(JSON.toJSONString(Result.error("NOT_LOGIN")));

            // 注意：这里不能调用 chain.doFilter()，因为请求被拒绝
            return;
        }

        // ========================================
        // 第5步：解析并校验 Token
        // ========================================
        try {
            // JwtUtils.parseToken() 会验证签名和过期时间
            // 如果 Token 过期或被篡改，会抛出异常
            JwtUtils.parseToken(token);

            log.info("Token 校验通过，Filter 放行：{}", url);
            // 校验通过，放行到下一个 Filter 或 Controller
            chain.doFilter(req, res);

        } catch (Exception e) {
            // 捕获所有解析异常（过期、签名错误、格式错误等）
            log.error("Token 校验失败：{}，原因：{}", url, e.getMessage());

            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(Result.error("NOT_LOGIN")));
            // 不放行，直接返回
        }
    }

    /**
     * 销毁方法，Filter 销毁时执行
     * 可以在这里释放资源
     */
    @Override
    public void destroy() {
        log.info("TokenFilter 销毁");
    }
}
```

#### 3.2 启动类开启 Filter 扫描

```java
package com.tlias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * 项目启动类
 *
 * @ServletComponentScan 是 Spring Boot 提供的注解
 * 作用：扫描当前包及子包下的 @WebFilter、@WebServlet、@WebListener
 * 不加这个注解，@WebFilter 不会生效！
 */
@SpringBootApplication
@ServletComponentScan
public class TliasApplication {
    public static void main(String[] args) {
        SpringApplication.run(TliasApplication.class, args);
    }
}
```

#### 3.3 Version B 的 Filter（JDK17 / jakarta.servlet）

```java
package com.tlias.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Token 校验过滤器 - Version B（JDK17 / jakarta.servlet）
 * 注意：JDK17 使用 jakarta.servlet 包，而不是 javax.servlet
 */
@Slf4j
@WebFilter(urlPatterns = "/*")
public class TokenFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // 逻辑与 Version A 完全相同，只是包名从 javax 变成了 jakarta
        String url = request.getRequestURI();
        log.info("Filter 拦截到请求：{}", url);

        if (url.contains("login")) {
            chain.doFilter(req, res);
            return;
        }

        // ... 其余校验逻辑相同
    }
}
```

**javax vs jakarta 的区别**：
- Java EE 8 及之前使用 `javax.*` 包名
- 从 Jakarta EE 9 开始，Oracle 将 Java EE 捐赠给 Eclipse 基金会，包名改为 `jakarta.*`
- SpringBoot 2.x 基于 Java EE 8，用 `javax.servlet`
- SpringBoot 3.x 基于 Jakarta EE 9+，用 `jakarta.servlet`
- 除了包名，API 完全一致

---

### 4. Interceptor 实现登录校验

Interceptor 是 Spring MVC 的组件，需要实现 `HandlerInterceptor` 接口。

```java
package com.tlias.interceptor;

import com.alibaba.fastjson.JSON;
import com.tlias.pojo.Result;
import com.tlias.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Token 校验拦截器
 * 实现 HandlerInterceptor 接口，在 Controller 方法执行前后插入逻辑
 */
@Slf4j
@Component  // 必须加 @Component，让 Spring 管理，否则无法注入
public class TokenInterceptor implements HandlerInterceptor {

    /**
     * preHandle：在 Controller 方法执行前调用
     * 返回 true 表示放行，返回 false 表示拦截
     * 这是拦截器中最常用的方法
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // handler 参数是被拦截的目标对象，通常是 HandlerMethod（Controller 的方法）
        log.info("Interceptor preHandle，目标处理器：{}", handler.getClass().getName());

        // ========================================
        // 第1步：获取请求路径
        // ========================================
        String url = request.getRequestURI();

        // ========================================
        // 第2步：登录请求直接放行
        // ========================================
        if (url.contains("login")) {
            return true;  // true = 放行
        }

        // ========================================
        // 第3步：获取并校验 Token
        // ========================================
        String token = request.getHeader("token");

        if (token == null || token.trim().isEmpty()) {
            log.warn("Token 为空，Interceptor 拦截：{}", url);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(Result.error("NOT_LOGIN")));
            return false;  // false = 拦截
        }

        try {
            JwtUtils.parseToken(token);
            log.info("Token 校验通过，Interceptor 放行：{}", url);
            return true;
        } catch (Exception e) {
            log.error("Token 校验失败，Interceptor 拦截：{}，原因：{}", url, e.getMessage());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(Result.error("NOT_LOGIN")));
            return false;
        }
    }

    /**
     * postHandle：在 Controller 方法执行后，视图渲染前调用
     * 可以在这里修改 ModelAndView（如果有的话）
     * 如果 Controller 抛出异常，这个方法不会执行
     */
    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) throws Exception {
        log.info("Interceptor postHandle，Controller 方法已执行");
        // 前后端分离项目中通常没有视图渲染，这个方法用得较少
    }

    /**
     * afterCompletion：在整个请求完成后调用（视图渲染完毕）
     * 无论 Controller 是否抛出异常，这个方法都会执行
     * 适合做一些资源清理、日志记录
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {
        log.info("Interceptor afterCompletion，请求处理完成");
        // 如果有异常，ex 参数会携带异常信息
        if (ex != null) {
            log.error("请求处理过程中发生异常：{}", ex.getMessage());
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
 * Web MVC 配置类
 * 用于注册拦截器、配置跨域、视图解析器等
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 注入 Spring 管理的拦截器实例
    @Autowired
    private TokenInterceptor tokenInterceptor;

    /**
     * 注册拦截器
     * 可以注册多个拦截器，按注册顺序执行
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                // 添加第一个拦截器：Token 校验
                .addInterceptor(tokenInterceptor)
                // addPathPatterns：指定拦截哪些路径
                // "/**" 表示拦截所有路径（包括子路径）
                .addPathPatterns("/**")
                // excludePathPatterns：指定排除哪些路径
                // 登录接口不能拦截，否则用户永远登不上
                .excludePathPatterns("/login")
                // 排除静态资源路径
                .excludePathPatterns("/uploads/**")
                // 排除 Swagger / Knife4j 文档路径（如果有）
                .excludePathPatterns("/doc.html", "/webjars/**", "/swagger-resources/**");

        // 如果有第二个拦截器，继续链式注册：
        // registry.addInterceptor(anotherInterceptor)
        //         .addPathPatterns("/admin/**");
    }
}
```

---

### 5. 实际项目中如何组合使用

在实际项目中，Filter 和 Interceptor 不是"二选一"的关系，而是各司其职、配合使用。

#### 5.1 职责划分建议

| 功能 | 推荐组件 | 原因 |
|------|---------|------|
| 字符编码设置（UTF-8） | Filter | 越早设置越好，在 Spring 之前 |
| 跨域处理（CORS） | Filter | 需要在请求到达 Spring 前处理 |
| JWT 登录校验 | Interceptor | 可能需要注入 Service 查数据库 |
| 权限校验（RBAC） | Interceptor | 必须注入 Service / Mapper 查权限 |
| 请求耗时统计 | Interceptor | 需要 preHandle + afterCompletion |
| 请求日志记录 | Interceptor | 可以获取 Controller 方法名、参数 |
| 敏感词过滤 | Filter | 在请求进入 Spring 前处理请求体 |
| 响应数据压缩 | Filter | 在响应离开 Spring 后处理 |

#### 5.2 组合使用示例：Filter 做编码 + Interceptor 做权限

```java
/**
 * 编码设置 Filter
 * 确保所有请求的编码都是 UTF-8
 */
@WebFilter(urlPatterns = "/*")
public class EncodingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        // 在请求进入 Spring 前设置编码
        req.setCharacterEncoding("UTF-8");
        res.setCharacterEncoding("UTF-8");
        // 放行
        chain.doFilter(req, res);
    }
}
```

```java
/**
 * 权限校验 Interceptor
 * 需要查询数据库判断用户是否有权限
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private PermissionService permissionService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        // 从 Token 解析用户ID
        Long userId = JwtUtils.getEmpId(request.getHeader("token"));

        // 获取请求路径和方法
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 查询数据库判断权限
        boolean hasPermission = permissionService.check(userId, uri, method);
        if (!hasPermission) {
            response.setStatus(403);
            response.getWriter().write("权限不足");
            return false;
        }
        return true;
    }
}
```

#### 5.3 完整请求链路示例

```
  客户端请求 GET /emps?page=1&pageSize=10
    |
    v
+-------------------------------------------+
| EncodingFilter                            |
|   - 设置 request 编码 UTF-8               |
|   - 设置 response 编码 UTF-8              |
|   chain.doFilter()                        |
+-------------------------------------------+
    |
    v
+-------------------------------------------+
| TokenInterceptor (preHandle)              |
|   - 从 Header 取 token                    |
|   - 调用 JwtUtils.parseToken()            |
|   - Token 有效 → return true              |
+-------------------------------------------+
    |
    v
+-------------------------------------------+
| EmpController.list()                      |
|   - 执行业务逻辑                           |
|   - 返回 Result.success(pageResult)       |
+-------------------------------------------+
    |
    v
+-------------------------------------------+
| TokenInterceptor (postHandle)             |
|   - Controller 已执行，视图渲染前          |
+-------------------------------------------+
    |
    v
+-------------------------------------------+
| TokenInterceptor (afterCompletion)        |
|   - 请求完成，记录日志                     |
+-------------------------------------------+
    |
    v
+-------------------------------------------+
| EncodingFilter 结束                       |
+-------------------------------------------+
    |
    v
  响应返回客户端
```

---

### 6. Filter 的另一种注册方式（配置类）

除了 `@WebFilter + @ServletComponentScan`，Filter 还可以通过配置类注册，这种方式更灵活（可以设置顺序、指定参数等）。

```java
package com.tlias.config;

import com.tlias.filter.TokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Filter 配置类
 * 通过 Bean 方式注册 Filter，比 @WebFilter 更灵活
 */
@Configuration
public class FilterConfig {

    /**
     * 注册 TokenFilter
     * FilterRegistrationBean 可以设置顺序、URL 映射等
     */
    @Bean
    public FilterRegistrationBean<TokenFilter> tokenFilterRegistration() {
        FilterRegistrationBean<TokenFilter> registration = new FilterRegistrationBean<>();

        // 设置过滤器实例
        registration.setFilter(new TokenFilter());

        // 设置过滤的 URL 模式
        registration.addUrlPatterns("/*");

        // 设置过滤器名称
        registration.setName("tokenFilter");

        // 设置顺序（数字越小越先执行）
        registration.setOrder(1);

        return registration;
    }

    /**
     * 如果有多个 Filter，按顺序注册
     */
    @Bean
    public FilterRegistrationBean<EncodingFilter> encodingFilterRegistration() {
        FilterRegistrationBean<EncodingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new EncodingFilter());
        registration.addUrlPatterns("/*");
        registration.setName("encodingFilter");
        // 编码 Filter 应该最先执行
        registration.setOrder(0);
        return registration;
    }
}
```

**两种方式对比**：

| 方式 | 优点 | 缺点 |
|------|------|------|
| `@WebFilter` | 简单直观，一行注解搞定 | 无法设置执行顺序，无法灵活配置参数 |
| `FilterRegistrationBean` | 可设置顺序、参数、URL 映射 | 代码稍多 |

---

## 动手练习

### 练习 1：Filter 放行静态资源

**目标**：修改 TokenFilter，放行 `/uploads/**` 路径的图片访问请求。

**提示**：在 `doFilter` 方法的登录放行判断后面，增加对 URL 的判断：

```java
// 静态资源直接放行
if (url.startsWith("/uploads/")) {
    chain.doFilter(req, res);
    return;
}
```

**验证方法**：上传一张图片后，直接浏览器访问 `http://localhost:8080/uploads/xxx.jpg`，应该能看到图片而不是 NOT_LOGIN。

### 练习 2：Interceptor 记录请求耗时

**目标**：使用 Interceptor 的 `preHandle` 和 `afterCompletion` 方法，记录每个 Controller 方法的执行耗时。

**步骤**：
1. 在 `preHandle` 中将开始时间存入 request 属性：`request.setAttribute("startTime", System.currentTimeMillis())`
2. 在 `afterCompletion` 中取出开始时间，计算耗时并打印日志
3. 访问任意接口，观察控制台输出的耗时信息

**参考代码**：

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    request.setAttribute("startTime", System.currentTimeMillis());
    return true;
}

@Override
public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                            Object handler, Exception ex) {
    Long startTime = (Long) request.getAttribute("startTime");
    long duration = System.currentTimeMillis() - startTime;
    log.info("请求 {} 耗时 {} ms", request.getRequestURI(), duration);
}
```

### 练习 3：Filter 与 Interceptor 执行顺序验证

**目标**：直观感受 Filter 和 Interceptor 的执行顺序。

**步骤**：
1. 创建一个 Filter，在 `doFilter` 前后打印日志
2. 创建一个 Interceptor，在 `preHandle` 和 `afterCompletion` 打印日志
3. 访问一个接口，观察控制台日志输出顺序
4. 验证：Filter 开始 → Interceptor preHandle → Controller → Interceptor afterCompletion → Filter 结束

---

## 常见错误排查

### 依赖配置问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 启动期 | `@WebFilter` 不生效 | 启动类缺少 `@ServletComponentScan` | 在启动类添加 `@ServletComponentScan` |
| 启动期 | Filter 执行顺序混乱 | 多个 Filter 没有设置 Order | 使用 `FilterRegistrationBean` 设置 `setOrder()` |
| 编译期 | `javax.servlet` 找不到 | JDK17 项目用了 javax 包 | 改为 `jakarta.servlet` |
| 编译期 | `jakarta.servlet` 找不到 | JDK8 项目用了 jakarta 包 | 改为 `javax.servlet` |

### 参数请求问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 请求期 | Interceptor 注入 Bean 为 null | 拦截器没加 `@Component` | 给 Interceptor 加 `@Component` |
| 请求期 | Interceptor 没执行 | 没有在 WebMvcConfigurer 中注册 | 创建配置类实现 `addInterceptors()` |
| 请求期 | 返回 NOT_LOGIN 但前端收不到 | 响应 Content-Type 不对 | 设置 `response.setContentType("application/json;charset=UTF-8")` |
| 请求期 | 拦截器排除了 `/login` 但登录仍被拦截 | 请求路径带上下文或斜杠 | 检查实际路径，使用 `url.contains("login")` 更宽松 |

### 代码逻辑问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 运行期 | Filter 里无法注入 Spring Bean | Filter 由 Servlet 容器管理，不是 Spring | 用 `FilterRegistrationBean` 注册，或在 Filter 里通过 `WebApplicationContextUtils` 获取 |
| 运行期 | `chain.doFilter()` 后代码还执行 | `return` 语句漏了 | 放行后加 `return`，否则会继续执行后面的代码 |
| 运行期 | Interceptor 的 `postHandle` 不执行 | Controller 抛异常 | `postHandle` 在异常时不执行，用 `afterCompletion` |
| 运行期 | 多个 Interceptor 顺序不对 | 注册顺序就是执行顺序 | 调整 `addInterceptors` 中的注册顺序 |

### 性能安全问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 性能 | Filter 中做数据库查询 | Filter 不能注入 Service，查询方式笨拙 | 数据库查询放到 Interceptor 中做 |
| 性能 | 每个请求都解析 JWT 多次 | Filter 和 Interceptor 都解析了 Token | 统一在一个地方解析，另一个直接复用结果 |
| 安全 | Filter 放行后响应被篡改 | 没有正确处理响应流 | 使用 `HttpServletResponseWrapper` 包装响应 |
| 安全 | 拦截器绕过了某些路径 | `excludePathPatterns` 配置太宽泛 | 精确配置排除路径，避免放过敏感接口 |

---

## 本节小结

```
+-----------------------------------------------------------+
|                    Filter 与 Interceptor                   |
+-----------------------------------------------------------+
|                                                           |
|   +----------------+     +----------------+              |
|   |    Filter      |     |  Interceptor   |              |
|   |  (Servlet规范)  |     |  (Spring MVC)  |              |
|   +----------------+     +----------------+              |
|   | - 更底层        |     | - 更贴近业务    |              |
|   | - 不能注入Bean  |     | - 可以注入Bean  |              |
|   | - 所有Web项目   |     | - 仅Spring项目  |              |
|   +----------------+     +----------------+              |
|            |                       |                      |
|            +-----------+-----------+                      |
|                        |                                  |
|                        v                                  |
|              +--------------------+                       |
|              |    执行顺序         |                       |
|              |  Filter1 → Filter2  |                       |
|              |       ↓            |                       |
|              |  DispatcherServlet  |                       |
|              |       ↓            |                       |
|              |  Interceptor1       |                       |
|              |       ↓            |                       |
|              |  Interceptor2       |                       |
|              |       ↓            |                       |
|              |   Controller        |                       |
|              |       ↓            |                       |
|              |  Interceptor2(返回) |                       |
|              |  Interceptor1(返回) |                       |
|              |  Filter2(返回)      |                       |
|              |  Filter1(返回)      |                       |
|              +--------------------+                       |
|                                                           |
|   组合使用建议：                                            |
|   - Filter：编码、跨域、压缩                                |
|   - Interceptor：登录校验、权限校验、日志、耗时统计           |
|                                                           |
+-----------------------------------------------------------+
```

---

## 参考文档

- [Servlet Filter 官方文档](https://docs.oracle.com/javaee/7/api/javax/servlet/Filter.html)
- [Jakarta Servlet Filter 文档](https://jakarta.ee/specifications/servlet/5.0/apidocs/jakarta/servlet/filter)
- [Spring HandlerInterceptor 文档](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-handlermapping-interceptor)
- [Spring Boot WebMvcConfigurer 配置](https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web.servlet.spring-mvc.auto-configuration)
