$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Jar = Join-Path $Root "release\cursorlink-0.1.0.jar"

if (-not (Test-Path $Jar)) {
    Write-Error "Missing $Jar"
}

function Install-Into([string]$ModsDir) {
    New-Item -ItemType Directory -Force -Path $ModsDir | Out-Null
    Copy-Item -Force $Jar (Join-Path $ModsDir "cursorlink-0.1.0.jar")
    Write-Host "Installed: $(Join-Path $ModsDir 'cursorlink-0.1.0.jar')"
    $api = Get-ChildItem -Path $ModsDir -Filter "*abric*api*.jar" -ErrorAction SilentlyContinue
    if (-not $api) {
        Write-Host "Note: I did not see Fabric API in that folder. Download it for 26.2 and put it in the same mods folder."
    }
}

$official = Join-Path $env:APPDATA ".minecraft\mods"
$searchRoots = @(
    $official,
    (Join-Path $env:APPDATA "PrismLauncher\instances"),
    (Join-Path $env:APPDATA "ModrinthApp\profiles"),
    (Join-Path $env:APPDATA "curseforge\minecraft\Instances")
)

$installed = $false
if (Test-Path (Split-Path $official -Parent)) {
    Install-Into $official
    $installed = $true
}

foreach ($root in $searchRoots) {
    if (-not (Test-Path $root)) { continue }
    Get-ChildItem -Path $root -Directory -Recurse -Filter "mods" -ErrorAction SilentlyContinue | ForEach-Object {
        Install-Into $_.FullName
        $installed = $true
    }
}

if (-not $installed) {
    Write-Host "Could not find a Minecraft mods folder."
    Write-Host "Create this folder, then run this script again:"
    Write-Host "  $official"
    exit 1
}

Write-Host ""
Write-Host "Next: start the helper on this same computer, then launch the 26.2 Fabric profile and press K."
