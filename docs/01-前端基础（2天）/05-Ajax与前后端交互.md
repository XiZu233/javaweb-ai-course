# Ajax 与前后端交互

## 学习目标

- 理解 HTTP 请求的基本流程
- 掌握 Axios 的使用方法
- 能够封装统一的请求拦截器
- 理解 RESTful API 的规范

## 核心知识点

### 1. 什么是 Ajax

Ajax（Asynchronous JavaScript and XML）允许网页在不刷新整个页面的情况下，与服务器交换数据并更新部分内容。

现代开发中，Ajax 通常通过 `fetch` 或 `axios` 实现。

### 2. Axios 基础用法

```javascript
import axios from 'axios'

// GET 请求
axios.get('/api/users')
    .then(response => {
        console.log(response.data)
    })
    .catch(error => {
        console.error(error)
    })

// GET 带参数
axios.get('/api/users', {
    params: { page: 1, size: 10 }
})

// POST 请求
axios.post('/api/users', {
    name: '张三',
    age: 25
})

// async/await 写法（推荐）
async function fetchUsers() {
    try {
        const response = await axios.get('/api/users')
        return response.data
    } catch (error) {
        console.error('请求失败：', error.message)
    }
}
```

### 3. 请求/响应拦截器

在项目中，通常需要封装一个统一的请求工具：

```javascript
// utils/request.js
import axios from 'axios'
import { Message } from 'element-ui'
import router from '@/router'

const request = axios.create({
    baseURL: process.env.VUE_APP_BASE_API || '/api',
    timeout: 10000
})

// 请求拦截器：在请求发送前做一些处理
request.interceptors.request.use(
    config => {
        // 在请求头中添加 Token
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = 'Bearer ' + token
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 响应拦截器：在收到响应后做一些处理
request.interceptors.response.use(
    response => {
        const res = response.data
        // 如果后端返回的 code 不是 200，说明业务出错
        if (res.code !== 200) {
            Message.error(res.message || '请求失败')
            // 401：未登录或 Token 过期
            if (res.code === 401) {
                localStorage.removeItem('token')
                router.push('/login')
            }
            return Promise.reject(new Error(res.message))
        }
        return res.data
    },
    error => {
        Message.error(error.message || '网络错误')
        return Promise.reject(error)
    }
)

export default request
```

### 4. RESTful API 规范

RESTful 是一种 API 设计风格，使用 HTTP 方法表示对资源的操作：

| HTTP 方法 | URL | 操作 | 说明 |
|-----------|-----|------|------|
| GET | /api/users | 查询列表 | 获取所有用户 |
| GET | /api/users/1 | 查询单个 | 获取 ID 为 1 的用户 |
| POST | /api/users | 新增 | 创建新用户 |
| PUT | /api/users/1 | 修改 | 更新 ID 为 1 的用户 |
| DELETE | /api/users/1 | 删除 | 删除 ID 为 1 的用户 |

请求响应格式统一：

```json
// 成功响应
{
    "code": 200,
    "message": "success",
    "data": { /* 实际数据 */ }
}

// 错误响应
{
    "code": 500,
    "message": "用户不存在",
    "data": null
}
```

### 5. 跨域问题

当前端（localhost:3000）调用后端（localhost:8080）时，浏览器会阻止跨域请求。

**解决方案**：

1. **开发环境**：配置代理（推荐）
```javascript
// vue.config.js
developmentServer: {
    proxy: {
        '/api': {
            target: 'http://localhost:8080',
            changeOrigin: true
        }
    }
}
```

2. **生产环境**：后端开启 CORS
```java
// SpringBoot 后端配置
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*");
    }
}
```

## 动手练习

### 练习 1：封装 API 模块

在 `src/api/user.js` 中封装用户相关的 API：

```javascript
import request from '@/utils/request'

export const getUserList = (params) => request.get('/users', { params })
export const addUser = (data) => request.post('/users', data)
export const updateUser = (id, data) => request.put(`/users/${id}`, data)
export const deleteUser = (id) => request.delete(`/users/${id}`)
```

### 练习 2：登录流程

实现完整的登录流程：
1. 前端提交用户名和密码
2. 后端验证后返回 JWT Token
3. 前端存储 Token（localStorage）
4. 后续请求自动携带 Token
5. Token 过期时跳转登录页

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| 404 Not Found | 接口地址错误或后端未启动 | 检查 URL 和后端服务状态 |
| CORS 错误 | 跨域被阻止 | 配置代理或后端开启 CORS |
| 401 Unauthorized | Token 缺失或过期 | 检查 localStorage 中的 Token |
| 请求超时 | 网络慢或后端处理慢 | 增加 timeout 或优化后端 |
| 参数格式错误 | 发送的数据格式不对 | 检查 Content-Type 和数据结构 |

## 本节小结

前后端交互是现代 Web 开发的核心环节。掌握 Axios 封装、拦截器和 RESTful 规范，你就能顺畅地与后端 API 协作。跨域问题是开发中的常见坑，理解其原理和解决方案至关重要。

## 参考文档

- [Axios 官方文档](https://axios-http.com/)
- [MDN HTTP 概述](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Overview)
- [RESTful API 设计指南](https://restfulapi.net/)
