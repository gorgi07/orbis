# Выполнять в PowerShell на Windows: .\scripts\check-local.ps1
$ErrorActionPreference = 'Stop'
$port = 8080
Write-Host 'Orbis: локальная проверка входа и регистрации' -ForegroundColor Cyan

try {
    $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction Stop
    foreach ($listener in $listeners) {
        $ownerPid = $listener.OwningProcess
        $process = Get-CimInstance Win32_Process -Filter "ProcessId = $ownerPid"
        Write-Host "Порт $port занят PID $ownerPid"
        Write-Host "Командная строка: $($process.CommandLine)"
        if ($process.CommandLine -notmatch 'OrbisApplicationKt|orbis\.jar|GradleDaemon|GradleWorkerMain') {
            Write-Warning 'Возможно, на порту 8080 запущено НЕ приложение Orbis. Остановите старый запуск в IntelliJ.'
        }
    }
} catch {
    Write-Warning 'Не удалось определить процесс на порту 8080; проверьте, запущено ли приложение.'
}

$loginPage = $null
try {
    $loginPage = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/login' -UseBasicParsing -MaximumRedirection 3 -TimeoutSec 8
} catch {
    Write-Error 'Не удалось открыть http://127.0.0.1:8080/login. Сначала запустите Orbis с профилем local.'
    exit 1
}
if ($loginPage.Content -match 'custom-login-v1' -and $loginPage.Content -match 'Зарегистрироваться') {
    Write-Host 'OK: Orbis показывает фирменную страницу входа со ссылкой на регистрацию' -ForegroundColor Green
} else {
    Write-Host 'ОШИБКА: на порту 8080 НЕ текущая страница входа Orbis.' -ForegroundColor Red
    Write-Host 'Остановите все старые Spring Boot процессы в IntelliJ и заново запустите OrbisApplicationKt.'
    exit 2
}
try {
    $registerPage = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/register' -UseBasicParsing -MaximumRedirection 3 -TimeoutSec 8
    if ($registerPage.Content -match 'custom-register-v1' -and $registerPage.Content -match 'confirmPassword') {
        Write-Host 'OK: публичная страница регистрации доступна' -ForegroundColor Green
    } else {
        Write-Host 'ОШИБКА: получена не форма регистрации Orbis' -ForegroundColor Red
        exit 3
    }
} catch {
    Write-Host 'ОШИБКА: /register не открывается.' -ForegroundColor Red
    exit 3
}
Write-Host 'Для входа на локальной машине: bob / local_bob_password' -ForegroundColor Green
