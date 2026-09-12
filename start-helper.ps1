$ErrorActionPreference = "Stop"
$Bridge = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "bridge"
Set-Location $Bridge

if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    Write-Host "Node.js is not installed. Install it from https://nodejs.org/ (22 or newer), then run this script again."
    exit 1
}

$envFile = Join-Path $Bridge ".env"
$example = Join-Path $Bridge ".env.example"
if (-not (Test-Path $envFile)) {
    Copy-Item $example $envFile
    Write-Host "Created bridge\.env — open that file, add your CURSOR_API_KEY and CURSOR_WORKSPACE, then run this script again."
    Write-Host $envFile
    exit 1
}

if (-not (Test-Path (Join-Path $Bridge "node_modules"))) {
    npm install
}

npm start
