# Spring AOP 核心概念

## 学习目标

- 深入理解 AOP 的底层实现原理（动态代理）
- 掌握切入点表达式（Pointcut Expression）的写法
- 能够使用 AOP 实现通用的横切逻辑

## 核心知识点

### 1. AOP 底层原理：动态代理

Spring AOP 基于**动态代理**实现，有两种方式：

- **JDK 动态代理**：目标类实现了接口，基于接口生成代理类
- **CGLIB 代理**：目标类没有实现接口，基于子类生成代理类

Spring Boot 2.x 默认使用 CGLIB（即使实现了接口）。Spring Boot 3.x 默认优先使用 JDK 动态代理。

### 2. 切入点表达式

切入点表达式用于精确匹配需要拦截的方法：

```java
// 匹配所有 Controller 类的方法
@Pointcut("execution(* com.tlias.controller.*.*(..))")

// 匹配所有以 get 开头的方法
@Pointcut("execution(* get*(..))")

// 匹配指定注解标记的方法
@Pointcut("@annotation(com.tlias.anno.Log)")

// 匹配 Service 层所有方法
@Pointcut("execution(* com.tlias.service.*.*(..))")

// 组合切入点
@Pointcut("controllerPointcut() && !excludePointcut()")
```

**execution 表达式语法**：

```
execution(修饰符? 返回值 包名.类名.方法名(参数) throws 异常?)
```

| 符号 | 含义 |
|------|------|
| `*` | 任意匹配 |
| `..` | 包下任意子包，或方法任意参数 |
| `+` | 当前类及其子类 |

### 3. 五种通知的完整示例

```java
@Slf4j
@Aspect
@Component
public class CompleteAspect {

    @Pointcut("execution(* com.tlias.service.*.*(..))")
    public void servicePointcut() {}

    @Before("servicePointcut()")
    public void before(JoinPoint joinPoint) {
        log.info("【Before】方法准备执行：{}", joinPoint.getSignature().getName());
    }

    @AfterReturning(pointcut = "servicePointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        log.info("【AfterReturning】方法返回：{}，结果：{}",
                joinPoint.getSignature().getName(), result);
    }

    @AfterThrowing(pointcut = "servicePointcut()", throwing = "e")
    public void afterThrowing(JoinPoint joinPoint, Exception e) {
        log.error("【AfterThrowing】方法异常：{}，异常：{}",
                joinPoint.getSignature().getName(), e.getMessage());
    }

    @After("servicePointcut()")
    public void after(JoinPoint joinPoint) {
        log.info("【After】方法执行完毕（无论是否异常）：{}",
                joinPoint.getSignature().getName());
    }

    @Around("servicePointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("【Around】方法执行前");
        long start = System.currentTimeMillis();

        Object result = joinPoint.proceed();  // 执行目标方法

        long end = System.currentTimeMillis();
        log.info("【Around】方法执行后，耗时：{}ms", end - start);
        return result;
    }
}
```

**执行顺序**：

```
@Around 前半部分
  @Before
    目标方法执行
  @AfterReturning / @AfterThrowing
  @After
@Around 后半部分
```

### 4. AOP 获取请求信息

```java
@Aspect
@Component
public class RequestAspect {

    @Autowired
    private HttpServletRequest request;

    @Around("execution(* com.tlias.controller.*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求信息
        String url = request.getRequestURI();
        String method = request.getMethod();
        String ip = request.getRemoteAddr();

        // 获取方法参数
        Object[] args = joinPoint.getArgs();

        // 获取方法签名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();

        log.info("请求：{} {}，IP：{}，类：{}，方法：{}",
                method, url, ip, className, methodName);

        return joinPoint.proceed();
    }
}
```

### 5. AOP 注意事项

- **同类方法互相调用不走代理**：`this.method()` 不会触发 AOP
  - 解决：注入自身代理对象，或拆分到另一个类
- **final 方法 / final 类** 无法被 CGLIB 代理
- **私有方法** 无法被代理

## 动手练习

### 练习 1：性能监控切面

编写一个 AOP 切面，统计所有 Service 方法的执行时间，超过 100ms 的打印警告日志。

### 练习 2：请求日志切面

记录每个请求的 URL、方法、参数、响应结果、执行时间，保存到数据库或文件中。

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| AOP 不生效 | 目标对象未被 Spring 代理 | 确保通过 Spring 容器获取 Bean |
| 同类调用 AOP 失效 | this 调用不走代理 | 注入自身代理或使用 AspectJ |
| 返回 null | @Around 中未返回 joinPoint.proceed() 的结果 | 确保 return result |

## 本节小结

AOP 是 Spring 框架的核心特性之一，通过动态代理在不修改原有代码的情况下增强功能。掌握切入点表达式和五种通知类型，你就能灵活运用 AOP 解决日志、权限、性能监控等横切关注点问题。

## 参考文档

- [Spring AOP 官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop)

