# CampusPilot 比赛 Demo 冒烟
# 验证 5 个 Demo 场景主链路；前提：后端已启动（scripts\start-dev.ps1）
# 用法：.\scripts\run-demo.ps1
param(
  [string]$Base = 'http://127.0.0.1:8080'
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

try {
  $h = Invoke-RestMethod -Uri "$Base/api/health" -TimeoutSec 5
  if (-not $h.data -or $h.data.status -ne 'UP') { throw 'backend DOWN' }
} catch {
  Write-Host '!! 后端未就绪，请先运行 scripts\start-dev.ps1' -ForegroundColor Red
  exit 1
}

node (Join-Path $repoRoot 'scripts\demo-smoke.mjs') $Base
exit $LASTEXITCODE