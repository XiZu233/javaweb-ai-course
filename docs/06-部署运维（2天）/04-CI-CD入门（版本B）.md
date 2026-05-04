# CI/CD 入门（版本 B 专属）

## 学习目标

- 理解 CI/CD 的概念和价值
- 掌握 GitHub Actions 的基础配置
- 能够配置自动化的构建和测试流水线

## 核心知识点

### 1. 什么是 CI/CD

**CI（Continuous Integration，持续集成）**：
- 代码提交后自动编译、测试
- 尽早发现问题，降低修复成本

**CD（Continuous Deployment，持续部署）**：
- 测试通过后自动部署到服务器
- 缩短交付周期，快速响应需求

### 2. GitHub Actions 简介

GitHub Actions 是 GitHub 提供的自动化工作流平台，通过 YAML 配置文件定义工作流：

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Run tests
        run: mvn test
```

### 3. 核心概念

| 概念 | 说明 |
|------|------|
| Workflow | 工作流，由 YAML 文件定义 |
| Job | 任务，一个工作流可包含多个任务 |
| Step | 步骤，一个任务由多个步骤组成 |
| Action | 可复用的动作，如 actions/checkout |
| Runner | 执行工作流的虚拟机 |

### 4. 版本 B 的 CI 配置

```yaml
name: Tlias Pro CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  backend-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build Backend
        working-directory: ./version-b/tlias-pro-backend
        run: mvn clean package -DskipTests

      - name: Run Backend Tests
        working-directory: ./version-b/tlias-pro-backend
        run: mvn test

  frontend-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: npm
          cache-dependency-path: ./version-b/tlias-pro-frontend/package-lock.json

      - name: Install Dependencies
        working-directory: ./version-b/tlias-pro-frontend
        run: npm ci

      - name: Build Frontend
        working-directory: ./version-b/tlias-pro-frontend
        run: npm run build

  docker-build:
    runs-on: ubuntu-latest
    needs: [backend-build, frontend-build]
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build Docker Images
        working-directory: ./version-b
        run: |
          docker-compose -f docker-compose.yml build
```

### 5. 工作流语法要点

**触发条件**：

```yaml
on:
  push:
    branches: [main, develop]
    paths:
      - 'version-b/**'
  schedule:
    - cron: '0 0 * * 0'   # 每周日执行
```

**多 Job 并行与依赖**：

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps: [/* ... */]

  deploy:
    runs-on: ubuntu-latest
    needs: test    # deploy 等 test 完成后才执行
    steps: [/* ... */]
```

**使用 Secrets**：

```yaml
- name: Deploy to Server
  env:
    API_KEY: ${{ secrets.KIMI_API_KEY }}
  run: |
    echo "Deploying with API key..."
```

### 6. 构建状态徽章

在 README.md 中展示构建状态：

```markdown
![CI](https://github.com/XiZu233/javaweb-ai-course/workflows/Tlias%20Pro%20CI/badge.svg)
```

## 动手练习

### 练习 1：配置 GitHub Actions

1. 在仓库中创建 `.github/workflows/ci.yml`
2. 提交代码触发工作流
3. 在 GitHub 仓库的 Actions 标签页查看执行结果

### 练习 2：添加构建缓存

为 Maven 和 npm 添加缓存配置，减少重复下载依赖的时间。

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| Workflow 未触发 | 分支或路径不匹配 | 检查 on 配置 |
| 依赖下载超时 | 网络问题 | 配置国内镜像或缓存 |
| 步骤执行失败 | 命令错误或环境问题 | 查看详细日志定位 |

## 本节小结

CI/CD 是现代软件工程的标配。GitHub Actions 让开源项目的自动化构建变得免费且简单。配置好 CI 后，每次提交代码都会自动编译和测试，确保代码质量。

## 参考文档

- [GitHub Actions 官方文档](https://docs.github.com/en/actions)
- [GitHub Actions 工作流语法](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)

