# Vue3 工程化与 ElementPlus（版本 B 专属）

## 学习目标

- 掌握 Vite + Vue3 项目的创建和配置
- 理解 Composition API 与 Options API 的区别
- 掌握 Pinia 状态管理
- 了解 Vue3 与 Vue2 的主要差异

## 核心知识点

### 1. 项目初始化

```bash
npm create vue@latest tlias-pro-frontend

# 选择配置：
# ✔ TypeScript
# ✔ Vue Router
# ✔ Pinia
# ✔ ESLint
```

```bash
cd tlias-pro-frontend
npm install
npm install element-plus
npm install axios
npm run dev
```

### 2. Vue3 Composition API

Vue3 引入了 `<script setup>` 语法，代码更简洁：

```vue
<template>
  <div>
    <h1>{{ message }}</h1>
    <el-button @click="changeMessage">点击</el-button>
    <p>计数：{{ count }}</p>
    <el-button @click="count++">+1</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const message = ref('Hello Vue3')
const count = ref(0)

const changeMessage = () => {
  message.value = 'Hello World'
}

onMounted(() => {
  console.log('DOM 挂载完成')
})
</script>
```

**核心 API 对比**：

| Vue2 (Options API) | Vue3 (Composition API) |
|-------------------|----------------------|
| data | ref / reactive |
| computed | computed() |
| watch | watch() / watchEffect() |
| methods | 普通函数 |
| created / mounted | onMounted / onUnmounted |
| this.xxx | 直接引用变量 |

### 3. 引入 ElementPlus

```typescript
// main.ts
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'

const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.use(createPinia())
app.mount('#app')
```

### 4. Pinia 状态管理

Pinia 是 Vue3 官方推荐的状态管理库，比 Vuex 更轻量、TypeScript 支持更好。

```typescript
// store/user.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  // state
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<any>({})

  // getters
  const isLoggedIn = computed(() => !!token.value)

  // actions
  const setToken = (val: string) => {
    token.value = val
    localStorage.setItem('token', val)
  }

  const logout = () => {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
  }

  return { token, userInfo, isLoggedIn, setToken, logout }
})
```

**组件中使用**：

```vue
<script setup lang="ts">
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const logout = () => {
  userStore.logout()
}
</script>
```

### 5. Axios 封装（TypeScript 版）

```typescript
// utils/request.ts
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = 'Bearer ' + userStore.token
  }
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      if (res.code === 401) {
        useUserStore().logout()
        router.push('/login')
      }
      return Promise.reject(new Error(res.msg))
    }
    return res.data
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
```

### 6. Vue3 与 Vue2 的主要差异

| 特性 | Vue2 | Vue3 |
|------|------|------|
| 响应式原理 | Object.defineProperty | Proxy |
| 新增属性响应 | 需用 Vue.set | 直接赋值即可 |
| 根节点 | 只能有一个 | 可以有多个 |
| 生命周期 | created / beforeDestroy | setup 中 onMounted / onUnmounted |
| 性能 | 好 | 更好（静态提升、PatchFlag） |
| TypeScript | 支持一般 | 原生支持 |

## 动手练习

### 练习 1：搭建 Vue3 项目

1. 使用 create-vue 创建项目
2. 引入 ElementPlus 和 Pinia
3. 配置 Axios 和路由
4. 编写一个简单的计数器组件体验 Composition API

### 练习 2：Pinia 状态管理

创建 userStore，实现登录状态的管理：登录后存储 token，页面刷新后从 localStorage 恢复 token。

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| ref 值未更新 | 直接赋值而非 .value | ref 需要通过 .value 读写 |
| 响应式丢失 | 解构 reactive 对象 | 使用 toRefs 或保持对象引用 |
| TS 类型报错 | 类型推断失败 | 显式指定泛型参数 |

## 本节小结

Vue3 + Vite + TypeScript + Pinia + ElementPlus 是版本 B 的前端技术栈，也是当前企业的主流选择。Composition API 让逻辑复用更方便，Pinia 让状态管理更简洁，TypeScript 让代码更健壮。

## 参考文档

- [Vue3 官方文档](https://cn.vuejs.org/)
- [ElementPlus 官方文档](https://element-plus.org/)
- [Pinia 官方文档](https://pinia.vuejs.org/)

