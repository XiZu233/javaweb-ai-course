# 附录 B：API 接口文档

## 版本 A 接口汇总

### 部门管理

| 方法 | URL | 功能 | 请求参数 |
|------|-----|------|---------|
| GET | /depts | 查询部门列表 | - |
| GET | /depts/{id} | 根据ID查询 | Path: id |
| POST | /depts | 新增部门 | Body: {name} |
| PUT | /depts | 修改部门 | Body: {id, name} |
| DELETE | /depts/{id} | 删除部门 | Path: id |

### 员工管理

| 方法 | URL | 功能 | 请求参数 |
|------|-----|------|---------|
| GET | /emps | 分页查询 | Query: name, gender, begin, end, page, pageSize |
| GET | /emps/{id} | 根据ID查询 | Path: id |
| POST | /emps | 新增员工 | Body: Emp 对象 |
| PUT | /emps | 修改员工 | Body: Emp 对象 |
| DELETE | /emps/{ids} | 批量删除 | Path: ids(逗号分隔) |

### 文件上传

| 方法 | URL | 功能 | 请求参数 |
|------|-----|------|---------|
| POST | /upload | 文件上传 | FormData: file |

### 登录

| 方法 | URL | 功能 | 请求参数 |
|------|-----|------|---------|
| POST | /login | 用户登录 | Body: {username, password} |

**响应格式**：

```json
// 成功
{ "code": 1, "msg": "success", "data": /* 实际数据 */ }

// 失败
{ "code": 0, "msg": "错误信息", "data": null }
```

## 版本 B 接口汇总（AI 模块）

| 方法 | URL | 功能 | 请求参数 |
|------|-----|------|---------|
| POST | /ai/nl2sql | 自然语言转SQL | Body: {question} |
| POST | /ai/rag/ingest | 上传知识库文档 | Body: multipart/form-data |
| POST | /ai/rag | 知识库问答 | Body: {question} |
| POST | /ai/resume/parse | AI简历解析 | Body: multipart/form-data |
| GET | /ai/chat/stream | AI流式对话 | Query: message |

