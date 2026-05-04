# Git 入门与仓库管理

## 学习目标

- 掌握 Git 基本操作：clone、add、commit、push
- 理解分支管理和远程仓库协作
- 能够使用 GitHub 托管代码

## 核心知识点

### 1. Git 是什么

Git 是目前最流行的分布式版本控制系统。它记录了代码的每一次变更，让你可以：
- 随时回滚到历史版本
- 多人协作不冲突
- 追溯每一行代码是谁改的

### 2. 安装与配置

```bash
# 安装 Git（官网 https://git-scm.com/）
# 配置用户信息
git config --global user.name "你的名字"
git config --global user.email "your@email.com"
```

### 3. 常用命令速查

```bash
# 克隆仓库
git clone https://github.com/your-org/tlias-training.git

# 查看状态
git status

# 添加文件到暂存区
git add .

# 提交更改
git commit -m "feat: 新增部门管理功能"

# 推送到远程
git push origin main

# 拉取最新代码
git pull origin main

# 查看提交历史
git log --oneline
```

### 4. 分支管理

```bash
# 创建并切换分支
git checkout -b feature/dept-module

# 切换分支
git checkout main

# 合并分支
git merge feature/dept-module

# 删除本地分支
git branch -d feature/dept-module
```

### 5. Git 提交规范（Conventional Commits）

```
feat: 新增功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 重构代码
test: 添加测试
chore: 构建/工具调整
```

## 动手练习

1. 配置好 Git 用户信息
2. 克隆本课程仓库到本地
3. 创建一个新分支 `practice/day1`
4. 修改任意文件后提交，推送分支到远程
5. 查看提交历史

## 本节小结

Git 是程序员的必备技能。从今天开始，你的每一次代码提交都在 GitHub 贡献图上留下绿色记录——这本身就是能力的可视化证明。
