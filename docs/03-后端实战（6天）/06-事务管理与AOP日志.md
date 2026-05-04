# 事务管理与 AOP 操作日志

## 学习目标

学完本节后，你将能够：

- 理解什么是数据库事务，以及 ACID 四个特性的含义
- 用 Spring 的 `@Transactional` 注解控制事务
- 知道事务失效的 6 种常见场景及解决方案
- 理解 AOP（面向切面编程）的核心概念
- 掌握 5 种通知类型的区别和使用场景
- 用 AOP + 自定义注解实现"无侵入"的操作日志记录

---

## 核心知识点

### 1. 为什么需要事务——"要么全成，要么全败"

#### 1.1 真实场景：银行转账

```
张三有 1000 元，李四有 500 元
张三要给李四转账 200 元

操作步骤：
  ① 从张三账户扣 200 元
  ② 给李四账户加 200 元

正常流程：
  张三：1000 → 800
  李四：500  → 700

异常流程（第二步失败）：
  张三：1000 → 800    ← 钱已经扣了！
  李四：500  → 500    ← 没收到！

  结果：200 元凭空消失了！
```

#### 1.2 事务的解决方案

把①②"捆绑"在一起：

- 如果第二步失败 → 第一步自动撤销（回滚）
- 只有两步都成功 → 才算完成（提交）

```
┌──────────────────────────────────────────────────────────┐
│                    事务执行流程                            │
├──────────────────────────────────────────────────────────┤
│                                                           │
│   BEGIN 开始事务                                          │
│     │                                                     │
│     ▼                                                     │
│   扣张三 200 元  ← 记录到 undo log（撤销日志）            │
│     │                                                     │
│     ▼                                                     │
│   加李四 200 元  ← 同样记录 undo log                      │
│     │                                                     │
│     ├─ 如果报错 ──► ROLLBACK 回滚                         │
│     │              张三：800 → 1000（恢复）                │
│     │              李四：700 → 500（恢复）                 │
│     │                                                     │
│     └─ 全部成功 ──► COMMIT 提交                           │
│                    数据永久生效                            │
│                                                           │
└──────────────────────────────────────────────────────────┘
```

#### 1.3 Tlias 系统中的事务场景

```
• 新增员工 + 保存工作经历（emp + emp_expr 两张表）
• 删除部门 + 转走员工（dept + emp 联动）
• 订单下单 + 扣减库存（oms + wms 多表）
• 任何"一次业务 = 多次数据库操作"的地方
```

---

### 2. 事务的 ACID 特性

| 特性 | 英文名 | 含义 | 通俗比喻 |
|------|--------|------|---------|
| **原子性** | Atomicity | 一组操作要么全成，要么全败 | 签合同，双方签完才生效，一方没签就作废 |
| **一致性** | Consistency | 事务前后，数据状态始终合法 | 转账前后，两人总金额不变 |
| **隔离性** | Isolation | 多个事务互不干扰 | 你在取钱，你老婆在存卡，柜员处理互不影响 |
| **持久性** | Durability | 提交后数据永久保存 | 转账确认后，即使银行停电也不会丢 |

---

### 3. Spring 声明式事务——`@Transactional`

#### 3.1 最简用法

```java
@Service
public class EmpServiceImpl implements EmpService {

    @Autowired
    private EmpMapper empMapper;
    @Autowired
    private EmpExprMapper empExprMapper;

    /**
     * 新增员工 + 工作经历
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(Emp emp) {
        // ① 插入员工
        empMapper.insert(emp);

        // ② 设置 emp_id
        if (emp.getExprList() != null) {
            emp.getExprList().forEach(e -> e.setEmpId(emp.getId()));
            // ③ 插入工作经历
            empExprMapper.insertBatch(emp.getExprList());
        }
    }
}
```

**`@Transactional` 做了什么？**

1. 方法执行前 → Spring 开启数据库连接，设为不自动提交（autoCommit=false）
2. 方法正常结束 → 调用 `commit()` 提交事务
3. 方法抛出异常 → 调用 `rollback()` 回滚所有操作

#### 3.2 参数详解

```java
@Transactional(
    rollbackFor = Exception.class,      // 哪些异常触发回滚
    propagation = Propagation.REQUIRED,  // 传播行为
    isolation = Isolation.READ_COMMITTED // 隔离级别
)
public void save(Emp emp) { ... }
```

**`rollbackFor`**：

```java
// 默认只回滚 RuntimeException 和 Error，不回滚受检异常
@Transactional                    // RuntimeException 才回滚
@Transactional(rollbackFor = Exception.class)  // 任何异常都回滚
```

**传播行为 Propagation**：

| 类型 | 含义 | 场景 |
|------|------|------|
| `REQUIRED`（默认） | 有事务就加入，没有就新建 | 大多数场景 |
| `REQUIRES_NEW` | 挂起当前事务，创建新事务 | 日志记录（不能影响主业务） |
| `SUPPORTS` | 有事务就加入，没有就算了 | 查询类方法 |
| `NOT_SUPPORTED` | 挂起当前事务，在无事务环境执行 | 定时任务 |
| `MANDATORY` | 必须有事务，否则报错 | 强制要求在事务内执行 |
| `NEVER` | 必须无事务，否则报错 | 查询统计 |

**隔离级别 Isolation**：

| 级别 | 效果 | 问题 |
|------|------|------|
| `READ_UNCOMMITTED` | 能读未提交数据 | 脏读（不推荐） |
| `READ_COMMITTED` | 只读已提交数据 | 不可重复读 |
| `REPEATABLE_READ`（MySQL 默认） | 事务内多次读一致 | 幻读 |
| `SERIALIZABLE` | 完全串行 | 性能极差 |

**新手建议：** 只关注 `REQUIRED`（默认）和 `REQUIRES_NEW`，隔离级别用数据库默认。

---

### 4. 事务失效的 6 种场景——90% 的人踩过

#### 4.1 第 1 种：同类方法互相调用

```java
@Service
public class EmpServiceImpl {

    // ✅ 外部调用 add  → 有事务
    @Transactional
    public void add(Emp emp) {
        save(emp);   // ❌ 内部 this.save() 不走代理，事务不生效！
    }

    @Transactional
    public void save(Emp emp) {
        empMapper.insert(emp);
    }
}
```

**原因：** Spring 事务通过 AOP 代理实现。类内方法互相调用时用的是 `this`（原对象），不是代理对象。

**解决：**

```java
@Autowired
private EmpServiceImpl self;   // 注入自己（代理对象）

@Transactional
public void add(Emp emp) {
    self.save(emp);   // ✅ 走代理，事务生效
}
```

#### 4.2 第 2 种：非 public 方法

```java
// ❌ 不生效
@Transactional
private void save(Emp emp) { ... }

// ❌ 不生效
@Transactional
protected void save(Emp emp) { ... }

// ✅ 生效
@Transactional
public void save(Emp emp) { ... }
```

#### 4.3 第 3 种：异常被吞掉了

```java
@Transactional
public void save(Emp emp) {
    try {
        empMapper.insert(emp);
        exprMapper.insert(...);   // 这里抛异常
    } catch (Exception e) {
        // ❌ 异常被 catch，Spring 感知不到，不会回滚！
        log.error("出错了", e);
    }
}
```

**解决：**

```java
catch (Exception e) {
    log.error("出错了", e);
    throw new RuntimeException("保存失败", e);   // ✅ 重新抛出
}
```

#### 4.4 第 4 种：异常类型不匹配

```java
@Transactional   // 默认只回滚 RuntimeException
public void save() throws SQLException {
    ...
    throw new SQLException("数据库错误");   // ❌ 受检异常，不回滚！
}

@Transactional(rollbackFor = Exception.class)  // ✅ 改成这样
public void save() throws SQLException {
    ...
    throw new SQLException("数据库错误");   // 回滚了
}
```

#### 4.5 第 5 种：数据库引擎不支持事务

```sql
-- ❌ MyISAM 引擎不支持事务
CREATE TABLE emp (...) ENGINE = MyISAM;

-- ✅ InnoDB 支持事务（默认引擎）
CREATE TABLE emp (...) ENGINE = InnoDB;
```

MySQL 5.5+ 默认就是 InnoDB，一般没问题。

#### 4.6 第 6 种：未开启事务管理

SpringBoot 2.x 默认已开启。如果是老项目：

```java
@EnableTransactionManagement   // 启动类加这个注解
@SpringBootApplication
public class TliasApplication { ... }
```

---

### 5. AOP 面向切面编程——"在不改代码的情况下加功能"

#### 5.1 类比：切蛋糕

```
业务代码就像一块完整的蛋糕：

  ┌──────────────────────────────────────────┐
  │          原始的 save() 方法               │
  │    ┌──────┐  ┌──────┐  ┌──────┐         │
  │    │      │  │      │  │      │         │
  │    │ 权限 │  │ save │  │ 日志 │         │
  │    │ 校验 │  │ 业务 │  │ 记录 │         │
  │    │      │  │      │  │      │         │
  │    └──────┘  └──────┘  └──────┘         │
  │         ↑          ↑          ↑          │
  │     Before     Proceed     After        │
  └──────────────────────────────────────────┘

AOP 做的事情：
  你只需写 "save 业务逻辑"
  AOP 自动帮你把"权限校验"和"日志记录"切进去
```

#### 5.2 核心术语

| 术语 | 英文 | 含义 | 类比 |
|------|------|------|------|
| **切面** | Aspect | 横切关注点的模块化 | 切蛋糕的工具（刀） |
| **连接点** | JoinPoint | 程序执行中可能插入切面的点 | 蛋糕的每一层切面 |
| **切入点** | Pointcut | 定义哪些连接点要切 | 按什么标准切（如"所有 save 方法"） |
| **通知** | Advice | 切面上要执行的动作 | 切进去后放什么馅料 |
| **目标对象** | Target | 被代理的原始对象 | 原始蛋糕 |

#### 5.3 五种通知类型

```
┌────────────────────────────────────────────────────────────────┐
│                      方法执行的时间线                           │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│   @Before ──►  目标方法执行前                                    │
│                  │                                              │
│   @Around ──►   │  ── 前置代码                                  │
│                  │                                              │
│                  │  joinPoint.proceed()  ← 执行业务方法         │
│                  │                                              │
│   @AfterReturning ──► 正常返回后                                 │
│   @AfterThrowing  ──► 异常抛出后                                 │
│                  │                                              │
│   @Around ──►    │  ── 后置代码                                 │
│                  │                                              │
│   @After ──►    方法结束后（无论是否异常）                       │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

| 通知 | 执行时机 | 能否阻止方法执行 | 使用场景 |
|------|---------|----------------|---------|
| `@Before` | 方法前 | 否 | 参数校验、初始化 |
| `@After` | 方法后（最终） | 否 | 资源释放 |
| `@AfterReturning` | 方法正常返回后 | 否 | 成功处理（修改返回值） |
| `@AfterThrowing` | 方法抛出异常后 | 否 | 异常处理 |
| `@Around` | 方法前后 | 能 | 日志、权限、性能统计（最常用） |

---

### 6. 完整实战：AOP 记录操作日志

#### 6.1 需求

系统需要记录"谁在什么时间做了什么操作"，但不能侵入每个 Controller：

```
需求：
  • 每个增删改操作都要记录日志
  • 记录：操作人、时间、接口名、参数、返回值、耗时
  • 不能在每个 Controller 里写重复的日志代码

方案：
  ① 自定义注解 @Log
  ② 在需要记录的方法上加 @Log
  ③ AOP 拦截带 @Log 注解的方法，自动记录
```

#### 6.2 数据库表

```sql
CREATE TABLE operate_log (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    operate_emp_id  INT,                        -- 操作人ID
    operate_time    DATETIME,                   -- 操作时间
    class_name      VARCHAR(255),               -- 类名
    method_name     VARCHAR(255),               -- 方法名
    method_params   VARCHAR(2000),              -- 请求参数
    return_value    VARCHAR(2000),              -- 返回值
    cost_time       BIGINT,                     -- 耗时(ms)
    ip              VARCHAR(50)                 -- IP地址
);
```

#### 6.3 步骤 1：自定义注解

```java
package com.tlias.anno;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标记在需要记录日志的方法上
 */
@Target(ElementType.METHOD)              // 只能用在方法上
@Retention(RetentionPolicy.RUNTIME)      // 运行时保留，AOP 才能读取到
public @interface Log {
}
```

#### 6.4 步骤 2：编写切面

```java
package com.tlias.aop;

import com.alibaba.fastjson.JSON;
import com.tlias.mapper.OperateLogMapper;
import com.tlias.pojo.OperateLog;
import com.tlias.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Aspect          // 标记这是一个切面
@Component       // 交给 Spring 管理
public class LogAspect {

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Autowired
    private HttpServletRequest request;

    /**
     * 环绕通知：拦截所有带有 @Log 注解的方法
     */
    @Around("@annotation(com.tlias.anno.Log)")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long begin = System.currentTimeMillis();  // 记录开始时间

        // 执行目标方法（ proceed = 前进/继续 ）
        Object result = joinPoint.proceed();

        long end = System.currentTimeMillis();    // 记录结束时间

        // 组装日志数据
        OperateLog operateLog = new OperateLog();
        operateLog.setOperateEmpId(getCurrentEmpId());
        operateLog.setOperateTime(LocalDateTime.now());
        operateLog.setClassName(joinPoint.getTarget().getClass().getName());
        operateLog.setMethodName(joinPoint.getSignature().getName());
        operateLog.setMethodParams(Arrays.toString(joinPoint.getArgs()));
        operateLog.setReturnValue(JSON.toJSONString(result));
        operateLog.setCostTime(end - begin);
        operateLog.setIp(request.getRemoteAddr());

        // 保存日志
        operateLogMapper.insert(operateLog);

        return result;   // 必须返回目标方法的返回值
    }

    private Integer getCurrentEmpId() {
        try {
            String token = request.getHeader("token");
            if (token != null) {
                return JwtUtils.getEmpId(token);   // 从 JWT 解析用户 ID
            }
        } catch (Exception e) {
            log.warn("获取当前用户ID失败", e);
        }
        return null;
    }
}
```

#### 6.5 JoinPoint API 详解

| 方法 | 说明 |
|------|------|
| `joinPoint.getTarget()` | 获取目标对象（被代理的原始对象） |
| `joinPoint.getSignature()` | 获取方法签名 |
| `joinPoint.getSignature().getName()` | 方法名 |
| `joinPoint.getArgs()` | 方法参数数组 |
| `joinPoint.proceed()` | 执行目标方法（`@Around` 特有） |

#### 6.6 步骤 3：在 Controller 上加注解

```java
@Slf4j
@RestController
@RequestMapping("/depts")
public class DeptController {

    @Log    // 只有加了 @Log 的方法才会被切面拦截
    @PostMapping
    public Result save(@RequestBody Dept dept) { ... }

    @Log
    @PutMapping
    public Result update(@RequestBody Dept dept) { ... }

    @Log
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) { ... }

    // 查询方法不需要记录日志
    @GetMapping
    public Result list() { ... }
}
```

#### 6.7 AOP 底层原理（了解）

Spring AOP 默认用 **JDK 动态代理**（目标类实现了接口）或 **CGLIB 代理**（没有接口）。

```
┌──────────────────────────────────────────────────────────┐
│                  AOP 代理机制                             │
├──────────────────────────────────────────────────────────┤
│                                                           │
│   目标对象           AOP 代理对象          你的代码        │
│   DeptService      DeptServiceProxy     controller       │
│       │                  │                  │             │
│       │  ◄────────────── │ ◄──────────────│             │
│       │   内部持有原对象    │   调用代理方法   │             │
│       │                  │                  │             │
│       │  before advice   │                  │             │
│       │  ──────────────► │                  │             │
│       │                  │  target.save()   │             │
│       │  after advice    │                  │             │
│       │  ◄────────────── │                  │             │
│                                                           │
└──────────────────────────────────────────────────────────┘
```

---

## 动手练习

### 练习 1：事务回滚验证

1. 写一个 `@Transactional` 方法，插入 emp 后故意让 insertBatch 报异常
2. 用数据库工具观察 emp 表 → 异常时 emp 数据被回滚
3. 去掉 `@Transactional` 再试 → emp 残留（脏数据）

### 练习 2：AOP 日志扩展

在操作日志中增加：

- 请求 IP 地址
- 请求 URL（`request.getRequestURI()`）
- 请求方式（`request.getMethod()`）

### 练习 3：AOP 统一参数校验

写一个 `@Around` 切面，拦截所有 Controller 方法，在方法执行前打印请求参数日志，执行后打印返回值日志（类似操作日志，但不入库，只打印到控制台）。

---

## 常见错误排查

### 阶段 1：事务配置问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `@Transactional` 不生效 | 同类 this 调用 | 注入自身代理对象 |
| `@Transactional` 不生效 | 方法不是 public | 改成 public |
| 事务不回滚 | 异常被 catch | 重新抛出 |
| 事务不回滚 | 受检异常 | 加 `rollbackFor = Exception.class` |
| 事务不回滚 | MyISAM 引擎 | 改 InnoDB |

### 阶段 2：AOP 配置问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| AOP 没执行 | 没加 `@Aspect` 或 `@Component` | 两个注解都要 |
| AOP 没执行 | 切入点表达式写错 | 检查 `@Around("...")` 中的表达式 |
| AOP 拦截了所有方法 | Pointcut 太宽泛 | 缩小范围，如只拦截 Controller 包 |

### 阶段 3：日志相关问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 日志阻塞主流程 | 同步保存日志太慢 | 加 `@Async` 异步保存 |
| 日志记录异常影响业务 | AOP 里抛异常 | AOP 捕获异常，只打印不抛 |
| 操作人 ID 为 null | Token 没传或解析失败 | 检查登录状态和 JWT 配置 |

### 阶段 4：性能问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| AOP 日志导致接口变慢 | 每次操作都插入数据库 | 批量异步写入 |
| 事务范围太大 | `@Transactional` 包在方法最外层 | 缩小事务范围，只包必要操作 |
| 嵌套事务死锁 | 两个事务互相等待 | 统一访问顺序，加索引 |

---

## 本节小结

```
┌────────────────────────────────────────────────────────────────────┐
│                    事务管理与 AOP 操作日志                           │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  事务 ACID：                                                         │
│    A（原子性）：全成或全败                                            │
│    C（一致性）：数据状态合法                                          │
│    I（隔离性）：并发互不干扰                                          │
│    D（持久性）：提交后永久保存                                        │
│                                                                     │
│  Spring 声明式事务：                                                 │
│    @Transactional(rollbackFor = Exception.class)                    │
│                                                                     │
│  事务失效 6 种场景：                                                  │
│    ① 同类 this 调用   ② 非 public                                   │
│    ③ 异常被 catch      ④ 异常类型不匹配                              │
│    ⑤ 非 InnoDB 引擎    ⑥ 没开事务管理                                │
│                                                                     │
│  AOP 核心概念：                                                      │
│    Aspect（切面）+ Pointcut（切入点）+ Advice（通知）               │
│                                                                     │
│  5 种通知：                                                          │
│    @Before / @After / @AfterReturning / @AfterThrowing / @Around   │
│    @Around 最强大，可以控制方法是否执行                               │
│                                                                     │
│  操作日志实现步骤：                                                   │
│    ① 自定义 @Log 注解                                                │
│    ② @Around("@annotation(anno路径)") 编写切面                     │
│    ③ joinPoint.proceed() 执行业务方法                                │
│    ④ 提取信息后存入数据库                                             │
│    ⑤ 在 Controller 方法上加 @Log                                     │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

---

## 参考文档

- [Spring 事务管理官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)
- [Spring AOP 官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop)
- [Java 动态代理 vs CGLIB](https://www.baeldung.com/cglib-vs-jdk-dynamic-proxies)
- [MySQL 事务隔离级别](https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html)
