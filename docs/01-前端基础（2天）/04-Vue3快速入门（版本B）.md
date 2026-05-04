# Vue3 快速入门（版本 B）

## 学习目标

学完本节后，你将能够：
- 理解 Vue3 相比 Vue2 的核心改进和优势
- 使用组合式 API（Composition API）组织代码
- 掌握 `ref` 和 `reactive` 的响应式系统
- 使用 Vite 构建项目，使用 Pinia 管理状态

---

## 核心知识点

### 1. Vue3 是什么——更现代、更高效、更强大

#### 1.1 Vue3 的主要改进

Vue3 于 2020 年发布，是 Vue 的重大升级版本：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       Vue3 相比 Vue2 的核心改进                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. 性能提升                                                             │
│     • 打包体积减少 41%                                                   │
│     • 初次渲染快 55%，更新渲染快 133%                                     │
│     • 内存使用减少 54%                                                   │
│                                                                          │
│  2. 组合式 API（Composition API）                                         │
│     • 将相关逻辑组织在一起（而不是按选项类型分散）                          │
│     • 更好的代码复用（通过组合函数）                                       │
│     • 更好的 TypeScript 支持                                             │
│                                                                          │
│  3. 响应式系统重构                                                        │
│     • Vue2：Object.defineProperty（无法监听新增属性、数组索引）             │
│     • Vue3：Proxy（可以监听任意属性的增删改）                               │
│                                                                          │
│  4. 更好的 TypeScript 支持                                               │
│     • 使用 TypeScript 重写                                               │
│     • 更好的类型推断和 IDE 支持                                           │
│                                                                          │
│  5. 新特性                                                               │
│     • Fragment：组件可以有多个根元素                                       │
│     • Teleport：将组件渲染到 DOM 其他位置                                  │
│     • Suspense：异步组件加载状态处理                                       │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 1.2 Vue2 vs Vue3 对比

| 特性 | Vue2 | Vue3 |
|------|------|------|
| 创建方式 | `new Vue()` | `createApp()` |
| 根实例 | 只能有一个 | 可以有多个 |
| 响应式 | Object.defineProperty | Proxy |
| API 风格 | 选项式 API | 组合式 API（推荐）+ 选项式 API |
| 代码组织 | 按选项类型（data、methods...） | 按功能逻辑组合 |
| 模板 | 单个根元素 | 多根元素（Fragments） |
| TypeScript | 支持有限 | 原生支持 |
| 构建工具 | Vue CLI / Webpack | Vite（推荐） |
| 状态管理 | Vuex | Pinia（推荐） |
| 事件总线 | EventBus | mitt 库 |

---

### 2. 创建 Vue3 项目

#### 2.1 使用 Vite 创建项目

Vite 是下一代前端构建工具，比 Webpack 快 10-100 倍：

```bash
# 使用 npm create vue@latest 创建项目
npm create vue@latest

# 交互式选项：
# ? Project name: tlias-pro-frontend
# ? Add TypeScript? Yes              ← 版本 B 使用 TypeScript
# ? Add JSX Support? No
# ? Add Vue Router? Yes              ← 路由管理
# ? Add Pinia? Yes                   ← 状态管理（替代 Vuex）
# ? Add Vitest? No
# ? Add Cypress? No
# ? Add ESLint? Yes                  ← 代码规范
# ? Add Prettier? Yes                ← 代码格式化

# 进入项目目录
cd tlias-pro-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产环境
npm run build
```

#### 2.2 项目目录结构

```
tlias-pro-frontend/
├── public/                  # 静态资源（不会被构建工具处理）
│   └── favicon.ico
├── src/
│   ├── assets/              # 静态资源（图片、字体等）
│   │   └── logo.png
│   ├── components/          # 公共组件
│   │   └── HelloWorld.vue
│   ├── router/              # 路由配置
│   │   └── index.ts
│   ├── stores/              # Pinia 状态管理
│   │   └── user.ts
│   ├── views/               # 页面组件
│   │   ├── HomeView.vue
│   │   └── AboutView.vue
│   ├── App.vue              # 根组件
│   └── main.ts              # 入口文件
├── index.html               # HTML 模板
├── package.json             # 项目依赖
├── tsconfig.json            # TypeScript 配置
├── vite.config.ts           # Vite 配置
└── README.md
```

---

### 3. 组合式 API——Vue3 的核心

#### 3.1 为什么需要组合式 API

**选项式 API 的问题：**

```javascript
// Vue2 选项式 API：同一个功能的代码分散在不同选项中
export default {
    data() {
        return {
            searchText: '',     // 搜索功能的数据
            searchResults: [],
            loading: false,
            page: 1,            // 分页功能的数据
            pageSize: 10,
            total: 0,
            userInfo: null      // 用户信息的数据
        };
    },
    computed: {
        // 搜索功能的计算属性...
        // 分页功能的计算属性...
    },
    watch: {
        // 搜索功能的监听器...
        // 分页功能的监听器...
    },
    methods: {
        // 搜索功能的方法...
        // 分页功能的方法...
        // 用户信息的方法...
    },
    mounted() {
        // 搜索功能的初始化...
        // 分页功能的初始化...
    }
};
// 问题：一个功能的代码分散在 data、computed、watch、methods 中，难以维护
```

**组合式 API 的解决方案：**

```vue
<script setup>
// 组合式 API：同一个功能的代码组织在一起
import { ref, computed, watch, onMounted } from 'vue';

// ========== 搜索功能 ==========
const searchText = ref('');
const searchResults = ref([]);
const loading = ref(false);

const search = async () => {
    loading.value = true;
    // 发送请求...
    loading.value = false;
};

watch(searchText, () => {
    search();
});

// ========== 分页功能 ==========
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

const totalPages = computed(() => Math.ceil(total.value / pageSize.value));

// ========== 用户信息功能 ==========
const userInfo = ref(null);

onMounted(() => {
    // 获取用户信息
});
<\/script>
```

#### 3.2 `<script setup>` 语法

`<script setup>` 是 Vue3 推荐的写法，代码更简洁：

```vue
<script setup>
// 不用写 export default，直接写逻辑
// 变量和函数自动暴露给模板使用

import { ref } from 'vue';

// 定义响应式数据
const count = ref(0);

// 定义函数
function increment() {
    count.value++;
}
<\/script>

<template>
    <!-- 直接使用 count 和 increment，不需要 this -->
    <button @click="increment">Count: {{ count }}</button>
<\/template>
```

**`<script setup>` 的优势：**
- 更少的样板代码
- 更好的 TypeScript 支持
- 更好的运行时性能
- 更好的 IDE 类型推断

---

### 4. ref 与 reactive——响应式数据

#### 4.1 ref——适用于基本类型

```vue
<script setup>
import { ref } from 'vue';

// ref 接收一个值，返回一个响应式对象
const count = ref(0);
const name = ref('张三');
const isActive = ref(true);

// 访问值：需要用 .value
console.log(count.value);     // 0
console.log(name.value);      // '张三'

// 修改值：也需要用 .value
count.value++;
name.value = '李四';

// 在模板中直接使用（不需要 .value）
// <p>{{ count }}</p>  ← 模板中自动解包
<\/script>

<template>
    <p>数量：{{ count }}</p>
    <p>姓名：{{ name }}</p>
    <button @click="count++">+1</button>
<\/template>
```

**为什么需要 `.value`？**

```javascript
// 如果直接 let count = 0，JS 无法监听这个变量的变化
// ref 把它包装成对象：{ value: 0 }
// 通过 Object.defineProperty 或 Proxy 监听 .value 的变化

const count = ref(0);
// 实际上 count 是：{ value: 0 }
// Vue 监听的是 count.value 的修改
```

#### 4.2 reactive——适用于对象

```vue
<script setup>
import { reactive } from 'vue';

// reactive 接收一个对象，返回响应式代理
const user = reactive({
    name: '张三',
    age: 25,
    hobbies: ['读书', '游泳']
});

// 直接访问属性（不需要 .value）
console.log(user.name);   // '张三'

// 直接修改属性
user.name = '李四';
user.age++;
user.hobbies.push('编程');  // 数组方法也可用

// 注意：reactive 不能直接替换整个对象
// ❌ 错误：user = newUser  ← 会失去响应性！
// ✅ 正确：Object.assign(user, newUser)
<\/script>

<template>
    <p>姓名：{{ user.name }}</p>
    <p>年龄：{{ user.age }}</p>
    <ul>
        <li v-for="hobby in user.hobbies" :key="hobby">{{ hobby }}</li>
    <\/ul>
<\/template>
```

#### 4.3 ref vs reactive 的选择

| 场景 | 推荐 | 原因 |
|------|------|------|
| 基本类型（string/number/boolean） | `ref` | reactive 不支持基本类型 |
| 对象、数组 | `reactive` | 代码更简洁，不用 .value |
| 需要解构或重新赋值的对象 | `ref` | reactive 解构会丢失响应性 |
| 表单数据对象 | `ref` | 可以整体替换 |

**实际项目中的建议：**

```vue
<script setup>
import { ref, reactive } from 'vue';

// 简单数据用 ref
const count = ref(0);
const loading = ref(false);
const searchText = ref('');

// 复杂对象用 reactive（如果不需要整体替换）
const formState = reactive({
    username: '',
    password: '',
    remember: false
});

// 如果对象需要整体替换，用 ref
const userInfo = ref({ name: '张三', age: 25 });
// userInfo.value = newUser;  ← 可以整体替换
<\/script>
```

---

### 5. 计算属性和监听器

#### 5.1 computed——计算属性

```vue
<script setup>
import { ref, computed } from 'vue';

const firstName = ref('张');
const lastName = ref('三');

// 计算属性：基于响应式数据计算出新值，有缓存
const fullName = computed(() => {
    console.log('计算属性被调用了');
    return firstName.value + lastName.value;
});

// 可写的计算属性
const fullName2 = computed({
    get() {
        return firstName.value + lastName.value;
    },
    set(newVal) {
        // 当 fullName2 = '李四' 时触发
        [firstName.value, lastName.value] = newVal.split('');
    }
});
<\/script>

<template>
    <p>姓：{{ firstName }}</p>
    <p>名：{{ lastName }}</p>
    <p>全名：{{ fullName }}</p>
    <p>全名：{{ fullName }}</p>  <!-- 从缓存读取，不重新计算 -->
<\/template>
```

#### 5.2 watch——监听器

```vue
<script setup>
import { ref, watch } from 'vue';

const count = ref(0);
const user = ref({ name: '张三', age: 25 });

// 监听单个 ref
watch(count, (newVal, oldVal) => {
    console.log(`count 从 ${oldVal} 变为 ${newVal}`);
});

// 监听多个 ref
watch([count, () => user.value.name], ([newCount, newName], [oldCount, oldName]) => {
    console.log('多个值变化了');
});

// 深度监听对象
watch(user, (newVal) => {
    console.log('user 变化了：', newVal);
}, { deep: true });  // deep: true 表示深度监听

// 立即执行（初始化时就执行一次回调）
watch(count, (newVal) => {
    console.log('初始值：', newVal);
}, { immediate: true });
<\/script>
```

---

### 6. 生命周期钩子

Vue3 的生命周期钩子名称有变化（加上 `on` 前缀）：

```vue
<script setup>
import {
    onBeforeMount,    // DOM 挂载前
    onMounted,        // DOM 挂载后 ★ 最常用
    onBeforeUpdate,   // 数据更新前
    onUpdated,        // DOM 更新后
    onBeforeUnmount,  // 组件卸载前 ★ 常用
    onUnmounted       // 组件卸载后
} from 'vue';

// 发送请求获取数据
onMounted(() => {
    console.log('组件已挂载，可以操作 DOM');
    fetchData();
});

// 清理工作
onBeforeUnmount(() => {
    console.log('组件即将卸载，清除定时器和事件监听');
    clearInterval(timer);
});
<\/script>
```

**Vue2 和 Vue3 生命周期对比：**

| Vue2 | Vue3（选项式） | Vue3（组合式） |
|------|--------------|---------------|
| beforeCreate | beforeCreate | setup（替代） |
| created | created | setup（替代） |
| beforeMount | beforeMount | onBeforeMount |
| mounted | mounted | onMounted |
| beforeUpdate | beforeUpdate | onBeforeUpdate |
| updated | updated | onUpdated |
| beforeDestroy | beforeUnmount | onBeforeUnmount |
| destroyed | unmounted | onUnmounted |

---

### 7. 组件通信

#### 7.1 父传子：props

```vue
<!-- 子组件 Child.vue -->
<script setup>
// 定义 props
const props = defineProps({
    title: {
        type: String,
        default: '默认标题'
    },
    count: {
        type: Number,
        required: true
    }
});

console.log(props.title);  // 访问 props
<\/script>

<template>
    <div>
        <h3>{{ title }}</h3>
        <p>数量：{{ count }}</p>
    <\/div>
<\/template>
```

```vue
<!-- 父组件 Parent.vue -->
<script setup>
import { ref } from 'vue';
import Child from './Child.vue';

const message = ref('来自父组件');
<\/script>

<template>
    <!-- 传递 props -->
    <Child :title="message" :count="10" />
<\/template>
```

#### 7.2 子传父：emit

```vue
<!-- 子组件 Child.vue -->
<script setup>
// 定义 emits
const emit = defineEmits(['update', 'delete']);

const sendToParent = () => {
    emit('update', { message: '子组件的数据' });
};
<\/script>

<template>
    <button @click="sendToParent">通知父组件</button>
<\/template>
```

```vue
<!-- 父组件 Parent.vue -->
<script setup>
import Child from './Child.vue';

const handleUpdate = (data) => {
    console.log('收到子组件消息：', data.message);
};
<\/script>

<template>
    <Child @update="handleUpdate" />
<\/template>
```

---

### 8. Pinia 状态管理——Vuex 的继任者

#### 8.1 为什么用 Pinia

Pinia 是 Vue 官方推荐的状态管理库，相比 Vuex：
- API 更简单（没有 mutations）
- 更好的 TypeScript 支持
- 更轻量
- 支持组合式 API

#### 8.2 定义 Store

```javascript
// stores/user.js
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

// defineStore('唯一标识', 工厂函数)
export const useUserStore = defineStore('user', () => {
    // ========== State ==========
    const token = ref(localStorage.getItem('token') || '');
    const userInfo = ref(null);

    // ========== Getter（计算属性）==========
    const isLoggedIn = computed(() => !!token.value);

    // ========== Action（方法）==========
    function setToken(newToken) {
        token.value = newToken;
        localStorage.setItem('token', newToken);
    }

    function setUserInfo(info) {
        userInfo.value = info;
    }

    function logout() {
        token.value = '';
        userInfo.value = null;
        localStorage.removeItem('token');
    }

    // 返回所有需要在组件中使用的内容
    return { token, userInfo, isLoggedIn, setToken, setUserInfo, logout };
});
```

#### 8.3 在组件中使用 Store

```vue
<script setup>
import { useUserStore } from '@/stores/user';

// 获取 store 实例
const userStore = useUserStore();

// 直接访问 state
console.log(userStore.token);

// 访问 getter
console.log(userStore.isLoggedIn);

// 调用 action
const handleLogin = () => {
    userStore.setToken('abc123');
};

const handleLogout = () => {
    userStore.logout();
};
<\/script>

<template>
    <div v-if="userStore.isLoggedIn">
        欢迎，{{ userStore.userInfo?.name }}
        <button @click="handleLogout">退出</button>
    <\/div>
    <div v-else>
        <button @click="handleLogin">登录</button>
    <\/div>
<\/template>
```

---

## 动手练习

### 练习 1：待办清单（组合式 API）

使用 `<script setup>` 实现一个 Todo 应用：
- 输入框添加任务
- 列表显示所有任务（可标记完成/未完成）
- 底部显示未完成数量（用 `computed`）
- 使用 `computed` 过滤显示全部/未完成/已完成

### 练习 2：用户状态管理

使用 Pinia 实现：
- 登录状态管理（token）
- 用户信息存储
- 登录/登出功能
- 页面刷新后状态不丢失（用 localStorage）

---

## 常见错误排查

### 阶段 1：响应式问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `ref` 值未响应 | 忘记 `.value` | 记住 `ref` 需要通过 `.value` 读写 |
| `reactive` 解构丢失响应性 | 解构后变成普通变量 | 使用 `toRefs()` 转换，或直接用 `ref` |
| 数组/对象新增属性不响应 | 直接赋值新属性 | Vue3 Proxy 已解决，如果不行用 `ref` |

### 阶段 2：语法问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `<script setup>` 中组件未注册 | 自动注册但需要导入 | 确保 `import Component from './Component.vue'` |
| 模板中访问不到变量 | 变量未在 script setup 中定义 | 检查变量名拼写 |
| props 类型错误 | 传入的类型和定义不匹配 | 检查 props 的 type 定义 |

### 阶段 3：Pinia 问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| Pinia store 未初始化 | 在 main.js 中未挂载 | `app.use(createPinia())` |
| store 数据不持久化 | 刷新页面数据丢失 | 用 localStorage 手动保存 |
| 解构 store 失去响应性 | 解构是普通对象的解构 | 使用 `storeToRefs()` |

---

## 本节小结

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Vue3 核心知识                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Vue3 优势：性能更好、包更小、TypeScript 支持更好、组合式 API              │
│                                                                          │
│  创建项目：npm create vue@latest → 选择 TypeScript + Router + Pinia      │
│  构建工具：Vite（比 Webpack 快 10-100 倍）                                │
│                                                                          │
│  组合式 API（script setup）：                                              │
│    • 代码按功能组织，而不是按选项类型                                      │
│    • 更少的样板代码，更好的复用                                           │
│                                                                          │
│  响应式数据：                                                            │
│    • ref()：基本类型，访问需要 .value                                     │
│    • reactive()：对象类型，直接访问属性                                   │
│    • computed()：计算属性，有缓存                                         │
│    • watch()：监听器                                                     │
│                                                                          │
│  生命周期：                                                              │
│    • onMounted()：DOM 挂载后，发送请求                                   │
│    • onBeforeUnmount()：清理工作                                         │
│                                                                          │
│  组件通信：                                                              │
│    • 父传子：defineProps()                                               │
│    • 子传父：defineEmits()                                               │
│                                                                          │
│  Pinia 状态管理：                                                        │
│    • defineStore('id', () => { state, getters, actions })               │
│    • 使用：const store = useUserStore()                                  │
│                                                                          │
│  避坑指南：                                                              │
│    • ref 要记得 .value                                                   │
│    • reactive 解构会丢失响应性                                           │
│    • 组件名导入后直接使用，不用注册                                       │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 参考文档

- [Vue3 官方文档](https://vuejs.org/)
- [Vue3 中文文档](https://cn.vuejs.org/)
- [Pinia 官方文档](https://pinia.vuejs.org/zh/)
- [Vite 官方文档](https://cn.vitejs.dev/)
