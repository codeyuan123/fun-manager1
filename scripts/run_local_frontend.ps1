Push-Location (Join-Path $PSScriptRoot "..\frontend")
try {
  if (!(Test-Path ".\node_modules")) {
    Write-Host "Installing frontend dependencies..."
    npm.cmd ci
  }

  Write-Host "Starting frontend dev server..."
  npm.cmd run dev -- --host 127.0.0.1 --port 5173
} finally {
  Pop-Location
}
