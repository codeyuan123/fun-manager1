param(
  [string]$RuntimeRoot = "C:\pro\runtime"
)

$ErrorActionPreference = "Stop"

$mavenHome = Join-Path $RuntimeRoot "apache-maven-3.9.14"
$javaCandidates = @(
  "C:\pro\runtime\jdk-17*\bin\java.exe",
  "C:\Program Files\Eclipse Adoptium\jdk-17*\bin\java.exe",
  "C:\Program Files\Eclipse Adoptium\jdk-*\bin\java.exe",
  "C:\Users\17104\AppData\Local\Programs\*\bin\java.exe"
)

$javaExe = $null
foreach ($pattern in $javaCandidates) {
  $match = Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue | Sort-Object FullName | Select-Object -First 1
  if ($match) {
    $javaExe = $match.FullName
    break
  }
}

if (-not $javaExe) {
  throw "Java not found. Install JDK 17 first."
}

$mvnCmd = Join-Path $mavenHome "bin\mvn.cmd"
if (!(Test-Path $mvnCmd)) {
  throw "Maven not found: $mvnCmd"
}

$javaHome = Split-Path (Split-Path $javaExe -Parent) -Parent
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$mavenHome\bin;$env:Path"

$env:SPRING_PROFILES_ACTIVE = "local"
$env:SERVER_PORT = "18080"
$env:DB_HOST = "127.0.0.1"
$env:DB_PORT = "3306"
$env:DB_NAME = "fund_manager"
$env:DB_USERNAME = "fund_app"
$env:DB_PASSWORD = "fund_app_password"
$env:JWT_SECRET = "local-dev-secret-local-dev-secret"
$env:TZ = "Asia/Shanghai"

Write-Host "Starting local backend..."
Write-Host "JAVA_HOME=$javaHome"
Write-Host "MAVEN_HOME=$mavenHome"

Push-Location (Join-Path $PSScriptRoot "..\backend")
try {
  & $mvnCmd spring-boot:run
} finally {
  Pop-Location
}
