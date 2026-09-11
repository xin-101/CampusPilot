# CampusPilot 高校校园学生事务智能体

CampusPilot 面向高校学生打造 AI 智能事务办理系统：学生只需用自然语言告诉 AI「想办什么」，系统自动完成需求理解、政策检索（RAG）、资格判断（规则引擎）、流程编排（Workflow）、工具调用与待办跟踪，实现从「问得到」到「办得到」的全流程服务。

## 核心功能

- **智能政策咨询**：基于 RAG 的校园政策知识库检索，政策版本识别与时效判断，来源引用保障回答准确性
- **事务资格判断**：自动获取学生信息，多维度条件匹配校验，缺失条件识别与申请建议
- **事务办理执行**：自然语言办理请求解析 → Workflow 编排 → 工具调用 → 待办创建与进度跟踪
- **主动校园服务**：截止日期监控、未完成事项主动提醒、事务状态跟踪
- **安全防护**：6 类恶意输入识别（提示词泄露 / 越权伪装 / 注入攻击等），由确定性代码守卫，LLM 无法绕过

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + Vite + TypeScript + Element Plus |
| 后端 | Java 21 + Spring Boot 3.x + MyBatis-Plus |
| 数据库 | MySQL 8 |
| 认证 | JWT |
| Agent | LLM（OpenAI 兼容）+ RAG + Workflow + Tool Calling + 规则引擎 |
| 部署 | Docker / Docker Compose |

> Agent 实现不依赖 Spring AI / LangChain4j 等外部框架，LLM 作为「大脑」负责自然语言理解与生成，权限 / 规则 / 工具 / 工作流 / 安全均由确定性代码执行。

## 目录结构

```
campuspilot/
├── src/main/java/com/campuspilot/
│   ├── agent/           # Agent Provider（Mock/LLM）、LLM 网关、工具、安全守卫
│   ├── workflow/        # 工作流引擎（政策咨询 / 资格判断 / 事务办理）
│   ├── eligibility/     # 资格规则引擎
│   ├── rag/             # RAG 检索（TF-IDF 向量 + 关键词）
│   ├── task/            # 事务办理
│   ├── policy/          # 政策管理
│   ├── notification/    # 通知与主动提醒
│   └── ...
└── src/main/resources/  # application*.yml、db/schema.sql、db/data.sql

campuspilot-web/         # Vue 3 前端（管理台 + 对话聊天面板）
database/init.sql        # 完整 schema + 演示数据（Docker 初始化）
knowledge/               # 校园政策知识库（JSON 文档，RAG 数据源）
docs/                    # 27 篇设计与实现文档 + Phase0-6 自检报告
evaluation/              # 回归测试 / 综合评测 / RAG 评测脚本与结果
scripts/                 # 一键启动 / 冒烟测试 / 评测 / 环境检查
docker-compose.yml       # MySQL + 后端 + 前端 三服务编排
```

## 快速开始

### 方式一：Docker Compose（推荐）

```bash
# 复制环境变量配置
cp .env.example .env

# 启动全部服务（MySQL + 后端 8080 + 前端 80）
docker compose up -d --build

# 查看状态
docker compose ps
```

浏览器访问 `http://localhost`（前端），后端 API 见 `http://localhost:8080`。

### 方式二：本地开发

```bash
# 1. 启动 MySQL
docker compose up -d mysql

# 2. 一键启动后端（本地 mvn，dev 模式，端口 8080）
.\scripts\start-dev.ps1

# 3. 前端开发模式（端口 5173，代理 /api 到 8080）
cd campuspilot-web
npm install
npm run dev
```

### 环境健康检查

```bash
.\scripts\check-dev.ps1
```

## 演示账号

账号为 `2021001`（张三）/ `2021002`（李四）及 `demo_student` / `demo_counselor` / `demo_admin`，密码均为 `password`。

## Agent 模式

通过 `AGENT_PROVIDER` 环境变量切换（默认 `mock`）：

| 模式 | 说明 |
|------|------|
| `mock` | 关键词意图识别 + 确定性工作流（离线演示，无需 API Key） |
| `llm` | 真实大模型驱动（需配置 LLM 参数） |
| `fastgpt` | 对接 FastGPT 平台的适配器 |

### 启用真实大模型

```bash
# 环境变量（仅通过环境变量注入，绝不写入代码或 Git）
LLM_ENABLED=true
LLM_BASE_URL=https://api.openai.com/v1   # 支持 GPT / DeepSeek / Qwen 等 OpenAI 兼容接口
LLM_API_KEY=sk-xxxx
LLM_MODEL=gpt-4o
```

LLM 失败时返回友好错误（「AI 服务暂时不可用」），默认不会静默回退到 Mock；`LLM_FALLBACK_TO_MOCK` 可显式开启回退。

## 测试与评测

```bash
# Demo 冒烟测试（10 项）
.\scripts\run-demo.ps1

# 完整评测：Phase3 回归 + Phase4 综合评测(M1-M6) + RAG 评测
.\scripts\run-evaluation.ps1

# LLM 集成冒烟测试（未配置 API Key 时自动跳过，结果为 NOT_RUN）
node scripts/llm-smoke.mjs
```

当前基准：Phase3 回归 50/50 PASS、Phase4 综合评测 25/25 PASS、RAG 评测 16/16 PASS、Demo 冒烟 10/10 PASS、32-case 全量评测 32/32 PASS。

## 文档

设计与实现文档见 `docs/`，包括：总体技术架构（01-05）、RAG / 规则引擎 / Workflow / Agent 安全实现说明（17-21）、真实大模型接入设计（27）、部署架构（16）、比赛 Demo 脚本（24）、创新点（25）、答辩 Q&A（26）及各阶段自检报告（Phase0-6）。