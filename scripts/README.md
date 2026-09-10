# CampusPilot 开发运维脚本

提供比赛演示 / 开发自检需要的常用脚本（Windows PowerShell + Node）。

| 脚本 | 作用 |
|------|------|
| `check-dev.ps1` | 检查环境健康：MySQL 容器/端口、后端 8080、前端构建产物 |
| `start-dev.ps1` | 一键启动开发环境（MySQL + 后端） |
| `run-demo.ps1` | 运行 5 个比赛 Demo 场景的自动化冒烟 |
| `run-evaluation.ps1` | 运行 Phase 3 回归 + Phase 4 综合评测(M1-M6) + RAG 评测 |
| `demo-smoke.mjs` | Demo 冒烟脚本本体（也可直接 `node scripts/demo-smoke.mjs`） |

## 快速开始

```powershell
# 1. 准备 MySQL（Docker 或已有实例均可）并确保 3306 可连
docker compose up -d mysql

# 2. 启动后端（本地 mvn，日志写入 backend/startup.log）
.\scripts\start-dev.ps1

# 3. 健康检查
.\scripts\check-dev.ps1

# 4. 演示冒烟
.\scripts\run-demo.ps1

# 5. 完整评测（Phase3 / Phase4 / RAG）
.\scripts\run-evaluation.ps1
```

## 说明

- 演示账号：`demo_student` / `demo_counselor` / `demo_admin` / `2021001`（张三）/ `2021002`（李四），密码均为 `password`（见 `database/migrations/V002_demo_accounts.sql`）。
- 所有评测为真实 HTTP 调用，结果写入 `evaluation/results/`。
- 前端开发模式：`cd frontend && npm run dev`（默认 5173，代理 `/api` 到 8080）。