# Vue2 快速入门（版本A）

## 学习目标

- 理解 Vue 的响应式原理
- 掌握选项式 API 的基本用法
- 能够使用 Vue 指令完成常见交互
- 理解组件化开发思想

## 核心知识点

### 1. Vue 实例

Vue2 使用选项式 API，通过 `new Vue()` 创建实例：

```html
<div id="app">
    {{ message }}
    <button @click="reverseMessage">反转文字</button>
</div>

<script src="https://cdn.jsdelivr.net/npm/vue@2.6.14/dist/vue.js"></script>
<script>
    new Vue({
        el: '#app',
        data: {
            message: 'Hello Vue!'
        },
        methods: {
            reverseMessage() {
                this.message = this.message.split('').reverse().join('');
            }
        }
    });
</script>
```

### 2. 常用指令

```html
<!-- 文本插值 -->
<p>{{ user.name }}</p>

<!-- v-bind：绑定属性 -->
<img v-bind:src="imageUrl" :alt="description">
<a :href="link">链接</a>

<!-- v-model：双向绑定 -->
<input v-model="username" placeholder="请输入用户名">
<p>你输入的是：{{ username }}</p>

<!-- v-if / v-show：条件渲染 -->
<p v-if="isVip">VIP 专属内容</p>
<p v-else>普通用户内容</p>
<div v-show="isVisible">显示/隐藏（保留 DOM）</div>

<!-- v-for：列表渲染 -->
<ul>
    <li v-for="(item, index) in list" :key="item.id">
        {{ index + 1 }}. {{ item.name }}
    </li>
</ul>

<!-- v-on：事件绑定（简写 @） -->
<button @click="handleClick">点击</button>
<button @click="handleClick($event, '参数')">带参数</button>

<!-- 修饰符 -->
<form @submit.prevent="onSubmit">   <!-- .prevent 阻止默认行为 -->
<input @keyup.enter="onEnter">      <!-- .enter 监听回车键 -->
```

### 3. 计算属性与监听器

```javascript
new Vue({
    data: {
        firstName: '张',
        lastName: '三',
        age: 20
    },
    computed: {
        // 计算属性：有缓存，依赖不变不重新计算
        fullName() {
            return this.firstName + this.lastName;
        }
    },
    watch: {
        // 监听器：适合执行异步或开销大的操作
        age(newVal, oldVal) {
            console.log(`年龄从 ${oldVal} 变为 ${newVal}`);
        }
    }
});
```

### 4. 生命周期钩子

```javascript
new Vue({
    data: { msg: 'Hello' },

    beforeCreate() {
        // 实例创建前，data 和 methods 不可用
    },
    created() {
        // 实例创建完成，data 和 methods 可用
        // 常用于：初始化数据、调用 API
        console.log(this.msg); // Hello
    },
    mounted() {
        // DOM 挂载完成
        // 常用于：操作 DOM、初始化第三方库
        console.log(document.getElementById('app'));
    },
    beforeDestroy() {
        // 实例销毁前
        // 常用于：清除定时器、解绑事件
    }
});
```

### 5. 组件化

```javascript
// 定义全局组件
Vue.component('my-button', {
    props: ['text', 'type'],
    template: '<button :class="type" @click="onClick">{{ text }}</button>',
    methods: {
        onClick() {
            this.$emit('click'); // 向父组件发送事件
        }
    }
});

// 使用组件
<my-button text="提交" type="primary" @click="handleSubmit"></my-button>
```

## 动手练习

### 练习 1：商品列表

实现一个商品列表组件：
- 使用 `v-for` 渲染商品列表
- 每个商品有名称、价格、库存
- 库存为 0 时显示"售罄"并禁用购买按钮
- 点击购买按钮减少库存

### 练习 2：表单验证

实现一个注册表单：
- 用户名（必填，长度 3-20）
- 密码（必填，至少 6 位）
- 确认密码（必须与密码一致）
- 实时验证，错误提示使用 `v-show` 控制

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| 数据变了页面没更新 | 直接修改数组索引或对象属性 | 使用 `Vue.set()` 或数组变异方法（push/pop/splice） |
| `v-for` 没有 key | 列表渲染性能差或状态错乱 | 始终提供唯一 `:key` |
| `this` 指向 undefined | 箭头函数中的 `this` | 使用普通函数定义 methods |
| 组件数据共享 | data 直接返回对象 | data 必须是返回对象的函数 `data() { return {} }` |

## 本节小结

Vue2 的选项式 API（data、methods、computed、watch、生命周期钩子）符合初学者的线性思维。掌握指令系统和组件化思想，你就能构建复杂的前端应用。

## 参考文档

- [Vue2 官方文档](https://v2.vuejs.org/)
- [Vue2 中文文档](https://cn.vuejs.org/v2/guide/)
