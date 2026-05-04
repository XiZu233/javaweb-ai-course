# HTTP 协议详解

## 学习目标

- 理解 HTTP 请求和响应的基本结构
- 掌握常见的 HTTP 方法、状态码和请求头
- 能够使用浏览器开发者工具分析 HTTP 通信
- 理解前后端交互的完整流程

## 核心知识点

### 1. HTTP 是什么

HTTP（HyperText Transfer Protocol）是浏览器与服务器之间通信的协议。它是**无状态**的，即每次请求都是独立的，服务器不会记住之前的请求。

### 2. HTTP 请求结构

```
POST /api/users HTTP/1.1              <-- 请求行：方法 + URL + 协议版本
Host: localhost:8080                  <-- 请求头
Content-Type: application/json        <-- 请求头
Authorization: Bearer xxx             <-- 请求头
                                      <-- 空行
{ "name": "张三", "age": 25 }        <-- 请求体（Body）
```

### 3. HTTP 响应结构

```
HTTP/1.1 200 OK                       <-- 状态行：协议 + 状态码 + 描述
Content-Type: application/json        <-- 响应头
Content-Length: 123                   <-- 响应头
                                      <-- 空行
{ "code": 200, "data": { ... } }     <-- 响应体（Body）
```

### 4. HTTP 方法

| 方法 | 用途 | 幂等性 | 安全性 |
|------|------|--------|--------|
| GET | 获取资源 | 是 | 是 |
| POST | 创建资源 | 否 | 否 |
| PUT | 更新资源（全量） | 是 | 否 |
| PATCH | 更新资源（部分） | 否 | 否 |
| DELETE | 删除资源 | 是 | 否 |

> **幂等性**：多次执行结果相同。**安全性**：不改变服务器状态。

### 5. 常见状态码

| 状态码 | 含义 | 场景 |
|--------|------|------|
| 200 | OK | 请求成功 |
| 201 | Created | 资源创建成功 |
| 204 | No Content | 删除成功，无返回内容 |
| 400 | Bad Request | 请求参数错误 |
| 401 | Unauthorized | 未登录/Token无效 |
| 403 | Forbidden | 无权限访问 |
| 404 | Not Found | 资源不存在 |
| 500 | Internal Server Error | 服务器内部错误 |

### 6. 常见请求头/响应头

**请求头**：

| 头字段 | 说明 |
|--------|------|
| Content-Type | 请求体数据格式（application/json、multipart/form-data） |
| Authorization | 认证信息（Bearer Token、Basic） |
| Accept | 客户端可接受的响应格式 |
| User-Agent | 客户端标识 |

**响应头**：

| 头字段 | 说明 |
|--------|------|
| Content-Type | 响应体数据格式 |
| Set-Cookie | 设置 Cookie |
| Location | 重定向地址 |
| Cache-Control | 缓存策略 |

### 7. 使用浏览器开发者工具分析 HTTP

打开 Chrome 开发者工具（F12）→ Network 标签：

1. **All/XHR**：过滤 AJAX 请求
2. **Headers**：查看请求/响应头
3. **Preview/Response**：查看响应数据
4. **Timing**：查看请求各阶段耗时

### 8. 前后端交互完整流程

```
用户点击按钮
    ↓
前端发送 HTTP 请求（Axios）
    ↓
浏览器处理请求头、跨域检查
    ↓
后端 Controller 接收请求，解析参数
    ↓
Service 处理业务逻辑
    ↓
Mapper 执行 SQL
    ↓
数据库返回数据
    ↓
后端封装统一响应（Result）
    ↓
HTTP 响应返回前端
    ↓
Axios 拦截器处理响应
    ↓
前端更新页面
```

## 动手练习

### 练习 1：抓包分析

1. 启动版本A项目
2. 打开浏览器开发者工具 → Network
3. 点击"部门管理"页面
4. 观察请求：方法、URL、请求头、响应数据
5. 找到 `/depts` 请求，分析完整的请求/响应流程

### 练习 2：模拟请求

使用 Postman 或 curl 发送请求：

```bash
# GET 请求
curl http://localhost:8080/depts

# POST 请求
curl -X POST http://localhost:8080/depts \
  -H "Content-Type: application/json" \
  -d '{"name": "测试部"}'

# 带 Token
curl http://localhost:8080/emps \
  -H "token: your-jwt-token"
```

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| 404 | URL 错误或后端未启动 | 检查地址和端口号 |
| 400 | 请求参数格式错误 | 检查 Content-Type 和请求体 JSON |
| 401 | Token 缺失或过期 | 检查请求头中的 token |
| 415 | Content-Type 不支持 | 确保发送的是后端支持的格式 |
| CORS | 跨域被阻止 | 配置 CORS 或使用代理 |

## 本节小结

HTTP 是 Web 开发的根基。理解请求/响应结构、状态码和头部字段，能够帮助你快速定位前后端交互中的问题。善用浏览器开发者工具抓包分析，是后端开发者的必备技能。

## 参考文档

- [MDN HTTP](https://developer.mozilla.org/zh-CN/docs/Web/HTTP)
- [HTTP 状态码](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Status)
