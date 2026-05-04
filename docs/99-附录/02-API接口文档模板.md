# 附录二：API 接口文档规范与模板

> 适用版本：Version A（SpringBoot 2.7 + JWT）/ Version B（SpringBoot 3.2 + Sa-Token）
> 目标：掌握 RESTful API 设计规范，能够编写规范的接口文档，理解前后端交互的数据格式

---

## 一、概述与目标

本文档定义了 Tlias 人事管理系统前后端交互的接口规范，涵盖以下学习目标：

1. **理解 RESTful 设计原则**：掌握 URL 设计、HTTP 方法选择、状态码使用规范
2. **统一响应格式**：前后端约定统一的 JSON 响应结构，降低沟通成本
3. **能独立编写接口文档**：使用 Markdown 表格模板描述接口的请求参数、响应数据
4. **了解 Swagger/OpenAPI**：知道自动化文档生成工具的基本用法

---

## 二、RESTful API 设计规范

### 2.1 什么是 RESTful？为什么要用？

REST（Representational State Transfer）是一种软件架构风格，核心思想是将服务器上的资源通过 URL 暴露出来，客户端使用 HTTP 方法对资源进行操作。RESTful 接口具有语义清晰、无状态、可缓存等优点，是现代 Web API 的事实标准。

```
RESTful 核心思想：
+-------------------+----------------------------------------+
| URL 表示资源      | /emps 表示员工集合，/emps/1 表示ID为1的员工 |
| HTTP 方法表示操作 | GET查、POST增、PUT改、DELETE删            |
| 无状态            | 每次请求独立，服务端不保存客户端状态        |
| 统一接口          | 相同的交互方式操作不同的资源               |
+-------------------+----------------------------------------+
```

### 2.2 URL 设计规范

```
规范示例：
+------------+--------------------------------+-------------+
| 操作       | URL                            | HTTP 方法   |
+------------+--------------------------------+-------------+
| 查询列表   | /emps                          | GET         |
| 查询单个   | /emps/{id}                     | GET         |
| 新增       | /emps                          | POST        |
| 修改       | /emps                          | PUT         |
| 删除单个   | /emps/{id}                     | DELETE      |
| 批量删除   | /emps/{ids}                    | DELETE      |
| 登录       | /login                         | POST        |
| 上传文件   | /upload                        | POST        |
+------------+--------------------------------+-------------+
```

**URL 设计原则**：

| 原则 | 正确示例 | 错误示例 | 说明 |
|------|---------|---------|------|
| 使用名词复数 | `/emps` | `/getEmp` | URL 是资源，不是动作 |
| 小写字母 | `/dept-list` | `/DeptList` | 统一小写，用连字符分隔 |
| 不用动词 | `/emps` | `/getAllEmps` | HTTP 方法已表达动作 |
| 层级关系用 `/` | `/depts/1/emps` | `/depts?id=1&action=emps` | 表达资源层级 |
| 过滤用 Query | `/emps?name=张` | `/emps/searchByName` | 查询条件放 URL 参数 |

### 2.3 HTTP 方法语义

| 方法 | 语义 | 幂等性 | 使用场景 |
|------|------|--------|---------|
| GET | 获取资源 | 是 | 查询列表、查询详情 |
| POST | 创建资源 | 否 | 新增数据、登录、上传 |
| PUT | 全量更新 | 是 | 修改数据（提交完整对象） |
| PATCH | 局部更新 | 否 | 修改部分字段（Tlias 中少用） |
| DELETE | 删除资源 | 是 | 删除数据 |

> **幂等性**：多次执行相同操作，结果相同。GET/PUT/DELETE 是幂等的，POST 不是幂等的。

### 2.4 HTTP 状态码规范

| 状态码 | 含义 | 使用场景 |
|--------|------|---------|
| 200 | OK | 请求成功，正常返回数据 |
| 201 | Created | 资源创建成功（如新增员工） |
| 204 | No Content | 删除成功，无返回体 |
| 400 | Bad Request | 请求参数错误（如缺少必填字段） |
| 401 | Unauthorized | 未登录或 Token 失效 |
| 403 | Forbidden | 已登录但无权限访问 |
| 404 | Not Found | 资源不存在（如员工ID不存在） |
| 500 | Internal Server Error | 服务端内部错误 |

**Tlias 项目实际做法**：

Tlias 项目中，无论成功还是失败，HTTP 状态码都返回 200，通过响应体中的 `code` 字段区分业务状态。这种做法对前端更友好，因为前端不需要处理多种 HTTP 错误状态。

```json
// 成功响应
{
  "code": 1,
  "msg": "success",
  "data": { /* 实际数据 */ }
}

// 失败响应
{
  "code": 0,
  "msg": "员工姓名不能为空",
  "data": null
}

// Version B（Sa-Token）可能使用更细粒度的状态码
{
  "code": 200,
  "msg": "操作成功",
  "data": { /* 实际数据 */ }
}
```

---

## 三、统一响应格式规范

### 3.1 响应结构定义

```java
// Java 后端统一响应类（Result / AjaxResult）
public class Result {
    private Integer code;    // 业务状态码：1成功，0失败（Version A）
    private String msg;      // 提示信息
    private Object data;     // 响应数据

    // 成功静态方法
    public static Result success(Object data) {
        Result r = new Result();
        r.code = 1;
        r.msg = "success";
        r.data = data;
        return r;
    }

    // 失败静态方法
    public static Result error(String msg) {
        Result r = new Result();
        r.code = 0;
        r.msg = msg;
        r.data = null;
        return r;
    }
}
```

### 3.2 不同场景的响应示例

```json
// 1. 查询列表成功
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 35,
    "rows": [
      { "id": 1, "name": "张三", "gender": 1, "deptName": "学工部" },
      { "id": 2, "name": "李四", "gender": 2, "deptName": "教研部" }
    ]
  }
}

// 2. 查询单个成功
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "name": "张三",
    "gender": 1,
    "job": 1,
    "entrydate": "2021-03-15",
    "deptId": 1,
    "deptName": "学工部"
  }
}

// 3. 新增成功
{
  "code": 1,
  "msg": "success",
  "data": null
}

// 4. 参数校验失败
{
  "code": 0,
  "msg": "员工姓名不能为空",
  "data": null
}

// 5. 未登录
{
  "code": 0,
  "msg": "NOT_LOGIN",
  "data": null
}
```

---

## 四、分页接口规范

### 4.1 分页请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|---------|------|
| page | Integer | 是 | 当前页码，从 1 开始 |
| pageSize | Integer | 是 | 每页条数，默认 10 |
| name | String | 否 | 姓名模糊查询 |
| gender | Integer | 否 | 性别：1男 2女 |
| begin | String | 否 | 入职日期起始（格式：yyyy-MM-dd） |
| end | String | 否 | 入职日期截止（格式：yyyy-MM-dd） |

### 4.2 分页响应格式

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 35,           // 总记录数（用于前端计算总页数）
    "rows": [              // 当前页数据列表
      { "id": 1, "name": "张三", ... },
      { "id": 2, "name": "李四", ... }
    ]
  }
}
```

### 4.3 分页实现原理

```
前端请求：GET /emps?page=2&pageSize=10&name=张
              |
              v
后端处理：
  1. 计算 LIMIT 参数：offset = (2-1) * 10 = 10
  2. 执行 COUNT(*) 查询总条数
  3. 执行 SELECT ... LIMIT 10, 10 查询当前页数据
  4. 封装为 {total, rows} 返回
              |
              v
前端接收：total=35, rows=[...]  ->  计算总页数 = ceil(35/10) = 4页
```

---

## 五、错误码定义规范

### 5.1 Version A 错误码

| 错误码 | 含义 | 触发场景 |
|--------|------|---------|
| 1 | 成功 | 所有正常业务操作 |
| 0 | 失败 | 通用错误，msg 中携带具体信息 |
| 0 + msg="NOT_LOGIN" | 未登录 | Token 缺失或过期 |

### 5.2 Version B 扩展错误码（推荐）

| 错误码 | 含义 | 触发场景 |
|--------|------|---------|
| 200 | 成功 | 操作成功 |
| 400 | 参数错误 | 请求参数缺失或格式不正确 |
| 401 | 未认证 | Token 缺失、过期或无效 |
| 403 | 无权限 | 已登录但无权访问该资源 |
| 404 | 资源不存在 | 查询的ID对应数据不存在 |
| 500 | 服务器错误 | 程序异常、数据库连接失败 |

---

## 六、Tlias 系统接口文档模板

以下按照模块逐一给出完整的接口文档示例，使用 Markdown 表格格式，可直接用于项目文档。

### 6.1 部门管理模块

#### 6.1.1 查询部门列表

| 项目 | 内容 |
|------|------|
| 接口地址 | `GET /depts` |
| 功能说明 | 查询所有部门列表 |
| 请求参数 | 无 |
| 响应数据 | 部门对象数组 |

**响应示例**：
```json
{
  "code": 1,
  "msg": "success",
  "data": [
    { "id": 1, "name": "学工部", "createTime": "2024-01-01 10:00:00" },
    { "id": 2, "name": "教研部", "createTime": "2024-01-01 10:00:00" }
  ]
}
```

#### 6.1.2 根据 ID 查询部门

| 项目 | 内容 |
|------|------|
| 接口地址 | `GET /depts/{id}` |
| 功能说明 | 根据部门 ID 查询单个部门详情 |
| 请求参数 | Path: `id`（部门ID，Integer） |
| 响应数据 | 部门对象 |

**请求示例**：`GET /depts/1`

**响应示例**：
```json
{
  "code": 1,
  "msg": "success",
  "data": { "id": 1, "name": "学工部", "createTime": "2024-01-01 10:00:00" }
}
```

#### 6.1.3 新增部门

| 项目 | 内容 |
|------|------|
| 接口地址 | `POST /depts` |
| 功能说明 | 新增部门 |
| 请求参数 | Body（JSON）: `{ "name": "部门名称" }` |
| 响应数据 | 无 |

**请求示例**：
```json
{ "name": "财务部" }
```

#### 6.1.4 修改部门

| 项目 | 内容 |
|------|------|
| 接口地址 | `PUT /depts` |
| 功能说明 | 修改部门信息 |
| 请求参数 | Body（JSON）: `{ "id": 1, "name": "新名称" }` |
| 响应数据 | 无 |

#### 6.1.5 删除部门

| 项目 | 内容 |
|------|------|
| 接口地址 | `DELETE /depts/{id}` |
| 功能说明 | 根据 ID 删除部门 |
| 请求参数 | Path: `id`（部门ID，Integer） |
| 响应数据 | 无 |

**请求示例**：`DELETE /depts/5`

---

### 6.2 员工管理模块

#### 6.2.1 分页查询员工列表

| 项目 | 内容 |
|------|------|
| 接口地址 | `GET /emps` |
| 功能说明 | 分页查询员工列表，支持多条件筛选 |
| 请求参数 | Query: `page`, `pageSize`, `name`, `gender`, `begin`, `end` |
| 响应数据 | `{ total, rows }` |

**请求示例**：`GET /emps?page=1&pageSize=10&name=张&gender=1`

**参数说明**：

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|---------|------|
| page | Integer | 是 | 页码，从1开始 |
| pageSize | Integer | 是 | 每页条数 |
| name | String | 否 | 姓名模糊查询 |
| gender | Integer | 否 | 性别：1男 2女 |
| begin | String | 否 | 入职起始日期（yyyy-MM-dd） |
| end | String | 否 | 入职截止日期（yyyy-MM-dd） |

**响应示例**：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 35,
    "rows": [
      {
        "id": 1,
        "username": "zhangsan",
        "name": "张三",
        "gender": 1,
        "image": "https://xxx.com/avatar.jpg",
        "job": 1,
        "entrydate": "2021-03-15",
        "deptId": 1,
        "deptName": "学工部",
        "createTime": "2024-01-01 10:00:00"
      }
    ]
  }
}
```

#### 6.2.2 根据 ID 查询员工

| 项目 | 内容 |
|------|------|
| 接口地址 | `GET /emps/{id}` |
| 功能说明 | 根据员工 ID 查询详情（含工作经历） |
| 请求参数 | Path: `id`（员工ID，Integer） |
| 响应数据 | 员工对象 + empExprList |

**响应示例**：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 2,
    "username": "zhangsan",
    "name": "张三",
    "gender": 1,
    "job": 1,
    "entrydate": "2021-03-15",
    "deptId": 1,
    "deptName": "学工部",
    "empExprList": [
      { "id": 1, "begin": "2019-07-01", "end": "2021-02-28", "company": "ABC教育", "job": "助教" }
    ]
  }
}
```

#### 6.2.3 新增员工

| 项目 | 内容 |
|------|------|
| 接口地址 | `POST /emps` |
| 功能说明 | 新增员工（含工作经历） |
| 请求参数 | Body（JSON）: 员工对象 |
| 响应数据 | 无 |

**请求示例**：
```json
{
  "username": "wanger",
  "name": "王二",
  "gender": 1,
  "job": 2,
  "entrydate": "2024-06-01",
  "deptId": 2,
  "empExprList": [
    { "begin": "2022-01-01", "end": "2024-05-30", "company": "XYZ公司", "job": "开发" }
  ]
}
```

#### 6.2.4 修改员工

| 项目 | 内容 |
|------|------|
| 接口地址 | `PUT /emps` |
| 功能说明 | 修改员工信息（含工作经历） |
| 请求参数 | Body（JSON）: 员工对象（必须包含 id） |
| 响应数据 | 无 |

#### 6.2.5 删除员工（单个）

| 项目 | 内容 |
|------|------|
| 接口地址 | `DELETE /emps/{id}` |
| 功能说明 | 根据 ID 删除员工 |
| 请求参数 | Path: `id`（员工ID，Integer） |
| 响应数据 | 无 |

#### 6.2.6 批量删除员工

| 项目 | 内容 |
|------|------|
| 接口地址 | `DELETE /emps/{ids}` |
| 功能说明 | 根据 ID 批量删除员工（逗号分隔） |
| 请求参数 | Path: `ids`（例：`1,2,3`） |
| 响应数据 | 无 |

**请求示例**：`DELETE /emps/5,6,7`

---

### 6.3 登录认证模块

#### 6.3.1 用户登录（Version A - JWT）

| 项目 | 内容 |
|------|------|
| 接口地址 | `POST /login` |
| 功能说明 | 用户登录，成功返回 JWT Token |
| 请求参数 | Body（JSON）: `{ "username": "", "password": "" }` |
| 响应数据 | JWT Token 字符串 |

**请求示例**：
```json
{ "username": "admin", "password": "123456" }
```

**响应示例**：
```json
{
  "code": 1,
  "msg": "success",
  "data": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Token 使用方式**：
后续请求在 HTTP Header 中携带：`Authorization: eyJhbGciOiJIUzI1NiIs...`

#### 6.3.2 用户登录（Version B - Sa-Token）

| 项目 | 内容 |
|------|------|
| 接口地址 | `POST /login` |
| 功能说明 | 用户登录，Sa-Token 自动管理 Session |
| 请求参数 | Body（JSON）: `{ "username": "", "password": "" }` |
| 响应数据 | Token 信息对象 |

**响应示例**：
```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "tokenName": "satoken",
    "tokenValue": "xxxx-xxxx-xxxx",
    "isLogin": true
  }
}
```

---

### 6.4 文件上传模块

#### 6.4.1 文件上传

| 项目 | 内容 |
|------|------|
| 接口地址 | `POST /upload` |
| 功能说明 | 上传图片/文件到服务器或 OSS |
| 请求参数 | FormData: `file`（文件） |
| 响应数据 | 文件访问 URL |

**请求示例**（前端 Axios）：
```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);
axios.post('/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
});
```

**响应示例**：
```json
{
  "code": 1,
  "msg": "success",
  "data": "https://tlias.oss-cn-beijing.aliyuncs.com/avatar/xxx.jpg"
}
```

---

### 6.5 AI 模块（Version B 专属）

#### 6.5.1 自然语言转 SQL（NL2SQL）

| 项目 | 内容 |
|------|------|
| 接口地址 | `POST /ai/nl2sql` |
| 功能说明 | 将自然语言问题转换为 SQL 并执行 |
| 请求参数 | Body（JSON）: `{ "question": "查询教研部有多少人" }` |
| 响应数据 | 查询结果 |

**响应示例**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "sql": "SELECT COUNT(*) FROM emp e JOIN dept d ON e.dept_id = d.id WHERE d.name = '教研部'",
    "result": [{ "count": 2 }]
  }
}
```

#### 6.5.2 知识库问答（RAG）

| 项目 | 内容 |
|------|------|
| 接口地址 | `POST /ai/rag` |
| 功能说明 | 基于上传的文档进行知识库问答 |
| 请求参数 | Body（JSON）: `{ "question": "公司的年假政策是什么" }` |
| 响应数据 | AI 生成的回答 |

#### 6.5.3 AI 流式对话

| 项目 | 内容 |
|------|------|
| 接口地址 | `GET /ai/chat/stream` |
| 功能说明 | SSE 流式对话，逐字返回 AI 回复 |
| 请求参数 | Query: `message`（用户输入） |
| 响应数据 | text/event-stream 流 |

**前端接收示例**：
```javascript
const eventSource = new EventSource('/ai/chat/stream?message=你好');
eventSource.onmessage = (event) => {
  console.log(event.data); // 逐段接收 AI 回复
};
```

---

## 七、Swagger / OpenAPI 简介

### 7.1 什么是 Swagger？

Swagger 是一套开源的 API 文档规范和工具集，通过在代码中添加注解，可以自动生成可视化的接口文档页面，支持在线测试接口。

### 7.2 SpringBoot 集成 Swagger（Version A - Knife4j）

```xml
<!-- pom.xml 添加依赖 -->
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>
```

```java
// 配置类开启 Swagger
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.SWAGGER_2)
            .apiInfo(new ApiInfoBuilder()
                .title("Tlias 人事管理系统接口文档")
                .version("1.0")
                .build())
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.tlias.controller"))
            .paths(PathSelectors.any())
            .build();
    }
}
```

```java
// Controller 中添加注解
@Api(tags = "员工管理")
@RestController
public class EmpController {

    @ApiOperation("分页查询员工列表")
    @GetMapping("/emps")
    public Result page(@RequestParam Integer page,
                       @RequestParam Integer pageSize) {
        // ...
    }
}
```

### 7.3 访问文档页面

启动项目后访问：`http://localhost:8080/doc.html`

```
+--------------------------------------------------+
|              Knife4j 文档页面                     |
+--------------------------------------------------+
|  左侧：接口列表（按 Controller 分组）              |
|  中间：接口详情（参数、响应、示例）                |
|  右侧：调试按钮（直接发送请求测试）                |
+--------------------------------------------------+
```

---

## 八、Postman 集合导出规范

### 8.1 为什么用 Postman？

Postman 是 API 开发和测试的常用工具，可以保存接口请求、设置环境变量、批量运行测试。在团队协作中，导出 Postman Collection 分享给其他成员，可以确保大家使用一致的请求参数。

### 8.2 Postman 使用要点

| 功能 | 操作步骤 |
|------|---------|
| 创建 Collection | 点击 "Collections" -> "Create Collection" -> 命名为 "Tlias API" |
| 添加请求 | 在 Collection 内点击 "Add Request" -> 填写 URL、方法、参数 |
| 设置环境变量 | 点击右上角齿轮 -> "Add" -> 定义 `baseUrl`、`token` 等变量 |
| 使用变量 | URL 中填写 `{{baseUrl}}/emps`，Headers 中填写 `{{token}}` |
| 导出 Collection | 点击 Collection 右侧 "..." -> "Export" -> 选择 Collection v2.1 |

### 8.3 环境变量配置示例

```json
{
  "name": "Tlias Local",
  "values": [
    { "key": "baseUrl", "value": "http://localhost:8080", "enabled": true },
    { "key": "token", "value": "", "enabled": true }
  ]
}
```

### 8.4 登录后自动保存 Token

在登录接口的 "Tests" 标签页添加脚本：
```javascript
// 从响应中提取 token 并保存到环境变量
var jsonData = pm.response.json();
pm.environment.set("token", jsonData.data);
```

在其他接口的 Headers 中添加：`Authorization: {{token}}`

---

## 九、接口设计常见错误排查

| 错误现象 | 可能原因 | 解决方案 |
|---------|---------|---------|
| 404 Not Found | URL 路径错误或方法不匹配 | 检查 `@RequestMapping` 路径和 HTTP 方法 |
| 400 Bad Request | 参数类型不匹配或缺少必填参数 | 检查参数名、类型；确认 `@RequestBody` 用法 |
| 415 Unsupported Media Type | Content-Type 不匹配 | POST/PUT 请求设置 `Content-Type: application/json` |
| 返回 200 但 data 为 null | 业务逻辑返回 null 未封装 | 检查 Service 层是否返回了正确数据 |
| 前端收不到响应 | 跨域问题（CORS） | 后端添加 `@CrossOrigin` 或配置 CORS 过滤器 |
| Token 校验失败 | Token 过期或格式错误 | 检查 Token 是否正确传递；确认是否包含 Bearer 前缀 |
| 中文乱码 | 字符集不一致 | 数据库、表、连接 URL 都使用 utf8mb4 |
| 分页数据不对 | page 从 0 还是从 1 开始 | 前后端约定统一；后端做 (page-1)*pageSize 计算 |

---

## 十、速查总结

```
+----------------------------------------------------------+
|              API 接口文档速查                               |
+----------------------------------------------------------+
|                                                          |
|  URL 设计：/资源复数/{id}                                 |
|  方法语义：GET查 POST增 PUT改 DELETE删                    |
|                                                          |
|  统一响应：                                               |
|  {                                                       |
|    "code": 1,        // 1成功 0失败                      |
|    "msg": "提示信息",                                    |
|    "data": {} / [] / null                                |
|  }                                                       |
|                                                          |
|  分页响应：                                               |
|  { "total": 35, "rows": [...] }                          |
|                                                          |
|  分页请求：page=1&pageSize=10                            |
|  SQL分页：LIMIT (page-1)*pageSize, pageSize              |
|                                                          |
|  认证传递：Authorization: token                          |
|  文件上传：Content-Type: multipart/form-data             |
|                                                          |
+----------------------------------------------------------+
```

---

## 十一、参考文档

1. [RESTful API 设计最佳实践](https://restfulapi.net/)
2. [MDN - HTTP 请求方法](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Methods)
3. [Knife4j 官方文档](https://doc.xiaominfo.com/)
4. [Postman 官方文档](https://learning.postman.com/)
5. Tlias 项目课程文档：`docs/02-后端基础（4天）/07-工程规范与Restful-API.md`
