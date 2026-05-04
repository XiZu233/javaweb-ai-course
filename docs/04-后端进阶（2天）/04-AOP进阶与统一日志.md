# AOP 进阶与统一日志

## 学习目标

- 理解 AOP 的底层实现原理（JDK 动态代理 vs CGLIB），知道 Spring 如何选择代理方式
- 掌握切入点表达式 `execution` 和 `@annotation` 的完整语法，能够写出精确的切点
- 能够使用 AOP 实现统一的请求日志记录，包括 URL、参数、响应、耗时
- 掌握日志级别规范和日志脱敏处理（密码、手机号打码）
- 理解 AOP 的注意事项和常见陷阱（同类调用、final 方法等）

---

## 核心知识点

### 1. 为什么需要 AOP

#### 1.1 横切关注点的困扰

在实际开发中，有很多功能是"横切"在业务逻辑之上的：

```
业务方法 A：查询员工列表
  - 记录操作日志 ← 横切逻辑
  - 检查用户权限 ← 横切逻辑
  - 统计执行耗时 ← 横切逻辑
  - 真正查询数据库 ← 核心业务

业务方法 B：新增员工
  - 记录操作日志 ← 横切逻辑
  - 检查用户权限 ← 横切逻辑
  - 统计执行耗时 ← 横切逻辑
  - 真正插入数据库 ← 核心业务
```

如果没有 AOP，每个方法都要写一遍日志、权限、耗时统计，代码严重重复。AOP 的作用就是把这些横切逻辑**抽离出来，统一处理**。

**真实场景类比**：
- 没有 AOP：每个员工（方法）上班都要自己打卡、自己算考勤、自己写日报——重复劳动。
- 有 AOP：公司装了一个智能系统（AOP），所有人自动打卡、自动算考勤、自动生成日报——员工专心做业务。

#### 1.2 AOP 的核心概念

| 术语 | 英文 | 类比理解 |
|------|------|---------|
| 切面 | Aspect | 智能考勤系统（包含所有横切逻辑的模块） |
| 连接点 | JoinPoint | 每个员工可以被打卡的位置（方法执行前、后、异常时） |
| 切入点 | Pointcut | 考勤规则：哪些员工需要打卡（匹配哪些方法） |
| 通知 | Advice | 打卡动作本身（在什么时候做什么） |
| 目标对象 | Target | 被考勤的员工（被代理的原始对象） |
| 代理 | Proxy | 替身员工（代理对象，先执行打卡再执行工作） |

---

### 2. AOP 底层原理：动态代理

AOP 不是魔法，它的底层是**动态代理**——在运行时动态生成一个代理类，代替原始类接收调用。

#### 2.1 两种代理方式

```
+-----------------------------------------------------------+
|                    动态代理的两种方式                        |
+-----------------------------------------------------------+
|                                                           |
|   +--------------------+     +--------------------+      |
|   |   JDK 动态代理      |     |   CGLIB 代理        |      |
|   +--------------------+     +--------------------+      |
|   | 前提：目标类实现了接口 |     | 前提：目标类无接口    |      |
|   |                    |     |                     |      |
|   | 原理：生成接口的     |     | 原理：生成目标类的    |      |
|   | 实现类作为代理       |     | 子类作为代理         |      |
|   |                    |     |                     |      |
|   |  UserService       |     |   UserService       |      |
|   |      ↑             |     |      ↑              |      |
|   |  $Proxy0           |     |  UserService$$Enhancer |   |
|   | (实现 UserService)  |     | (继承 UserService)   |      |
|   +--------------------+     +--------------------+      |
|                                                           |
|   SpringBoot 2.x 默认：CGLIB（即使实现了接口）              |
|   SpringBoot 3.x 默认：优先 JDK 动态代理（实现了接口时）     |
|                                                           |
+-----------------------------------------------------------+
```

#### 2.2 JDK 动态代理示例

```java
package com.tlias.demo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * JDK 动态代理演示
 * 目标类必须实现接口
 */
public class JdkProxyDemo {

    // 定义一个接口
    public interface UserService {
        void addUser(String name);
    }

    // 目标类：实现接口
    public static class UserServiceImpl implements UserService {
        @Override
        public void addUser(String name) {
            System.out.println("【目标方法】添加用户：" + name);
        }
    }

    public static void main(String[] args) {
        // 创建目标对象
        UserService target = new UserServiceImpl();

        // ========================================
        // 创建代理对象
        // ========================================
        // Proxy.newProxyInstance 参数说明：
        // 参数1：类加载器，用目标类的类加载器
        // 参数2：代理类要实现的接口数组
        // 参数3：InvocationHandler，调用处理器（核心逻辑）
        UserService proxy = (UserService) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),      // 类加载器
                target.getClass().getInterfaces(),       // 实现的接口
                new InvocationHandler() {                 // 调用处理器
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        // proxy：代理对象本身（一般不用）
                        // method：被调用的方法对象
                        // args：方法参数

                        System.out.println("【代理】方法执行前：记录日志");

                        // 调用目标对象的原始方法
                        // method.invoke(target, args) 就是执行真正的业务逻辑
                        Object result = method.invoke(target, args);

                        System.out.println("【代理】方法执行后：记录日志");

                        return result;
                    }
                }
        );

        // 通过代理对象调用方法
        // 实际上执行的是 InvocationHandler.invoke()
        proxy.addUser("张三");
    }
}
```

**输出**：
```
【代理】方法执行前：记录日志
【目标方法】添加用户：张三
【代理】方法执行后：记录日志
```

#### 2.3 CGLIB 代理示例

```java
package com.tlias.demo;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * CGLIB 代理演示
 * 目标类不需要实现接口，通过生成子类来代理
 */
public class CglibProxyDemo {

    // 目标类：没有实现任何接口
    public static class UserService {
        public void addUser(String name) {
            System.out.println("【目标方法】添加用户：" + name);
        }
    }

    public static void main(String[] args) {
        // 创建 Enhancer，CGLIB 的核心类
        Enhancer enhancer = new Enhancer();

        // 设置父类（目标类）
        enhancer.setSuperclass(UserService.class);

        // 设置回调（方法拦截器）
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                // obj：生成的子类对象
                // method：被调用的方法
                // args：方法参数
                // proxy：方法代理，用于调用父类方法

                System.out.println("【CGLIB代理】方法执行前：记录日志");

                // 调用父类（目标类）的方法
                // proxy.invokeSuper(obj, args) 是 CGLIB 的关键
                Object result = proxy.invokeSuper(obj, args);

                System.out.println("【CGLIB代理】方法执行后：记录日志");

                return result;
            }
        });

        // 创建代理对象
        UserService proxy = (UserService) enhancer.create();

        // 调用方法
        proxy.addUser("李四");
    }
}
```

#### 2.4 Spring 如何选择代理方式

| 条件 | SpringBoot 2.x | SpringBoot 3.x |
|------|---------------|----------------|
| 目标类实现了接口 | CGLIB（默认） | JDK 动态代理（默认） |
| 目标类没有接口 | CGLIB | CGLIB |
| 强制使用 CGLIB | `spring.aop.proxy-target-class=true`（默认） | 可配置 |
| 强制使用 JDK 代理 | 设置上述为 false | 默认行为 |

---

### 3. 切入点表达式详解

切入点表达式（Pointcut Expression）用于精确匹配需要拦截的方法。

#### 3.1 execution 表达式语法

```
execution(修饰符? 返回值 包名.类名.方法名(参数) throws 异常?)
```

| 符号 | 含义 | 示例 |
|------|------|------|
| `*` | 任意匹配 | `* com.tlias..*.*(..)` |
| `..` | 包下任意子包，或方法任意参数 | `com.tlias..*` 匹配所有子包 |
| `+` | 当前类及其子类 | `com.tlias.service.UserService+` |

**常用 execution 示例**：

```java
/**
 * 切入点表达式示例
 */
@Aspect
@Component
public class PointcutDemo {

    // ========================================
    // 示例1：匹配所有 Controller 类的方法
    // ========================================
    // *：任意返回值
    // com.tlias.controller：包名
    // *：任意类
    // *：任意方法
    // (..)：任意参数
    @Pointcut("execution(* com.tlias.controller.*.*(..))")
    public void controllerPointcut() {}

    // ========================================
    // 示例2：匹配 Service 层所有以 get 开头的方法
    // ========================================
    @Pointcut("execution(* com.tlias.service.*.get*(..))")
    public void serviceGetPointcut() {}

    // ========================================
    // 示例3：匹配指定类的所有方法
    // ========================================
    @Pointcut("execution(* com.tlias.service.EmpService.*(..))")
    public void empServicePointcut() {}

    // ========================================
    // 示例4：匹配所有返回值为 Result 的方法
    // ========================================
    @Pointcut("execution(com.tlias.pojo.Result *(..))")
    public void resultReturnPointcut() {}

    // ========================================
    // 示例5：匹配第一个参数为 String 的方法
    // ========================================
    // (String, ..)：第一个参数是 String，后面任意
    @Pointcut("execution(* *(String, ..))")
    public void firstParamStringPointcut() {}

    // ========================================
    // 示例6：匹配所有 public 方法
    // ========================================
    @Pointcut("execution(public * *(..))")
    public void publicMethodPointcut() {}

    // ========================================
    // 示例7：匹配所有 Service 层及其子包的方法
    // ========================================
    // com.tlias.service..*：service 包及其所有子包
    @Pointcut("execution(* com.tlias.service..*.*(..))")
    public void serviceAllPointcut() {}

    // ========================================
    // 示例8：组合切入点（与、或、非）
    // ========================================
    // &&：与（同时满足）
    // ||：或（满足一个）
    // !：非（排除）
    @Pointcut("controllerPointcut() && !loginPointcut()")
    public void controllerExceptLogin() {}
}
```

#### 3.2 @annotation 表达式

`@annotation` 用于匹配被特定注解标记的方法，这种方式更灵活、更精确。

```java
/**
 * 自定义日志注解
 * 标记在需要记录日志的方法上
 */
@Target(ElementType.METHOD)  // 注解只能用在方法上
@Retention(RetentionPolicy.RUNTIME)  // 注解在运行时保留（AOP 需要）
public @interface OperationLog {
    // 操作描述
    String value() default "";
    // 操作类型
    String type() default "OTHER";
}
```

```java
@Aspect
@Component
public class LogAspect {

    // ========================================
    // 匹配所有标记了 @OperationLog 注解的方法
    // ========================================
    @Pointcut("@annotation(com.tlias.anno.OperationLog)")
    public void operationLogPointcut() {}

    // ========================================
    // 匹配指定注解类型（全限定名）
    // ========================================
    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public void getMappingPointcut() {}

    // ========================================
    // 匹配类上标记了 @RestController 的所有方法
    // ========================================
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void restControllerPointcut() {}
}
```

**execution vs @annotation 对比**：

| 方式 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| execution | 按包/类/方法名匹配，批量拦截 | 不够精确，可能拦截到不想拦截的方法 | 统一日志、统一异常处理 |
| @annotation | 精确控制哪些方法被拦截 | 需要给每个方法加注解 | 操作日志、权限校验 |

---

### 4. 五种通知类型

```
+-----------------------------------------------------------+
|                     五种通知的执行顺序                       |
+-----------------------------------------------------------+
|                                                           |
|   @Around 开始                                             |
|     |                                                     |
|     |-- @Before                                           |
|     |     |                                               |
|     |     |-- 目标方法执行                                 |
|     |     |                                               |
|     |-- @AfterReturning（方法正常返回）或                   |
|     |   @AfterThrowing（方法抛出异常）                     |
|     |                                                     |
|     |-- @After（无论是否异常都执行）                        |
|     |                                                     |
|   @Around 结束（返回结果）                                 |
|                                                           |
+-----------------------------------------------------------+
```

```java
package com.tlias.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/**
 * 完整通知示例
 * 演示五种通知类型的使用
 */
@Slf4j
@Aspect      // 声明这是一个切面类
@Component   // 交给 Spring 管理
public class CompleteAspect {

    // ========================================
    // 定义切入点：匹配 Service 层所有方法
    // ========================================
    @Pointcut("execution(* com.tlias.service.*.*(..))")
    public void servicePointcut() {}

    /**
     * @Before：目标方法执行前执行
     * 不能阻止方法执行（除非抛异常）
     */
    @Before("servicePointcut()")
    public void before(JoinPoint joinPoint) {
        // JoinPoint 包含连接点的信息
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getName();
        log.info("【@Before】{}.{} 准备执行", className, methodName);
    }

    /**
     * @AfterReturning：目标方法正常返回后执行
     * returning 参数绑定方法的返回值
     */
    @AfterReturning(pointcut = "servicePointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        log.info("【@AfterReturning】{} 返回结果：{}", methodName, result);
    }

    /**
     * @AfterThrowing：目标方法抛出异常后执行
     * throwing 参数绑定抛出的异常
     */
    @AfterThrowing(pointcut = "servicePointcut()", throwing = "e")
    public void afterThrowing(JoinPoint joinPoint, Exception e) {
        String methodName = joinPoint.getSignature().getName();
        log.error("【@AfterThrowing】{} 发生异常：{}", methodName, e.getMessage());
    }

    /**
     * @After：目标方法执行后执行（无论是否异常）
     * 类似 finally 块
     */
    @After("servicePointcut()")
    public void after(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        log.info("【@After】{} 执行完毕（无论是否异常）", methodName);
    }

    /**
     * @Around：环绕通知，包裹整个方法执行
     * 最强大的通知，可以控制是否执行目标方法
     * 必须有返回值，且必须返回 joinPoint.proceed() 的结果
     */
    @Around("servicePointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();

        log.info("【@Around】{} 执行前", methodName);

        // 记录开始时间
        long start = System.currentTimeMillis();

        // ========================================
        // joinPoint.proceed() 是执行目标方法的关键
        // 如果不调用，目标方法不会执行
        // 可以多次调用（但不推荐）
        // ========================================
        Object result = joinPoint.proceed();

        // 记录结束时间
        long end = System.currentTimeMillis();

        log.info("【@Around】{} 执行后，耗时：{}ms", methodName, end - start);

        // 必须返回结果，否则调用者收到 null
        return result;
    }
}
```

---

### 5. 统一日志切面实现

#### 5.1 请求日志切面

```java
package com.tlias.aspect;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 统一请求日志切面
 * 记录每个请求的 URL、IP、方法、参数、响应、耗时
 */
@Slf4j
@Aspect
@Component
public class RequestLogAspect {

    // ========================================
    // 切入点：匹配所有 Controller 层的方法
    // ========================================
    @Pointcut("execution(* com.tlias.controller.*.*(..))")
    public void controllerPointcut() {}

    /**
     * 环绕通知：记录请求和响应的完整信息
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

        // ========================================
        // 第1步：获取请求信息
        // ========================================
        // RequestContextHolder 是 Spring 提供的工具类
        // 可以获取当前线程绑定的请求属性
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        // 获取 HttpServletRequest 对象
        HttpServletRequest request = attributes.getRequest();

        // 获取请求路径
        String url = request.getRequestURI();
        // 获取请求方法（GET/POST/PUT/DELETE）
        String httpMethod = request.getMethod();
        // 获取客户端 IP
        String ip = getClientIp(request);
        // 获取目标类名和方法名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();

        // ========================================
        // 第2步：获取请求参数
        // ========================================
        Object[] args = joinPoint.getArgs();
        // 将参数转为 JSON 字符串（方便查看）
        // 注意：这里可能需要过滤敏感参数
        String params = JSON.toJSONString(args);

        // ========================================
        // 第3步：打印请求日志
        // ========================================
        log.info("========== 请求开始 ==========");
        log.info("URL        : {} {}", httpMethod, url);
        log.info("IP         : {}", ip);
        log.info("ClassMethod: {}.{}", className, methodName);
        log.info("Params     : {}", params);

        // ========================================
        // 第4步：执行目标方法，记录耗时
        // ========================================
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        // ========================================
        // 第5步：打印响应日志
        // ========================================
        log.info("Response   : {}", JSON.toJSONString(result));
        log.info("Duration   : {}ms", duration);
        log.info("========== 请求结束 ==========");

        return result;
    }

    /**
     * 获取客户端真实 IP
     * 考虑代理服务器的情况（如 Nginx）
     */
    private String getClientIp(HttpServletRequest request) {
        // X-Forwarded-For 是代理服务器添加的头部，记录原始客户端 IP
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
```

#### 5.2 日志级别规范

```yaml
# application.yml 中配置日志级别
logging:
  level:
    # root 级别：WARN（生产环境只记录警告和错误）
    root: WARN
    # 自己的包：INFO（记录正常业务日志）
    com.tlias: INFO
    # SQL 日志：DEBUG（开发环境看 SQL）
    com.tlias.mapper: DEBUG
```

| 日志级别 | 使用场景 | 生产环境建议 |
|---------|---------|------------|
| TRACE | 最详细的跟踪信息，如方法入参出参 | 关闭 |
| DEBUG | 调试信息，如 SQL 语句、变量值 | 关闭 |
| INFO | 正常业务日志，如请求记录、操作记录 | 开启 |
| WARN | 警告信息，如参数异常、业务规则冲突 | 开启 |
| ERROR | 错误信息，如数据库连接失败、空指针 | 开启 |

#### 5.3 日志脱敏处理

日志中不能打印密码、手机号、身份证号等敏感信息，需要脱敏处理。

```java
package com.tlias.utils;

/**
 * 脱敏工具类
 * 对敏感信息进行脱敏处理
 */
public class DesensitizationUtil {

    /**
     * 密码脱敏：全部替换为 ******
     */
    public static String desensitizePassword(String password) {
        if (password == null || password.isEmpty()) {
            return password;
        }
        return "******";
    }

    /**
     * 手机号脱敏：138****8888
     */
    public static String desensitizePhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        // 保留前3位和后4位，中间用 **** 替换
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 身份证号脱敏：110101********1234
     */
    public static String desensitizeIdCard(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return idCard;
        }
        // 保留前6位和后4位
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    /**
     * 邮箱脱敏：a***@qq.com
     */
    public static String desensitizeEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 1) {
            return email;
        }
        return name.charAt(0) + "***@" + domain;
    }
}
```

```java
/**
 * 在日志切面中使用脱敏
 */
@Around("controllerPointcut()")
public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    Object[] args = joinPoint.getArgs();

    // 对参数进行脱敏处理
    Object[] desensitizedArgs = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
        desensitizedArgs[i] = desensitizeObject(args[i]);
    }

    String params = JSON.toJSONString(desensitizedArgs);
    log.info("Params     : {}", params);

    // ...
}

/**
 * 对对象中的敏感字段进行脱敏
 */
private Object desensitizeObject(Object obj) {
    if (obj == null) {
        return null;
    }
    // 如果是字符串，直接返回（基本类型参数）
    if (obj instanceof String) {
        return obj;
    }
    // 如果是 DTO/VO 对象，需要递归处理字段
    // 实际项目中可以使用注解标记敏感字段，通过反射自动脱敏
    return obj;
}
```

---

### 6. AOP 注意事项

#### 6.1 同类方法互相调用不走代理

```java
@Service
public class EmpService {

    public void methodA() {
        // 这里调用 methodB 不会触发 AOP！
        // 因为 this.methodB() 是通过原始对象调用的，不是代理对象
        this.methodB();
    }

    public void methodB() {
        // 假设有 @Around 切面拦截 methodB
        // 通过 this 调用时，切面不会生效
    }
}
```

**解决方案**：
```java
@Service
public class EmpService {

    // 注入自身的代理对象
    @Autowired
    private EmpService self;

    public void methodA() {
        // 通过代理对象调用，AOP 生效
        self.methodB();
    }

    public void methodB() {
        // 切面会生效
    }
}
```

#### 6.2 final 方法和 final 类无法被代理

- CGLIB 通过生成子类来代理，final 方法不能被重写，所以无法代理
- final 类不能被继承，所以 CGLIB 无法生成子类

#### 6.3 私有方法无法被代理

- JDK 动态代理基于接口，接口没有私有方法
- CGLIB 基于子类，私有方法对子类不可见

---

## 动手练习

### 练习 1：性能监控切面

**目标**：编写一个 AOP 切面，统计所有 Service 方法的执行时间，超过 100ms 的打印警告日志。

**参考代码**：

```java
@Slf4j
@Aspect
@Component
public class PerformanceAspect {

    @Around("execution(* com.tlias.service.*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        if (duration > 100) {
            log.warn("【性能警告】{}.{} 执行耗时 {}ms",
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    duration);
        }
        return result;
    }
}
```

### 练习 2：操作日志切面

**目标**：使用自定义注解 `@OperationLog`，在方法执行后记录操作日志到数据库。

**步骤**：
1. 创建 `@OperationLog` 注解（上面已有示例）
2. 在需要记录日志的 Controller 方法上加注解：`@OperationLog("新增员工")`
3. 编写切面，拦截 `@OperationLog` 注解标记的方法
4. 在 `@AfterReturning` 中将操作信息保存到数据库

### 练习 3：切入点表达式练习

**目标**：写出匹配以下方法的切入点表达式。

1. 匹配 `com.tlias.controller` 包下所有类的所有方法
2. 匹配 `com.tlias.service` 包及其子包下所有以 `get` 开头的方法
3. 匹配所有返回类型为 `Result` 的方法
4. 匹配所有标记了 `@PostMapping` 注解的方法
5. 匹配 `EmpService` 类中参数为 `Integer` 类型的方法

---

## 常见错误排查

### 依赖配置问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 编译期 | `@Aspect` 找不到 | 缺少 spring-boot-starter-aop 依赖 | 添加依赖：`spring-boot-starter-aop` |
| 编译期 | `ProceedingJoinPoint` 找不到 | 缺少 aspectjweaver | Spring Boot Starter 已包含，检查依赖是否完整 |
| 启动期 | 切面类没有生效 | 类没有被 Spring 扫描 | 确认加了 `@Component`，且在扫描路径下 |

### 参数请求问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 运行期 | AOP 不生效，没有日志输出 | 切入点表达式写错，没有匹配到方法 | 检查 execution 表达式，可用 `*` 通配测试 |
| 运行期 | AOP 拦截了不想拦截的方法 | 切入点表达式太宽泛 | 缩小匹配范围，或使用 `@annotation` 精确控制 |
| 运行期 | `RequestContextHolder.getRequestAttributes()` 为 null | 在非 Web 线程中调用 | 确保在 Web 请求线程中执行 |

### 代码逻辑问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 运行期 | `@Around` 返回 null | 忘记 `return joinPoint.proceed()` 的结果 | 确保返回 proceed() 的结果 |
| 运行期 | 同类调用 AOP 失效 | `this.method()` 不走代理 | 注入自身代理对象再调用 |
| 运行期 | final 方法没有被拦截 | CGLIB 无法代理 final 方法 | 去掉 final 修饰符 |
| 运行期 | 私有方法没有被拦截 | 代理机制无法访问私有方法 | 将方法改为 public 或 protected |

### 性能安全问题

| 阶段 | 错误现象 | 原因分析 | 解决方案 |
|------|---------|---------|---------|
| 性能 | 日志打印太频繁影响性能 | 每个请求都打印大量日志 | 调整日志级别，生产环境关闭 DEBUG |
| 性能 | AOP 切面执行太慢 | 切面中做了数据库查询等耗时操作 | 切面只做轻量操作，数据库操作异步处理 |
| 安全 | 日志中打印了用户密码 | 没有脱敏处理 | 使用 DesensitizationUtil 对敏感字段脱敏 |
| 安全 | 日志文件泄露敏感信息 | 日志文件权限设置不当 | 限制日志文件访问权限，定期清理 |

---

## 本节小结

```
+-----------------------------------------------------------+
|                  AOP 进阶与统一日志                        |
+-----------------------------------------------------------+
|                                                           |
|   +----------------+     +----------------+              |
|   |   为什么用 AOP  |     |  横切关注点    |              |
|   |   日志/权限/监控 | --> |  代码复用      |              |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   |   底层原理      | --> |  JDK 动态代理  |              |
|   |   动态代理      |     |  CGLIB 代理    |              |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   |   切入点表达式   | --> |  execution()   |              |
|   |   Pointcut      |     |  @annotation() |              |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   |   五种通知      | --> |  @Before       |              |
|   |   Advice        |     |  @AfterReturning|             |
|   |                 |     |  @AfterThrowing |             |
|   |                 |     |  @After         |              |
|   |                 |     |  @Around        |              |
|   +----------------+     +----------------+              |
|            |                                              |
|            v                                              |
|   +----------------+     +----------------+              |
|   |   统一日志      | --> |  请求/响应记录  |              |
|   |   日志脱敏      |     |  耗时统计       |              |
|   +----------------+     +----------------+              |
|                                                           |
+-----------------------------------------------------------+
```

---

## 参考文档

- [Spring AOP 官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop)
- [AspectJ 切入点表达式语法](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop-pointcuts)
- [Spring Boot AOP 自动配置](https://docs.spring.io/spring-boot/docs/current/reference/html/aop.html)
- [logback 日志配置文档](https://logback.qos.ch/documentation.html)
