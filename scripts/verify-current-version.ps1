# Run in PowerShell from the project root: .\scripts\verify-current-version.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$web = Join-Path $root 'src\main\kotlin\ru\itmo\personlab\web'
$routes = @(
  @{ Path = 'LocationPageController.kt'; Pattern = '/locations' },
  @{ Path = 'LocationPageController.kt'; Pattern = '/\{id\}' },
  @{ Path = 'PeoplePageController.kt'; Pattern = '/people' },
  @{ Path = 'PageErrorAdvice.kt'; Pattern = 'error/404' },
  @{ Path = 'ApiControllers.kt'; Pattern = '\.toPageResponse\(\)' }
)
Write-Host 'Orbis: checking source routes and paging contract' -ForegroundColor Cyan
foreach ($r in $routes) {
  $path = Join-Path $web $r.Path
  if (!(Test-Path $path)) { throw "Missing file: $path" }
  if ((Get-Content $path -Raw) -notmatch $r.Pattern) { throw "Expected contract missing in $($r.Path): $($r.Pattern)" }
}
Write-Host 'OK: page controllers, 404 handler, and page DTO present' -ForegroundColor Green
$listen = @(Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue)
if ($listen.Count -eq 0) {
  Write-Host 'Port 8080 is free. Start Orbis with the local profile.' -ForegroundColor Yellow
} else {
  foreach ($item in $listen) {
    $owningPid = $item.OwningProcess
    $proc = Get-CimInstance Win32_Process -Filter "ProcessId = $owningPid"
    Write-Host "Port 8080: PID $owningPid; command: $($proc.CommandLine)"
  }
  Write-Host 'If this is a different Java application, stop it before starting Orbis.' -ForegroundColor Yellow
}
Write-Host 'Build: gradle clean test bootJar' -ForegroundColor Cyan
Write-Host 'Run:   gradle bootRun --args="--spring.profiles.active=local"' -ForegroundColor Cyan
