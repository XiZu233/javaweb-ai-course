# Vue3 快速入门（版本B）

## 学习目标

- 理解 Vue3 相比 Vue2 的核心改进
- 掌握组合式 API 和 `<script setup>` 语法
- 理解响应式系统（ref/reactive）
- 能够使用 Vite 构建 Vue3 项目

## 核心知识点

### 1. 创建 Vue3 项目

```bash
# 使用官方脚手架（推荐）
npm create vue@latest

# 选项选择：
# - Project name: tlias-pro-frontend
# - Add TypeScript? Yes
# - Add JSX Support? No
# - Add Vue Router? Yes
# - Add Pinia? Yes
# - Add Vitest? No
# - Add Cypress? No
# - Add ESLint? Yes
# - Add Prettier? Yes

cd tlias-pro-frontend
npm install
npm run dev
```

### 2. 组合式 API vs 选项式 API

Vue3 推荐使用组合式 API，将相关逻辑组织在一起：

```vue
<script setup>
import { ref, computed, watch, onMounted } from 'vue'

// 响应式状态
const count = ref(0)
const user = reactive({ name: '张三', age: 25 })

// 计算属性
const doubleCount = computed(() => count.value * 2)

// 方法
function increment() {
    count.value++
}

// 监听器
watch(count, (newVal, oldVal) => {
    console.log(`count 从 ${oldVal} 变为 ${newVal}`)
})

// 生命周期
onMounted(() => {
    console.log('组件已挂载')
})
</script>

<template>
    <button @click="increment">Count: {{ count }} (x2: {{ doubleCount }})</button>
    <p>用户名：{{ user.name }}</p>
</template>
```

### 3. ref 与 reactive

```javascript
import { ref, reactive } from 'vue'

// ref：适用于基本类型，返回 { value: T }
const count = ref(0)
console.log(count.value) // 0
count.value++            // 修改需要用 .value

// reactive：适用于对象，深层响应式
const state = reactive({
    user: { name: '张三', age: 25 },
    list: []
})
state.user.name = '李四'  // 直接修改属性
state.list.push('新项')   // 数组方法可用

// 注意：reactive 不能直接替换整个对象
// 错误：state = newState
// 正确：Object.assign(state, newState)
```

### 4. 生命周期钩子

```javascript
import {
    onBeforeMount, onMounted,
    onBeforeUpdate, onUpdated,
    onBeforeUnmount, onUnmounted
} from 'vue'

onBeforeMount(() => { /* DOM 挂载前 */ })
onMounted(() => { /* DOM 挂载后，可操作 DOM */ })
onBeforeUpdate(() => { /* 数据更新前 */ })
onUpdated(() => { /* DOM 更新后 */ })
onBeforeUnmount(() => { /* 组件卸载前，清理工作 */ })
onUnmounted(() => { /* 组件卸载后 */ })
```

### 5. 组件通信

```vue
<!-- 父组件 Parent.vue -->
<script setup>
import { ref } from 'vue'
import Child from './Child.vue'

const message = ref('来自父组件')
const handleChildEvent = (data) => {
    console.log('收到子组件消息：', data)
}
</script>

<template>
    <Child :title="message" @custom-event="handleChildEvent" />
</template>

<!-- 子组件 Child.vue -->
<script setup>
// 定义 props
const props = defineProps(['title'])

// 定义 emits
const emit = defineEmits(['custom-event'])

const sendToParent = () => {
    emit('custom-event', '子组件的数据')
}
</script>

<template>
    <div>{{ props.title }}</div>
    <button @click="sendToParent">通知父组件</button>
</template>
```

### 6. Pinia 状态管理

```javascript
// stores/user.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
    // State
    const token = ref(localStorage.getItem('token') || '')
    const userInfo = ref(null)

    // Getter
    const isLoggedIn = computed(() => !!token.value)

    // Action
    function setToken(newToken) {
        token.value = newToken
        localStorage.setItem('token', newToken)
    }

    function logout() {
        token.value = ''
        userInfo.value = null
        localStorage.removeItem('token')
    }

    return { token, userInfo, isLoggedIn, setToken, logout }
})
```

使用：

```vue
<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const handleLogout = () => {
    userStore.logout()
}
</script>

<template>
    <div v-if="userStore.isLoggedIn">
        欢迎，{{ userStore.userInfo?.name }}
        <button @click="handleLogout">退出</button>
    </div>
</template>
```

## Vue2 到 Vue3 的关键变化

| 特性 | Vue2 | Vue3 |
|------|------|------|
| 创建方式 | `new Vue()` | `createApp()` |
| 根实例 | 只能有一个 | 可以有多个 |
| 响应式 | Object.defineProperty | Proxy |
| API 风格 | 选项式 API | 组合式 API（推荐） |
| 代码组织 | 按选项类型 | 按功能逻辑 |
| 模板 | 单个根元素 | 多根元素（Fragments） |
| TypeScript | 支持有限 | 原生支持 |
| 构建工具 | Vue CLI / Webpack | Vite（推荐） |
| 状态管理 | Vuex | Pinia（推荐） |
| 事件总线 | EventBus | 推荐使用 mitt 库 |

## 动手练习

### 练习 1：待办清单（组合式 API）

使用 `<script setup>` 实现一个 Todo 应用：
- 输入框添加任务
- 列表显示所有任务（可标记完成/未完成）
- 底部显示未完成数量
- 使用 `computed` 过滤显示全部/未完成/已完成

### 练习 2：用户状态管理

使用 Pinia 实现：
- 登录状态管理
- 用户信息存储
- 登录/登出功能

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| `ref` 值未响应 | 忘记 `.value` | 记住 `ref` 需要通过 `.value` 读写 |
| `reactive` 解构丢失响应性 | 解构后变成普通变量 | 使用 `toRefs()` 转换 |
| `<script setup>` 中组件未注册 | 自动注册但需要导入 | 确保 `import Component from './Component.vue'` |
| Pinia store 未初始化 | 在 main.js 中未挂载 | `app.use(createPinia())` |

## 本节小结

Vue3 的组合式 API 让代码组织更灵活，TypeScript 支持更好，性能更强。对于实训项目，建议直接上手 `<script setup>` + TypeScript + Pinia 的组合，这是 2025 年企业前端开发的标准范式。

## 参考文档

- [Vue3 官方文档](https://vuejs.org/)
- [Vue3 中文文档](https://cn.vuejs.org/)
- [Pinia 官方文档](https://pinia.vuejs.org/zh/)
- [Vite 官方文档](https://cn.vitejs.dev/)
