# JavaScript 核心语法

## 学习目标

学完本节后，你将能够：
- 理解 JavaScript 在网页中的角色，能写出简单的交互逻辑
- 掌握变量、数据类型、函数、条件判断、循环等基础语法
- 使用 DOM 操作动态修改页面内容
- 理解 JSON 格式，能在前后端之间传递数据

---

## 核心知识点

### 1. JavaScript 是什么——给网页添加"大脑"

#### 1.1 JS 的角色

HTML 搭好了骨架，CSS 穿上了衣服，但页面还是"死的"——不会动、不会响应用户操作。

JavaScript（简称 JS）让网页"活"起来：

```
没有 JS 的页面：
  • 点击按钮 → 没反应
  • 填写表单 → 不能校验
  • 加载数据 → 必须刷新整个页面

有 JS 的页面：
  • 点击按钮 → 弹出提示、切换内容
  • 填写表单 → 实时校验、错误提示
  • 加载数据 → 只更新部分内容（Ajax）
```

#### 1.2 JS 的三种引入方式

```html
<!-- 方式一：内联（写在标签的 onclick 等属性中，不推荐） -->
<button onclick="alert('你好')">点击</button>

<!-- 方式二：内部脚本（写在 script 标签中） -->
<script>
    console.log('Hello, JavaScript!');
<\/script>

<!-- 方式三：外部脚本（推荐！代码写在单独的 .js 文件中） -->
<script src="app.js"><\/script>
```

**`app.js`：**
```javascript
console.log('Hello, JavaScript!');
```

---

### 2. 变量与数据类型——JS 的"储物柜"

#### 2.1 声明变量

```javascript
// let：声明可变变量（推荐）
let name = '张三';
name = '李四';  // 可以重新赋值

// const：声明常量（不可重新赋值）
const PI = 3.14159;
// PI = 3.14;  // ❌ 报错！常量不能重新赋值

// 注意：const 声明的对象，其属性可以修改
const user = { name: '张三' };
user.name = '李四';  // ✅ 可以修改属性
// user = {};        // ❌ 不能重新赋值整个对象

// var：旧写法（不推荐，有作用域问题）
var oldWay = '过时了';
```

**let vs const 的选择：**
- 值会改变 → 用 `let`（如计数器、循环变量）
- 值不改变 → 用 `const`（如配置项、DOM 元素引用）

#### 2.2 基本数据类型

```javascript
// ========== 字符串（String）==========
let name = '张三';           // 单引号
let greeting = "你好";       // 双引号
let template = `你好，${name}`;  // 反引号（模板字符串，可嵌入变量）

// 字符串常用方法
name.length;                 // 长度：2
name.charAt(0);              // 第 0 个字符：'张'
name.includes('张');         // 是否包含：true
name.replace('张', '李');     // 替换：'李四'
name.split('');              // 分割成数组：['张', '三']

// ========== 数字（Number）==========
let age = 25;                // 整数
let price = 99.99;           // 小数
let infinity = Infinity;     // 无穷大
let notANumber = NaN;        // 不是数字（如 'abc' * 2）

// 数字运算
let sum = 10 + 5;            // 15
let diff = 10 - 5;           // 5
let product = 10 * 5;        // 50
let quotient = 10 / 3;       // 3.333...（JS 没有整数除法）
let remainder = 10 % 3;      // 1（取余）

// 注意：浮点数精度问题
console.log(0.1 + 0.2);      // 0.30000000000000004（不是 0.3！）
// 解决方案：先乘 100 再除 100
console.log((0.1 * 100 + 0.2 * 100) / 100);  // 0.3

// ========== 布尔（Boolean）==========
let isActive = true;
let isDeleted = false;

// 比较运算
console.log(5 > 3);          // true
console.log(5 === 5);        // true（全等，值和类型都相同）
console.log(5 == '5');       // true（相等，只比较值，不比较类型）
console.log(5 === '5');      // false（全等，类型不同）
// 推荐：永远使用 === 和 !==，避免类型转换的坑

// ========== 空值和未定义 ==========
let empty = null;            // null：明确表示"空值"（程序员主动设置的）
let notDefined;              // undefined：声明了但没赋值（JS 自动给的）
console.log(notDefined);     // undefined

// ========== 数组（Array）==========
let fruits = ['苹果', '香蕉', '橙子'];
console.log(fruits[0]);      // '苹果'（索引从 0 开始）
console.log(fruits.length);  // 3

// 数组常用方法
fruits.push('葡萄');         // 末尾添加
fruits.pop();                // 末尾删除
fruits.unshift('西瓜');      // 开头添加
fruits.shift();              // 开头删除
fruits.indexOf('香蕉');      // 查找索引：1
fruits.includes('苹果');     // 是否包含：true
fruits.splice(1, 1);         // 从索引 1 开始删除 1 个

// ========== 对象（Object）==========
let person = {
    name: '张三',
    age: 25,
    isStudent: false,
    hobbies: ['读书', '游泳'],
    sayHi: function() {
        console.log('你好，我是' + this.name);
    }
};

// 访问属性
console.log(person.name);           // '张三'（点语法）
console.log(person['age']);         // 25（中括号语法，属性名是变量时用）

// 修改属性
person.age = 26;
person.email = 'zhangsan@qq.com';   // 新增属性

// 遍历对象
for (let key in person) {
    console.log(key + ': ' + person[key]);
}
```

---

### 3. 函数——可复用的代码块

#### 3.1 函数定义

```javascript
// 方式一：函数声明（会提升，可以在定义前调用）
function greet(name) {
    return '你好，' + name;
}
console.log(greet('张三'));  // '你好，张三'

// 方式二：函数表达式
const sayHello = function(name) {
    return 'Hello, ' + name;
};

// 方式三：箭头函数（最常用！）
const add = (a, b) => a + b;
console.log(add(2, 3));  // 5

// 箭头函数完整写法（多行代码需要大括号和 return）
const multiply = (a, b) => {
    let result = a * b;
    return result;
};

// 只有一个参数，可以省略括号
const square = x => x * x;

// 默认参数
const greetWithDefault = (name = '访客') => {
    return '你好，' + name;
};
console.log(greetWithDefault());       // '你好，访客'
console.log(greetWithDefault('张三'));  // '你好，张三'
```

#### 3.2 回调函数

回调函数就是把一个函数作为参数传给另一个函数，在"某个时刻"再执行它。

```javascript
// 示例：2 秒后执行回调
setTimeout(() => {
    console.log('2 秒到了！');
}, 2000);

// 示例：数组的 forEach 方法
let nums = [1, 2, 3];
nums.forEach((num, index) => {
    console.log(`索引 ${index} 的值是 ${num}`);
});
// 输出：
// 索引 0 的值是 1
// 索引 1 的值是 2
// 索引 2 的值是 3

// 示例：数组的 map 方法（转换数组）
let doubled = nums.map(num => num * 2);
console.log(doubled);  // [2, 4, 6]

// 示例：数组的 filter 方法（筛选数组）
let evens = nums.filter(num => num % 2 === 0);
console.log(evens);    // [2]
```

---

### 4. 条件判断和循环——控制代码流程

#### 4.1 条件判断

```javascript
let score = 85;

// if / else if / else
if (score >= 90) {
    console.log('优秀');
} else if (score >= 80) {
    console.log('良好');
} else if (score >= 60) {
    console.log('及格');
} else {
    console.log('不及格');
}

// 三元运算符（简单条件）
let result = score >= 60 ? '通过' : '不通过';

// switch（多分支判断）
let day = 1;
switch (day) {
    case 1:
        console.log('星期一');
        break;  // 不要忘记 break！
    case 2:
        console.log('星期二');
        break;
    default:
        console.log('其他');
}
```

#### 4.2 循环

```javascript
// for 循环（知道循环次数时用）
for (let i = 0; i < 5; i++) {
    console.log(i);  // 0, 1, 2, 3, 4
}

// while 循环（不知道循环次数时用）
let count = 0;
while (count < 5) {
    console.log(count);
    count++;
}

// for...of（遍历数组的值）
let fruits = ['苹果', '香蕉', '橙子'];
for (let fruit of fruits) {
    console.log(fruit);
}

// for...in（遍历对象的键）
let person = { name: '张三', age: 25 };
for (let key in person) {
    console.log(key + ': ' + person[key]);
}
```

---

### 5. DOM 操作——用 JS 操控网页

#### 5.1 获取元素

```javascript
// 通过 ID 获取（唯一元素）
const title = document.getElementById('title');

// 通过类名获取（返回集合）
const buttons = document.getElementsByClassName('btn');

// 通过标签名获取
const paragraphs = document.getElementsByTagName('p');

// 通过 CSS 选择器获取第一个匹配的元素（最常用！）
const form = document.querySelector('form');
const submitBtn = document.querySelector('#submit');
const navLinks = document.querySelector('.nav a');

// 通过 CSS 选择器获取所有匹配的元素（返回 NodeList）
const allButtons = document.querySelectorAll('button');
const cards = document.querySelectorAll('.card');
```

#### 5.2 修改元素

```javascript
const title = document.getElementById('title');

// 修改文本内容（纯文本，不解析 HTML）
title.textContent = '新标题';

// 修改 HTML 内容（会解析 HTML 标签）
title.innerHTML = '<span style="color: red">红色标题</span>';

// 修改样式
title.style.color = 'blue';
title.style.fontSize = '24px';  // CSS 属性名变成驼峰式
title.style.backgroundColor = '#f5f5f5';

// 修改类名
title.classList.add('active');       // 添加类
title.classList.remove('hidden');    // 移除类
title.classList.toggle('expanded');  // 切换类（有则移除，无则添加）
title.classList.contains('active');  // 是否包含某个类

// 修改属性
title.setAttribute('data-id', '123');
title.getAttribute('data-id');       // '123'
title.removeAttribute('data-id');
```

#### 5.3 创建和删除元素

```javascript
// 创建新元素
const newDiv = document.createElement('div');
newDiv.textContent = '动态创建的元素';
newDiv.className = 'dynamic-item';

// 添加到页面中
document.body.appendChild(newDiv);           // 添加到 body 末尾
container.insertBefore(newDiv, firstChild);  // 插入到某个元素前面

// 删除元素
oldNode.remove();  // 现代浏览器
// 或者
parentNode.removeChild(oldNode);
```

---

### 6. 事件监听——响应用户操作

#### 6.1 添加事件监听

```javascript
const button = document.querySelector('#submit');

// 方式一：onXXX 属性（不推荐，只能绑定一个处理函数）
button.onclick = function() {
    console.log('点击了');
};

// 方式二：addEventListener（推荐！可以绑定多个）
button.addEventListener('click', function(event) {
    console.log('点击了按钮');
    console.log('事件对象：', event);

    // 阻止默认行为（如表单提交、链接跳转）
    event.preventDefault();

    // 阻止事件冒泡（不向父元素传播）
    event.stopPropagation();
});

// 绑定多个事件
button.addEventListener('mouseenter', () => {
    button.style.background = '#1890ff';
});
button.addEventListener('mouseleave', () => {
    button.style.background = '';
});
```

#### 6.2 常见事件类型

| 事件 | 触发时机 |
|------|---------|
| `click` | 点击 |
| `dblclick` | 双击 |
| `mouseenter` / `mouseleave` | 鼠标移入 / 移出 |
| `mousedown` / `mouseup` | 鼠标按下 / 松开 |
| `keydown` / `keyup` | 键盘按下 / 松开 |
| `input` | 输入框内容变化（实时） |
| `change` | 输入框内容变化（失去焦点时） |
| `submit` | 表单提交 |
| `focus` / `blur` | 获得焦点 / 失去焦点 |
| `load` | 资源加载完成 |
| `scroll` | 页面滚动 |
| `resize` | 窗口大小改变 |

---

### 7. JSON——前后端的数据桥梁

#### 7.1 什么是 JSON

JSON（JavaScript Object Notation）是一种轻量级的数据交换格式。它长得和 JS 对象很像，但有两个关键区别：

```javascript
// JS 对象
let person = {
    name: '张三',
    age: 25,
    isStudent: false,
    hobbies: ['读书', '游泳']
};

// JSON 字符串（注意：键必须用双引号包裹）
let jsonStr = '{"name": "张三", "age": 25, "isStudent": false, "hobbies": ["读书", "游泳"]}';
```

**JSON 的规则：**
1. 键必须用双引号包裹（`"name"`）
2. 字符串值必须用双引号包裹
3. 不能有函数、undefined、注释
4. 末尾不能有多余的逗号

#### 7.2 JSON 和 JS 对象的转换

```javascript
// JS 对象 → JSON 字符串（发送给后端）
let user = { name: '张三', age: 25 };
let jsonStr = JSON.stringify(user);
console.log(jsonStr);  // '{"name":"张三","age":25}'

// 格式化输出（方便阅读）
let prettyJson = JSON.stringify(user, null, 2);
console.log(prettyJson);
// {
//   "name": "张三",
//   "age": 25
// }

// JSON 字符串 → JS 对象（接收后端数据）
let received = '{"name": "李四", "age": 30}';
let obj = JSON.parse(received);
console.log(obj.name);  // '李四'
console.log(obj.age);   // 30
```

---

## 动手练习

### 练习 1：计数器

```html
<!DOCTYPE html>
<html>
<head>
    <style>
        .counter {
            display: flex;
            align-items: center;
            gap: 20px;
            font-size: 24px;
        }
        button {
            width: 40px;
            height: 40px;
            font-size: 20px;
            cursor: pointer;
        }
        .negative {
            color: red;
        }
    <\/style>
<\/head>
<body>
    <div class="counter">
        <button id="decrease">-</button>
        <span id="count">0</span>
        <button id="increase">+</button>
    <\/div>

    <script>
        let count = 0;
        const countSpan = document.getElementById('count');
        const decreaseBtn = document.getElementById('decrease');
        const increaseBtn = document.getElementById('increase');

        function updateDisplay() {
            countSpan.textContent = count;
            if (count < 0) {
                countSpan.classList.add('negative');
            } else {
                countSpan.classList.remove('negative');
            }
        }

        decreaseBtn.addEventListener('click', () => {
            count--;
            updateDisplay();
        });

        increaseBtn.addEventListener('click', () => {
            count++;
            updateDisplay();
        });
    <\/script>
<\/body>
<\/html>
```

### 练习 2：Todo 列表

实现一个简单的 Todo 列表：
- 输入框 + 添加按钮
- 列表显示所有任务
- 每个任务可删除（点击删除按钮）
- 任务完成时可标记（点击文字划线）

---

## 常见错误排查

### 阶段 1：语法错误

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Uncaught SyntaxError` | 语法错误 | 检查括号是否配对、引号是否闭合、是否有中文标点 |
| `let/const 重复声明` | 同一作用域内重复声明 | 改用不同的变量名 |
| `Cannot access 'x' before initialization` | 暂时性死区（TDZ） | `let/const` 声明前不能使用 |

### 阶段 2：类型错误

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Cannot read property 'xxx' of null/undefined` | 对象为 null 或 undefined | 使用前判断 `if (obj)` |
| `xxx is not a function` | 调用了不存在的方法 | 检查变量类型是否正确 |
| `1 + '1' = '11'` | 隐式类型转换 | 使用 `Number()` 或 `parseInt()` 转换 |

### 阶段 3：DOM 操作错误

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| `Cannot read property of null` | DOM 元素未找到 | 确保 `querySelector` 的选择器正确，元素已存在 |
| 事件未触发 | 元素是动态创建的 | 使用事件委托，绑定到父元素 |
| `this` 指向错误 | 箭头函数没有自己的 `this` | 需要指向当前对象时用普通函数 |

### 阶段 4：逻辑错误

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 数组遍历时索引越界 | 循环条件错误 | 检查循环终止条件 |
| 比较两个对象不相等 | 对象比较的是引用 | 比较属性值，或使用 `JSON.stringify` |
| 异步代码先执行 | 代码执行顺序错误 | 使用 `async/await` 或回调函数 |

---

## 本节小结

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      JavaScript 核心知识                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  JS 是什么：让网页"活"起来的编程语言                                       │
│                                                                          │
│  变量声明：                                                              │
│    • let：可变变量（推荐）                                               │
│    • const：常量（推荐）                                                 │
│    • 优先用 const，需要改变时用 let                                       │
│                                                                          │
│  数据类型：                                                              │
│    • 基本：String、Number、Boolean、null、undefined                      │
│    • 引用：Array、Object                                                 │
│    • 比较：永远用 === 和 !==（全等）                                      │
│                                                                          │
│  函数：                                                                  │
│    • 箭头函数：(a, b) => a + b（最常用）                                 │
│    • 回调函数：作为参数传递的函数                                         │
│    • 数组方法：forEach、map、filter                                       │
│                                                                          │
│  条件循环：                                                              │
│    • if / else if / else                                                │
│    • for、while、for...of                                               │
│                                                                          │
│  DOM 操作：                                                              │
│    • 获取：querySelector、querySelectorAll                               │
│    • 修改：textContent、innerHTML、style、classList                       │
│    • 创建：createElement、appendChild                                     │
│                                                                          │
│  事件：                                                                  │
│    • addEventListener('click', handler)                                  │
│    • 常见：click、input、submit、focus                                    │
│                                                                          │
│  JSON：                                                                  │
│    • JSON.stringify(obj) → 字符串                                        │
│    • JSON.parse(str) → 对象                                              │
│                                                                          │
│  避坑指南：                                                              │
│    • 用 === 而不是 ==                                                     │
│    • 浮点数运算先乘 100 再除 100                                          │
│    • DOM 操作前确保元素已存在                                             │
│    • 箭头函数的 this 指向外层                                              │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 参考文档

- [MDN JavaScript 基础](https://developer.mozilla.org/zh-CN/docs/Learn/JavaScript/First_steps)
- [MDN DOM 操作](https://developer.mozilla.org/zh-CN/docs/Learn/JavaScript/Client-side_web_APIs/Manipulating_documents)
- [MDN 事件](https://developer.mozilla.org/zh-CN/docs/Learn/JavaScript/Building_blocks/Events)
- [JavaScript 高级程序设计（书籍）](https://book.douban.com/subject/35175341/)
