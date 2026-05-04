# 工程规范与 RESTful API 设计

## 学习目标

- 理解 Java 工程常见的代码规范和目录结构
- 掌握 RESTful API 的设计原则和最佳实践
- 能够设计统一响应格式和错误码
- 理解 API 版本控制和文档自动生成

## 核心知识点

### 1. 工程目录规范

```
com.tlias
├── controller          # 控制层：接收请求、返回响应
├── service             # 业务层：业务逻辑接口
│   └── impl            # 业务实现类
├── mapper              # 数据层：数据库操作
├── pojo / entity       # 实体类：与数据库表对应
├── dto                 # 数据传输对象：接口入参/出参
├── vo                  # 视图对象：返回给前端的专用对象
├── utils               # 工具类
├── config              # 配置类
├── filter / interceptor # 过滤器/拦截器
├── aspect              # AOP 切面
├── exception           # 异常定义与全局处理
├── anno                # 自定义注解
└── TliasApplication.java  # 启动类
```

### 2. 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | 大驼峰，名词 | `UserService`, `DeptController` |
| 方法名 | 小驼峰，动词开头 | `getById`, `saveBatch`, `deleteByIds` |
| 常量 | 全大写 + 下划线 | `MAX_PAGE_SIZE`, `DEFAULT_PASSWORD` |
| 包名 | 全小写，域名倒写 | `com.tlias.service.impl` |
| 布尔方法 | is/has/can 开头 | `isAdmin`, `hasPermission` |

### 3. RESTful API 设计

RESTful 是一种基于 HTTP 协议的 API 设计风格，核心思想是将一切抽象为**资源**，用 HTTP 方法表示对资源的操作。

**URL 设计原则**：
- 使用名词复数，不使用动词
- 使用小写 + 斜杠分隔层级
- 不使用文件扩展名

```
GET    /depts          # 查询部门列表
GET    /depts/1        # 查询 ID 为 1 的部门
POST   /depts          # 新增部门
PUT    /depts/1        # 全量更新 ID 为 1 的部门
PATCH  /depts/1        # 部分更新
DELETE /depts/1        # 删除 ID 为 1 的部门
GET    /depts/1/emps   # 查询部门 1 下的所有员工
```

**对比传统风格**：

```
# 传统风格（不推荐）
GET /getDeptList
GET /getDeptById?id=1
POST /addDept
POST /updateDept
GET /deleteDept?id=1

# RESTful 风格（推荐）
GET    /depts
GET    /depts/1
POST   /depts
PUT    /depts/1
DELETE /depts/1
```

### 4. 统一响应格式

前后端交互采用统一的数据格式，便于前端统一处理：

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Integer code;    // 1 成功，0 失败
    private String msg;      // 提示信息
    private Object data;     // 返回数据

    public static Result success() {
        return new Result(1, "success", null);
    }

    public static Result success(Object data) {
        return new Result(1, "success", data);
    }

    public static Result error(String msg) {
        return new Result(0, msg, null);
    }
}
```

响应示例：

```json
// 成功
{ "code": 1, "msg": "success", "data": { "id": 1, "name": "教研部" } }

// 列表
{ "code": 1, "msg": "success", "data": [ { "id": 1, "name": "教研部" } ] }

// 失败
{ "code": 0, "msg": "部门名称不能为空", "data": null }
```

### 5. Controller 编写规范

```java
@RestController
@RequestMapping("/depts")
public class DeptController {

    @Autowired
    private DeptService deptService;

    @GetMapping
    public Result list() {
        return Result.success(deptService.list());
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        return Result.success(deptService.getById(id));
    }

    @PostMapping
    public Result save(@RequestBody Dept dept) {
        deptService.save(dept);
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody Dept dept) {
        deptService.update(dept);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        deptService.delete(id);
        return Result.success();
    }
}
```

**规范要点**：
- 方法名与 HTTP 方法保持一致语义
- 查询用 `@GetMapping`，新增用 `@PostMapping`，更新用 `@PutMapping`，删除用 `@DeleteMapping`
- 路径参数用 `@PathVariable`，请求体用 `@RequestBody`，URL 参数用 `@RequestParam`
- 返回统一 `Result` 包装

### 6. 异常处理规范

自定义业务异常：

```java
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

全局异常处理器：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        e.printStackTrace();
        return Result.error("系统繁忙，请稍后再试");
    }
}
```

**分层抛出异常**：
- Controller：参数校验失败 → 直接返回 Result.error
- Service：业务规则不满足 → 抛出 BusinessException
- Mapper：数据库异常 → 由全局处理器捕获返回友好提示

### 7. 常用 HTTP 状态码

| 状态码 | 含义 | 使用场景 |
|--------|------|---------|
| 200 | OK | 请求成功 |
| 201 | Created | 资源创建成功 |
| 204 | No Content | 删除成功 |
| 400 | Bad Request | 请求参数错误 |
| 401 | Unauthorized | 未登录/Token 失效 |
| 403 | Forbidden | 无权限 |
| 404 | Not Found | 资源不存在 |
| 500 | Internal Server Error | 服务器内部错误 |

> 版本 A 中，无论成功失败 HTTP 状态码均为 200，业务状态通过 `Result.code` 区分。版本 B 中根据 yudao 框架规范，部分接口会返回对应 HTTP 状态码。

### 8. API 文档（Swagger / Knife4j）

版本 B 使用 Knife4j 自动生成 API 文档：

```java
@Tag(name = "部门管理")
@RestController
@RequestMapping("/depts")
public class DeptController {

    @Operation(summary = "查询部门列表")
    @GetMapping
    public Result<List<Dept>> list() { ... }

    @Operation(summary = "新增部门")
    @PostMapping
    public Result save(@RequestBody Dept dept) { ... }
}
```

启动后访问 `http://localhost:48080/doc.html` 查看在线文档。

## 动手练习

### 练习 1：设计员工管理 API

根据 RESTful 规范，设计员工管理的完整 API 接口（URL + HTTP 方法 + 参数）。

### 练习 2：统一异常处理

在项目中添加 `GlobalExceptionHandler`，并测试以下场景：
1. 查询不存在的员工 ID，返回友好提示
2. 参数校验失败（如姓名为空），返回具体错误信息

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| 405 Method Not Allowed | HTTP 方法与后端不匹配 | 检查前端请求方法和 @XxxMapping 是否一致 |
| 400 Bad Request | 参数类型不匹配或 JSON 格式错误 | 检查 @RequestBody 对象字段类型 |
| 404 | URL 路径错误 | 检查 @RequestMapping 路径和前端调用地址 |
| 返回 double 被截断 | Jackson 序列化精度丢失 | 配置 Jackson 将 Long 转为 String |

## 本节小结

规范的工程结构和 RESTful API 设计是团队协作的基础。统一响应格式让前后端协作更高效，全局异常处理让系统更健壮。版本 B 基于 yudao 框架，天然集成了这些最佳实践。

## 参考文档

- [RESTful API 设计指南](https://restfulapi.net/)
- [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- [Knife4j 官方文档](https://doc.xiaominfo.com/)

