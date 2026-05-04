# Filter 与 Interceptor（版本 A 专属）

## 学习目标

- 理解 Filter 和 Interceptor 的区别和使用场景
- 掌握 Filter 实现登录校验的完整流程
- 能够根据业务需求选择合适的拦截机制

## 核心知识点

### 1. Filter 与 Interceptor 对比

| 特性 | Filter（过滤器） | Interceptor（拦截器） |
|------|----------------|---------------------|
| 所属规范 | Servlet 规范 | Spring MVC |
| 执行时机 | Servlet 之前 | Controller 方法前后 |
| 依赖容器 | 依赖 Web 容器 | 依赖 Spring 容器 |
| 获取 Bean | 不能直接获取 | 可以注入 Spring Bean |
| 使用范围 | 所有 Web 应用 | 仅 Spring 应用 |

执行顺序：请求 → Filter → DispatcherServlet → Interceptor → Controller → Interceptor → Filter → 响应

### 2. Filter 实现登录校验

**@WebFilter 注解方式**：

```java
@Slf4j
@WebFilter(urlPatterns = "/*")
public class TokenFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String url = request.getRequestURI();
        log.info("请求URL：{}", url);

        // 登录请求直接放行
        if (url.contains("login")) {
            chain.doFilter(req, res);
            return;
        }

        // 获取token
        String token = request.getHeader("token");
        if (!StringUtils.hasLength(token)) {
            log.info("token为空，返回未登录信息");
            response.getWriter().write(JSON.toJSONString(Result.error("NOT_LOGIN")));
            return;
        }

        // 校验token
        try {
            JwtUtils.parseToken(token);
            chain.doFilter(req, res);
        } catch (Exception e) {
            log.error("token校验失败：{}", e.getMessage());
            response.getWriter().write(JSON.toJSONString(Result.error("NOT_LOGIN")));
        }
    }
}
```

**启动类开启 Filter 扫描**：

```java
@SpringBootApplication
@ServletComponentScan  // 扫描 @WebFilter
public class TliasApplication {
    public static void main(String[] args) {
        SpringApplication.run(TliasApplication.class, args);
    }
}
```

### 3. Interceptor 实现登录校验

```java
@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String url = request.getRequestURI();
        if (url.contains("login")) {
            return true;
        }

        String token = request.getHeader("token");
        if (!StringUtils.hasLength(token)) {
            response.getWriter().write(JSON.toJSONString(Result.error("NOT_LOGIN")));
            return false;
        }

        try {
            JwtUtils.parseToken(token);
            return true;
        } catch (Exception e) {
            response.getWriter().write(JSON.toJSONString(Result.error("NOT_LOGIN")));
            return false;
        }
    }
}
```

**注册拦截器**：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TokenInterceptor tokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login");
    }
}
```

### 4. 如何选择

- **Filter**：适用于与 Spring 无关的通用处理（编码设置、跨域、压缩）
- **Interceptor**：适用于需要操作 Spring Bean 的场景（权限校验、日志记录）

本项目使用 Filter 实现登录校验，是因为教学简化。实际项目中，Interceptor 更方便注入 Service 做权限数据库查询。

## 动手练习

### 练习 1：Filter 放行静态资源

修改 TokenFilter，放行 `/uploads/**` 路径的图片访问请求。

### 练习 2：Interceptor 记录请求耗时

使用 Interceptor 的 `preHandle` 和 `afterCompletion` 方法，记录每个 Controller 方法的执行耗时。

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| @WebFilter 不生效 | 缺少 @ServletComponentScan | 在启动类添加该注解 |
| Interceptor 注入 Bean 为 null | 未交给 Spring 管理 | 加 @Component 注解 |
| 返回 NOT_LOGIN 但前端收不到 | 响应 Content-Type 不对 | 设置 response.setContentType("application/json") |

## 本节小结

Filter 和 Interceptor 都是请求拦截机制，Filter 更接近底层，Interceptor 更贴近 Spring。版本 A 使用 Filter + JWT 实现登录校验，是学习 Web 安全的基础。版本 B 使用 Sa-Token 框架，功能更完善。

## 参考文档

- [Servlet Filter 文档](https://docs.oracle.com/javaee/7/api/javax/servlet/Filter.html)
- [Spring HandlerInterceptor 文档](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-handlermapping-interceptor)
