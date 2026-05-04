# HTML/CSS 基础

## 学习目标

学完本节后，你将能够：
- 理解网页的本质：HTML 是骨架，CSS 是皮肤，JavaScript 是肌肉
- 使用常用 HTML 标签搭建页面结构
- 使用 CSS 选择器和 Flex 布局美化页面
- 理解盒模型，解决元素尺寸和间距问题

---

## 核心知识点

### 1. 网页的本质——从一张白纸到精美页面

#### 1.1 网页是什么

你在浏览器看到的每一个页面，本质上是三个技术协作的结果：

```
┌─────────────────────────────────────────────────────────────┐
│                     网页 = HTML + CSS + JS                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  HTML（HyperText Markup Language）                            │
│  → 网页的骨架和结构                                          │
│  → 决定页面上有什么内容（文字、图片、按钮、表格）              │
│  → 类比：建筑的结构图纸                                      │
│                                                              │
│  CSS（Cascading Style Sheets）                               │
│  → 网页的皮肤和样式                                          │
│  → 决定内容长什么样（颜色、大小、位置、动画）                  │
│  → 类比：建筑的装修效果图                                    │
│                                                              │
│  JavaScript                                                  │
│  → 网页的肌肉和行为                                          │
│  → 决定页面能做什么交互（点击、输入、动画、请求数据）          │
│  → 类比：建筑的智能系统（电梯、灯光控制）                      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

#### 1.2 第一个网页

创建一个文件 `index.html`，用记事本或 VS Code 打开，输入：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>我的第一个网页</title>
</head>
<body>
    <h1>你好，世界！</h1>
    <p>这是我的第一个网页。</p>
</body>
</html>
```

保存后，双击文件用浏览器打开。你会看到页面上显示"你好，世界！"和"这是我的第一个网页。"

**每一行的含义：**

```html
<!DOCTYPE html>           <!-- 告诉浏览器：这是 HTML5 文档 -->
<html lang="zh-CN">       <!-- HTML 的根标签，lang 表示语言（中文） -->
<head>                    <!-- 头部：放配置信息（不显示在页面上） -->
    <meta charset="UTF-8"> <!-- 字符编码：UTF-8 支持中文 -->
    <title>...<\/title>    <!-- 浏览器标签页上显示的标题 -->
<\/head>
<body>                    <!-- 主体：页面上显示的所有内容都在这里 -->
    <h1>...<\/h1>          <!-- 一级标题（最大的标题） -->
    <p>...<\/p>            <!-- 段落 -->
<\/body>
<\/html>
```

---

### 2. HTML 常用标签——积木块

HTML 标签就像乐高积木，不同的标签有不同的形状和功能。

#### 2.1 文本标签

```html
<!-- 标题：h1 最大，h6 最小，一个页面建议只有一个 h1 -->
<h1>这是一级标题</h1>
<h2>这是二级标题</h2>
<h3>这是三级标题</h3>

<!-- 段落 -->
<p>这是一个段落。段落会自动换行，并且段与段之间有间距。</p>
<p>这是另一个段落。</p>

<!-- 换行（br 是空标签，不需要闭合） -->
<p>第一行<br>第二行</p>

<!-- 水平分割线 -->
<hr>

<!-- 粗体和斜体 -->
<p><strong>重要内容</strong>（加粗，有语义）</p>
<p><b>普通加粗</b>（仅样式）</p>
<p><em>强调内容</em>（斜体，有语义）</p>

<!-- 上标和下标 -->
<p>水的化学式：H<sub>2</sub>O</p>
<p>2 的 3 次方：2<sup>3</sup> = 8</p>
```

#### 2.2 链接和图片

```html
<!-- 超链接：点击后跳转到另一个页面 -->
<!-- href = 跳转的目标地址 -->
<!-- target="_blank" = 在新标签页打开 -->
<a href="https://www.baidu.com" target="_blank">点击访问百度</a>

<!-- 图片：src = 图片地址，alt = 图片加载失败时的替代文字 -->
<img src="logo.png" alt="公司 Logo" width="200" height="100">

<!-- 链接包裹图片（点击图片跳转） -->
<a href="https://example.com">
    <img src="banner.jpg" alt="广告横幅">
<\/a>
```

#### 2.3 列表

```html
<!-- 无序列表（ul = unordered list，li = list item） -->
<!-- 默认显示为圆点 -->
<ul>
    <li>苹果</li>
    <li>香蕉</li>
    <li>橙子</li>
<\/ul>

<!-- 有序列表（ol = ordered list） -->
<!-- 默认显示为数字 1, 2, 3 -->
<ol>
    <li>第一步：注册账号</li>
    <li>第二步：填写信息</li>
    <li>第三步：提交审核</li>
<\/ol>
```

#### 2.4 表格

```html
<!-- 表格：table > thead/tbody > tr > th/td -->
<table border="1">
    <thead>                    <!-- 表头 -->
        <tr>                   <!-- tr = table row（行） -->
            <th>姓名</th>      <!-- th = table header（表头单元格，默认加粗居中） -->
            <th>年龄</th>
            <th>部门</th>
        <\/tr>
    <\/thead>
    <tbody>                    <!-- 表体 -->
        <tr>
            <td>张三</td>      <!-- td = table data（数据单元格） -->
            <td>25</td>
            <td>研发部</td>
        <\/tr>
        <tr>
            <td>李四</td>
            <td>30</td>
            <td>市场部</td>
        <\/tr>
    <\/tbody>
<\/table>
```

#### 2.5 表单（用户输入）

```html
<!-- 表单：收集用户输入的数据 -->
<!-- action = 数据提交到哪里（后端接口地址） -->
<!-- method = 提交方式（GET 或 POST） -->
<form action="/login" method="POST">
    <!-- 文本输入框 -->
    <label for="username">用户名：</label>
    <input type="text" id="username" name="username" placeholder="请输入用户名">

    <br><br>

    <!-- 密码输入框（输入内容显示为圆点） -->
    <label for="password">密码：</label>
    <input type="password" id="password" name="password" placeholder="请输入密码">

    <br><br>

    <!-- 单选框（name 相同表示一组，只能选一个） -->
    <label>性别：</label>
    <input type="radio" name="gender" value="1" id="male">
    <label for="male">男</label>
    <input type="radio" name="gender" value="2" id="female">
    <label for="female">女</label>

    <br><br>

    <!-- 复选框 -->
    <label>爱好：</label>
    <input type="checkbox" name="hobby" value="read"> 读书
    <input type="checkbox" name="hobby" value="sport"> 运动
    <input type="checkbox" name="hobby" value="music"> 音乐

    <br><br>

    <!-- 下拉选择框 -->
    <label>部门：</label>
    <select name="dept">
        <option value="1">研发部</option>
        <option value="2">市场部</option>
        <option value="3">人事部</option>
    <\/select>

    <br><br>

    <!-- 文本域（多行文本输入） -->
    <label>备注：</label><br>
    <textarea name="remark" rows="4" cols="50" placeholder="请输入备注..."><\/textarea>

    <br><br>

    <!-- 提交按钮 -->
    <button type="submit">登录</button>
    <!-- 重置按钮 -->
    <button type="reset">重置</button>
<\/form>
```

#### 2.6 语义化标签（HTML5 新增）

语义化标签让 HTML 结构更清晰，对搜索引擎（SEO）和屏幕阅读器更友好。

```html
<!DOCTYPE html>
<html>
<head><title>新闻网站</title><\/head>
<body>
    <!-- 页头：放 Logo、导航等 -->
    <header>
        <img src="logo.png" alt="Logo">
        <nav>                   <!-- 导航栏 -->
            <a href="/">首页</a>
            <a href="/news">新闻</a>
            <a href="/about">关于</a>
        <\/nav>
    <\/header>

    <!-- 主体内容 -->
    <main>
        <!-- 文章 -->
        <article>
            <h1>新闻标题</h1>
            <p>新闻内容...</p>
        <\/article>

        <!-- 侧边栏 -->
        <aside>
            <h3>热门推荐</h3>
            <ul>
                <li><a href="#">推荐文章 1</a><\/li>
                <li><a href="#">推荐文章 2</a><\/li>
            <\/ul>
        <\/aside>
    <\/main>

    <!-- 页脚：放版权信息、联系方式等 -->
    <footer>
        <p>版权所有  2024 某公司</p>
    <\/footer>
<\/body>
<\/html>
```

**语义化标签 vs div：**

```html
<!-- ❌ 全是 div，看不出结构 -->
<div class="header">...<\/div>
<div class="nav">...<\/div>
<div class="content">...<\/div>
<div class="sidebar">...<\/div>
<div class="footer">...<\/div>

<!-- ✅ 语义化标签，结构清晰 -->
<header>...</header>
<nav>...</nav>
<main>
    <article>...</article>
    <aside>...</aside>
<\/main>
<footer>...</footer>
```

---

### 3. CSS 基础——给页面穿上衣服

#### 3.1 CSS 的三种引入方式

```html
<!-- 方式一：内联样式（写在标签的 style 属性中，优先级最高，不推荐） -->
<p style="color: red; font-size: 20px;">红色文字</p>

<!-- 方式二：内部样式表（写在 head 的 style 标签中） -->
<head>
    <style>
        p { color: blue; }
    <\/style>
<\/head>

<!-- 方式三：外部样式表（推荐！样式写在单独的 .css 文件中） -->
<head>
    <link rel="stylesheet" href="style.css">
<\/head>
```

**外部样式表（style.css）：**
```css
/* 这是注释 */

/* 选择器 { 属性: 值; } */
p {
    color: blue;           /* 文字颜色 */
    font-size: 16px;       /* 字体大小 */
    line-height: 1.5;      /* 行高 */
}
```

#### 3.2 CSS 选择器——找到要修饰的元素

```css
/* ========== 基础选择器 ========== */

/* 元素选择器：选中所有 p 标签 */
p {
    color: red;
}

/* 类选择器：选中 class="active" 的所有元素 */
.active {
    background: yellow;
}

/* ID 选择器：选中 id="header" 的元素（一个页面只能有一个） */
#header {
    height: 60px;
}

/* 通用选择器：选中所有元素 */
* {
    margin: 0;
    padding: 0;
}

/* ========== 组合选择器 ========== */

/* 后代选择器：选中 .nav 里面的所有 a */
.nav a {
    text-decoration: none;
}

/* 子选择器：选中 .menu 的直接子元素 li（不包括孙元素） */
.menu > li {
    border-bottom: 1px solid #ccc;
}

/* 相邻兄弟选择器：选中紧接在 h2 后面的 p */
h2 + p {
    font-size: 14px;
}

/* 群组选择器：同时选中多个 */
h1, h2, h3 {
    color: #333;
}

/* ========== 伪类选择器 ========== */

/* :hover：鼠标悬停时的样式 */
button:hover {
    background: #409EFF;
    cursor: pointer;
}

/* :focus：输入框获得焦点时的样式 */
input:focus {
    border-color: #409EFF;
    outline: none;           /* 去掉浏览器默认的蓝色轮廓 */
}

/* :nth-child()：选中第 n 个子元素 */
li:nth-child(odd) {         /* 奇数行 */
    background: #f5f5f5;
}
li:nth-child(even) {        /* 偶数行 */
    background: white;
}

/* ========== 选择器优先级 ========== */
/*
优先级从高到低：
  1. !important（最高，尽量少用）
  2. 内联样式（style="..."）
  3. ID 选择器（#header）
  4. 类选择器（.active）、伪类（:hover）
  5. 元素选择器（p）、伪元素（::before）
  6. 通用选择器（*）

记忆口诀：内联 > ID > 类 > 元素
*/
```

#### 3.3 常用 CSS 属性

```css
/* ========== 文字样式 ========== */
.text-demo {
    color: #333;                    /* 文字颜色 */
    font-size: 16px;                /* 字体大小 */
    font-weight: bold;              /* 字体粗细：normal/bold/100-900 */
    font-family: "Microsoft YaHei", sans-serif;  /* 字体 */
    text-align: center;             /* 文字对齐：left/center/right */
    text-decoration: underline;     /* 文字装饰：none/underline/line-through */
    line-height: 1.5;               /* 行高（1.5 倍字体大小） */
}

/* ========== 背景样式 ========== */
.bg-demo {
    background-color: #f5f5f5;      /* 背景颜色 */
    background-image: url("bg.jpg"); /* 背景图片 */
    background-size: cover;          /* 图片填充方式 */
    background-position: center;     /* 图片位置 */
}

/* ========== 边框和圆角 ========== */
.border-demo {
    border: 1px solid #ccc;         /* 边框：粗细 样式 颜色 */
    border-radius: 4px;             /* 圆角 */
    box-shadow: 0 2px 12px rgba(0,0,0,0.1);  /* 阴影 */
}

/* ========== 尺寸和间距 ========== */
.size-demo {
    width: 300px;                   /* 宽度 */
    height: 200px;                  /* 高度 */
    padding: 20px;                  /* 内边距（内容和边框之间的距离） */
    margin: 10px;                   /* 外边距（元素和其他元素之间的距离） */
}

/* ========== 显示和定位 ========== */
.display-demo {
    display: block;                 /* 块级元素（独占一行） */
    display: inline;                /* 行内元素（不独占一行） */
    display: inline-block;          /* 行内块（可设宽高，不独占一行） */
    display: none;                  /* 隐藏 */

    position: relative;             /* 相对定位（相对于自己原来的位置） */
    position: absolute;             /* 绝对定位（相对于最近的定位祖先） */
    position: fixed;                /* 固定定位（相对于浏览器窗口） */
}
```

---

### 4. CSS 盒模型——理解元素的"占地面积"

#### 4.1 盒模型的组成

每个 HTML 元素都是一个矩形盒子，由四部分组成：

```
┌─────────────────────────────────────────┐
│              margin（外边距）              │  ← 元素与其他元素的距离
│   ┌─────────────────────────────────┐   │
│   │           border（边框）         │   │  ← 盒子的边框线
│   │   ┌─────────────────────────┐   │   │
│   │   │       padding（内边距）   │   │   │  ← 内容和边框的距离
│   │   │   ┌─────────────────┐   │   │   │
│   │   │   │                 │   │   │   │
│   │   │   │   content       │   │   │   │  ← 实际内容（文字、图片）
│   │   │   │   （宽 × 高）    │   │   │   │
│   │   │   │                 │   │   │   │
│   │   │   └─────────────────┘   │   │   │
│   │   └─────────────────────────┘   │   │
│   └─────────────────────────────────┘   │
└─────────────────────────────────────────┘

元素实际占据的宽度 = width + padding-left + padding-right + border-left + border-right + margin-left + margin-right
```

#### 4.2 两种盒模型

```css
/* 标准盒模型（默认）：width 只包含 content */
.box1 {
    width: 300px;
    padding: 20px;
    border: 1px solid #ccc;
    /* 实际宽度 = 300 + 20 + 20 + 1 + 1 = 342px */
}

/* 怪异盒模型：width 包含 content + padding + border */
.box2 {
    box-sizing: border-box;     /* 关键属性！ */
    width: 300px;
    padding: 20px;
    border: 1px solid #ccc;
    /* 实际宽度 = 300px（content 自动缩小为 258px） */
}
```

**推荐：全局设置 `box-sizing: border-box`，这样设置 width 就是最终看到的宽度。**

```css
/* 在每个项目的样式文件开头加上： */
* {
    box-sizing: border-box;
}
```

---

### 5. Flex 布局——一维布局的神器

#### 5.1 为什么需要 Flex

传统布局用 `float`（浮动）和 `position`（定位），代码复杂、容易出问题。Flex 布局让一维排列（横向或纵向）变得非常简单。

#### 5.2 Flex 基础

```css
/* 给父元素设置 display: flex，它的子元素就变成 flex 项目 */
.container {
    display: flex;           /* 开启 Flex 布局 */
}
```

```html
<div class="container">
    <div class="item">项目 1</div>
    <div class="item">项目 2</div>
    <div class="item">项目 3</div>
</div>
```

**默认效果：**三个项目横向排列，从左到右。

#### 5.3 Flex 容器属性（给父元素设置）

```css
.container {
    display: flex;

    /* ========== 主轴方向 ========== */
    flex-direction: row;            /* 横向排列（默认） */
    flex-direction: row-reverse;    /* 横向反向排列 */
    flex-direction: column;         /* 纵向排列 */

    /* ========== 主轴对齐（水平方向） ========== */
    justify-content: flex-start;    /* 左对齐（默认） */
    justify-content: center;        /* 居中对齐 */
    justify-content: flex-end;      /* 右对齐 */
    justify-content: space-between; /* 两端对齐，项目之间间距相等 */
    justify-content: space-around;  /* 项目两侧间距相等 */

    /* ========== 交叉轴对齐（垂直方向） ========== */
    align-items: stretch;           /* 拉伸填满（默认） */
    align-items: center;            /* 垂直居中 */
    align-items: flex-start;        /* 顶部对齐 */
    align-items: flex-end;          /* 底部对齐 */

    /* ========== 换行 ========== */
    flex-wrap: nowrap;              /* 不换行（默认） */
    flex-wrap: wrap;                /* 换行 */
}
```

#### 5.4 Flex 项目属性（给子元素设置）

```css
.item {
    /* 占据剩余空间的比例 */
    /* 如果所有项目的 flex 都是 1，则平分空间 */
    flex: 1;

    /* 不收缩 */
    flex-shrink: 0;

    /* 单独设置自己在交叉轴的对齐方式 */
    align-self: center;
}
```

#### 5.5 Flex 实战示例

```css
/* 示例 1：导航栏（左右分布） */
.navbar {
    display: flex;
    justify-content: space-between;  /* Logo 在左，菜单在右 */
    align-items: center;              /* 垂直居中 */
    padding: 0 20px;
    height: 60px;
    background: #001529;
    color: white;
}

/* 示例 2：卡片列表（等宽排列） */
.card-list {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;                        /* 项目之间的间距 */
}
.card-list .card {
    flex: 1;
    min-width: 250px;                 /* 最小宽度 */
}

/* 示例 3：居中布局（最常用的！） */
.center-box {
    display: flex;
    justify-content: center;          /* 水平居中 */
    align-items: center;              /* 垂直居中 */
    height: 100vh;                    /* 占满整个视口高度 */
}
```

---

## 动手练习

### 练习 1：新闻列表页面

**目标**：创建一个包含导航栏、内容区和页脚的页面。

**要求**：
- 顶部导航栏：左侧 Logo，右侧菜单（Flex 布局，space-between）
- 中间内容区：左侧占 2/3（新闻列表），右侧占 1/3（热门推荐）
- 底部页脚：居中的版权信息
- 使用语义化标签（header、nav、main、article、aside、footer）

**参考代码：**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>新闻网站</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: "Microsoft YaHei", sans-serif; }

        /* 导航栏 */
        header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0 20px;
            height: 60px;
            background: #001529;
            color: white;
        }
        nav a {
            color: white;
            text-decoration: none;
            margin-left: 20px;
        }
        nav a:hover { color: #1890ff; }

        /* 主体内容 */
        main {
            display: flex;
            max-width: 1200px;
            margin: 20px auto;
            gap: 20px;
        }
        article { flex: 2; }
        aside { flex: 1; }

        .news-item {
            padding: 15px;
            border-bottom: 1px solid #eee;
        }
        .news-item:hover { background: #f5f5f5; }

        /* 侧边栏 */
        .hot-list {
            background: #f5f5f5;
            padding: 15px;
            border-radius: 4px;
        }
        .hot-list li {
            padding: 8px 0;
            border-bottom: 1px dashed #ddd;
        }

        /* 页脚 */
        footer {
            text-align: center;
            padding: 20px;
            background: #f5f5f5;
            margin-top: 40px;
        }
    <\/style>
<\/head>
<body>
    <header>
        <div class="logo">新闻网站</div>
        <nav>
            <a href="#">首页</a>
            <a href="#">国内</a>
            <a href="#">国际</a>
            <a href="#">科技</a>
        <\/nav>
    <\/header>

    <main>
        <article>
            <h1>今日新闻</h1>
            <div class="news-item">
                <h3>新闻标题 1</h3>
                <p>新闻摘要...</p>
            <\/div>
            <div class="news-item">
                <h3>新闻标题 2</h3>
                <p>新闻摘要...</p>
            <\/div>
        <\/article>

        <aside>
            <div class="hot-list">
                <h3>热门推荐</h3>
                <ul>
                    <li>热门文章 1</li>
                    <li>热门文章 2</li>
                    <li>热门文章 3</li>
                <\/ul>
            <\/div>
        <\/aside>
    <\/main>

    <footer>
        <p>版权所有  2024 新闻网站</p>
    <\/footer>
<\/body>
<\/html>
```

### 练习 2：登录表单

**目标**：创建一个居中显示、美观的登录表单。

**要求：**
- 表单在页面正中间（水平垂直居中）
- 输入框有圆角和聚焦效果
- 按钮有 hover 效果
- 使用 `box-sizing: border-box`

---

## 常见错误排查

### 阶段 1：元素显示问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 元素宽度超出容器 | 默认 `content-box`，padding 和 border 会增加实际宽度 | 设置 `box-sizing: border-box` |
| 图片下方有空白 | img 是行内元素，默认和文字基线对齐 | 设置 `img { display: block; }` 或 `vertical-align: middle;` |
| 两个块元素之间有额外间距 | margin 塌陷（垂直方向 margin 合并） | 只设置一个方向的 margin，或用 padding 替代 |

### 阶段 2：Flex 布局问题

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| Flex 子元素未等高 | 默认 `align-items: stretch`，但被其他样式覆盖 | 检查是否设置了 `align-items: center` 等 |
| 子元素没有按预期排列 | `flex-direction` 设置错误 | 默认是 `row`（横向），纵向用 `column` |
| `justify-content` 不生效 | 子元素设置了固定宽度，没有剩余空间 | 给子元素设置 `flex: 1` 或去掉固定宽度 |
| 最后一行项目少时，分布不均 | Flex 的 space-between 会让最后一行也分散 | 使用 `justify-content: flex-start` + `gap` |

### 阶段 3：样式不生效

| 错误现象 | 原因分析 | 解决方案 |
|---------|---------|---------|
| 写的 CSS 没效果 | 选择器优先级不够 | 检查是否有更高优先级的样式覆盖 |
| 颜色、字体不生效 | 选择器没选中目标元素 | 用浏览器开发者工具检查元素的实际样式 |
| 引入的 CSS 文件没加载 | 路径错误或文件名拼写错误 | 检查 `link` 标签的 `href` 路径 |
| 中文乱码 | 没设置字符编码 | `<meta charset="UTF-8">` |

---

## 本节小结

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        HTML/CSS 核心知识                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  网页 = HTML（结构）+ CSS（样式）+ JS（行为）                              │
│                                                                          │
│  HTML 常用标签：                                                          │
│    • 文本：h1-h6, p, br, strong, em                                      │
│    • 链接图片：a, img                                                     │
│    • 列表：ul/ol + li                                                     │
│    • 表格：table > tr > th/td                                             │
│    • 表单：form, input, select, textarea, button                          │
│    • 语义化：header, nav, main, article, aside, footer                    │
│                                                                          │
│  CSS 核心：                                                              │
│    • 选择器：元素、类（.）、ID（#）、后代、伪类（:hover）                    │
│    • 优先级：内联 > ID > 类 > 元素                                         │
│    • 盒模型：content + padding + border + margin                          │
│    • 必设：* { box-sizing: border-box; }                                  │
│                                                                          │
│  Flex 布局（最常用）：                                                    │
│    • 父元素：display: flex                                                │
│    • 主轴对齐：justify-content（flex-start/center/space-between）          │
│    • 交叉轴对齐：align-items（stretch/center）                            │
│    • 子元素：flex: 1（平分空间）                                           │
│    • 居中神器：justify-content: center + align-items: center              │
│                                                                          │
│  避坑指南：                                                              │
│    • 全局设置 box-sizing: border-box                                     │
│    • 用语义化标签而不是全是 div                                            │
│    • Flex 布局能解决 90% 的排列问题                                        │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 参考文档

- [MDN HTML 基础](https://developer.mozilla.org/zh-CN/docs/Learn/HTML/Introduction_to_HTML)
- [MDN CSS 基础](https://developer.mozilla.org/zh-CN/docs/Learn/CSS/First_steps)
- [MDN Flexbox 布局](https://developer.mozilla.org/zh-CN/docs/Learn/CSS/CSS_layout/Flexbox)
- [CSS Tricks Flexbox 指南](https://css-tricks.com/snippets/css/a-guide-to-flexbox/)
