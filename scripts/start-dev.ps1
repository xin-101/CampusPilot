# CampusPilot 一键启动：MySQL + 后端（本地 mvn）
# 用法：.\scripts\start-dev.ps1
# 可选：-Frontend 额外启动前端 dev server；-NoMysql 跳过 MySQL 检查
param(
  [switch]$Frontend,
  [switch]$NoMysql
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $repoRoot 'campuspilot'
$logFile = Join-Path $backend 'startup.log'

# 1. MySQL
if (-not $NoMysql) {
  $mysqlPort = Test-NetConnection -ComputerName 127.0.0.1 -Port 3306 -WarningAction SilentlyContinue -InformationLevel Quiet
  if (-not $mysqlPort) {
    Write-Output '>> 启动 MySQL 容器...'
    Push-Location $repoRoot
    docker compose up -d mysql
    Pop-Location
    $deadline = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $deadline) {
      if (Test-NetConnection -ComputerName 127.0.0.1 -Port 3306 -WarningAction SilentlyContinue -InformationLevel Quiet) { break }
      Start-Sleep -Seconds 3
    }
  }
}

# 2. 后端（若 8080 已占用则先停掉旧进程）
$conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($conn) {
  Write-Output '>> 停止旧后端进程...'
  Stop-Process -Id $conn.OwningProcess -Force
  Start-Sleep -Seconds 3
  Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
  Start-Sleep -Seconds 2
}

Write-Output '>> 启动后端 (spring-boot:run, dev) ...'
Start-Process -FilePath 'cmd.exe' -ArgumentList "/c", "cd /d $backend && mvn -q spring-boot:run -Dspring-boot.run.profiles=dev > $logFile 2>&1" -WindowStyle Hidden

$deadline = (Get-Date).AddSeconds(120)
$up = $false
while ((Get-Date) -lt $deadline) {
  if (Test-NetConnection -ComputerName 127.0.0.1 -Port 8080 -WarningAction SilentlyContinue -InformationLevel Quiet) { $up = $true; break }
  Start-Sleep -Seconds 3
}
if ($up) {
  Write-Output '>> 后端已就绪 http://127.0.0.1:8080 (log: campuspilot/startup.log)'
} else {
  Write-Output '!! 后端未就绪，请查看 campuspilot/startup.log'
  if (Test-Path $logFile) { Get-Content $logFile -Tail 30 }
}

# 3. 前端 dev（可选）
if ($Frontend) {
  $fe = Join-Path $repoRoot 'campuspilot-web'
  Write-Output '>> 启动前端 dev server (http://localhost:5173) ...'
  Start-Process -FilePath 'cmd.exe' -ArgumentList "/c", "cd /d $fe && npm run dev" -WindowStyle Hidden
}
Write-Output '提示：前端推荐用 npm run dev（README 见 campuspilot-web/README.md），浏览器默认代理 /api 到 8080。'