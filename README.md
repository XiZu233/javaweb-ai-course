# Tlias Training - JavaWeb + AI 实训课程

[![Java](https://img.shields.io/badge/Java-8%2F17-blue)](https://www.oracle.com/java/)
[![SpringBoot](https://img.shields.io/badge/SpringBoot-2.7%2F3.2-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-2.6%2F3.5-4FC08D)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](./LICENSE)

> 一套可自学、可部署的双版本JavaWeb实训课程。版本A面向零基础建立Web开发全局认知，版本B面向进阶学生产出简历级AI全栈项目。

---

## 快速开始

### 方式一：Docker Compose 一键启动（推荐）

```bash
# 版本A - 传统实训版（SpringBoot2 + Vue2 + 原生MyBatis）
cd version-a
docker-compose up -d
# 访问 http://localhost

# 版本B - AI增强实战版（SpringBoot3 + Vue3 + MyBatis-Plus + Redis + AI功能）
cd version-b
docker-compose up -d
# 访问 http://localhost
```

### 方式二：本地开发环境启动

详见 [docs/00-导学/00-课程介绍与环境准备.md](docs/00-导学/00-课程介绍与环境准备.md)

---

## 双版本对比

| 维度 | 版本A：传统实训版 | 版本B：AI增强实战版 |
|------|-----------------|-------------------|
| **目标** | 零基础入门，理解Web开发原理 | 进阶实战，产出简历级项目 |
| **后端** | SpringBoot 2.7 + JDK8 | **SpringBoot 3.2 + JDK17** |
| **前端** | Vue 2.6 + ElementUI | **Vue 3.5 + ElementPlus + TypeScript** |
| **持久层** | 原生MyBatis（XML配置） | **MyBatis-Plus（代码生成）** |
| **缓存** | - | **Redis** |
| **认证** | 手写JWT + Filter | **Sa-Token + JWT + Redis** |
| **AI功能** | - | **NL2SQL + RAG知识库 + AI简历解析** |
| **部署** | Docker Compose | **Docker Compose + CI/CD** |

---

## 课程目录

| 章节 | 内容 | 天数 |
|------|------|------|
| [00-导学](docs/00-导学/) | 环境准备、Git入门、Docker基础 | - |
| [01-前端基础](docs/01-前端基础（2天）/) | HTML/CSS/JS、Vue快速入门、Ajax | 2天 |
| [02-后端基础](docs/02-后端基础（4天）/) | Maven、SpringBoot、HTTP、MySQL、MyBatis | 4天 |
| [03-后端实战](docs/03-后端实战（6天）/) | 部门/员工CRUD、分页、文件上传、AOP日志 | 6天 |
| [04-后端进阶](docs/04-后端进阶（2天）/) | JWT认证、Filter/Sa-Token、AOP、自动配置原理 | 2天 |
| [05-前端实战](docs/05-前端实战（4天）/) | Vue工程化、ElementUI/Plus、页面联调 | 4天 |
| [06-部署运维](docs/06-部署运维（2天）/) | Linux、Docker、Docker Compose、CI/CD | 2天 |
| [07-AI功能专题](docs/07-AI功能专题（版本B专属，3天）/) | NL2SQL、RAG、简历解析、Prompt工程 | 3天 |

---

## 项目结构

```
tlias-training/
├── docs/                    # 课程文档（Markdown，可自学）
├── version-a/               # 传统实训版代码
│   ├── tlias-backend/       # SpringBoot2后端
│   ├── tlias-frontend/      # Vue2前端
│   └── docker-compose.yml   # 一键启动
├── version-b/               # AI增强实战版代码
│   ├── tlias-pro-backend/   # SpringBoot3后端（基于yudao-boot-mini改造）
│   ├── tlias-pro-frontend/  # Vue3前端
│   └── docker-compose.yml   # 一键启动（含MySQL+Redis+后端+前端）
└── shared/                  # 共享资源（Docker配置、文档配图）
```

---

## 技术栈

### 版本A
- **后端**：SpringBoot 2.7, MyBatis, MySQL 8, JWT, PageHelper, 阿里云OSS
- **前端**：Vue 2.6, ElementUI, Vue Router, Vuex, Axios
- **运维**：Docker, Docker Compose, Nginx

### 版本B
- **后端**：SpringBoot 3.2, MyBatis-Plus, MySQL 8, Redis, Sa-Token, Knife4j, Spring AI
- **前端**：Vue 3.5, ElementPlus, TypeScript, Pinia, Vite, ECharts
- **AI**：Kimi API / 通义千问, NL2SQL, RAG, 简历解析
- **运维**：Docker Compose, GitHub Actions

---

## 贡献

欢迎提交Issue和PR。课程文档采用Markdown编写，代码遵循阿里巴巴Java开发手册规范。

---

## 许可

[MIT License](./LICENSE)
