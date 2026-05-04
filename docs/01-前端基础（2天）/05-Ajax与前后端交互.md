# Ajax 与前后端交互

## 学习目标

学完本节后，你将能够：
- 理解 Ajax 的本质：网页不刷新就能获取数据
- 使用 Axios 发送各种 HTTP 请求
- 封装统一的请求工具（拦截器、错误处理）
- 理解 RESTful API 规范，正确调用后端接口
- 解决开发中的跨域问题

---

## 核心知识点

### 1. 什么是 Ajax——网页的"异步快递"

#### 1.1 传统网页的问题

想象你在一个购物网站：

**传统方式（没有 Ajax）：**
1. 你点击"下一页" → 浏览器发送请求 → 等待服务器响应
2. 服务器返回完整的 HTML 页面
3. 浏览器重新加载整个页面（白屏闪烁）
4. 页面从头开始渲染，你之前滚动到哪儿忘了

**问题：**
- 每次交互都要刷新整个页面，体验很差
- 浪费带宽（只改了一小部分内容，却传输了整个页面）
- 用户等待时间长

#### 1.2 Ajax 的诞生

Ajax（Asynchronous JavaScript and XML）让网页可以在**不刷新**的情况下，与服务器交换数据。

```
传统方式：        Ajax 方式：
┌─────────┐     ┌─────────┐
│  点击    │     │  点击    │
└────┬────┘     └────┬────┘
     │               │
     ▼               ▼
┌─────────┐     ┌─────────┐
│ 请求页面  │     │ 请求数据 │  ← 只请求需要的数据（JSON）
│ (HTML)  │     │ (JSON)  │
└────┬────┘     └────┬────┘
     │               │
     ▼               ▼
┌─────────┐     ┌─────────┐
│ 白屏加载  │     │ 局部更新 │  ← 只更新页面的某一部分
│ 整个页面  │     │ 不刷新  │
└─────────┘     └─────────┘
```

**Ajax 的实际应用：**
- 无限滚动加载（微博、知乎）
- 搜索框实时提示（百度、Google）
- 表单实时校验（注册时检查用户名是否已存在）
- 点赞/收藏（不刷新页面）

#### 1.3 Ajax 的技术本质

Ajax 不是一项新技术，而是多项技术的组合：

| 技术 | 作用 |
|------|------|
| JavaScript | 发起请求、处理响应 |
| XMLHttpRequest / Fetch | 浏览器内置的 HTTP 请求 API |
| JSON | 数据格式（替代了早期的 XML） |
| DOM | 更新页面内容 |

现代开发中，我们不再直接使用底层的 XMLHttpRequest，而是使用封装好的库——**Axios**。

---

### 2. Axios 基础用法

#### 2.1 什么是 Axios

Axios 是一个基于 Promise 的 HTTP 库，用于浏览器和 Node.js：
- 从浏览器创建 XMLHttpRequest
- 从 Node.js 创建 HTTP 请求
- 支持 Promise API
- 自动转换 JSON 数据
- 支持请求和响应拦截器
- 支持取消请求

#### 2.2 安装 Axios

```bash
# Vue2 项目（版本 A）
npm install axios

# Vue3 项目（版本 B）
npm install axios
```

#### 2.3 基本用法

```javascript
import axios from 'axios';

// ========== GET 请求：获取数据 ==========
// 方式一： then/catch
axios.get('http://localhost:8080/depts')
    .then(response => {
        // response 是 Axios 的响应对象
        console.log('状态码：', response.status);     // 200
        console.log('响应头：', response.headers);
        console.log('响应数据：', response.data);      // 后端返回的 JSON
    })
    .catch(error => {
        console.error('请求失败：', error.message);
    });

// 方式二：async/await（推荐！代码更易读）
async function fetchDepts() {
    try {
        const response = await axios.get('http://localhost:8080/depts');
        return response.data;
    } catch (error) {
        console.error('请求失败：', error.message);
        throw error;
    }
}

// ========== GET 带查询参数 ==========
// 查询第 1 页，每页 10 条
axios.get('http://localhost:8080/emps', {
    params: {
        page: 1,
        pageSize: 10,
        name: '张三'
    }
});
// 最终请求的 URL：/emps?page=1&pageSize=10&name=张三

// ========== POST 请求：新增数据 ==========
axios.post('http://localhost:8080/depts', {
    name: '新部门'
});

// ========== PUT 请求：修改数据 ==========
axios.put('http://localhost:8080/depts', {
    id: 1,
    name: '修改后的部门'
});

// ========== DELETE 请求：删除数据 ==========
axios.delete('http://localhost:8080/depts/1');
```

#### 2.4 Axios 配置选项

```javascript
axios({
    method: 'POST',                    // HTTP 方法
    url: 'http://localhost:8080/depts', // 请求地址
    data: { name: '新部门' },           // 请求体（POST/PUT/PATCH）
    params: { page: 1 },               // URL 查询参数（GET）
    headers: {                         // 请求头
        'Content-Type': 'application/json',
        'Authorization': 'Bearer token123'
    },
    timeout: 10000                     // 超时时间（毫秒）
});
```

---

### 3. 封装统一的请求工具

在实际项目中，我们不会每个地方都直接调用 `axios.get()`，而是封装一个统一的请求工具。

#### 3.1 为什么要封装

**不封装的问题：**
- 每个请求都要写完整的 URL
- Token 需要在每个请求中手动添加
- 错误处理分散在各处
- 响应数据格式不统一

**封装后的好处：**
- 统一配置 baseURL
- 统一添加 Token
- 统一错误处理
- 统一响应格式转换

#### 3.2 版本 A 的封装（Vue2 + ElementUI）

```javascript
// src/utils/request.js
import axios from 'axios';
import { Message } from 'element-ui';
import router from '@/router';

// 创建 Axios 实例
const request = axios.create({
    // baseURL：所有请求的前缀
    // 开发时请求 /api/depts，实际发送到 http://localhost:8080/depts
    baseURL: process.env.VUE_APP_BASE_API || '/api',

    // 超时时间：10 秒
    timeout: 10000
});

// ========== 请求拦截器 ==========
// 在请求发送之前做一些处理
request.interceptors.request.use(
    config => {
        // 从 localStorage 获取 Token
        const token = localStorage.getItem('token');

        // 如果 Token 存在，添加到请求头
        if (token) {
            config.headers['token'] = token;
            // 注意：版本 A 的后端用 'token' 作为请求头名
            // 版本 B 可能用 'Authorization': 'Bearer ' + token
        }

        return config;  // 必须返回 config
    },
    error => {
        // 请求发送失败（通常是网络问题）
        console.error('请求发送失败：', error);
        return Promise.reject(error);
    }
);

// ========== 响应拦截器 ==========
// 在收到响应后做一些处理
request.interceptors.response.use(
    response => {
        // response.data 是后端返回的数据
        const res = response.data;

        // 版本 A 的响应格式：{ code: 1, msg: 'success', data: ... }
        if (res.code === 1) {
            // 成功，返回 data
            return res.data;
        } else {
            // 业务错误（如参数校验失败、权限不足）
            Message.error(res.msg || '请求失败');

            // 未登录
            if (res.msg === 'NOT_LOGIN') {
                localStorage.removeItem('token');
                router.push('/login');
            }

            return Promise.reject(new Error(res.msg));
        }
    },
    error => {
        // HTTP 错误（404、500 等）
        console.error('响应错误：', error);
        Message.error(error.message || '网络错误');
        return Promise.reject(error);
    }
);

export default request;
```

#### 3.3 版本 B 的封装（Vue3 + TypeScript）

```typescript
// src/utils/request.ts
import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';

const request = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 10000
});

// 请求拦截器
request.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = 'Bearer ' + token;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// 响应拦截器
request.interceptors.response.use(
    (response) => {
        const res = response.data;
        if (res.code === 200) {
            return res.data;
        } else {
            ElMessage.error(res.message || '请求失败');
            if (res.code === 401) {
                localStorage.removeItem('token');
                router.push('/login');
            }
            return Promise.reject(new Error(res.message));
        }
    },
    (error) => {
        ElMessage.error(error.message || '网络错误');
        return Promise.reject(error);
    }
);

export default request;
```

---

### 4. 封装 API 模块

按照业务模块封装 API，代码更清晰：

```javascript
// src/api/dept.js —— 部门相关接口
import request from '@/utils/request';

// 查询部门列表
export const getDeptList = () => request.get('/depts');

// 根据 ID 查询部门
export const getDeptById = (id) => request.get(`/depts/${id}`);

// 新增部门
export const addDept = (data) => request.post('/depts', data);

// 修改部门
export const updateDept = (data) => request.put('/depts', data);

// 删除部门
export const deleteDept = (id) => request.delete(`/depts/${id}`);
```

```javascript
// src/api/emp.js —— 员工相关接口
import request from '@/utils/request';

// 分页查询员工
export const getEmpList = (params) => request.get('/emps', { params });

// 新增员工
export const addEmp = (data) => request.post('/emps', data);

// 删除员工（批量）
export const deleteEmps = (ids) => request.delete(`/emps/${ids}`);

// 上传文件
export const uploadFile = (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return request.post('/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    });
};
```

**在组件中使用：**

```vue
<script setup>
import { ref, onMounted } from 'vue';
import { getDeptList, deleteDept } from '@/api/dept';

const deptList = ref([]);

const fetchData = async () => {
    try {
        deptList.value = await getDeptList();
    } catch (error) {
        console.error('获取数据失败：', error);
    }
};

const handleDelete = async (id) => {
    try {
        await deleteDept(id);
        // 删除成功后重新获取列表
        fetchData();
    } catch (error) {
        // 错误已在拦截器中提示
    }
};

onMounted(() => {
    fetchData();
});
<\/script>
```

---

### 5. RESTful API 规范

RESTful 是一种 API 设计风格，核心原则：

#### 5.1 URL 设计

```
资源（名词） + HTTP 方法（动作）

部门管理：
  GET    /depts        → 查询所有部门
  GET    /depts/1      → 查询 ID=1 的部门
  POST   /depts        → 新增部门
  PUT    /depts/1      → 修改 ID=1 的部门
  DELETE /depts/1      → 删除 ID=1 的部门

员工管理：
  GET    /emps?page=1&pageSize=10  → 分页查询
  GET    /emps/1                   → 查询员工详情
  POST   /emps                     → 新增员工
  PUT    /emps                     → 修改员工
  DELETE /emps/1,2,3               → 批量删除
```

#### 5.2 请求和响应格式

**版本 A（本课程）：**

```json
// 成功响应
{
    "code": 1,
    "msg": "success",
    "data": [
        { "id": 1, "name": "研发部" },
        { "id": 2, "name": "市场部" }
    ]
}

// 失败响应
{
    "code": 0,
    "msg": "部门名称不能为空",
    "data": null
}
```

**版本 B（基于 yudao）：**

```json
// 成功响应
{
    "code": 200,
    "message": "success",
    "data": { ... }
}
```

---

### 6. 跨域问题——开发环境的"拦路虎"

#### 6.1 什么是跨域

浏览器的**同源策略**（Same-Origin Policy）限制：
- 协议相同（http/https）
- 域名相同
- 端口相同

**开发时的场景：**
- 前端运行在 `http://localhost:3000`
- 后端运行在 `http://localhost:8080`
- **端口不同 → 跨域！** 浏览器会阻止请求。

#### 6.2 解决方案一：开发环境代理（推荐）

前端配置代理，把对 `/api` 的请求转发到后端：

**版本 A（Vue CLI）：**

```javascript
// vue.config.js
module.exports = {
    devServer: {
        proxy: {
            '/api': {
                target: 'http://localhost:8080',  // 后端地址
                changeOrigin: true,               // 改变请求源头
                pathRewrite: {
                    '^/api': ''                   // 把 /api 替换为空
                }
            }
        }
    }
};
```

**配置后：**
- 前端请求 `/api/depts`
- 代理转发到 `http://localhost:8080/depts`
- 浏览器看到的前端 → 前端（同源），没有跨域

**版本 B（Vite）：**

```typescript
// vite.config.ts
export default defineConfig({
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:48080',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, '')
            }
        }
    }
});
```

#### 6.3 解决方案二：后端开启 CORS

后端允许特定来源访问：

```java
// SpringBoot 配置
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")           // 允许所有路径
                .allowedOrigins("*")         // 允许所有来源（生产环境要限制！）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);               // 预检请求缓存时间
    }
}
```

**注意：**
- 开发环境用**代理**
- 生产环境用**后端 CORS** 或**同域部署**（前端静态文件放在后端服务器）

---

## 动手练习

### 练习 1：封装 API 模块

在 `src/api/` 目录下创建 `user.js`：

```javascript
import request from '@/utils/request';

export const login = (data) => request.post('/login', data);
export const getUserInfo = () => request.get('/user/info');
export const logout = () => request.post('/logout');
```

### 练习 2：登录流程

实现完整的登录流程：

```vue
<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { login } from '@/api/user';

const router = useRouter();
const userStore = useUserStore();

const form = ref({
    username: '',
    password: ''
});

const handleLogin = async () => {
    try {
        const res = await login(form.value);
        // 保存 Token
        userStore.setToken(res.token);
        // 跳转到首页
        router.push('/');
    } catch (error) {
        // 错误已在拦截器中提示
    }
};
<\/script>
```

---

## 常见错误排查

### 阶段 1：网络层问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Network Error` | 后端服务没启动 | 检查后端是否正常运行 |
| `timeout of 10000ms exceeded` | 请求超时 | 检查网络连接，或增加 timeout |
| `404 Not Found` | 接口地址错误 | 检查 URL 和后端接口是否匹配 |

### 阶段 2：HTTP 错误

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `401 Unauthorized` | Token 缺失或过期 | 检查 localStorage 中的 Token |
| `403 Forbidden` | 没有权限 | 检查用户角色 |
| `400 Bad Request` | 参数错误 | 检查请求参数格式 |
| `500 Internal Server Error` | 后端报错 | 查看后端日志 |

### 阶段 3：跨域问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `CORS policy` 错误 | 跨域被阻止 | 配置代理（开发）或后端开启 CORS |
| 代理不生效 | 配置错误 | 检查 proxy 配置和请求路径 |

### 阶段 4：数据问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 返回数据格式不对 | 拦截器处理错误 | 检查响应拦截器的逻辑 |
| 请求没携带 Token | 请求拦截器未生效 | 检查请求拦截器配置 |
| 中文乱码 | 编码问题 | 确保前后端都使用 UTF-8 |

---

## 本节小结

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      Ajax 与前后端交互                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Ajax：网页不刷新就能获取数据                                             │
│  Axios：基于 Promise 的 HTTP 库，最常用                                   │
│                                                                          │
│  Axios 基本用法：                                                        │
│    • axios.get(url, { params })                                          │
│    • axios.post(url, data)                                               │
│    • axios.put(url, data)                                                │
│    • axios.delete(url)                                                   │
│    • 推荐用 async/await                                                  │
│                                                                          │
│  封装请求工具：                                                          │
│    • 统一 baseURL                                                        │
│    • 请求拦截器：自动添加 Token                                           │
│    • 响应拦截器：统一处理错误、提取 data                                  │
│                                                                          │
│  封装 API 模块：                                                         │
│    • 按业务模块拆分（dept.js、emp.js、user.js）                          │
│    • 组件中直接调用封装好的函数                                           │
│                                                                          │
│  RESTful API：                                                           │
│    • URL = 资源（名词）                                                   │
│    • HTTP 方法 = 动作（GET/POST/PUT/DELETE）                              │
│    • 统一响应格式：{ code, msg, data }                                    │
│                                                                          │
│  跨域：                                                                  │
│    • 开发环境：配置代理（vue.config.js / vite.config.ts）                 │
│    • 生产环境：后端开启 CORS 或同域部署                                    │
│                                                                          │
│  避坑指南：                                                              │
│    • 永远封装请求工具，不要直接调用 axios                                 │
│    • 拦截器中统一处理错误，不要在每个请求中写错误处理                      │
│    • 开发环境用代理解决跨域                                               │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 参考文档

- [Axios 官方文档](https://axios-http.com/)
- [MDN HTTP 概述](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Overview)
- [RESTful API 设计指南](https://restfulapi.net/)
- [MDN 跨域资源共享 CORS](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/CORS)
