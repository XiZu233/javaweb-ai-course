# Git 入门与团队协作

> Git 是现代软件开发的"时光机"和"协作桥梁"。
> 学完本节，你将掌握代码版本管理的核心技能，能够自信地与团队成员协作开发。
> 即使你是第一次接触版本控制，也不用担心——我们会从最基础的概念开始，一步一步带你入门。

---

## 一、学习目标

完成本节学习后，你将能够：

- **理解 Git 的核心工作原理**：清晰区分工作区、暂存区、本地仓库、远程仓库四个区域，知道代码在它们之间如何流转。
- **熟练使用 15+ 个常用 Git 命令**：包括 init、clone、add、commit、push、pull、status、log、diff、branch、merge、checkout 等。
- **掌握分支管理策略**：能够创建、切换、合并分支，理解 main / develop / feature 分支模型的使用场景。
- **独立解决代码冲突**：当多人修改同一文件时，能够定位冲突标记、判断保留哪部分代码、完成合并提交。
- **编写规范的 .gitignore 文件**：知道哪些文件应该被 Git 忽略，避免将敏感信息或临时文件提交到仓库。
- **完成完整的团队协作流程**：从克隆仓库、创建功能分支、开发提交、推送到远程、发起 Pull Request 的全流程。

---

## 二、核心知识点

### 2.1 为什么需要 Git？——版本管理的痛苦与解决

**没有版本管理的日子（真实场景）**：

想象你和同学合作写一个课程设计报告：

```
第 1 天：你写了报告_v1.docx，发给同学
第 2 天：同学修改后变成 报告_v2_小明改.docx，发回给你
第 3 天：你又改了内容，变成 报告_v3_最终版.docx
第 4 天：老师提意见，变成 报告_v4_最终最终版.docx
第 5 天：同学又改了，变成 报告_v5_最终最终版_真的.docx
...
第 N 天：桌面有 20 个版本的报告，你完全不知道哪个才是"真的最终版"
```

**写代码比写文档更痛苦**：
- 代码文件更多（一个项目几百个文件）
- 修改更频繁（一天可能改几十次）
- 需要多人同时修改（你写前端，他写后端）
- 改错了需要回退（"刚才还好好的，怎么突然报错了？"）
- 需要知道"谁改了什么、什么时候改的、为什么改"

**Git 如何解决这些问题**：

| 痛点 | 没有 Git 时 | 有了 Git 后 |
|------|-----------|-----------|
| 版本混乱 | 手动复制文件，命名 v1/v2/v3 | 每次提交自动保存快照，随时回退到任意版本 |
| 多人协作 | 用 U 盘/微信传文件，互相覆盖 | 各开发各的分支，最后合并，不会丢失任何人的工作 |
| 改错了想恢复 | 拼命按 Ctrl+Z，希望还能撤销 | `git checkout` 或 `git reset` 一键回退 |
| 想知道谁改了什么 | 翻聊天记录，问同事 | `git log` 和 `git blame` 精确到每一行代码的修改记录 |
| 代码审查 | 站同事身后看屏幕 | Pull Request 线上审查，逐行评论 |

**一句话总结**：Git 让你的代码有了"历史记录"，让你的团队有了"协作规范"。

### 2.2 Git 核心概念：四个区域的流转

Git 管理代码的核心是理解四个"区域"，想象成一个工厂的流水线：

```
+---------------------------------------------------------------+
|                    Git 代码流转示意图                            |
+---------------------------------------------------------------+
|                                                               |
|   +----------------+                                          |
|   |   工作区        |  <-- 你实际编辑的文件                     |
|   | (Working Dir)  |       用编辑器打开、修改、保存的文件       |
|   +--------+-------+                                          |
|            |                                                  |
|            | git add                                          |
|            v                                                  |
|   +----------------+                                          |
|   |   暂存区        |  <-- 准备提交的"购物车"                  |
|   |   (Stage)      |       你可以分批挑选要提交的文件           |
|   +--------+-------+                                          |
|            |                                                  |
|            | git commit                                       |
|            v                                                  |
|   +----------------+                                          |
|   |   本地仓库      |  <-- 你电脑上的完整历史                   |
|   |  (Repository)  |       包含所有提交记录，可以离线查看       |
|   +--------+-------+                                          |
|            |                                                  |
|            | git push                                         |
|            v                                                  |
|   +----------------+                                          |
|   |   远程仓库      |  <-- 团队共享的中央仓库                   |
|   |   (Remote)     |       GitHub / Gitee / GitLab 等         |
|   +----------------+                                          |
|                                                               |
+---------------------------------------------------------------+
```

**四个区域详解**：

| 区域 | 英文 | 类比 | 说明 |
|------|------|------|------|
| 工作区 | Working Directory | 你的办公桌 | 你正在编辑的文件，可以任意修改 |
| 暂存区 | Stage / Index | 购物篮 | 你已经挑选好、准备结账（提交）的文件 |
| 本地仓库 | Local Repository | 你的私人档案柜 | `git commit` 后，代码快照永久保存在这里 |
| 远程仓库 | Remote Repository | 团队共享的文件柜 | `git push` 后，团队成员都能看到你的提交 |

**关键理解**：
- `git add` 只是把文件从"办公桌"放到"购物篮"，还没真正保存。
- `git commit` 才是真正的"存档"，生成一个永久的历史记录点。
- `git push` 是把你的"存档"同步到"团队共享文件柜"。
- 在 `git commit` 之前，所有修改都可以撤销；`git commit` 之后也可以回退，但会留下记录。

### 2.3 Git 安装与配置

#### 步骤 1：下载并安装 Git

- Windows 用户：访问 https://git-scm.com/download/win，下载安装包，一路"Next"即可。
- Mac 用户：打开终端，执行 `brew install git`（需先安装 Homebrew）。
- Linux 用户：`sudo apt-get install git`（Ubuntu/Debian）或 `sudo yum install git`（CentOS）。

**Windows 安装注意事项**：
- 选择编辑器时，如果你不熟悉 Vim，建议选择"Use Visual Studio Code as Git's default editor"。
- 调整 PATH 环境时，选择"Git from the command line and also from 3rd-party software"（推荐）。

#### 步骤 2：验证安装

```bash
# 打开终端（Windows 右键桌面选"Git Bash Here"，Mac/Linux 直接打开终端）
# 执行以下命令验证 Git 是否安装成功

git --version
# 预期输出类似：git version 2.42.0.windows.2
# 如果显示版本号，说明安装成功
```

#### 步骤 3：配置用户信息

Git 需要知道"你是谁"，这样每次提交都会记录作者信息。

```bash
# 配置全局用户名（替换为你的真实姓名或昵称）
# 这个信息会出现在每次提交记录中，团队成员能看到
git config --global user.name "张三"

# 配置全局邮箱（替换为你的真实邮箱）
# 建议与 GitHub/Gitee 注册邮箱一致
git config --global user.email "zhangsan@example.com"

# 验证配置是否成功
git config --global user.name
# 预期输出：张三

git config --global user.email
# 预期输出：zhangsan@example.com

# 查看所有 Git 配置
git config --global --list
# 预期看到 user.name 和 user.email 的配置项
```

#### 步骤 4：配置默认分支名（可选但推荐）

```bash
# 设置新建仓库的默认分支名为 main（符合现代规范）
# 旧版 Git 默认是 master，新版推荐 main
git config --global init.defaultBranch main
```

#### 步骤 5：配置命令别名（提高效率）

```bash
# 给常用命令设置短别名，以后可以少敲几个字母
git config --global alias.st status      # git st 代替 git status
git config --global alias.co checkout    # git co 代替 git checkout
git config --global alias.br branch      # git br 代替 git branch
git config --global alias.ci commit      # git ci 代替 git commit
git config --global alias.lg "log --oneline --graph --decorate"  # 漂亮的日志视图

# 测试别名是否生效
git st
# 应该显示当前目录的 git 状态
```

### 2.4 常用 Git 命令详解

#### 2.4.1 仓库初始化与克隆

```bash
# ========== 场景 1：从零创建新仓库 ==========

# 步骤 1：创建项目目录
mkdir my-first-project
cd my-first-project

# 步骤 2：初始化 Git 仓库
# 这会在当前目录下创建一个隐藏的 .git 文件夹，里面存放版本历史
git init
# 预期输出：Initialized empty Git repository in ...

# 步骤 3：创建第一个文件
echo "# My First Project" > README.md

# 步骤 4：添加到暂存区
git add README.md

# 步骤 5：提交到本地仓库
# -m 后面是提交信息，描述这次提交做了什么
git commit -m "init: 初始化项目，添加 README"


# ========== 场景 2：克隆已有仓库 ==========

# 当你要参与一个已有项目时，用 clone 命令把远程仓库复制到本地
# 语法：git clone <仓库地址> [<本地目录名>]

# 克隆本课程仓库
git clone https://github.com/XiZu233/javaweb-ai-course.git

# 克隆到指定目录名（如果不指定，默认用仓库名）
git clone https://github.com/XiZu233/javaweb-ai-course.git my-course

# 克隆后进入目录
cd javaweb-ai-course

# 查看远程仓库地址
git remote -v
# 预期输出：origin  https://github.com/XiZu233/javaweb-ai-course.git (fetch)
#          origin  https://github.com/XiZu233/javaweb-ai-course.git (push)
```

#### 2.4.2 日常开发命令（最常用）

```bash
# ========== git status：查看当前状态 ==========
# 这是你最常使用的命令，相当于"看看现在什么情况"

git status
# 可能的输出 1：工作区干净（没有未提交的修改）
# On branch main
# nothing to commit, working tree clean

# 可能的输出 2：有未跟踪的新文件（红色）
# Untracked files:
#   (use "git add <file>..." to include in what will be committed)
#         newfile.txt

# 可能的输出 3：有已修改但未暂存的文件（红色）
# Changes not staged for commit:
#   modified:   existing.txt

# 可能的输出 4：有已暂存等待提交的文件（绿色）
# Changes to be committed:
#   modified:   existing.txt


# ========== git add：添加到暂存区 ==========

# 添加单个文件到暂存区
git add filename.txt

# 添加当前目录下所有修改过的文件（最常用）
git add .

# 添加某个目录下的所有文件
git add src/

# 交互式添加（可以选择性地添加文件的部分修改）
git add -p


# ========== git commit：提交到本地仓库 ==========

# 基本提交（必须带 -m 提交信息）
git commit -m "feat: 新增用户登录功能"

# 提交并自动添加所有已跟踪文件的修改（跳过 git add 步骤）
# 注意：不会添加新文件（Untracked files），新文件仍需先 git add
git commit -am "fix: 修复登录页面样式问题"

# 修改最近一次的提交信息（仅在未 push 到远程时使用）
git commit --amend -m "feat: 新增用户登录功能（修正提交信息）"


# ========== git push：推送到远程仓库 ==========

# 推送到默认远程仓库（origin）的当前分支
git push

# 首次推送新分支到远程（建立关联后以后可以直接 git push）
git push -u origin feature/login

# 强制推送（危险操作！会覆盖远程历史，团队项目禁止使用）
git push --force


# ========== git pull：拉取远程更新 ==========

# 拉取远程当前分支的最新代码并合并到本地
git pull

# 等价于先 fetch 再 merge
git fetch origin
git merge origin/main


# ========== git log：查看提交历史 ==========

# 简洁查看（每行一个提交）
git log --oneline
# 输出示例：
# a1b2c3d feat: 新增部门管理功能
# e4f5g6h fix: 修复分页查询 bug
# i7j8k9l docs: 更新 API 文档

# 图形化查看分支合并历史（推荐）
git log --oneline --graph --decorate
# 输出示例：
# * a1b2c3d (HEAD -> main, origin/main) feat: 新增部门管理
# * e4f5g6h fix: 修复分页查询
# | * i7j8k9l (feature/login) feat: 新增登录页面
# |/
# * d0e1f2g init: 初始化项目

# 查看某个文件的修改历史
git log -p filename.txt

# 查看最近 5 条提交
git log --oneline -5


# ========== git diff：查看差异 ==========

# 查看工作区 vs 暂存区的差异（未 add 的修改）
git diff

# 查看暂存区 vs 本地仓库的差异（已 add 未 commit 的修改）
git diff --staged
# 旧版 Git 用 git diff --cached，效果相同

# 查看工作区 vs 本地仓库的差异（所有未提交的修改）
git diff HEAD

# 查看某两个提交之间的差异
git diff a1b2c3d..e4f5g6h
```

#### 2.4.3 分支管理

分支是 Git 最强大的功能之一，它让你可以"平行宇宙"式地开发：

```bash
# ========== 查看分支 ==========

# 查看本地所有分支
git branch
# 输出示例：
# * main          <-- 带 * 号的是当前所在分支
#   develop
#   feature/login

# 查看所有分支（包括远程分支）
git branch -a

# 查看分支最后提交信息
git branch -v


# ========== 创建与切换分支 ==========

# 方式 1：先创建，再切换（两条命令）
git branch feature/user-management    # 创建分支
git checkout feature/user-management  # 切换到该分支

# 方式 2：创建并立即切换（一条命令，推荐）
git checkout -b feature/user-management

# Git 2.23+ 新语法（更直观）
git switch -c feature/user-management  # -c = create

# 切换到已有分支
git checkout main
# 或
git switch main


# ========== 合并分支 ==========

# 场景：功能开发完成，将 feature 分支合并到 main

# 步骤 1：切换到目标分支（要合并"到"哪个分支）
git checkout main

# 步骤 2：执行合并
git merge feature/user-management

# 如果合并顺利，会显示：
# Updating a1b2c3d..e4f5g6h
# Fast-forward
#  ...（文件列表）

# 如果产生冲突，会显示：
# Auto-merging src/xxx.java
# CONFLICT (content): Merge conflict in src/xxx.java
# Automatic merge failed; fix conflicts and then commit the result.


# ========== 删除分支 ==========

# 删除已合并的分支（安全）
git branch -d feature/user-management

# 强制删除未合并的分支（会丢失该分支上的修改，慎用）
git branch -D feature/experiment

# 删除远程分支
git push origin --delete feature/user-management
```

### 2.5 分支管理策略（Git Flow 简化版）

在团队协作中，我们需要一套分支命名和使用规范：

```
+---------------------------------------------------------------+
|                    推荐分支模型                                |
+---------------------------------------------------------------+
|                                                               |
|   main 分支（生产环境）                                        |
|   |                                                           |
|   |-- 永远保持稳定，随时可部署                                  |
|   |-- 只能通过合并 develop 或 hotfix 分支来更新                 |
|   |                                                           |
|   +-- develop 分支（开发环境）                                 |
|   |   |                                                       |
|   |   |-- 日常开发的主分支                                     |
|   |   |-- 新功能从这里切出 feature 分支                        |
|   |   |                                                       |
|   |   +-- feature/login 分支（功能分支）                       |
|   |   |   |-- 开发登录功能                                     |
|   |   |   |-- 开发完成后合并回 develop                         |
|   |   |   |-- 合并后删除该 feature 分支                        |
|   |   |                                                       |
|   |   +-- feature/dept-crud 分支（另一个功能分支）              |
|   |       |-- 开发部门 CRUD                                    |
|   |       |-- 完成后合并回 develop                             |
|   |                                                           |
|   +-- hotfix/auth-bug 分支（热修复分支）                        |
|       |-- 从 main 直接切出                                     |
|       |-- 修复线上紧急 bug                                     |
|       |-- 修复后同时合并回 main 和 develop                      |
|                                                               |
+---------------------------------------------------------------+
```

**分支命名规范**：

| 分支类型 | 命名格式 | 示例 | 说明 |
|---------|---------|------|------|
| 主分支 | `main` | `main` | 生产环境代码，永远稳定 |
| 开发分支 | `develop` | `develop` | 日常开发集成 |
| 功能分支 | `feature/功能名` | `feature/user-login` | 开发单个功能 |
| 修复分支 | `fix/bug描述` | `fix/login-timeout` | 修复非紧急 bug |
| 热修复分支 | `hotfix/bug描述` | `hotfix/payment-error` | 修复线上紧急 bug |

### 2.6 冲突解决

冲突是协作开发中不可避免的情况。当两个人修改了同一文件的同一位置，Git 无法自动判断保留哪个，就需要人工介入。

**冲突产生场景**：

```
开发者 A                          开发者 B
    |                                |
    v                                v
修改了 UserService.java 第 20 行    也修改了 UserService.java 第 20 行
    |                                |
    v                                v
git add .                         git add .
git commit -m "A的修改"            git commit -m "B的修改"
git push origin main               git push origin main
    |                                |
    |                                v
    |                           报错：! [rejected] main -> main (fetch first)
    |                                |
    v                                v
                           git pull origin main
                           # 此时产生冲突！
```

**解决冲突步骤**：

```bash
# 步骤 1：当 push 失败时，先拉取远程最新代码
git pull origin main
# 此时终端会提示：
# Auto-merging src/main/java/com/tlias/service/UserService.java
# CONFLICT (content): Merge conflict in .../UserService.java
# Automatic merge failed; fix conflicts and then commit the result.

# 步骤 2：查看冲突文件列表
git status
# 冲突文件会显示在 "Unmerged paths" 区域

# 步骤 3：打开冲突文件，找到冲突标记
```

冲突文件内容示例：

```java
public class UserService {

    public void login() {
<<<<<<< HEAD
        // 这是你的代码（当前分支的修改）
        System.out.println("A开发者写的登录逻辑");
=======
        // 这是别人的代码（从远程拉下来的修改）
        System.out.println("B开发者写的登录逻辑");
>>>>>>> branch-name
    }
}
```

**冲突标记说明**：

```
<<<<<<< HEAD
    这部分是你的代码（当前分支的修改）
=======
    这部分是别人的代码（从远程合并进来的修改）
>>>>>>> branch-name
```

```bash
# 步骤 4：手动编辑文件，保留正确的代码，删除冲突标记

# 编辑后的文件应该像这样（删除了 <<<、=======、>>>> 标记）：
# public class UserService {
#     public void login() {
#         System.out.println("合并后的登录逻辑");
#     }
# }

# 步骤 5：保存文件后，重新添加到暂存区
git add src/main/java/com/tlias/service/UserService.java

# 步骤 6：提交合并结果
git commit -m "merge: 解决 UserService 合并冲突"

# 步骤 7：推送到远程
git push origin main
```

**减少冲突的建议**：
1. 开始工作前先 `git pull` 拉取最新代码
2. 功能粒度要小，频繁提交、频繁推送
3. 不要在一个文件中写太多代码，合理拆分
4. 与团队成员沟通，避免同时修改同一文件

### 2.7 .gitignore 编写

`.gitignore` 文件告诉 Git 哪些文件不应该被跟踪。每个项目都应该有一个精心配置的 .gitignore。

**为什么需要 .gitignore**：

| 文件类型 | 示例 | 为什么忽略 |
|---------|------|-----------|
| 编译输出 | `target/`、`dist/`、`build/` | 自动生成的，不需要版本控制 |
| IDE 配置 | `.idea/`、`.vscode/`、`*.iml` | 每个人的 IDE 配置不同 |
| 依赖目录 | `node_modules/` | 体积巨大，可通过 package.json 重建 |
| 日志文件 | `*.log`、`logs/` | 运行时生成，不需要保存 |
| 本地配置 | `application-local.yml`、`.env` | 包含个人敏感信息 |
| 上传文件 | `uploads/`、`temp/` | 用户上传的内容，不应入仓库 |
| 操作系统文件 | `.DS_Store`、`Thumbs.db` | 系统自动生成 |

**Java 项目的 .gitignore 模板**：

```gitignore
# ============================================
# Java / Maven 项目 .gitignore
# ============================================

# 编译输出目录
target/
*.jar
*.war
*.ear
*.class

# IDE 配置文件（IntelliJ IDEA）
.idea/
*.iml
*.iws
*.ipr
out/

# IDE 配置文件（Eclipse）
.classpath
.project
.settings/
bin/

# IDE 配置文件（VS Code）
.vscode/

# Maven 相关
.mvn/
!/.mvn/wrapper/maven-wrapper.jar

# 日志文件
*.log
logs/

# 本地配置文件（包含敏感信息）
application-local.yml
application-dev.yml
application-prod.yml
.env

# 上传文件目录
uploads/
static/upload/

# 测试相关
test-output/

# 操作系统文件
.DS_Store
Thumbs.db
desktop.ini
```

**前端项目的 .gitignore 模板**：

```gitignore
# ============================================
# Node.js / Vue 项目 .gitignore
# ============================================

# 依赖目录
node_modules/

# 构建输出
dist/
build/

# 本地环境文件
.env.local
.env.*.local

# 日志
npm-debug.log*
yarn-debug.log*
yarn-error.log*
pnpm-debug.log*

# 编辑器配置
.idea/
.vscode/
*.suo
*.ntvs*
*.njsproj
*.sln
*.sw?

# 测试覆盖率报告
coverage/

# 操作系统文件
.DS_Store
Thumbs.db
```

**验证 .gitignore 是否生效**：

```bash
# 查看哪些文件被忽略了
git status --ignored

# 如果某个文件已经被 Git 跟踪，再添加到 .gitignore 不会生效
# 需要先停止跟踪：
git rm --cached filename.txt

# 如果 .gitignore 本身不生效，检查是否有拼写错误
# 特别注意：.gitignore 文件本身必须被 Git 跟踪
git add .gitignore
git commit -m "chore: 添加 .gitignore 配置"
```

### 2.8 远程仓库操作（GitHub / Gitee）

#### 2.8.1 配置 SSH 密钥（免密推送）

每次 push 都输入密码很麻烦，配置 SSH 密钥后可以实现免密操作。

```bash
# 步骤 1：生成 SSH 密钥对
# -t 指定算法类型，-C 是注释（通常用邮箱）
ssh-keygen -t ed25519 -C "zhangsan@example.com"

# 如果系统不支持 ed25519，使用 rsa：
# ssh-keygen -t rsa -b 4096 -C "zhangsan@example.com"

# 执行后会提示保存位置，直接回车使用默认路径
# 然后提示输入密码（passphrase），可以直接回车（不设置密码）

# 步骤 2：启动 SSH 代理
# Windows（Git Bash）：
eval "$(ssh-agent -s)"
# Mac：
eval "$(ssh-agent -s)"

# 步骤 3：添加私钥到代理
ssh-add ~/.ssh/id_ed25519

# 步骤 4：复制公钥内容
# Windows：
cat ~/.ssh/id_ed25519.pub | clip
# Mac：
pbcopy < ~/.ssh/id_ed25519.pub
# Linux：
cat ~/.ssh/id_ed25519.pub
# 然后手动复制输出的内容

# 步骤 5：将公钥添加到 GitHub / Gitee
# GitHub：Settings → SSH and GPG keys → New SSH key → 粘贴公钥
# Gitee：设置 → SSH 公钥 → 添加公钥 → 粘贴公钥

# 步骤 6：测试连接
ssh -T git@github.com
# 预期输出：Hi username! You've successfully authenticated...
```

#### 2.8.2 远程仓库常用操作

```bash
# 查看远程仓库地址
git remote -v

# 添加远程仓库（给本地仓库关联远程地址）
# 先创建好 GitHub/Gitee 上的空仓库，然后执行：
git remote add origin https://github.com/yourname/your-repo.git
# 或使用 SSH 地址（推荐）：
git remote add origin git@github.com:yourname/your-repo.git

# 修改远程仓库地址
git remote set-url origin https://new-url.git

# 删除远程仓库关联
git remote remove origin

# 抓取远程分支信息（不合并）
git fetch origin

# 查看远程分支列表
git branch -r

# 拉取远程分支到本地
git checkout -b feature/login origin/feature/login
```

### 2.9 团队协作流程（Fork / PR / Code Review）

在开源项目或规范化的团队中，通常采用"Fork + Pull Request"的工作流程：

```
+---------------------------------------------------------------+
|                  Fork / PR 协作流程                            |
+---------------------------------------------------------------+
|                                                               |
|  步骤 1：Fork 仓库                                             |
|  +-- 在 GitHub 上点击项目页面的 "Fork" 按钮                    |
|  +-- 这会在你的账号下创建一个项目的副本                         |
|                                                               |
|  步骤 2：克隆你的 Fork                                         |
|  +-- git clone https://github.com/你的账号/项目名.git           |
|                                                               |
|  步骤 3：添加上游仓库                                          |
|  +-- git remote add upstream https://github.com/原项目/项目.git |
|  +-- 这样你可以同步原项目的更新                                 |
|                                                               |
|  步骤 4：创建功能分支                                          |
|  +-- git checkout -b feature/你的功能                          |
|                                                               |
|  步骤 5：开发并提交                                            |
|  +-- 写代码 → git add . → git commit -m "..."                 |
|                                                               |
|  步骤 6：推送到你的 Fork                                       |
|  +-- git push origin feature/你的功能                          |
|                                                               |
|  步骤 7：发起 Pull Request                                     |
|  +-- 在 GitHub 上点击 "Compare & pull request"                 |
|  +-- 填写 PR 标题和描述，说明做了什么、为什么做                  |
|                                                               |
|  步骤 8：Code Review                                           |
|  +-- 团队成员审查你的代码，提出修改意见                         |
|  +-- 你根据意见修改，push 后 PR 自动更新                        |
|                                                               |
|  步骤 9：合并                                                  |
|  +-- 审查通过后，维护者合并你的 PR 到主分支                     |
|                                                               |
+---------------------------------------------------------------+
```

**Pull Request 最佳实践**：

1. **PR 标题要清晰**：`feat: 新增部门管理 CRUD 接口` 比 `更新代码` 好一百倍
2. **PR 描述要详细**：说明做了什么、为什么做、如何测试
3. **一个 PR 只做一件事**：不要把登录功能和文件上传混在一起
4. **保持 PR 小巧**：代码量控制在 500 行以内，方便审查
5. **及时响应 Review 意见**：收到评论后尽快修改或回复

---

## 三、动手练习

### 练习 1：Git 基础流程实战

按照以下步骤完成一次完整的 Git 工作流程：

```bash
# 步骤 1：创建一个测试目录
mkdir git-practice
cd git-practice

# 步骤 2：初始化仓库
git init

# 步骤 3：配置本地用户信息（仅用于本次练习）
git config user.name "练习用户"
git config user.email "practice@example.com"

# 步骤 4：创建第一个文件并提交
echo "# Git 练习项目" > README.md
git add README.md
git commit -m "init: 初始化项目"

# 步骤 5：创建并切换到功能分支
git checkout -b feature/add-greeting

# 步骤 6：在分支上开发
echo "Hello, Git!" > greeting.txt
git add greeting.txt
git commit -m "feat: 添加问候语文件"

# 步骤 7：查看提交历史
git log --oneline
# 预期看到两条提交记录

# 步骤 8：切换回 main 分支
git checkout main

# 步骤 9：合并功能分支
git merge feature/add-greeting

# 步骤 10：删除已合并的功能分支
git branch -d feature/add-greeting

# 步骤 11：查看最终状态
git log --oneline --graph
git branch
```

**完成标准**：
- `git log --oneline` 显示两条提交记录
- `git branch` 只显示 main 分支
- `cat greeting.txt` 能看到 "Hello, Git!"

### 练习 2：模拟冲突解决

```bash
# 步骤 1：在 main 分支上创建基础文件
git checkout main
echo "原始内容" > conflict.txt
git add conflict.txt
git commit -m "init: 添加冲突测试文件"

# 步骤 2：创建分支 A，修改文件第一行
git checkout -b branch-A
# 用文本编辑器将 conflict.txt 改为 "分支A修改的内容"
# Windows 用 notepad conflict.txt，Mac/Linux 用 vi 或 nano
echo "分支A修改的内容" > conflict.txt
git add conflict.txt
git commit -m "branch-A: 修改 conflict.txt"

# 步骤 3：切换回 main，创建分支 B，也修改同一行
git checkout main
git checkout -b branch-B
echo "分支B修改的内容" > conflict.txt
git add conflict.txt
git commit -m "branch-B: 修改 conflict.txt"

# 步骤 4：将 branch-B 合并到 main（应该成功，因为 main 没有新修改）
git checkout main
git merge branch-B

# 步骤 5：再将 branch-A 合并到 main（这次会产生冲突！）
git merge branch-A
# 预期看到 CONFLICT 提示

# 步骤 6：查看冲突文件
cat conflict.txt
# 预期看到 <<<<<<<、=======、>>>>>>> 标记

# 步骤 7：手动解决冲突（保留你想要的版本）
echo "合并后的最终内容" > conflict.txt

# 步骤 8：标记冲突已解决并提交
git add conflict.txt
git commit -m "merge: 解决 branch-A 和 branch-B 的冲突"

# 步骤 9：查看合并后的历史
git log --oneline --graph
```

**完成标准**：
- 成功模拟并解决一次合并冲突
- `git log --oneline --graph` 显示合并节点
- `cat conflict.txt` 显示"合并后的最终内容"

### 练习 3：编写 .gitignore 并验证

```bash
# 步骤 1：创建测试目录
mkdir gitignore-practice
cd gitignore-practice
git init

# 步骤 2：创建 .gitignore 文件
# 用文本编辑器创建 .gitignore，写入以下内容：
cat > .gitignore << 'EOF'
# 忽略日志文件
*.log

# 忽略临时目录
temp/

# 忽略本地配置
config.local.json
EOF

# 步骤 3：创建各类文件测试
echo "正常文件" > readme.txt
echo "日志内容" > app.log
echo "临时文件" > temp/temp.txt
echo "本地配置" > config.local.json

# 创建 temp 目录
mkdir temp

# 步骤 4：查看 Git 状态
git status
# 预期：readme.txt 和 .gitignore 是未跟踪文件
# 预期：app.log、temp/、config.local.json 不显示（被忽略了）

# 步骤 5：提交 .gitignore 和正常文件
git add .gitignore readme.txt
git commit -m "init: 添加 .gitignore 和项目文件"

# 步骤 6：验证忽略是否生效
git status --ignored
# 预期：被忽略的文件会显示在列表中
```

**完成标准**：
- `git status` 不显示被忽略的文件
- `git status --ignored` 能正确列出被忽略的文件

---

## 四、常见错误排查

| 阶段 | 错误现象 | 可能原因 | 解决方案 |
|------|---------|---------|---------|
| **配置** | `git config` 后仍提示 "Please tell me who you are" | 没有配置 user.name 和 user.email | 执行 `git config --global user.name "你的名字"` 和 `git config --global user.email "你的邮箱"` |
| **提交** | `nothing to commit, working tree clean` | 工作区没有修改，或修改未保存 | 确认文件已保存；执行 `git status` 查看哪些文件有变化 |
| **提交** | `Changes not staged for commit` | 修改了文件但没有 `git add` | 执行 `git add .` 或 `git add 文件名` 后再 commit |
| **推送** | `failed to push some refs to ...` | 远程仓库有更新，本地没有 | 先执行 `git pull origin main`，解决冲突后再 push |
| **推送** | `Permission denied` | 没有权限推送，或 SSH 密钥未配置 | 检查是否是仓库成员；检查 SSH 密钥是否正确添加到 GitHub/Gitee |
| **克隆** | `Repository not found` | 仓库地址错误，或没有访问权限 | 检查 URL 是否拼写正确；确认你有该仓库的访问权限 |
| **合并** | `CONFLICT` 冲突标记 | 多人修改了同一文件的同一位置 | 按 2.6 节的步骤手动解决冲突 |
| **分支** | `error: pathspec 'xxx' did not match any file(s) known to git` | 分支名拼写错误，或分支不存在 | 用 `git branch -a` 查看所有分支，确认名称正确 |
| **忽略** | `.gitignore` 不生效 | 文件已经被 Git 跟踪过 | 先执行 `git rm --cached 文件名` 停止跟踪，再提交 .gitignore |
| **忽略** | `.gitignore` 本身被忽略了 | 在 .gitignore 中写了 `.gitignore` | 从 .gitignore 中删除 `.gitignore` 这一行，确保 .gitignore 文件被跟踪 |

---

## 五、本节小结

```
+---------------------------------------------------------------+
|                      Git 知识图谱                               |
+---------------------------------------------------------------+
|                                                               |
|   +----------------+                                          |
|   |   为什么用 Git  |                                          |
|   |  版本管理 + 协作 |                                         |
|   +--------+-------+                                          |
|            |                                                  |
|            v                                                  |
|   +----------------+                                          |
|   |   四个区域      |                                          |
|   |  工作区→暂存区  |                                          |
|   |  →本地→远程    |                                          |
|   +--------+-------+                                          |
|            |                                                  |
|            v                                                  |
|   +----------------+     +----------------+                  |
|   |   常用命令      |<--->|  add/commit/   |                  |
|   |  15+ 个核心命令 |     |  push/pull/    |                  |
|   |                |     |  branch/merge  |                  |
|   +--------+-------+     +----------------+                  |
|            |                                                  |
|            v                                                  |
|   +----------------+                                          |
|   |   分支策略      |                                          |
|   |  main/develop  |                                          |
|   |  /feature/fix  |                                          |
|   +--------+-------+                                          |
|            |                                                  |
|            v                                                  |
|   +----------------+     +----------------+                  |
|   |   冲突解决      |<--->|  <<<<<<</=======/   |                  |
|   |  定位→判断→    |     |  >>>>>>> 标记   |                  |
|   |  编辑→提交     |     |  保留正确代码   |                  |
|   +--------+-------+     +----------------+                  |
|            |                                                  |
|            v                                                  |
|   +----------------+                                          |
|   |   团队协作      |                                          |
|   |  Fork→PR→     |                                          |
|   |  Code Review   |                                          |
|   +----------------+                                          |
|                                                               |
+---------------------------------------------------------------+
```

**核心要点回顾**：

1. Git 是分布式版本控制系统，每个开发者本地都有完整的代码历史。
2. 代码在四个区域流转：工作区 → 暂存区（add）→ 本地仓库（commit）→ 远程仓库（push）。
3. 日常开发最常用命令：`git status`、`git add .`、`git commit -m "..."`、`git push`、`git pull`。
4. 分支让多人协作成为可能，推荐模型：main（生产）、develop（开发）、feature/xxx（功能）。
5. 冲突不可怕，找到 `<<<<<<<` 标记，判断保留哪部分，删除标记后重新提交即可。
6. .gitignore 很重要，避免将敏感信息和临时文件提交到仓库。
7. 团队协作使用 Fork + Pull Request 流程，配合 Code Review 保证代码质量。

---

## 六、参考文档

- [Git 官方文档](https://git-scm.com/doc)
- [Pro Git 中文版](https://git-scm.com/book/zh/v2)（最权威的 Git 学习资料，免费）
- [GitHub Git 备忘单](https://education.github.com/git-cheat-sheet-education.pdf)
- [Git 可视化学习工具](https://learngitbranching.js.org/?locale=zh_CN)（交互式学习分支操作）
- [Conventional Commits 规范](https://www.conventionalcommits.org/zh-hans/v1.0.0/)（提交信息规范）
- [GitHub Flow 工作流](https://docs.github.com/zh/get-started/quickstart/github-flow)
- [Gitee 帮助中心](https://gitee.com/help)
