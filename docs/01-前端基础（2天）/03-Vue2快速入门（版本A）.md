# Vue2 快速入门（版本 A）

## 学习目标

学完本节后，你将能够：
- 理解 Vue 的核心思想：数据驱动视图
- 使用 Vue 指令完成常见交互（条件渲染、列表渲染、事件绑定、双向绑定）
- 理解 Vue 的响应式原理和生命周期
- 使用组件化思想拆分页面

---

## 核心知识点

### 1. Vue 是什么——数据驱动视图的"魔法"

#### 1.1 传统 JS 操作 DOM 的痛点

假设你要实现一个功能：页面上显示一个数字，点击按钮数字 +1。

```html
<!-- 传统 JS 方式 -->
<div>
    <span id="count">0</span>
    <button id="btn">+1</button>
</div>

<script>
    let count = 0;
    const countSpan = document.getElementById('count');
    const btn = document.getElementById('btn');

    btn.addEventListener('click', () => {
        count++;
        countSpan.textContent = count;  // 手动更新 DOM
    });
<\/script>
```

**问题：**
- 数据和 DOM 是分离的，你需要手动维护两者的同步
- 如果页面有 10 个地方显示这个数字，要更新 10 次 DOM
- 代码复杂后，状态管理会变得非常困难

#### 1.2 Vue 的方式

Vue 的核心思想：**数据驱动视图**。你只需要关心数据，Vue 自动帮你更新 DOM。

```html
<!-- Vue 方式 -->
<div id="app">
    <span>{{ count }}</span>
    <button @click="count++">+1</button>
</div>

<script src="https://cdn.jsdelivr.net/npm/vue@2.6.14/dist/vue.js"><\/script>
<script>
    new Vue({
        el: '#app',
        data: {
            count: 0
        }
    });
<\/script>
```

**对比：**

| 方面 | 传统 JS | Vue |
|------|--------|-----|
| 代码量 | 多（要手动获取元素、更新 DOM） | 少（只需定义数据） |
| 可维护性 | 差（数据和 DOM 分离） | 好（数据驱动，自动同步） |
| 多地方显示 | 要手动更新每个地方 | 改一次数据，所有地方自动更新 |

#### 1.3 Vue 的响应式原理（简化版）

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   data      │  ────→  │   Vue 监听  │  ────→  │     DOM     │
│  { count:0 }│  数据变化 │  到 count   │  自动更新 │   <span>0</span> │
└─────────────┘         └─────────────┘         └─────────────┘
       ↑                                              │
       └──────────── 用户点击按钮 ──────────────────────┘
                         count++
```

Vue2 使用 `Object.defineProperty` 监听对象属性的变化，当数据改变时，自动重新渲染相关的 DOM。

---

### 2. Vue 实例——一切的开始

#### 2.1 创建 Vue 实例

```html
<!DOCTYPE html>
<html>
<head>
    <title>Vue2 入门</title>
    <!-- 引入 Vue2（开发版，有错误提示） -->
    <script src="https://cdn.jsdelivr.net/npm/vue@2.6.14/dist/vue.js"><\/script>
<\/head>
<body>
    <!-- Vue 管理的区域：id="app" -->
    <div id="app">
        <h1>{{ message }}</h1>
        <p>反转后的文字：{{ reversedMessage }}</p>
        <button @click="reverseMessage">反转文字</button>
    <\/div>

    <script>
        // 创建 Vue 实例
        new Vue({
            // el：element，指定 Vue 管理的 DOM 元素（CSS 选择器）
            el: '#app',

            // data：数据对象，这里定义的数据可以在模板中使用
            data: {
                message: 'Hello Vue!'
            },

            // computed：计算属性，基于已有数据计算出新数据
            computed: {
                reversedMessage() {
                    // this.message 访问 data 中的数据
                    return this.message.split('').reverse().join('');
                }
            },

            // methods：方法，定义可以在模板中调用的函数
            methods: {
                reverseMessage() {
                    // 修改 data 中的数据，Vue 会自动更新 DOM
                    this.message = this.message.split('').reverse().join('');
                }
            }
        });
    <\/script>
<\/body>
<\/html>
```

#### 2.2 Vue 选项式 API 的核心选项

```javascript
new Vue({
    // === 数据 ===
    data: {
        // 响应式数据
    },

    // === 计算属性 ===
    computed: {
        // 基于 data 计算出的值，有缓存
    },

    // === 监听器 ===
    watch: {
        // 监听某个数据的变化，执行回调
    },

    // === 方法 ===
    methods: {
        // 定义函数
    },

    // === 生命周期钩子 ===
    created() {
        // 实例创建完成
    },
    mounted() {
        // DOM 挂载完成
    },

    // === 组件 ===
    components: {
        // 注册子组件
    }
});
```

---

### 3. 常用指令——Vue 的"魔法咒语"

指令是以 `v-` 开头的特殊属性，Vue 会根据指令的值对 DOM 进行某种操作。

#### 3.1 文本插值 `{{ }}`

```html
<div id="app">
    <!-- 最简单的方式：把数据渲染到页面上 -->
    <p>{{ message }}</p>

    <!-- 支持表达式 -->
    <p>{{ message.toUpperCase() }}</p>
    <p>{{ count + 1 }}</p>
    <p>{{ isShow ? '显示' : '隐藏' }}</p>

    <!-- 注意：不能写语句（如 if、for） -->
    <!-- {{ if (true) { ... } }} ❌ 报错！ -->
</div>

<script>
    new Vue({
        el: '#app',
        data: {
            message: 'Hello',
            count: 10,
            isShow: true
        }
    });
<\/script>
```

#### 3.2 `v-bind` —— 绑定属性

```html
<div id="app">
    <!-- v-bind:属性名="表达式" -->
    <img v-bind:src="imageUrl" v-bind:alt="description">

    <!-- 简写：省略 v-bind，只留冒号 -->
    <img :src="imageUrl" :alt="description">

    <!-- 绑定 class（对象语法：类名是否添加取决于布尔值） -->
    <div :class="{ active: isActive, hidden: !isVisible }">
        动态类名
    <\/div>
    <!-- 如果 isActive=true, isVisible=false，最终渲染： -->
    <!-- <div class="active hidden"> -->

    <!-- 绑定 style -->
    <div :style="{ color: textColor, fontSize: fontSize + 'px' }">
        动态样式
    <\/div>
</div>

<script>
    new Vue({
        el: '#app',
        data: {
            imageUrl: 'logo.png',
            description: '公司 Logo',
            isActive: true,
            isVisible: false,
            textColor: 'red',
            fontSize: 20
        }
    });
<\/script>
```

#### 3.3 `v-model` —— 双向绑定

双向绑定 = 数据改变 → 视图更新，同时 视图改变（用户输入）→ 数据更新。

```html
<div id="app">
    <!-- 输入框 -->
    <input v-model="username" placeholder="请输入用户名">
    <p>你输入的是：{{ username }}</p>

    <!-- 单选框 -->
    <input type="radio" v-model="gender" value="1"> 男
    <input type="radio" v-model="gender" value="2"> 女
    <p>选择的性别：{{ gender }}</p>

    <!-- 复选框 -->
    <input type="checkbox" v-model="hobbies" value="read"> 读书
    <input type="checkbox" v-model="hobbies" value="sport"> 运动
    <p>爱好：{{ hobbies }}</p>

    <!-- 下拉框 -->
    <select v-model="selectedDept">
        <option value="1">研发部</option>
        <option value="2">市场部</option>
    <\/select>
    <p>选择的部门：{{ selectedDept }}</p>
</div>

<script>
    new Vue({
        el: '#app',
        data: {
            username: '',
            gender: '1',
            hobbies: [],       // 复选框用数组
            selectedDept: '1'
        }
    });
<\/script>
```

#### 3.4 `v-if` / `v-show` —— 条件渲染

```html
<div id="app">
    <!-- v-if：条件为 false 时，元素从 DOM 中移除 -->
    <p v-if="isVip">VIP 专属内容</p>
    <p v-else-if="isMember">会员内容</p>
    <p v-else>普通用户内容</p>

    <!-- v-show：条件为 false 时，元素还在 DOM 中，只是 display: none -->
    <div v-show="isVisible">显示/隐藏（保留 DOM）</div>
</div>

<script>
    new Vue({
        el: '#app',
        data: {
            isVip: false,
            isMember: true,
            isVisible: true
        }
    });
<\/script>
```

**`v-if` vs `v-show` 的区别：**

| 特性 | v-if | v-show |
|------|------|--------|
| 渲染方式 | 条件 false 时不渲染（DOM 中没有） | 始终渲染，只是隐藏 |
| 切换开销 | 高（需要创建/销毁 DOM） | 低（只是改 CSS） |
| 适用场景 | 条件很少改变 | 条件频繁切换 |

#### 3.5 `v-for` —— 列表渲染

```html
<div id="app">
    <!-- 遍历数组 -->
    <ul>
        <!--
            item：当前元素
            index：当前索引
            :key：唯一标识（必须提供，用于 Vue 高效更新）
        -->
        <li v-for="(item, index) in list" :key="item.id">
            {{ index + 1 }}. {{ item.name }} - {{ item.price }}元
        <\/li>
    <\/ul>

    <!-- 遍历对象 -->
    <div v-for="(value, key) in person" :key="key">
        {{ key }}: {{ value }}
    <\/div>
</div>

<script>
    new Vue({
        el: '#app',
        data: {
            list: [
                { id: 1, name: '苹果', price: 5 },
                { id: 2, name: '香蕉', price: 3 },
                { id: 3, name: '橙子', price: 4 }
            ],
            person: {
                name: '张三',
                age: 25,
                job: '工程师'
            }
        }
    });
<\/script>
```

**`:key` 的重要性：**

```
没有 key 时：
  列表：[A, B, C] → [A, C, B]
  Vue 不知道谁移动了，可能重新渲染整个列表

有 key 时：
  列表：[A(id=1), B(id=2), C(id=3)] → [A(id=1), C(id=3), B(id=2)]
  Vue 通过 key 识别每个元素，只做最小修改
```

**永远给 `v-for` 提供唯一的 `:key`！**

#### 3.6 `v-on` —— 事件绑定

```html
<div id="app">
    <!-- v-on:事件名="处理函数" -->
    <button v-on:click="handleClick">点击</button>

    <!-- 简写：@事件名 -->
    <button @click="handleClick">点击</button>

    <!-- 传递参数 -->
    <button @click="greet('张三')">打招呼</button>

    <!-- 传递事件对象 + 自定义参数 -->
    <button @click="handleClick($event, '参数')">点击</button>

    <!-- 事件修饰符 -->
    <form @submit.prevent="onSubmit">       <!-- .prevent 阻止默认提交 -->
        <input @keyup.enter="onEnter">      <!-- .enter 监听回车键 -->
        <input @input.debounce="onInput">   <!-- .debounce 防抖（需要额外配置） -->
    <\/form>
</div>

<script>
    new Vue({
        el: '#app',
        methods: {
            handleClick(event, msg) {
                console.log('点击了按钮');
                console.log('事件对象：', event);
                console.log('自定义参数：', msg);
            },
            greet(name) {
                alert('你好，' + name);
            },
            onSubmit() {
                console.log('表单提交（已阻止默认行为）');
            },
            onEnter() {
                console.log('按下了回车键');
            }
        }
    });
<\/script>
```

**常用事件修饰符：**

| 修饰符 | 作用 |
|--------|------|
| `.prevent` | 阻止默认行为（如表单提交、链接跳转） |
| `.stop` | 阻止事件冒泡 |
| `.once` | 只触发一次 |
| `.capture` | 使用捕获模式 |
| `.self` | 只当事件从元素本身触发时才触发 |
| `.enter` / `.esc` / `.tab` | 监听特定键盘按键 |

---

### 4. 计算属性与监听器

#### 4.1 计算属性 computed

计算属性基于已有数据计算出新数据，**有缓存**——依赖不变时不会重新计算。

```html
<div id="app">
    <p>姓：{{ firstName }}</p>
    <p>名：{{ lastName }}</p>
    <p>全名：{{ fullName }}</p>
    <p>全名：{{ fullName }}</p>  <!-- 第二次使用，从缓存读取，不重新计算 -->
</div>

<script>
    new Vue({
        el: '#app',
        data: {
            firstName: '张',
            lastName: '三'
        },
        computed: {
            // 计算属性：fullName
            // 当 firstName 或 lastName 改变时，fullName 自动重新计算
            fullName() {
                console.log('计算属性被调用了');
                return this.firstName + this.lastName;
            }
        }
    });
<\/script>
```

**计算属性 vs 方法的区别：**

```html
<!-- 方法：每次都会重新执行 -->
<p>{{ getFullName() }}</p>
<p>{{ getFullName() }}</p>  <!-- 执行两次 -->

<!-- 计算属性：依赖不变时从缓存读取 -->
<p>{{ fullName }}</p>
<p>{{ fullName }}</p>       <!-- 只计算一次 -->
```

#### 4.2 监听器 watch

监听器用于监听某个数据的变化，在变化时执行操作（适合异步或开销大的操作）。

```html
<div id="app">
    <input v-model="question">
    <p>{{ answer }}</p>
</div>

<script>
    new Vue({
        el: '#app',
        data: {
            question: '',
            answer: '请输入问题'
        },
        watch: {
            // 监听 question 的变化
            question(newVal, oldVal) {
                // newVal：新值
                // oldVal：旧值
                this.answer = '等待输入停止...';

                // 模拟异步操作（如发送请求）
                setTimeout(() => {
                    if (newVal.includes('?')) {
                        this.answer = '这是一个好问题！';
                    } else {
                        this.answer = '请输入带问号的问题。';
                    }
                }, 500);
            }
        }
    });
<\/script>
```

---

### 5. 生命周期钩子——Vue 实例的"一生"

Vue 实例从创建到销毁有一系列阶段，每个阶段可以插入自定义逻辑。

```
┌─────────────────────────────────────────────────────────────┐
│                    Vue 实例生命周期                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  new Vue()                                                   │
│      │                                                       │
│      ▼                                                       │
│  ┌─────────────┐                                             │
│  │ beforeCreate │  实例刚创建，data 和 methods 还未初始化      │
│  └─────────────┘                                             │
│      │                                                       │
│      ▼                                                       │
│  ┌─────────────┐                                             │
│  │   created    │  实例创建完成，data 和 methods 可用           │
│  └─────────────┘  ★ 常用于：初始化数据、调用 API                │
│      │                                                       │
│      ▼                                                       │
│  ┌─────────────┐                                             │
│  │ beforeMount  │  DOM 挂载前，模板编译完成                     │
│  └─────────────┘                                             │
│      │                                                       │
│      ▼                                                       │
│  ┌─────────────┐                                             │
│  │   mounted    │  DOM 挂载完成，可以操作 DOM                   │
│  └─────────────┘  ★ 常用于：操作 DOM、初始化第三方库            │
│      │                                                       │
│      ├── 数据变化 ──→ beforeUpdate → updated ──┐             │
│      │                                          │             │
│      ▼                                          │             │
│  ┌─────────────┐                                │             │
│  │ beforeDestroy│  实例销毁前                    │             │
│  └─────────────┘  ★ 常用于：清除定时器、解绑事件  │             │
│      │                                          │             │
│      ▼                                          │             │
│  ┌─────────────┐                                │             │
│  │  destroyed   │  实例销毁完成                   │             │
│  └─────────────┘                                │             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**最常用的两个钩子：**

```javascript
new Vue({
    data: {
        list: []
    },

    created() {
        // 实例创建完成，data 已可用，但 DOM 还未生成
        // 适合：发送 Ajax 请求获取数据
        console.log('created：发送请求获取数据');
        this.fetchData();
    },

    mounted() {
        // DOM 已挂载完成
        // 适合：操作 DOM、初始化图表库、绑定第三方事件
        console.log('mounted：DOM 已生成');
        console.log(document.getElementById('app'));  // 可以获取到 DOM
    },

    beforeDestroy() {
        // 实例销毁前
        // 适合：清理工作，防止内存泄漏
        console.log('beforeDestroy：清除定时器');
        clearInterval(this.timer);
    }
});
```

---

### 6. 组件化——把页面拆成"积木"

#### 6.1 为什么需要组件

一个复杂的页面如果全写在一个文件里：
- 代码几千行，难以维护
- 功能耦合在一起，无法复用
- 多人协作时容易冲突

**组件化 = 把页面拆成独立的、可复用的"零件"。**

#### 6.2 定义和使用组件

```javascript
// 定义全局组件
Vue.component('my-button', {
    // props：接收父组件传入的数据
    props: ['text', 'type'],

    // template：组件的 HTML 模板（只能有一个根元素）
    template: `
        <button :class="type" @click="onClick">
            {{ text }}
        <\/button>
    `,

    methods: {
        onClick() {
            // $emit：向父组件发送自定义事件
            this.$emit('click');
        }
    }
});

// 父组件中使用
new Vue({
    el: '#app',
    data: {
        msg: 'Hello'
    },
    methods: {
        handleSubmit() {
            console.log('提交按钮被点击');
        }
    }
});
```

```html
<div id="app">
    <!-- 使用自定义组件 -->
    <my-button text="提交" type="primary" @click="handleSubmit"><\/my-button>
    <my-button text="取消" type="default" @click="msg = 'Cancel'"><\/my-button>
</div>
```

#### 6.3 组件通信

```
父组件 ──props──→ 子组件
       ←─$emit──
```

```javascript
// 子组件
Vue.component('child-component', {
    props: {
        // 定义 props 的类型和默认值
        title: {
            type: String,
            default: '默认标题'
        },
        count: {
            type: Number,
            required: true  // 必填
        }
    },
    template: `
        <div>
            <h3>{{ title }}</h3>
            <p>数量：{{ count }}</p>
            <button @click="notifyParent">通知父组件</button>
        <\/div>
    `,
    methods: {
        notifyParent() {
            // 发送事件，携带数据
            this.$emit('update', { message: '来自子组件的消息' });
        }
    }
});

// 父组件
new Vue({
    el: '#app',
    data: {
        parentTitle: '父组件的标题',
        parentCount: 10
    },
    methods: {
        handleUpdate(data) {
            console.log('收到子组件消息：', data.message);
        }
    }
});
```

```html
<div id="app">
    <child-component
        :title="parentTitle"
        :count="parentCount"
        @update="handleUpdate">
    <\/child-component>
</div>
```

#### 6.4 组件的 data 必须是函数

```javascript
// ❌ 错误：data 直接返回对象
Vue.component('counter', {
    data: {
        count: 0  // 所有实例共享同一个 count！
    }
});

// ✅ 正确：data 必须是返回对象的函数
Vue.component('counter', {
    data() {
        return {
            count: 0  // 每个实例有自己独立的 count
        };
    },
    template: '<button @click="count++">{{ count }}</button>'
});
```

**为什么必须是函数？**

```html
<div id="app">
    <counter><\/counter>
    <counter><\/counter>
    <counter><\/counter>
</div>
<!-- 如果 data 是对象，三个组件会共享同一个 count，点击一个全都变 -->
<!-- 如果 data 是函数，每个组件有独立的 count，互不影响 -->
```

---

## 动手练习

### 练习 1：商品列表

实现一个商品列表组件：
- 使用 `v-for` 渲染商品列表
- 每个商品有名称、价格、库存
- 库存为 0 时显示"售罄"并禁用购买按钮（`v-if` 或 `:disabled`）
- 点击购买按钮减少库存（`@click`）

### 练习 2：表单验证

实现一个注册表单：
- 用户名（必填，长度 3-20）
- 密码（必填，至少 6 位）
- 确认密码（必须与密码一致）
- 使用 `watch` 实时验证，错误提示使用 `v-show` 控制

---

## 常见错误排查

### 阶段 1：数据不更新

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 数据变了页面没更新 | 直接修改数组索引或对象属性 | 使用 `Vue.set()` 或数组变异方法（push/pop/splice） |
| `v-for` 渲染错乱 | 没有提供 `:key` | 始终提供唯一的 `:key` |
| 表单输入不更新 | `v-model` 绑定错误 | 检查绑定的属性名是否正确 |

### 阶段 2：语法错误

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Property or method "xxx" is not defined` | 方法或数据未定义 | 检查是否在 `data` 或 `methods` 中声明 |
| `Templates should only be responsible for mapping the state` | 模板中写了赋值语句 | 模板只能写表达式，不能写语句 |
| 组件未显示 | 组件名拼写错误 | 检查 HTML 中组件标签名和注册名是否一致 |

### 阶段 3：作用域问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `this` 指向 undefined | 箭头函数中的 `this` | 在 `methods` 中使用普通函数 |
| 组件数据共享 | data 直接返回对象 | data 必须是返回对象的函数 |
| 无法访问父组件数据 | 没有通过 props 传递 | 使用 props 向下传递数据 |

### 阶段 4：生命周期问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 在 created 中操作 DOM 失败 | 此时 DOM 还未生成 | 移到 `mounted` 中操作 |
| 定时器/事件未清除 | 组件销毁时未清理 | 在 `beforeDestroy` 中清除 |
| 异步数据未渲染 | 请求完成前组件已渲染 | 添加 loading 状态或 `v-if` |

---

## 本节小结

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Vue2 核心知识                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Vue 是什么：数据驱动视图的渐进式框架                                     │
│  核心思想：修改数据，Vue 自动更新 DOM                                     │
│                                                                          │
│  创建实例：new Vue({ el, data, computed, watch, methods })               │
│                                                                          │
│  常用指令：                                                              │
│    • {{ }}       文本插值                                               │
│    • v-bind:     绑定属性（简写 :）                                      │
│    • v-model     双向绑定                                               │
│    • v-if/v-show 条件渲染                                               │
│    • v-for       列表渲染（必须加 :key）                                  │
│    • v-on:       事件绑定（简写 @）                                      │
│                                                                          │
│  计算属性 vs 方法：                                                       │
│    • computed：有缓存，依赖不变不重新计算                                 │
│    • methods：每次调用都执行                                              │
│                                                                          │
│  生命周期（最常用）：                                                     │
│    • created()：实例创建完成，发送请求                                    │
│    • mounted()：DOM 挂载完成，操作 DOM                                    │
│    • beforeDestroy()：清理工作                                            │
│                                                                          │
│  组件化：                                                                │
│    • 全局注册：Vue.component('name', { ... })                            │
│    • 通信：props 向下传，$emit 向上传                                     │
│    • data 必须是函数，返回对象                                            │
│                                                                          │
│  避坑指南：                                                              │
│    • v-for 必须加 :key                                                   │
│    • methods 中不要用箭头函数（this 指向不对）                            │
│    • data 必须是函数                                                     │
│    • 模板中不能写语句（if/for）                                           │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 参考文档

- [Vue2 官方文档](https://v2.vuejs.org/)
- [Vue2 中文文档](https://cn.vuejs.org/v2/guide/)
- [Vue 生命周期图示](https://cn.vuejs.org/v2/guide/instance.html#%E7%94%9F%E5%91%BD%E5%91%A8%E6%9C%9F%E5%9B%BE%E7%A4%BA)
