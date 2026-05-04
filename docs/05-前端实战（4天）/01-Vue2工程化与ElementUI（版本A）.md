# Vue2 工程化与 ElementUI（版本 A 专属）

## 学习目标

- 掌握 Vue CLI 创建项目的流程
- 理解 Vue2 Options API 的核心概念
- 掌握 ElementUI 常用组件的使用
- 能够配置 Axios 请求拦截器和路由

## 核心知识点

### 1. 项目初始化

```bash
# 安装 Vue CLI
npm install -g @vue/cli

# 创建项目
vue create tlias-frontend

# 选择预设：Manually select features
# 勾选：Babel、Router、Vuex、CSS Pre-processors、Linter
```

### 2. 引入 ElementUI

```bash
npm install element-ui -S
```

```javascript
// main.js
import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import App from './App.vue'
import router from './router'

Vue.use(ElementUI)

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
```

### 3. Vue2 Options API 核心

```vue
<template>
  <div>
    <h1>{{ message }}</h1>
    <el-button @click="changeMessage">点击</el-button>
  </div>
</template>

<script>
export default {
  data() {
    return {
      message: 'Hello Vue2'
    }
  },
  methods: {
    changeMessage() {
      this.message = 'Hello World'
    }
  },
  created() {
    console.log('组件创建完成')
  },
  mounted() {
    console.log('DOM 挂载完成')
  }
}
</script>
```

**常用选项**：

| 选项 | 说明 |
|------|------|
| data | 定义响应式数据 |
| methods | 定义方法 |
| computed | 计算属性（缓存） |
| watch | 监听数据变化 |
| props | 接收父组件传值 |
| created / mounted | 生命周期钩子 |

### 4. Axios 封装

```javascript
// utils/request.js
import axios from 'axios'
import { Message } from 'element-ui'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：添加 Token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.token = token
    }
    return config
  }
)

// 响应拦截器：处理错误和登录失效
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 0) {
      Message.error(res.msg || '请求失败')
      if (res.msg === 'NOT_LOGIN') {
        localStorage.removeItem('token')
        router.push('/login')
      }
      return Promise.reject(new Error(res.msg))
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

**挂载到 Vue 原型**（main.js）：

```javascript
import request from './utils/request'
Vue.prototype.$request = request
```

### 5. Vue Router 基础

```javascript
// router/index.js
import Vue from 'vue'
import VueRouter from 'vue-router'
import Login from '../views/login/index.vue'
import Layout from '../views/layout/index.vue'

Vue.use(VueRouter)

const routes = [
  { path: '/login', component: Login },
  {
    path: '/',
    component: Layout,
    redirect: '/dept',
    children: [
      { path: 'dept', component: () => import('../views/dept/index.vue'), meta: { title: '部门管理' } },
      { path: 'emp', component: () => import('../views/emp/index.vue'), meta: { title: '员工管理' } }
    ]
  }
]

export default new VueRouter({
  mode: 'history',
  routes
})
```

### 6. 开发环境代理

```javascript
// vue.config.js
module.exports = {
  devServer: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
```

## 动手练习

### 练习 1：搭建 Vue2 项目

1. 使用 Vue CLI 创建项目
2. 引入 ElementUI
3. 配置 Axios 请求工具
4. 配置路由和代理

### 练习 2：ElementUI 组件体验

在页面中使用以下组件，熟悉其属性和事件：
- el-button、el-input、el-select
- el-table、el-pagination
- el-dialog、el-form

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| ElementUI 样式不生效 | 未引入 CSS 文件 | import 'element-ui/lib/theme-chalk/index.css' |
| 跨域错误 | 前后端端口不同 | 配置 vue.config.js proxy |
| 路由跳转白屏 | 缺少 router-view | 在布局组件中添加 router-view |

## 本节小结

Vue2 + ElementUI 是版本 A 的前端技术栈。掌握 Options API、组件库使用和 Axios 封装，是完成管理后台页面的基础。

## 参考文档

- [Vue2 官方文档](https://v2.vuejs.org/)
- [ElementUI 官方文档](https://element.eleme.io/)

