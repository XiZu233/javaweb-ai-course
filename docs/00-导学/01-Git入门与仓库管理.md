# Git 入门与仓库管理

## 学习目标

- 掌握 Git 常用命令（提交、分支、合并、推送）
- 理解 Git 工作区、暂存区、版本库的概念
- 能够使用 Git 进行团队协作开发

## 核心知识点

### 1. Git 基础概念

Git 是分布式版本控制系统，每个开发者本地都有完整的代码仓库。

**三个区域**：
- **工作区（Working Directory）**：你看到的代码文件
- **暂存区（Stage/Index）**：`git add` 后文件存放的区域
- **版本库（Repository）**：`git commit` 后文件永久保存的区域

### 2. 常用命令

```bash
# 克隆仓库
git clone https://github.com/XiZu233/javaweb-ai-course.git

# 查看状态
git status

# 添加文件到暂存区
git add filename.txt       # 添加单个文件
git add .                  # 添加所有改动

# 提交到版本库
git commit -m "feat: 新增部门管理功能"

# 查看提交历史
git log --oneline

# 推送到远程
git push origin main

# 拉取远程更新
git pull origin main
```

### 3. 分支管理

```bash
# 查看分支
git branch

# 创建并切换分支
git checkout -b feature/login

# 切换分支
git checkout main

# 合并分支
git merge feature/login

# 删除分支
git branch -d feature/login
```

**推荐分支模型**：
- `main`：主分支，始终可部署
- `develop`：开发分支，集成新功能
- `feature/xxx`：功能分支，开发单个功能
- `hotfix/xxx`：热修复分支，紧急修复线上问题

### 4. 提交规范（Conventional Commits）

```
<type>(<scope>): <subject>

feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式（不影响功能）
refactor: 重构
perf: 性能优化
test: 测试相关
chore: 构建/工具相关
```

示例：
```bash
git commit -m "feat(dept): 实现部门增删改查接口"
git commit -m "fix(auth): 修复Token过期未跳转登录页的问题"
git commit -m "docs: 更新API接口文档"
```

### 5. 解决冲突

当多人修改同一文件的同一位置时，会产生冲突：

```bash
# 拉取代码时提示冲突
git pull origin main

# 打开冲突文件，找到冲突标记
<<<<<<< HEAD
你的代码
=======
别人的代码
>>>>>>> branch-name

# 手动编辑文件，保留正确代码，删除冲突标记
# 然后重新提交
git add .
git commit -m "merge: 解决分支合并冲突"
```

### 6. .gitignore 配置

```gitignore
# 编译输出
target/
dist/
build/

# IDE
.idea/
*.iml
.vscode/

# 依赖
node_modules/

# 日志
*.log

# 本地配置
application-local.yml
.env

# 上传文件
uploads/
```

## 动手练习

### 练习 1：Git 基础流程

1. 克隆本仓库到本地
2. 创建一个 `feature/test` 分支
3. 修改一个文件并提交
4. 推送到远程仓库
5. 合并到 main 分支

### 练习 2：模拟冲突解决

1. 在分支 A 中修改 README.md 的第一行
2. 在分支 B 中也修改 README.md 的第一行
3. 合并分支 B 到 A，解决冲突

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| failed to push | 远程有更新本地没有 | 先 git pull，解决冲突后再 push |
| untracked files | 有未跟踪文件 | git add . 后提交，或加入 .gitignore |
| commit 信息写错 | 笔误 | git commit --amend 修改最近一条 |

## 本节小结

Git 是现代软件开发的基础工具。掌握分支管理、提交规范和冲突解决，你就能在团队中高效协作。记住：多提交、勤推送、常拉取。

## 参考文档

- [Git 官方文档](https://git-scm.com/doc)
- [Pro Git 中文](https://git-scm.com/book/zh/v2)

