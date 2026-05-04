# 事务管理与 AOP 操作日志

## 学习目标

- 理解数据库事务的 ACID 特性
- 掌握 Spring `@Transactional` 注解的使用
- 理解 AOP 面向切面编程的核心概念
- 能够使用 AOP 记录操作日志

## 核心知识点

### 1. 什么是事务

事务是一组操作的集合，要么全部成功，要么全部失败。典型场景：转账操作（A 扣款 + B 收款必须同时成功）。

**ACID 特性**：
- **A**tomicity（原子性）：要么全成功，要么全回滚
- **C**onsistency（一致性）：数据状态始终合法
- **I**solation（隔离性）：并发事务互不干扰
- **D**urability（持久性）：提交后数据永久保存

### 2. Spring 事务管理

Spring 提供声明式事务，只需一个注解：

```java
@Transactional(rollbackFor = Exception.class)
public void save(Emp emp) {
    empMapper.insert(emp);
    if (emp.getExprList() != null && !emp.getExprList().isEmpty()) {
        for (EmpExpr expr : emp.getExprList()) {
            expr.setEmpId(emp.getId());
        }
        empExprMapper.insertBatch(emp.getExprList());
    }
}
```

**关键参数**：
- `rollbackFor`：指定哪些异常触发回滚（默认只回滚 RuntimeException）
- `propagation`：事务传播行为（默认 REQUIRED）
- `isolation`：事务隔离级别（默认数据库默认级别）

**事务失效的常见场景**：
- 非 public 方法
- 同一个类中方法互相调用（this 调用不走代理）
- 异常被 catch 吞掉
- 异步方法（@Async）

### 3. AOP 核心概念

AOP（Aspect Oriented Programming，面向切面编程）将**横切关注点**（如日志、权限、事务）从业务逻辑中分离。

| 术语 | 说明 |
|------|------|
| 切面（Aspect） | 横切关注点的模块化，如日志切面 |
| 连接点（JoinPoint） | 程序执行过程中的某个点，如方法调用 |
| 通知（Advice） | 切面的具体动作，如 Before、After、Around |
| 切入点（Pointcut） | 匹配连接点的表达式，如 `@annotation(com.tlias.anno.Log)` |
| 目标对象（Target） | 被代理的原始对象 |

### 4. 使用 AOP 记录操作日志

**步骤 1：自定义注解**

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
}
```

**步骤 2：编写切面**

```java
@Slf4j
@Aspect
@Component
public class LogAspect {

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Autowired
    private HttpServletRequest request;

    @Around("@annotation(com.tlias.anno.Log)")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long begin = System.currentTimeMillis();

        // 执行目标方法
        Object result = joinPoint.proceed();

        long end = System.currentTimeMillis();

        // 记录操作日志
        OperateLog operateLog = new OperateLog();
        operateLog.setOperateEmpId(getCurrentEmpId());
        operateLog.setOperateTime(LocalDateTime.now());
        operateLog.setClassName(joinPoint.getTarget().getClass().getName());
        operateLog.setMethodName(joinPoint.getSignature().getName());
        operateLog.setMethodParams(Arrays.toString(joinPoint.getArgs()));
        operateLog.setReturnValue(JSON.toJSONString(result));
        operateLog.setCostTime(end - begin);

        operateLogMapper.insert(operateLog);
        return result;
    }

    private Integer getCurrentEmpId() {
        try {
            String token = request.getHeader("token");
            if (token != null) {
                return JwtUtils.getEmpId(token);
            }
        } catch (Exception e) {
            log.warn("获取当前用户ID失败");
        }
        return null;
    }
}
```

**步骤 3：在需要记录日志的方法上加注解**

```java
@Log
@PostMapping
public Result save(@RequestBody Dept dept) { ... }

@Log
@DeleteMapping("/{id}")
public Result delete(@PathVariable Integer id) { ... }
```

### 5. 五种通知类型

```java
@Before("切入点")       // 方法执行前
@After("切入点")        // 方法执行后（无论是否异常）
@AfterReturning("切入点") // 方法正常返回后
@AfterThrowing("切入点")  // 方法抛出异常后
@Around("切入点")       // 环绕通知（最强，可以控制方法是否执行）
```

## 动手练习

### 练习 1：事务回滚测试

1. 在 `save` 方法中故意让 `empExprMapper.insertBatch` 抛出异常
2. 观察 `empMapper.insert` 的数据是否被回滚
3. 去掉 `@Transactional` 再次测试，观察是否出现脏数据

### 练习 2：AOP 日志扩展

在操作日志中增加请求 IP 地址的记录：

```java
operateLog.setIp(request.getRemoteAddr());
```

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| @Transactional 不生效 | 同类方法互相调用 | 注入自身代理对象，或拆分到另一个类 |
| 事务不回滚 | 异常被 catch | 在 catch 中重新抛出异常 |
| AOP 不生效 | 目标对象未被 Spring 代理 | 确保类被 @Component 等注解标记 |
| 日志记录阻塞主流程 | 日志保存耗时 | 改为异步记录（@Async） |

## 本节小结

`@Transactional` 让事务管理变得声明式、无侵入。AOP 将日志记录从业务代码中剥离，通过 `@Around` 环绕通知在方法执行前后插入日志逻辑，实现了真正的关注点分离。

## 参考文档

- [Spring 事务管理官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)
- [Spring AOP 官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop)

