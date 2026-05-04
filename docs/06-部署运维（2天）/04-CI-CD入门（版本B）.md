# CI/CD 入门（版本 B 专属）

---

## 学习目标

- 理解 CI/CD 的核心概念、价值和工作原理，明白为什么现代软件开发离不开自动化流水线
- 掌握 GitHub Actions 的核心概念：Workflow、Job、Step、Action、Runner
- 能够独立编写 `.github/workflows/xxx.yml` 工作流配置文件
- 掌握多种触发条件：push、pull_request、schedule、workflow_dispatch
- 能够配置 Java 项目的自动化构建（Maven 编译、测试、打包）
- 能够配置 Docker 镜像的自动构建并推送到镜像仓库
- 能够配置自动部署到远程服务器（SSH + scp/rsync）
- 能够搭建一条从代码提交到生产部署的完整 CI/CD 流水线

---

## 核心知识点

### 1. 什么是 CI/CD？

#### 1.1 CI（Continuous Integration，持续集成）

**是什么**：持续集成是一种开发实践，要求团队成员频繁地将代码合并到主干分支，每次合并都通过自动化的构建和测试来验证。

**为什么需要它**：

**真实场景类比——合唱团的排练**：

> 想象一个合唱团，每个歌手在家练习自己的声部（开发功能）。如果直到演出前一天才第一次合练（手动合并代码），结果可能是灾难性的——有人唱错了调、有人进错了拍。持续集成就像"每天固定时间合练"：每个歌手每天把自己的部分带到排练厅，指挥（自动化工具）立刻检查是否和谐，有问题当天就纠正，而不是等到演出前夜才发现。

**CI 的核心价值**：

| 价值 | 说明 |
|------|------|
| 尽早发现问题 | 每次提交都自动测试，问题在引入当天就被发现 |
| 减少集成痛苦 | 频繁小步合并，避免"合并地狱"（长时间分支导致大量冲突） |
| 自动化重复工作 | 编译、测试、代码检查全部自动化，开发者专注写代码 |
| 提供快速反馈 | 提交后几分钟内就知道代码是否通过测试 |

#### 1.2 CD（Continuous Deployment/Delivery，持续部署/交付）

**是什么**：持续部署是在持续集成的基础上，将通过测试的代码自动部署到生产环境。持续交付则是自动部署到预生产环境，由人工决定何时上生产。

**为什么需要它**：

> 还是合唱团 analogy：CI 是"每天合练确保和谐"，CD 是"合练通过后自动把演出视频发布到网上"。不需要每次演出都手动剪辑、上传、写文案——全部自动化。

**CI/CD 流水线全景图**：

```
+--------------------------------------------------------------------------+
|                           CI/CD 流水线全景图                               |
+--------------------------------------------------------------------------+
|                                                                          |
|  开发者提交代码                                                           |
|       |                                                                  |
|       v                                                                  |
|  +----------------+     +----------------+     +----------------+       |
|  |   代码推送      | --> |   触发构建      | --> |   编译打包      |       |
|  |   git push     |     |   GitHub       |     |   Maven Build  |       |
|  +----------------+     |   Actions      |     +----------------+       |
|                         +----------------+            |                  |
|                                                       v                  |
|  +----------------+     +----------------+     +----------------+       |
|  |   部署上线      | <-- |   构建镜像      | <-- |   运行测试      |       |
|  |   Deploy       |     |   Docker Build |     |   Unit Test    |       |
|  |   to Server    |     |   & Push       |     |   Integration  |       |
|  +----------------+     +----------------+     +----------------+       |
|                                                                          |
|  整个过程自动化，从代码提交到上线只需几分钟                                    |
+--------------------------------------------------------------------------+
```

---

### 2. GitHub Actions 简介

#### 2.1 是什么

GitHub Actions 是 GitHub 提供的免费自动化工作流平台。你只需要在仓库中创建一个 YAML 文件，GitHub 就会在云端虚拟机上自动执行你定义的任务。

#### 2.2 为什么需要它

**真实场景类比——智能管家**：

> 你请了一个 24 小时不休息的智能管家（GitHub Actions）。每次你往家里放新东西（push 代码），管家立刻：检查东西是否完好（运行测试）、拍照记录（构建产物）、把东西放到该放的位置（部署）。你完全不用操心这些琐事，只管把东西带回家。

**GitHub Actions 的优势**：

| 优势 | 说明 |
|------|------|
| 免费 | 公共仓库完全免费，私有仓库有免费额度 |
| 与 GitHub 深度集成 | 代码仓库就是触发源，无需额外配置 Webhook |
| 生态丰富 | GitHub Marketplace 有数千个现成的 Action 可用 |
| 多平台支持 | 支持 Linux、Windows、macOS 运行环境 |
| 矩阵构建 | 可以同时在多个环境（如不同 JDK 版本）下测试 |

#### 2.3 核心概念

```
+----------------------------------------------------------+
|              GitHub Actions 核心概念层级                   |
+----------------------------------------------------------+
|                                                          |
|  Workflow（工作流）                                       |
|  ├── 定义：.github/workflows/xxx.yml 文件                  |
|  ├── 作用：描述完整的自动化流程                            |
|  └── 类比：一份完整的"今日待办清单"                        |
|                                                          |
|  └── Job（任务）                                          |
|      ├── 定义：workflow 中的 jobs 下的每个条目             |
|      ├── 作用：一组相关的步骤，在同一台 Runner 上执行       |
|      ├── 特性：多个 Job 默认并行执行，可用 needs 控制依赖   |
|      └── 类比：清单中的"买菜"这一项任务                     |
|                                                          |
|      └── Step（步骤）                                     |
|          ├── 定义：job 中的 steps 下的每个条目             |
|          ├── 作用：一个具体的操作命令                       |
|          ├── 类型：run（执行命令）或 uses（使用 Action）   |
|          └── 类比：买菜的步骤"1. 去蔬菜区 2. 挑西红柿"      |
|                                                          |
|              Action（动作）                               |
|              ├── 定义：可复用的自动化单元                   |
|              ├── 来源：GitHub Marketplace 或自建           |
|              ├── 使用：uses: actions/checkout@v4          |
|              └── 类比：超市的"预制菜"，拿来就能用           |
|                                                          |
|              Runner（运行器）                             |
|              ├── 定义：执行工作流的虚拟机                   |
|              ├── 类型：GitHub 托管（免费）或自托管          |
|              ├── 规格：ubuntu-latest、windows-latest 等   |
|              └── 类比：执行任务的"工人"                     |
|                                                          |
+----------------------------------------------------------+
```

| 概念 | 英文 | 说明 | 类比 |
|------|------|------|------|
| 工作流 | Workflow | 由 YAML 文件定义的完整自动化流程 | 一份完整的食谱 |
| 任务 | Job | 一组在同一 Runner 上执行的步骤 | 食谱中的一道菜 |
| 步骤 | Step | 一个具体的操作 | 炒菜的一个动作 |
| 动作 | Action | 可复用的自动化单元 | 超市的预制调料包 |
| 运行器 | Runner | 执行工作流的虚拟机 | 厨房里的厨师 |

---

### 3. 第一个 GitHub Actions 工作流

#### 3.1 工作流文件位置

```
你的仓库/
├── .github/
│   └── workflows/              # 工作流配置文件必须放在这里
│       ├── ci.yml              # CI 工作流（可以有多個 .yml 文件）
│       └── deploy.yml          # 部署工作流
├── src/
├── pom.xml
└── README.md
```

#### 3.2 基础工作流示例

```yaml
# .github/workflows/ci.yml
# 这个文件定义了一个持续集成工作流

name: CI                       # 工作流名称，显示在 GitHub Actions 页面

on:                            # 触发条件：什么时候执行这个工作流
  push:                        # 代码推送时触发
    branches: [main, develop]  # 只在 main 和 develop 分支推送时触发
  pull_request:                # 创建或更新 Pull Request 时触发
    branches: [main]           # 目标分支是 main 的 PR

jobs:                          # 定义任务
  build:                       # 任务名称：build
    name: Build and Test       # 任务的显示名称
    runs-on: ubuntu-latest     # 在最新的 Ubuntu 虚拟机上运行
    # 可选值：ubuntu-latest, windows-latest, macos-latest

    steps:                     # 定义步骤（按顺序执行）
      # --------------------------------------------------------
      # Step 1: 检出代码
      # --------------------------------------------------------
      - name: Checkout Code    # 步骤名称
        uses: actions/checkout@v4
        # uses: 使用 GitHub 官方提供的 Action
        # actions/checkout@v4: 将仓库代码检出到 Runner 上
        # @v4 表示使用第 4 个主要版本

      # --------------------------------------------------------
      # Step 2: 设置 JDK 17
      # --------------------------------------------------------
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:                  # 传递给 Action 的参数
          java-version: '17'   # JDK 版本
          distribution: 'temurin'   # JDK 发行版（Eclipse Temurin， formerly AdoptOpenJDK）
          cache: maven         # 启用 Maven 依赖缓存，加速后续构建

      # --------------------------------------------------------
      # Step 3: 使用 Maven 构建项目
      # --------------------------------------------------------
      - name: Build with Maven
        run: mvn clean package -DskipTests
        # run: 直接执行 Shell 命令
        # mvn clean: 清理之前的构建产物
        # package: 编译、测试、打包
        # -DskipTests: 跳过测试（加快构建，测试在下一步单独执行）

      # --------------------------------------------------------
      # Step 4: 运行测试
      # --------------------------------------------------------
      - name: Run tests
        run: mvn test
        # mvn test: 执行所有单元测试
        # 如果有测试失败，工作流会标记为失败
```

#### 3.3 触发条件详解

```yaml
on:
  # 1. 代码推送触发
  push:
    branches: [main, develop]         # 只在指定分支推送时触发
    paths:                            # 只在指定文件变化时触发
      - 'version-b/**'                # version-b 目录下的文件变化
      - '!version-b/**/*.md'          # ! 表示排除，忽略 markdown 文件变化

  # 2. Pull Request 触发
  pull_request:
    branches: [main]
    types: [opened, synchronize, reopened]   # PR 打开、更新、重新打开时触发

  # 3. 定时触发（Cron 表达式）
  schedule:
    - cron: '0 0 * * 0'              # 每周日 00:00 UTC 执行
    # cron 格式：分 时 日 月 星期
    # '0 2 * * *'    = 每天凌晨 2 点
    # '0 */6 * * *'  = 每 6 小时
    # '0 0 1 * *'    = 每月 1 号

  # 4. 手动触发
  workflow_dispatch:
    # 在 GitHub 仓库的 Actions 页面点击按钮手动触发
    inputs:                          # 可以定义输入参数
      environment:
        description: '部署环境'
        required: true
        default: 'staging'
        type: choice
        options:
          - staging
          - production

  # 5. 其他仓库事件触发
  release:
    types: [published]               # 发布 Release 时触发
```

---

### 4. 版本 B 的完整 CI 配置

#### 4.1 多 Job 并行构建

```yaml
# .github/workflows/tlias-pro-ci.yml
name: Tlias Pro CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  # ============================================================
  # Job 1: 后端构建
  # ============================================================
  backend-build:
    name: Backend Build & Test
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
          # cache: maven 会自动缓存 ~/.m2/repository
          # 下次构建时复用依赖，大幅加速

      - name: Build Backend
        working-directory: ./version-b/tlias-pro-backend
        # working-directory: 设置后续命令的工作目录
        run: mvn clean package -DskipTests

      - name: Run Backend Tests
        working-directory: ./version-b/tlias-pro-backend
        run: mvn test

      - name: Upload Build Artifact
        uses: actions/upload-artifact@v4
        # 将构建产物上传，供后续 Job 使用
        with:
          name: backend-jar           # 产物名称
          path: version-b/tlias-pro-backend/target/*.jar   # 产物路径
          retention-days: 7           # 保留 7 天

  # ============================================================
  # Job 2: 前端构建
  # ============================================================
  frontend-build:
    name: Frontend Build
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'         # Node.js 版本
          cache: npm                 # 启用 npm 缓存
          cache-dependency-path: ./version-b/tlias-pro-frontend/package-lock.json
          # 指定 package-lock.json 路径，用于精确缓存

      - name: Install Dependencies
        working-directory: ./version-b/tlias-pro-frontend
        run: npm ci
        # npm ci: 严格按 package-lock.json 安装，比 npm install 更快更稳定

      - name: Build Frontend
        working-directory: ./version-b/tlias-pro-frontend
        run: npm run build
        # 生成 dist 目录，包含打包后的静态文件

      - name: Upload Build Artifact
        uses: actions/upload-artifact@v4
        with:
          name: frontend-dist
          path: version-b/tlias-pro-frontend/dist
          retention-days: 7

  # ============================================================
  # Job 3: Docker 镜像构建
  # 依赖：backend-build 和 frontend-build 都成功后执行
  # ============================================================
  docker-build:
    name: Build Docker Images
    runs-on: ubuntu-latest
    needs: [backend-build, frontend-build]
    # needs: 声明依赖，等 backend-build 和 frontend-build 都成功后才执行
    # 如果任一前置 Job 失败，docker-build 会被跳过

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Download Backend Artifact
        uses: actions/download-artifact@v4
        with:
          name: backend-jar
          path: version-b/tlias-pro-backend/target/

      - name: Download Frontend Artifact
        uses: actions/download-artifact@v4
        with:
          name: frontend-dist
          path: version-b/tlias-pro-frontend/dist

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
        # Buildx 是 Docker 的高级构建工具，支持多平台构建和缓存

      - name: Build Docker Images
        working-directory: ./version-b
        run: |
          docker-compose -f docker-compose.yml build
          # -f 指定 compose 文件路径
          # build: 构建所有定义了 build 的服务的镜像
```

#### 4.2 Job 依赖关系图

```
+--------------------------------------------------------------------------+
|                        CI 工作流 Job 依赖图                               |
+--------------------------------------------------------------------------+
|                                                                          |
|   +------------------+        +------------------+                       |
|   | backend-build    |        | frontend-build   |                       |
|   | (后端编译+测试)   |        | (前端打包)        |                       |
|   | runs-on: ubuntu  |        | runs-on: ubuntu  |                       |
|   +--------+---------+        +--------+---------+                       |
|            |                           |                                 |
|            |         并行执行           |                                 |
|            |                           |                                 |
|            v                           v                                 |
|   +--------+---------+        +--------+---------+                       |
|   |    成功 ✅        |        |    成功 ✅        |                       |
|   +--------+---------+        +--------+---------+                       |
|            |                           |                                 |
|            +------------+--------------+                                 |
|                         |                                                |
|                         v  needs: [backend-build, frontend-build]       |
|              +----------+-----------+                                    |
|              | docker-build         |                                    |
|              | (构建 Docker 镜像)    |                                    |
|              +----------+-----------+                                    |
|                         |                                                |
|                         v                                                |
|              +----------+-----------+                                    |
|              |    deploy (可选)      |                                    |
|              | (部署到服务器)         |                                    |
|              +----------------------+                                    |
|                                                                          |
+--------------------------------------------------------------------------+
```

---

### 5. 构建 Docker 镜像并推送

#### 5.1 推送到 Docker Hub

```yaml
jobs:
  docker-push:
    name: Build and Push Docker Images
    runs-on: ubuntu-latest
    needs: [backend-build, frontend-build]

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Download Artifacts
        uses: actions/download-artifact@v4
        with:
          path: artifacts
          # 不指定 name，下载所有 artifact

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      # --------------------------------------------------------
      # 登录 Docker Hub
      # --------------------------------------------------------
      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
          # secrets: GitHub 仓库的加密环境变量
          # 在仓库 Settings -> Secrets and variables -> Actions 中设置

      # --------------------------------------------------------
      # 构建并推送后端镜像
      # --------------------------------------------------------
      - name: Build and Push Backend Image
        uses: docker/build-push-action@v5
        with:
          context: ./version-b/tlias-pro-server
          # 构建上下文目录
          push: true
          # push: true 表示构建后推送到仓库
          tags: |
            ${{ secrets.DOCKER_USERNAME }}/tlias-backend:latest
            ${{ secrets.DOCKER_USERNAME }}/tlias-backend:${{ github.sha }}
          # tags: 镜像标签
          # github.sha: 当前提交的完整 SHA，作为版本标识
          cache-from: type=gha
          cache-to: type=gha,mode=max
          # 使用 GitHub Actions 缓存加速构建

      # --------------------------------------------------------
      # 构建并推送前端镜像
      # --------------------------------------------------------
      - name: Build and Push Frontend Image
        uses: docker/build-push-action@v5
        with:
          context: ./version-b/tlias-pro-frontend
          push: true
          tags: |
            ${{ secrets.DOCKER_USERNAME }}/tlias-frontend:latest
            ${{ secrets.DOCKER_USERNAME }}/tlias-frontend:${{ github.sha }}
```

#### 5.2 推送到阿里云容器镜像服务（ACR）

```yaml
      - name: Login to Alibaba Cloud ACR
        uses: docker/login-action@v3
        with:
          registry: registry.cn-hangzhou.aliyuncs.com
          username: ${{ secrets.ACR_USERNAME }}
          password: ${{ secrets.ACR_PASSWORD }}

      - name: Build and Push to ACR
        uses: docker/build-push-action@v5
        with:
          context: ./version-b/tlias-pro-server
          push: true
          tags: |
            registry.cn-hangzhou.aliyuncs.com/${{ secrets.ACR_NAMESPACE }}/tlias-backend:latest
```

---

### 6. 自动部署到服务器

#### 6.1 使用 SSH 部署

```yaml
jobs:
  deploy:
    name: Deploy to Server
    runs-on: ubuntu-latest
    needs: docker-push
    # 只在 main 分支推送时部署
    if: github.ref == 'refs/heads/main'
    # if: 条件判断，满足条件才执行

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      # --------------------------------------------------------
      # 配置 SSH 密钥
      # --------------------------------------------------------
      - name: Setup SSH
        run: |
          # 创建 SSH 目录
          mkdir -p ~/.ssh
          # 写入私钥（从 secrets 读取）
          echo "${{ secrets.SSH_PRIVATE_KEY }}" > ~/.ssh/id_rsa
          # 设置私钥权限（SSH 要求私钥权限为 600）
          chmod 600 ~/.ssh/id_rsa
          # 添加服务器到 known_hosts，避免首次连接询问
          ssh-keyscan -H ${{ secrets.SERVER_HOST }} >> ~/.ssh/known_hosts

      # --------------------------------------------------------
      # 通过 SSH 在远程服务器执行部署命令
      # --------------------------------------------------------
      - name: Deploy via SSH
        env:
          SERVER_HOST: ${{ secrets.SERVER_HOST }}
          SERVER_USER: ${{ secrets.SERVER_USER }}
        run: |
          ssh $SERVER_USER@$SERVER_HOST << 'EOF'
            # 进入项目目录
            cd /opt/tlias-pro
            
            # 拉取最新代码
            git pull origin main
            
            # 拉取最新镜像
            docker-compose pull
            
            # 重启服务
            docker-compose down
            docker-compose up -d
            
            # 清理旧镜像
            docker image prune -f
            
            echo "Deployment completed!"
          EOF
          # << 'EOF' 表示 Here Document，将多行命令通过 SSH 发送到远程执行
          # 注意：'EOF' 用单引号包裹，防止本地变量扩展
```

#### 6.2 使用 scp 传输文件 + SSH 执行

```yaml
      - name: Copy files to server
        env:
          SERVER_HOST: ${{ secrets.SERVER_HOST }}
          SERVER_USER: ${{ secrets.SERVER_USER }}
        run: |
          # 将 docker-compose.yml 和配置文件上传到服务器
          scp -r ./version-b/docker-compose.yml \
                   ./version-b/nginx.conf \
                   ./version-b/.env \
                   $SERVER_USER@$SERVER_HOST:/opt/tlias-pro/
          # scp -r: 递归复制文件
          # 语法：scp 本地文件 用户名@主机:远程路径

      - name: Restart services on server
        env:
          SERVER_HOST: ${{ secrets.SERVER_HOST }}
          SERVER_USER: ${{ secrets.SERVER_USER }}
        run: |
          ssh $SERVER_USER@$SERVER_HOST "cd /opt/tlias-pro && docker-compose up -d --build"
```

#### 6.3 使用 appleboy/ssh-action（更简洁的 SSH 部署）

```yaml
      - name: Deploy to Server
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          port: 22
          script: |
            cd /opt/tlias-pro
            docker-compose down
            docker-compose pull
            docker-compose up -d
            docker image prune -f
```

---

### 7. 工作流语法进阶

#### 7.1 使用 Secrets 管理敏感信息

```yaml
# 在 GitHub 仓库中设置 Secrets：
# Settings -> Secrets and variables -> Actions -> New repository secret

env:                           # 工作流级别的环境变量
  GLOBAL_VAR: "hello"

jobs:
  build:
    runs-on: ubuntu-latest
    env:                       # Job 级别的环境变量
      JOB_VAR: "world"

    steps:
      - name: Use secrets
        env:                   # Step 级别的环境变量
          API_KEY: ${{ secrets.KIMI_API_KEY }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
        run: |
          echo "API Key is set: ${API_KEY:+yes}"
          # ${API_KEY:+yes}: 如果 API_KEY 有值则输出 yes，否则空
          # 这样可以在日志中确认变量已设置，同时不泄露值

      - name: Conditional step
        if: env.API_KEY != ''
        # if: 条件执行，只有 API_KEY 不为空时才执行
        run: echo "API_KEY is configured"
```

#### 7.2 矩阵构建（多环境测试）

```yaml
  test:
    name: Test on JDK ${{ matrix.java-version }}
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java-version: ['17', '21']
        os: [ubuntu-latest, windows-latest]
        # 矩阵组合：2 个 JDK 版本 × 2 个操作系统 = 4 个并行 Job

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK ${{ matrix.java-version }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java-version }}
          distribution: 'temurin'

      - name: Run tests
        run: mvn test
```

#### 7.3 构建状态徽章

在 README.md 中展示 CI 构建状态：

```markdown
# Tlias Pro

![CI](https://github.com/你的用户名/你的仓库名/workflows/Tlias%20Pro%20CI/badge.svg)
![Docker Build](https://github.com/你的用户名/你的仓库名/workflows/Docker%20Build/badge.svg)
```

徽章会根据最近一次的构建结果显示：
- 绿色 ✅：构建通过
- 红色 ❌：构建失败
- 黄色：构建中

---

### 8. 完整的 CI/CD 流水线示例

```yaml
# .github/workflows/full-pipeline.yml
name: Full CI/CD Pipeline

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  # ============================================================
  # 阶段 1：代码质量检查
  # ============================================================
  code-quality:
    name: Code Quality Check
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      - name: Check code formatting
        working-directory: ./version-b/tlias-pro-backend
        run: mvn spotless:check
        # spotless: 代码格式化检查工具

  # ============================================================
  # 阶段 2：后端构建与测试
  # ============================================================
  backend:
    name: Backend Build
    runs-on: ubuntu-latest
    needs: code-quality
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      - name: Build and Test
        working-directory: ./version-b/tlias-pro-backend
        run: mvn clean verify
        # verify: 包含编译、测试、集成测试、代码检查
      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./version-b/tlias-pro-backend/target/site/jacoco/jacoco.xml
          # 上传测试覆盖率报告到 Codecov

  # ============================================================
  # 阶段 3：前端构建
  # ============================================================
  frontend:
    name: Frontend Build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: npm
          cache-dependency-path: ./version-b/tlias-pro-frontend/package-lock.json
      - name: Install and Build
        working-directory: ./version-b/tlias-pro-frontend
        run: |
          npm ci
          npm run build
      - name: Lint Check
        working-directory: ./version-b/tlias-pro-frontend
        run: npm run lint
        # lint: 代码规范检查

  # ============================================================
  # 阶段 4：构建并推送 Docker 镜像
  # ============================================================
  docker:
    name: Docker Build & Push
    runs-on: ubuntu-latest
    needs: [backend, frontend]
    # 只在 main 分支推送时推送镜像
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
      - name: Build and Push Backend
        uses: docker/build-push-action@v5
        with:
          context: ./version-b/tlias-pro-server
          push: true
          tags: |
            ${{ secrets.DOCKER_USERNAME }}/tlias-backend:latest
            ${{ secrets.DOCKER_USERNAME }}/tlias-backend:${{ github.sha }}
      - name: Build and Push Frontend
        uses: docker/build-push-action@v5
        with:
          context: ./version-b/tlias-pro-frontend
          push: true
          tags: |
            ${{ secrets.DOCKER_USERNAME }}/tlias-frontend:latest
            ${{ secrets.DOCKER_USERNAME }}/tlias-frontend:${{ github.sha }}

  # ============================================================
  # 阶段 5：部署到生产服务器
  # ============================================================
  deploy:
    name: Deploy to Production
    runs-on: ubuntu-latest
    needs: docker
    # 严格限制：只有 main 分支的 push 事件才部署
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    steps:
      - name: Deploy
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            cd /opt/tlias-pro
            docker-compose down
            docker-compose pull
            docker-compose up -d
            docker image prune -f
            echo "Deployment completed at $(date)"
```

---

## 动手练习

### 练习 1：配置第一个 GitHub Actions 工作流

```bash
# 步骤 1：在本地仓库创建工作流目录
mkdir -p .github/workflows

# 步骤 2：创建工作流文件
cat > .github/workflows/ci.yml << 'EOF'
name: First CI

on:
  push:
    branches: [main]

jobs:
  hello:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      - name: Say Hello
        run: echo "Hello, GitHub Actions!"
      - name: Show OS Info
        run: |
          echo "OS: $(uname -a)"
          echo "Current directory: $(pwd)"
          echo "Files: $(ls -la)"
EOF

# 步骤 3：提交并推送
git add .github/workflows/ci.yml
git commit -m "ci: add first GitHub Actions workflow"
git push origin main

# 步骤 4：在 GitHub 仓库页面查看
# 打开 https://github.com/你的用户名/你的仓库/actions
# 应该能看到工作流正在运行
```

### 练习 2：配置 Java 项目的 CI 流水线

```bash
# 步骤 1：创建工作流文件
cat > .github/workflows/java-ci.yml << 'EOF'
name: Java CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      - name: Build with Maven
        run: mvn clean package -DskipTests
      - name: Run Tests
        run: mvn test
EOF

# 步骤 2：提交并推送
git add .github/workflows/java-ci.yml
git commit -m "ci: add Java CI workflow"
git push

# 步骤 3：验证
# 在 GitHub Actions 页面确认工作流触发并执行成功
```

### 练习 3：配置 Secrets 并测试部署

```bash
# 步骤 1：生成本地 SSH 密钥对（如果还没有）
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/github_actions
# -t ed25519: 密钥类型（比 rsa 更安全）
# -C: 注释
# -f: 输出文件路径

# 步骤 2：将公钥添加到服务器的 authorized_keys
cat ~/.ssh/github_actions.pub | ssh user@server "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"

# 步骤 3：在 GitHub 仓库设置 Secrets
# 进入仓库页面 -> Settings -> Secrets and variables -> Actions -> New repository secret
# 添加以下 Secrets：
#   SSH_PRIVATE_KEY: cat ~/.ssh/github_actions 的内容
#   SERVER_HOST: 你的服务器 IP 或域名
#   SERVER_USER: 登录用户名

# 步骤 4：创建部署工作流
cat > .github/workflows/deploy.yml << 'EOF'
name: Deploy

on:
  workflow_dispatch:   # 手动触发

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Deploy to Server
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            echo "Deployment triggered from GitHub Actions"
            echo "Current time: $(date)"
EOF

# 步骤 5：提交并手动触发测试
git add .github/workflows/deploy.yml
git commit -m "ci: add deploy workflow"
git push

# 步骤 6：在 GitHub 仓库的 Actions 页面，找到 Deploy 工作流，点击 Run workflow
```

---

## 常见错误排查

### 安装环境问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 安装环境问题 | GitHub Actions 页面没有显示工作流 | 文件路径或扩展名错误 | 确认文件放在 `.github/workflows/` 目录下，扩展名为 `.yml` 或 `.yaml` |
| 安装环境问题 | `Error: Unable to resolve action` | Action 名称或版本号错误 | 检查 `uses: 用户名/仓库名@版本` 的拼写；到 GitHub Marketplace 确认正确版本号 |

### 命令执行问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 命令执行问题 | Workflow 未触发 | 分支或路径不匹配 | 检查 `on.push.branches` 配置；确认代码推送到了正确的分支 |
| 命令执行问题 | `mvn: command not found` | Maven 未安装或不在 PATH | 确保先使用 `actions/setup-java` 并设置 `cache: maven`；或显式安装 Maven |
| 命令执行问题 | `npm ci` 失败 | package-lock.json 与 package.json 不同步 | 本地执行 `npm install` 更新 package-lock.json，提交后再触发 |
| 命令执行问题 | `docker: command not found` | Runner 没有 Docker | 使用 `ubuntu-latest` 自带 Docker；或安装 Docker |

### 配置问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 配置问题 | `Error: Input required and not supplied: java-version` | `with` 参数缺少必填项 | 检查 Action 文档，确保所有必填参数都已提供 |
| 配置问题 | Secrets 在日志中显示为空 | Secret 名称拼写错误或未设置 | 在仓库 Settings -> Secrets 中确认 Secret 已设置；检查拼写（区分大小写） |
| 配置问题 | SSH 连接失败 | 密钥格式错误、权限问题或服务器未配置公钥 | 确认私钥完整（包含 BEGIN/END 行）；确认服务器 `~/.ssh/authorized_keys` 包含对应公钥；确认服务器 SSH 服务运行中 |
| 配置问题 | 部署步骤执行了但代码未更新 | 工作目录错误或 git pull 失败 | 在 SSH script 中添加 `pwd && ls -la` 确认目录；添加 `git status` 查看状态 |

### 网络问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 网络问题 | Maven 依赖下载超时 | GitHub Runner 连接 Maven 中央仓库慢 | 在 pom.xml 中配置国内镜像（阿里云、华为云）；或使用 `cache: maven` 加速 |
| 网络问题 | npm 依赖安装慢 | 网络问题 | 配置 npm 国内镜像：`npm config set registry https://registry.npmmirror.com` |
| 网络问题 | Docker 镜像推送失败 | 登录凭据错误或网络问题 | 确认 `docker/login-action` 配置正确；确认仓库地址可访问；检查 Secrets 是否正确设置 |
| 网络问题 | 部署时服务器连接超时 | 服务器防火墙阻挡或 SSH 端口未开放 | 确认服务器安全组/防火墙允许 22 端口；确认服务器 IP 和端口正确 |

---

## 本节小结

```
+----------------------------------------------------------+
|                   CI/CD 思维导图                           |
+----------------------------------------------------------+
|                                                          |
|   +----------------+    +----------------+               |
|   |   CI 持续集成   |    |   CD 持续部署   |               |
|   |   自动构建      |    |   自动发布      |               |
|   |   自动测试      |    |   自动上线      |               |
|   +----------------+    +----------------+               |
|            |                      |                      |
|            v                      v                      |
|   +----------------+    +----------------+               |
|   |  GitHub Actions |    |  完整流水线     |               |
|   |  Workflow      |    |  代码提交       |               |
|   |  Job / Step    |    |    ↓           |               |
|   |  Action        |    |  编译测试       |               |
|   |  Runner        |    |    ↓           |               |
|   +----------------+    |  构建镜像       |               |
|                         |    ↓           |               |
|   触发条件：              |  推送仓库       |               |
|   push / pull_request   |    ↓           |               |
|   schedule / manual     |  部署服务器     |               |
|                         +----------------+               |
|                                                          |
|   核心 Secrets：                                          |
|   SSH_PRIVATE_KEY / SERVER_HOST / SERVER_USER            |
|   DOCKER_USERNAME / DOCKER_PASSWORD                      |
|                                                          |
|   黄金法则：                                              |
|   1. 敏感信息必须用 Secrets，绝不硬编码                    |
|   2. 部署 Job 加 if 条件，防止 PR 触发部署                 |
|   3. 使用 cache 加速依赖下载                              |
|   4. 使用 needs 控制 Job 执行顺序                         |
|   5. 部署前确保测试通过                                   |
|                                                          |
+----------------------------------------------------------+
```

---

## 参考文档

- [GitHub Actions 官方文档](https://docs.github.com/en/actions)
- [GitHub Actions 工作流语法参考](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)
- [GitHub Marketplace - Actions](https://github.com/marketplace?type=actions)
- [GitHub Actions 缓存文档](https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows)
- [Docker Build Push Action](https://github.com/docker/build-push-action)
- [appleboy/ssh-action - SSH 部署](https://github.com/appleboy/ssh-action)
- [GitHub Actions 中文指南](https://docs.github.com/cn/actions)
