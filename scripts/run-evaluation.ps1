# CampusPilot 运行评测套件：Phase3 回归 + Phase4 综合评测(M1-M6) + RAG 评测
# 用法：.\scripts\run-evaluation.ps1
# 输出：控制台汇总；详细结果在 evaluation/results/ 与 evaluation/rag/results/
param(
  [string]$Base = 'http://127.0.0.1:8080'
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

# 预检查后端
try {
  $h = Invoke-RestMethod -Uri "$Base/api/health" -TimeoutSec 5
  if (-not $h.data -or $h.data.status -ne 'UP') { throw 'backend DOWN' }
} catch {
  Write-Host '!! 后端未就绪，请先运行 scripts\start-dev.ps1' -ForegroundColor Red
  exit 1
}

Write-Host '== [1/3] Phase 3 回归 =========================================' -ForegroundColor Cyan
node (Join-Path $repoRoot 'evaluation\phase3\test-phase3.mjs') $Base
if ($LASTEXITCODE -ne 0) { Write-Host '!! Phase3 出现 FAIL' -ForegroundColor Red }

Write-Host '== [2/3] Phase 4 综合评测 (M1-M6) ==============================' -ForegroundColor Cyan
node (Join-Path $repoRoot 'evaluation\phase4\run-phase4-eval.mjs') $Base
if ($LASTEXITCODE -ne 0) { Write-Host '!! Phase4 出现 FAIL' -ForegroundColor Red }

Write-Host '== [3/3] RAG 评测 =============================================' -ForegroundColor Cyan
node (Join-Path $repoRoot 'evaluation\rag\run-rag-eval.mjs') $Base
if ($LASTEXITCODE -ne 0) { Write-Host '!! RAG 出现 FAIL' -ForegroundColor Red }

Write-Host ''
Write-Host '评测完成。报告：'
Write-Host '  evaluation/results/phase4-evaluation-report.md'
Write-Host '  evaluation/rag/results/rag-evaluation-report.md'