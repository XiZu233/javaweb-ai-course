# HTML-CSS 基础

## 学习目标

- 理解 HTML 语义化标签的作用
- 掌握常见 CSS 选择器和盒模型
- 能够使用 Flex 布局完成常见页面结构
- 理解 DOM 操作和事件监听的基本原理

## 核心知识点

### 1. HTML 语义化

HTML5 推荐使用语义化标签而非纯 div，这对可访问性和 SEO 都有帮助：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>页面标题</title>
</head>
<body>
    <header>页头</header>
    <nav>导航栏</nav>
    <main>
        <article>文章内容</article>
        <aside>侧边栏</aside>
    </main>
    <footer>页脚</footer>
</body>
</html>
```

常用标签：

| 标签 | 用途 |
|------|------|
| `<div>` | 通用容器 |
| `<span>` | 行内文本容器 |
| `<p>` | 段落 |
| `<a>` | 超链接 |
| `<img>` | 图片 |
| `<table>` | 表格 |
| `<form>` | 表单 |
| `<input>` | 输入框 |
| `<button>` | 按钮 |

### 2. CSS 盒模型

每个 HTML 元素都是一个矩形盒子，由四部分组成：

```
┌─────────────────────────────┐
│          margin（外边距）      │
│   ┌─────────────────────┐   │
│   │    border（边框）    │   │
│   │   ┌─────────────┐   │   │
│   │   │ padding（内边距）│   │   │
│   │   │   ┌─────┐   │   │   │
│   │   │   │content│   │   │   │
│   │   │   └─────┘   │   │   │
│   │   └─────────────┘   │   │
│   └─────────────────────┘   │
└─────────────────────────────┘
```

```css
.box {
    width: 300px;
    height: 200px;
    padding: 20px;
    border: 1px solid #ccc;
    margin: 10px;
    box-sizing: border-box; /* 边框盒模型：width 包含 padding 和 border */
}
```

### 3. Flex 布局

Flex 是目前最常用的 CSS 布局方式，用于一维排列：

```css
.container {
    display: flex;
    justify-content: space-between; /* 主轴对齐 */
    align-items: center;            /* 交叉轴对齐 */
    flex-wrap: wrap;                /* 允许换行 */
}

.item {
    flex: 1;        /* 占据剩余空间 */
    flex-shrink: 0; /* 不收缩 */
}
```

常用属性速查：

| 属性 | 作用 |
|------|------|
| `justify-content` | 主轴对齐（flex-start/center/space-between/space-around） |
| `align-items` | 交叉轴对齐（stretch/center/flex-start/flex-end） |
| `flex-direction` | 排列方向（row/column） |
| `flex-wrap` | 是否换行（nowrap/wrap） |

### 4. 常见选择器

```css
/* 元素选择器 */
p { color: red; }

/* 类选择器 */
.active { background: blue; }

/* ID 选择器 */
#header { height: 60px; }

/* 后代选择器 */
.nav a { text-decoration: none; }

/* 伪类 */
button:hover { opacity: 0.8; }
input:focus { border-color: #409EFF; }

/* 组合选择器 */
.card, .panel { border-radius: 4px; }
```

## 动手练习

### 练习 1：新闻列表页面

创建一个包含以下结构的 HTML 页面：
- 顶部导航栏（Flex 布局，左右分布）
- 中间内容区（左侧新闻列表，右侧热门推荐）
- 底部页脚

要求：
- 使用语义化标签
- 使用 Flex 布局
- 添加简单的 hover 效果

### 练习 2：登录表单

创建一个居中显示的登录表单：
- 用户名输入框
- 密码输入框
- 登录按钮

要求：
- 使用 `box-sizing: border-box`
- 输入框获取焦点时边框变色
- 按钮 hover 时背景色加深

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| 元素宽度超出容器 | 默认 `content-box` 未将 padding/border 算入宽度 | 设置 `box-sizing: border-box` |
| Flex 子元素未等高 | 默认 `align-items: stretch` 被覆盖 | 检查父元素是否设置了 `align-items` |
| margin 塌陷 | 相邻块级元素垂直 margin 合并 | 使用 padding 替代，或添加 border |

## 本节小结

HTML 提供页面结构，CSS 提供视觉表现。掌握语义化标签、盒模型和 Flex 布局，是前端开发的三大基石。

## 参考文档

- [MDN HTML 基础](https://developer.mozilla.org/zh-CN/docs/Learn/HTML/Introduction_to_HTML)
- [MDN CSS 盒模型](https://developer.mozilla.org/zh-CN/docs/Learn/CSS/Building_blocks/The_box_model)
- [MDN Flexbox 布局](https://developer.mozilla.org/zh-CN/docs/Learn/CSS/CSS_layout/Flexbox)
