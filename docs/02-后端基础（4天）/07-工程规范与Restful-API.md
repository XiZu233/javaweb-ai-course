# 工程规范与 RESTful API 设计

## 学习目标

学完本节后，你将能够：
- 理解为什么代码规范和工程结构比"能跑就行"更重要
- 设计符合 RESTful 风格的 API 接口
- 实现统一的响应格式和全局异常处理
- 写出团队协作中"别人看得懂、改得了"的代码

---

## 核心知识点

### 1. 为什么需要规范——从"个人项目"到"团队协作"

#### 1.1 一个真实的反面案例

假设你加入了一个没有规范的项目，看到以下代码：

```java
// ❌ 没有规范的代码（你能看懂吗？）
@RestController
public class a {
    @Autowired s s1;

    @RequestMapping("/getAllUsers")
    public Object b() {
        return s1.c();
    }

    @RequestMapping("/addUser")
    public Object d(@RequestBody Object o) {
        s1.e(o);
        return "ok";
    }
}
```

**问题：**
1. 类名 `a`、变量名 `s1`、方法名 `b/c/d/e`——完全看不懂是做什么的
2. `@RequestMapping` 没有指定请求方法——GET/POST/PUT/DELETE 都能访问
3. 返回 `Object`——前端不知道收到的是什么结构
4. 没有异常处理——出错了前端收到 500 页面
5. 没有日志——出问题时无法排查

**三个月后，连写这段代码的人自己也看不懂了。**

#### 1.2 规范的价值

规范不是束缚，而是**让团队高效协作的共识**：

| 场景 | 没有规范 | 有规范 |
|------|---------|--------|
| 新成员入职 | 花 2 周看懂代码结构 | 花 2 天就能上手 |
| 排查 Bug | 找不到日志、看不懂方法名 | 按命名规则快速定位 |
| 代码审查 | 每个人都有自己的风格 | 关注点放在业务逻辑上 |
| 项目交接 | 前任离职，代码成"天书" | 文档+规范，顺利交接 |

**本课程遵循《阿里巴巴 Java 开发手册》，这是国内使用最广泛的 Java 编码规范。**

---

### 2. 工程目录规范——"每个文件都有它的家"

#### 2.1 标准目录结构

```
com.tlias                              ← 公司/项目域名倒写
├── controller                         ← 控制层：接收 HTTP 请求，返回响应
│   └── DeptController.java
│
├── service                            ← 业务层：接口定义
│   ├── DeptService.java               ← 接口
│   └── impl                           ← 实现类（放在 impl 子包中）
│       └── DeptServiceImpl.java
│
├── mapper                             ← 数据层：数据库操作
│   └── DeptMapper.java
│
├── pojo                               ← 实体类（Plain Old Java Object）
│   ├── Dept.java                      ← 与数据库表对应
│   ├── Emp.java
│   └── Result.java                    ← 统一响应对象
│
├── dto                                ← DTO（Data Transfer Object）
│   └── EmpQueryDTO.java               ← 查询参数封装
│
├── vo                                 ← VO（View Object）
│   └── EmpVO.java                     ← 返回给前端的专用对象
│
├── utils                              ← 工具类
│   └── JwtUtils.java
│
├── config                             ← 配置类
│   └── WebConfig.java
│
├── filter                             ← 过滤器（版本 A）
│   └── TokenFilter.java
│
├── interceptor                        ← 拦截器（版本 B）
│   └── LoginInterceptor.java
│
├── aspect                             ← AOP 切面
│   └── LogAspect.java
│
├── exception                          ← 异常定义与全局处理
│   ├── BusinessException.java         ← 自定义业务异常
│   └── GlobalExceptionHandler.java    ← 全局异常处理器
│
├── anno                               ← 自定义注解
│   └── Log.java
│
└── TliasApplication.java              ← SpringBoot 启动类（放在根包）
```

#### 2.2 各层职责再强调

```
┌─────────────────────────────────────────────────────────────┐
│  Controller 控制层                                           │
│  • 只负责：接收请求 → 调用 Service → 返回 Result             │
│  • 不处理：业务逻辑、数据库操作                              │
│  • 命名：XxxController                                       │
├─────────────────────────────────────────────────────────────┤
│  Service 业务层                                              │
│  • 只负责：业务逻辑、数据校验、事务管理                      │
│  • 不处理：HTTP 请求、SQL 编写                               │
│  • 命名：XxxService（接口）、XxxServiceImpl（实现）           │
├─────────────────────────────────────────────────────────────┤
│  Mapper 数据层                                               │
│  • 只负责：SQL 执行、结果映射                                │
│  • 不处理：业务判断、参数校验                                │
│  • 命名：XxxMapper                                           │
└─────────────────────────────────────────────────────────────┘
```

#### 2.3 POJO / DTO / VO 的区别

这是初学者最容易混淆的三个概念：

| 类型 | 全称 | 用途 | 例子 |
|------|------|------|------|
| **POJO/Entity** | Plain Old Java Object | 与数据库表一一对应 | `Emp` 类有 `id, name, password, dept_id` |
| **DTO** | Data Transfer Object | 接口的入参封装 | `EmpQueryDTO` 有 `name, gender, begin, end`（查询条件） |
| **VO** | View Object | 返回给前端的专用对象 | `EmpVO` 有 `name, deptName`（没有 password） |

**为什么要区分？**

```java
// 场景：查询员工列表返回给前端

// ❌ 错误：直接返回 Entity
// 问题：password 敏感字段暴露给前端；部门只显示 ID，不显示名称
@GetMapping("/emps")
public Result list() {
    return Result.success(empService.list());  // 暴露了 password！
}

// ✅ 正确：返回 VO
@GetMapping("/emps")
public Result list() {
    List<EmpVO> voList = empService.listVO();  // Service 层做转换
    return Result.success(voList);
}

// EmpVO 的定义（没有敏感字段，有关联对象的名称）
public class EmpVO {
    private Integer id;
    private String name;
    private String gender;        // 1 → "男"，2 → "女"
    private String deptName;      // 显示"研发部"而不是 deptId=1
    private String jobName;       // 显示"班主任"而不是 job=1
    // 注意：没有 password！
}
```

**简单记忆：**
- **Entity** = 数据库有什么，它就有什么（忠实映射）
- **DTO** = 前端传什么参数，它就有什么（入参封装）
- **VO** = 前端需要什么数据，它就有什么（出参定制）

---

### 3. 命名规范——"见名知意"是基本功

#### 3.1 各类命名规则

| 类型 | 规范 | 正确示例 | 错误示例 |
|------|------|---------|---------|
| 类名 | 大驼峰，名词 | `UserService`, `DeptController` | `userService`, `dept_controller` |
| 接口名 | 大驼峰，形容词/名词 | `Serializable`, `UserService` | `IUserService`（不要加 I 前缀） |
| 方法名 | 小驼峰，动词开头 | `getById`, `saveBatch`, `deleteByIds` | `GetById`, `savebatch` |
| 变量名 | 小驼峰 | `userList`, `empCount` | `user_list`, `empcount` |
| 常量名 | 全大写 + 下划线 | `MAX_PAGE_SIZE`, `DEFAULT_PASSWORD` | `maxPageSize`, `defaultPassword` |
| 包名 | 全小写 | `com.tlias.service.impl` | `com.Tlias.Service` |
| 布尔方法 | is/has/can 开头 | `isAdmin()`, `hasPermission()` | `admin()`, `permission()` |
| 集合变量 | 复数或 List 结尾 | `users`, `empList` | `user`, `emps` |

#### 3.2 方法命名最佳实践

```java
// 查询类方法（返回数据，不修改）
getById(Integer id)              // 根据 ID 查询单个
getByUsername(String username)   // 根据某个字段查询
list()                           // 查询列表（无条件）
listByDeptId(Integer deptId)     // 根据条件查询列表
count()                          // 查询总数
pageQuery(PageParam param)       // 分页查询

// 新增类方法
save(Entity entity)              // 新增
saveBatch(List<Entity> list)     // 批量新增

// 更新类方法
updateById(Entity entity)        // 根据 ID 更新
updateStatus(Integer id, Integer status)  // 更新某个字段

// 删除类方法
deleteById(Integer id)           // 根据 ID 删除
deleteByIds(List<Integer> ids)   // 批量删除
remove(Integer id)               // 同 deleteById（Service 层常用）

// 校验类方法
validate(Entity entity)          // 校验数据合法性
checkExists(String username)     // 检查是否存在
```

---

### 4. RESTful API 设计——让 URL 会说话

#### 4.1 RESTful 的核心思想

RESTful 是一种 API 设计风格，核心思想：

> **URL 只表示资源（名词），HTTP 方法表示动作（动词）。**

**传统风格 vs RESTful 风格：**

```
传统风格（URL 里包含动作）：
  GET /getDeptList           ← "获取部门列表"
  GET /getDeptById?id=1      ← "根据 ID 获取部门"
  POST /addDept              ← "新增部门"
  POST /updateDept           ← "更新部门"
  GET /deleteDept?id=1       ← "删除部门"

RESTful 风格（URL 只有名词，动作用 HTTP 方法表示）：
  GET    /depts              ← 查询部门列表
  GET    /depts/1            ← 根据 ID 查询部门
  POST   /depts              ← 新增部门
  PUT    /depts/1            ← 全量更新部门
  PATCH  /depts/1            ← 部分更新部门
  DELETE /depts/1            ← 删除部门
```

**RESTful 的优势：**
- URL 更简洁、更统一
- HTTP 方法本身就表达了动作意图
- 更符合 HTTP 协议的设计哲学
- 前端一看 URL 就知道是做什么的

#### 4.2 URL 设计原则

**原则 1：使用名词复数**
```
✅ GET /depts        ← 部门集合
✅ GET /emps         ← 员工集合
❌ GET /getDepts     ← 不要包含动作
❌ GET /dept         ← 用复数
```

**原则 2：层级关系用斜杠**
```
✅ GET /depts/1/emps     ← 部门 1 下的所有员工
✅ GET /users/1/orders   ← 用户 1 的所有订单
```

**原则 3：不用文件扩展名**
```
❌ GET /depts.json
❌ GET /depts.xml
✅ GET /depts
  Content-Type: application/json  ← 用请求头表示格式
```

**原则 4：过滤、排序、分页用查询参数**
```
GET /emps?name=张&gender=1           ← 条件过滤
GET /emps?page=1&pageSize=10         ← 分页
GET /emps?sort=entrydate,desc        ← 排序
GET /emps?deptId=1&job=2&page=1      ← 组合条件
```

#### 4.3 HTTP 方法与 CRUD 对应

| 操作 | HTTP 方法 | URL 示例 | 说明 |
|------|----------|---------|------|
| 查询列表 | GET | `/depts` | |
| 查询单个 | GET | `/depts/1` | 1 是资源 ID |
| 新增 | POST | `/depts` | 数据放请求体 |
| 全量更新 | PUT | `/depts/1` | 替换整个资源 |
| 部分更新 | PATCH | `/depts/1` | 只改部分字段 |
| 删除 | DELETE | `/depts/1` | |

**POST vs PUT vs PATCH 的区别：**

```java
// POST：新增资源（服务器分配 ID）
POST /depts
Body: { "name": "新部门" }
→ 服务器创建资源，返回 201 和 Location: /depts/5

// PUT：全量更新（替换整个资源）
PUT /depts/1
Body: { "id": 1, "name": "改名后的部门", "createTime": "2024-01-01" }
→ 替换 ID=1 的整个资源，未提供的字段可能被清空

// PATCH：部分更新（只改传入的字段）
PATCH /depts/1
Body: { "name": "只改名字" }
→ 只更新 name 字段，其他字段保持不变
```

**本项目（版本 A）的实际 API 设计：**

```
部门管理：
┌──────────┬─────────────┬──────────────┬────────────────────┐
│ 操作     │ HTTP 方法   │ URL          │ 参数               │
├──────────┼─────────────┼──────────────┼────────────────────┤
│ 查询列表 │ GET         │ /depts       │ 无                 │
│ 查询详情 │ GET         │ /depts/{id}  │ PathVariable       │
│ 新增     │ POST        │ /depts       │ @RequestBody       │
│ 修改     │ PUT         │ /depts       │ @RequestBody       │
│ 删除     │ DELETE      │ /depts/{id}  │ PathVariable       │
└──────────┴─────────────┴──────────────┴────────────────────┘

员工管理：
┌──────────┬─────────────┬──────────────────────┬────────────────────┐
│ 操作     │ HTTP 方法   │ URL                  │ 参数               │
├──────────┼─────────────┼──────────────────────┼────────────────────┤
│ 分页查询 │ GET         │ /emps                │ page, pageSize     │
│ 条件查询 │ GET         │ /emps                │ name, gender...    │
│ 查询详情 │ GET         │ /emps/{id}           │ PathVariable       │
│ 新增     │ POST        │ /emps                │ @RequestBody       │
│ 修改     │ PUT         │ /emps                │ @RequestBody       │
│ 删除     │ DELETE      │ /depts/{ids}         │ 批量 ID（1,2,3）   │
│ 上传头像 │ POST        │ /upload              │ multipart/form-data│
└──────────┴─────────────┴──────────────────────┴────────────────────┘
```

---

### 5. 统一响应格式——前后端的"通用语言"

#### 5.1 为什么需要统一格式

假设你的系统有 50 个接口：
- 接口 A 成功返回：`{ "status": 200, "data": [...] }`
- 接口 B 成功返回：`{ "code": 0, "result": [...] }`
- 接口 C 失败返回：`{ "error": "xxx", "message": "yyy" }`

前端要处理 50 种不同的返回格式？疯了！

**统一格式后，前端只需要一种处理逻辑：**

```java
// 版本 A 的统一响应类
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Integer code;    // 1 = 成功，0 = 失败
    private String msg;      // 提示信息
    private Object data;     // 业务数据

    // 快速创建成功响应（无数据）
    public static Result success() {
        return new Result(1, "success", null);
    }

    // 快速创建成功响应（带数据）
    public static Result success(Object data) {
        return new Result(1, "success", data);
    }

    // 快速创建失败响应
    public static Result error(String msg) {
        return new Result(0, msg, null);
    }
}
```

#### 5.2 响应示例

```json
// 查询成功（返回对象）
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "name": "研发部",
    "createTime": "2024-01-15 10:30:00"
  }
}

// 查询成功（返回列表）
{
  "code": 1,
  "msg": "success",
  "data": [
    { "id": 1, "name": "研发部" },
    { "id": 2, "name": "市场部" }
  ]
}

// 操作成功（无数据返回）
{
  "code": 1,
  "msg": "success",
  "data": null
}

// 失败（参数错误）
{
  "code": 0,
  "msg": "部门名称不能为空",
  "data": null
}

// 失败（未登录）
{
  "code": 0,
  "msg": "NOT_LOGIN",
  "data": null
}
```

#### 5.3 前端统一处理

```javascript
// request.js（Axios 响应拦截器）
request.interceptors.response.use(
  response => {
    const result = response.data;

    if (result.code === 1) {
      // 成功，返回数据
      return result.data;
    } else {
      // 失败，显示错误信息
      Message.error(result.msg);
      return Promise.reject(result.msg);
    }
  },
  error => {
    // HTTP 错误（404、500 等）
    Message.error('网络错误');
    return Promise.reject(error);
  }
);
```

---

### 6. 全局异常处理——让错误变得"优雅"

#### 6.1 没有全局异常处理的问题

```java
// ❌ 每个 Controller 方法都要 try-catch
@GetMapping("/{id}")
public Result getById(@PathVariable Integer id) {
    try {
        Dept dept = deptService.getById(id);
        return Result.success(dept);
    } catch (Exception e) {
        return Result.error("查询失败：" + e.getMessage());
    }
}

// 如果有 50 个接口，要写 50 个 try-catch！
// 而且异常信息直接暴露给前端，不安全！
```

#### 6.2 自定义业务异常

```java
package com.tlias.exception;

/**
 * 业务异常
 * 用于表示业务规则不满足的情况（如参数错误、权限不足）
 * 继承 RuntimeException，不需要在方法上声明 throws
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
```

#### 6.3 全局异常处理器

```java
package com.tlias.exception;

import com.tlias.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * 作用：拦截所有 Controller 抛出的异常，统一处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * @ExceptionHandler(BusinessException.class) 表示只处理 BusinessException 类型
     */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        log.error("业务异常：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 处理所有其他异常
     * 兜底处理：防止异常信息直接暴露给前端
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常：", e);  // 打印完整堆栈，方便排查
        return Result.error("系统繁忙，请稍后再试");
    }
}
```

#### 6.4 异常分层抛出规范

```
Controller 层：
  • 参数校验失败 → 直接返回 Result.error("具体错误")
  • 调用 Service → 不需要 try-catch，让异常往上抛

Service 层：
  • 业务规则不满足 → 抛出 BusinessException("具体原因")
  • 如：用户不存在、权限不足、数据冲突
  • 不需要 try-catch 数据库异常，让异常往上抛

Mapper 层：
  • SQL 执行异常 → 由全局异常处理器捕获
  • Mapper 只关注 SQL，不处理异常

GlobalExceptionHandler：
  • BusinessException → 返回友好的错误提示
  • 其他 Exception → 返回"系统繁忙"，记录日志
```

#### 6.5 使用示例

```java
@Service
public class DeptServiceImpl implements DeptService {

    @Autowired
    private DeptMapper deptMapper;

    @Override
    public void delete(Integer id) {
        // 业务校验：检查部门下是否有员工
        int empCount = empMapper.countByDeptId(id);
        if (empCount > 0) {
            // 抛出自定义业务异常，前端会收到 "该部门下存在员工，无法删除"
            throw new BusinessException("该部门下存在员工，无法删除");
        }

        deptMapper.deleteById(id);
    }
}
```

---

### 7. Controller 编写规范——完整示例

```java
package com.tlias.controller;

import com.tlias.pojo.Dept;
import com.tlias.pojo.Result;
import com.tlias.service.DeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 * @Slf4j：Lombok 提供日志对象 log，可直接使用 log.info()、log.error()
 */
@Slf4j
@RestController
@RequestMapping("/depts")
public class DeptController {

    @Autowired
    private DeptService deptService;

    /**
     * 查询部门列表
     * GET /depts
     */
    @GetMapping
    public Result list() {
        log.info("查询部门列表");
        List<Dept> deptList = deptService.list();
        return Result.success(deptList);
    }

    /**
     * 根据 ID 查询部门
     * GET /depts/1
     * @PathVariable：从 URL 路径中提取 {id} 的值
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        log.info("根据 ID 查询部门：{}", id);
        Dept dept = deptService.getById(id);
        return Result.success(dept);
    }

    /**
     * 新增部门
     * POST /depts
     * @RequestBody：把请求体的 JSON 转换为 Dept 对象
     */
    @PostMapping
    public Result save(@RequestBody Dept dept) {
        log.info("新增部门：{}", dept.getName());
        deptService.save(dept);
        return Result.success();
    }

    /**
     * 修改部门
     * PUT /depts
     */
    @PutMapping
    public Result update(@RequestBody Dept dept) {
        log.info("修改部门：{}", dept);
        deptService.update(dept);
        return Result.success();
    }

    /**
     * 删除部门
     * DELETE /depts/1
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        log.info("删除部门：{}", id);
        deptService.delete(id);
        return Result.success();
    }
}
```

**Controller 编写 checklist：**

- [ ] 类名以 `Controller` 结尾
- [ ] 有 `@RestController` 和 `@RequestMapping` 注解
- [ ] HTTP 方法与操作语义一致（GET 查询、POST 新增、PUT 更新、DELETE 删除）
- [ ] 查询用 `@GetMapping`，新增用 `@PostMapping`，更新用 `@PutMapping`，删除用 `@DeleteMapping`
- [ ] 路径参数用 `@PathVariable`，请求体用 `@RequestBody`，查询参数用 `@RequestParam`
- [ ] 返回统一 `Result` 包装
- [ ] 关键操作添加日志

---

### 8. API 文档（Knife4j）——版本 B 专属

版本 B 使用 Knife4j 自动生成 API 文档，基于 Swagger。

```java
@Tag(name = "部门管理")              // API 分组名称
@RestController
@RequestMapping("/depts")
public class DeptController {

    @Operation(summary = "查询部门列表")  // 接口说明
    @GetMapping
    public Result<List<Dept>> list() { ... }

    @Operation(summary = "根据 ID 查询部门")
    @GetMapping("/{id}")
    public Result<Dept> getById(@Parameter(description = "部门ID") @PathVariable Integer id) { ... }

    @Operation(summary = "新增部门")
    @PostMapping
    public Result save(@RequestBody Dept dept) { ... }
}
```

启动后访问：`http://localhost:48080/doc.html`

功能：
- 在线查看所有 API 接口
- 直接在线调试（发送请求）
- 自动生成接口文档

---

## 动手练习

### 练习 1：设计员工管理 API

**目标**：根据 RESTful 规范，设计员工管理的完整 API。

**要求**：

| 操作 | HTTP 方法 | URL | 参数 |
|------|----------|-----|------|
| 分页查询员工 | ? | ? | page, pageSize, name, gender |
| 根据 ID 查询 | ? | ? | id |
| 新增员工 | ? | ? | Emp 对象 |
| 修改员工 | ? | ? | Emp 对象 |
| 删除员工 | ? | ? | ids（批量） |

**答案：**

| 操作 | HTTP 方法 | URL | 参数 |
|------|----------|-----|------|
| 分页查询员工 | GET | `/emps` | `@RequestParam` |
| 根据 ID 查询 | GET | `/emps/{id}` | `@PathVariable` |
| 新增员工 | POST | `/emps` | `@RequestBody` |
| 修改员工 | PUT | `/emps` | `@RequestBody` |
| 删除员工 | DELETE | `/emps/{ids}` | `@PathVariable` |

### 练习 2：实现全局异常处理

**目标**：在项目中添加全局异常处理，测试业务异常和系统异常。

**步骤**：

1. 创建 `BusinessException.java`
2. 创建 `GlobalExceptionHandler.java`
3. 在 Service 中抛出一个业务异常：
   ```java
   throw new BusinessException("测试业务异常");
   ```
4. 调用接口，观察返回：
   ```json
   { "code": 0, "msg": "测试业务异常", "data": null }
   ```
5. 在 Service 中抛出一个 RuntimeException：
   ```java
   throw new RuntimeException("测试系统异常");
   ```
6. 调用接口，观察返回：
   ```json
   { "code": 0, "msg": "系统繁忙，请稍后再试", "data": null }
   ```

---

## 常见错误排查

### 阶段 1：接口访问问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `405 Method Not Allowed` | HTTP 方法和后端不匹配 | 检查前端请求方法和 `@XxxMapping` 是否一致 |
| `400 Bad Request` | 参数绑定失败 | 1. 检查 JSON 格式<br>2. 检查字段名和类型<br>3. 检查是否有 `@RequestBody` |
| `404` | URL 路径错误 | 检查 `@RequestMapping` 路径和前端调用地址 |

### 阶段 2：响应格式问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 前端解析失败 | 返回格式不统一 | 确保所有接口都返回 `Result` 包装 |
| 返回 double 被截断 | Jackson 序列化精度丢失 | 配置 Jackson 将 Long 转为 String |
| 日期格式不对 | Jackson 默认时间戳 | 配置 `spring.jackson.date-format` |

### 阶段 3：异常处理问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 异常直接暴露给前端 | 全局异常处理器未生效 | 检查 `@RestControllerAdvice` 是否在扫描包下 |
| 业务异常返回 500 | 异常类型匹配错误 | 检查 `@ExceptionHandler` 的参数类型 |
| 异常堆栈没记录 | 没有打日志 | 使用 `@Slf4j` 添加日志输出 |

### 阶段 4：规范执行问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 代码风格不一致 | 团队没有统一规范 | 使用 Alibaba Java Coding Guidelines 插件 |
| 命名看不懂 | 命名不规范 | 参考本节命名规范表 |
| 包结构混乱 | 类放错位置 | 按 controller/service/mapper/pojo 分层 |

---

## 本节小结

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    工程规范与 RESTful API 设计                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  工程目录：                                                               │
│    controller → service → mapper → pojo/dto/vo                           │
│    utils / config / filter / exception / aspect                          │
│                                                                          │
│  三层对象：                                                               │
│    Entity（POJO）= 数据库表映射                                            │
│    DTO = 接口入参封装                                                      │
│    VO = 返回给前端的定制对象（不含敏感字段）                                │
│                                                                          │
│  命名规范：                                                               │
│    类名 = 大驼峰名词（DeptController）                                     │
│    方法名 = 小驼峰动词开头（getById, saveBatch）                           │
│    常量 = 全大写下划线（MAX_PAGE_SIZE）                                    │
│                                                                          │
│  RESTful API：                                                            │
│    URL = 名词复数，HTTP 方法 = 动作                                        │
│    GET /depts, POST /depts, PUT /depts/1, DELETE /depts/1                 │
│                                                                          │
│  统一响应：                                                               │
│    Result { code: 1/0, msg: "", data: {} }                                │
│    code=1 成功，code=0 失败                                               │
│                                                                          │
│  全局异常：                                                               │
│    BusinessException → 业务错误提示                                        │
│    Exception → "系统繁忙"（兜底）                                           │
│    @RestControllerAdvice 统一拦截处理                                      │
│                                                                          │
│  黄金法则：                                                               │
│    Controller 不处理业务逻辑                                               │
│    Service 不处理 HTTP 请求                                                │
│    所有接口返回统一 Result 格式                                            │
│    异常必须被处理，不能暴露给前端                                          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 参考文档

- [RESTful API 设计指南](https://restfulapi.net/)
- [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- [Knife4j 官方文档](https://doc.xiaominfo.com/)
