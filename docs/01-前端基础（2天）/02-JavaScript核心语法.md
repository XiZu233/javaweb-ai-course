# JavaScript 核心语法

## 学习目标

- 掌握 JS 变量、数据类型和基本运算
- 理解函数定义和调用方式
- 掌握 DOM 操作和事件监听
- 理解 JSON 格式和对象操作

## 核心知识点

### 1. 变量与数据类型

JavaScript 是弱类型语言，使用 `let` 和 `const` 声明变量（不推荐 `var`）：

```javascript
// 基本数据类型
let name = '张三';        // 字符串 String
let age = 25;             // 数字 Number
let isStudent = true;     // 布尔 Boolean
let score = null;         // 空值 Null
let address;              // 未定义 Undefined

// 引用数据类型
let person = {            // 对象 Object
    name: '李四',
    age: 30,
    sayHi: function() {
        console.log('Hi, ' + this.name);
    }
};

let hobbies = ['读书', '游泳', '编程']; // 数组 Array

// 常量
const PI = 3.14159;
const CONFIG = { apiUrl: '/api' };
```

### 2. 函数

```javascript
// 函数声明
function add(a, b) {
    return a + b;
}

// 箭头函数（推荐）
const multiply = (a, b) => a * b;

// 默认参数
const greet = (name = '访客') => {
    console.log('你好，' + name);
};

// 回调函数
setTimeout(() => {
    console.log('1秒后执行');
}, 1000);
```

### 3. DOM 操作

DOM（Document Object Model）是 JavaScript 操作 HTML 的接口：

```javascript
// 获取元素
const title = document.getElementById('title');
const buttons = document.querySelectorAll('.btn');
const form = document.querySelector('form');

// 修改内容
title.textContent = '新标题';
title.innerHTML = '<span style="color:red">新标题</span>';

// 修改样式
title.style.color = 'blue';
title.classList.add('active');
title.classList.remove('hidden');
title.classList.toggle('expanded');

// 创建和插入元素
const newDiv = document.createElement('div');
newDiv.textContent = '动态创建的元素';
document.body.appendChild(newDiv);

// 删除元素
const oldNode = document.querySelector('.old');
oldNode.remove();
```

### 4. 事件监听

```javascript
const button = document.querySelector('#submit');

// 方式一：onXXX 属性（不推荐）
button.onclick = function() {
    console.log('点击了按钮');
};

// 方式二：addEventListener（推荐，可绑定多个）
button.addEventListener('click', function(event) {
    console.log('点击了按钮');
    console.log('事件对象：', event);
    event.preventDefault(); // 阻止默认行为
});

// 常见事件类型
// click - 点击
// mouseover/mouseout - 鼠标移入/移出
// keydown/keyup - 键盘按下/松开
// submit - 表单提交
// change - 输入框内容变化
// input - 实时输入
```

### 5. JSON

JSON（JavaScript Object Notation）是前后端数据交换的标准格式：

```javascript
// JS 对象转 JSON 字符串
const user = { name: '张三', age: 25 };
const jsonStr = JSON.stringify(user);
// 结果：'{"name":"张三","age":25}'

// JSON 字符串转 JS 对象
const parsed = JSON.parse('{"name":"李四","age":30}');
console.log(parsed.name); // 李四

// 格式化输出
const prettyJson = JSON.stringify(user, null, 2);
```

## 动手练习

### 练习 1：计数器

创建一个计数器组件：
- 显示当前数字
- "+" 按钮增加 1
- "-" 按钮减少 1
- 数字为负数时显示红色

```html
<div>
    <button id="decrease">-</button>
    <span id="count">0</span>
    <button id="increase">+</button>
</div>
```

### 练习 2：Todo 列表

实现一个简单的 Todo 列表：
- 输入框 + 添加按钮
- 列表显示所有任务
- 每个任务可删除
- 使用 localStorage 保存数据

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| `Cannot read property of null` | 元素未找到就操作 | 确保 DOM 加载完成后再操作，或使用 `window.onload` |
| 事件未触发 | 元素是动态创建的 | 使用事件委托，绑定到父元素 |
| `this` 指向错误 | 箭头函数没有自己的 `this` | 在需要指向当前对象时使用普通函数 |
| 数字相加变成字符串拼接 | 隐式类型转换 | 使用 `parseInt()` 或 `Number()` 转换 |

## 本节小结

JavaScript 是前端开发的核心语言。掌握变量、函数、DOM 操作和事件监听，你就能实现页面的动态交互效果。JSON 是前后端沟通的桥梁，务必熟练掌握。

## 参考文档

- [MDN JavaScript 基础](https://developer.mozilla.org/zh-CN/docs/Learn/JavaScript/First_steps)
- [MDN DOM 操作](https://developer.mozilla.org/zh-CN/docs/Learn/JavaScript/Client-side_web_APIs/Manipulating_documents)
- [MDN 事件](https://developer.mozilla.org/zh-CN/docs/Learn/JavaScript/Building_blocks/Events)
