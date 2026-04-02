param(
  [string]$RuntimeRoot = "C:\pro\runtime",
  [string]$ServiceName = "FundManagerMariaDB",
  [int]$Port = 3306
)

$ErrorActionPreference = "Stop"

$mariaHome = Join-Path $RuntimeRoot "mariadb-10.11.16-winx64"
$installExe = Join-Path $mariaHome "bin\mysql_install_db.exe"
$clientExe = Join-Path $mariaHome "bin\mariadb.exe"
$serverExe = Join-Path $mariaHome "bin\mariadbd.exe"
$dataDir = Join-Path $RuntimeRoot "mariadb-data"
$pidFile = Join-Path $RuntimeRoot "mariadb-local.pid"
$rootPassword = "root_local_password"
$appDb = "fund_manager"
$appUser = "fund_app"
$appPassword = "fund_app_password"

if (!(Test-Path $mariaHome)) {
  throw "Local MariaDB runtime not found: $mariaHome"
}

if (!(Test-Path $installExe)) {
  throw "MariaDB init tool not found: $installExe"
}

if (!(Test-Path $clientExe)) {
  throw "MariaDB client not found: $clientExe"
}

if (!(Test-Path $serverExe)) {
  throw "MariaDB server not found: $serverExe"
}

New-Item -ItemType Directory -Force -Path $dataDir | Out-Null

$portListening = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
if ($portListening) {
  $ready = $true
} else {
  $ready = $false
}

$service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
$serviceMode = $false
$dbInitialized = Test-Path (Join-Path $dataDir "mysql")

if (-not $dbInitialized) {
  Write-Host "Initializing MariaDB data directory..."
  & $installExe `
    --datadir=$dataDir `
    --password=$rootPassword `
    --port=$Port `
    --silent
}

if (-not $ready) {
  if ($service) {
    $serviceMode = $true
    if ($service.StartType -ne "Automatic") {
      Set-Service -Name $ServiceName -StartupType Automatic
    }

    if ($service.Status -ne "Running") {
      Start-Service -Name $ServiceName
    }
  } else {
    Write-Host "Starting MariaDB in process mode..."
    $process = Start-Process `
      -FilePath $serverExe `
      -ArgumentList @(
        "--standalone",
        "--console",
        "--datadir=$dataDir",
        "--port=$Port",
        "--bind-address=127.0.0.1"
      ) `
      -WindowStyle Hidden `
      -PassThru
    Set-Content -Path $pidFile -Value $process.Id
  }
}

if (-not $ready) {
  for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 2
    $listening = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    if ($listening) {
      $ready = $true
      break
    }
  }
}

if (-not $ready) {
  throw "MariaDB did not become ready on port $Port"
}

$sql = @"
CREATE DATABASE IF NOT EXISTS ${appDb} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${appUser}'@'localhost' IDENTIFIED BY '${appPassword}';
CREATE USER IF NOT EXISTS '${appUser}'@'127.0.0.1' IDENTIFIED BY '${appPassword}';
GRANT ALL PRIVILEGES ON ${appDb}.* TO '${appUser}'@'localhost';
GRANT ALL PRIVILEGES ON ${appDb}.* TO '${appUser}'@'127.0.0.1';
FLUSH PRIVILEGES;
"@

& $clientExe `
  -u root `
  --password=$rootPassword `
  -P $Port `
  -e $sql

Write-Host "MariaDB is ready"
if ($serviceMode) {
  Write-Host "mode: service"
  Write-Host "service: $ServiceName"
} else {
  Write-Host "mode: process"
  if (Test-Path $pidFile) {
    Write-Host "pid: $(Get-Content $pidFile)"
  }
}
Write-Host "port: $Port"
Write-Host "root password: $rootPassword"
Write-Host "app db: $appDb"
Write-Host "app user: $appUser"
