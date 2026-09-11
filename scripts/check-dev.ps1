# CampusPilot 开发环境健康检查
# 检查：MySQL、后端 8080、前端构建产物、评测脚本可执行性
$ErrorActionPreference = 'SilentlyContinue'

$repoRoot = Split-Path -Parent $PSScriptRoot
$ok = $true

function CheckStep([string]$name, [bool]$cond, [string]$detail) {
  if ($cond) { Write-Output "[ OK ] $name ($detail)" }
  else { Write-Output "[FAIL] $name ($detail)"; $global:ok = $false }
}

Write-Output '== CampusPilot 环境检查 =='

# 1. MySQL
$mysqlPort = Test-NetConnection -ComputerName 127.0.0.1 -Port 3306 -WarningAction SilentlyContinue -InformationLevel Quiet
$mysqlName = $null
$containers = docker ps --format '{{.Names}} {{.Status}}' 2>$null
if ($containers) {
  foreach ($line in $containers) {
    if ($line -match 'mysql') { $mysqlName = $line }
  }
}
CheckStep 'MySQL@3306' $mysqlPort ('监听=' + $mysqlPort + ' ' + $mysqlName)

# 2. 后端
$health = $null
try {
  $health = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/health' -TimeoutSec 5
} catch {}
if ($health -and $health.data) {
  CheckStep 'Backend@8080' ($health.data.status -eq 'UP') ("db=" + $health.data.database)
} else {
  CheckStep 'Backend@8080' $false '未响应 /api/health'
}

# 3. 前端
$dist = Join-Path $repoRoot 'campuspilot-web\dist'
CheckStep 'Frontend-dist' (Test-Path (Join-Path $dist 'index.html')) $dist

# 4. 评测脚本
CheckStep 'Phase3回归脚本' (Test-Path (Join-Path $repoRoot 'evaluation\phase3\test-phase3.mjs')) 'evaluation/phase3'
CheckStep 'Phase4评测脚本' (Test-Path (Join-Path $repoRoot 'evaluation\phase4\run-phase4-eval.mjs')) 'evaluation/phase4'
CheckStep 'RAG评测脚本' (Test-Path (Join-Path $repoRoot 'evaluation\rag\run-rag-eval.mjs')) 'evaluation/rag'

Write-Output ""
if ($ok) { Write-Output '结论：环境正常' } else { Write-Output '结论：存在异常，请检查上方 [FAIL] 项'; exit 1 }