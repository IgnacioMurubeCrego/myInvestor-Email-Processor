#requires -Version 5.1
param(
    [string]$Version = "1.0.0"
)
$ErrorActionPreference = "Stop"

$projectDir = $PSScriptRoot
$appName = "myInvestor Email Processor"
$releaseName = "myInvestorEmailProcessor-v$Version-win64"
$distDir = Join-Path $projectDir "dist"
$jpackageInputDir = Join-Path $projectDir "jpackage-input"
$releaseRoot = Join-Path $projectDir "release"
$releaseDir = Join-Path $releaseRoot $releaseName
$iconPath = Join-Path $projectDir "icon.ico"

Write-Host "== Compilando el proyecto (mvn clean package) ==" -ForegroundColor Cyan
Push-Location $projectDir
try {
    # "clean" evita que clases obsoletas de compilaciones anteriores (target/classes) acaben en el jar.
    & mvn -q -DskipTests clean package
    if ($LASTEXITCODE -ne 0) { throw "La compilacion con Maven ha fallado." }
} finally {
    Pop-Location
}

Write-Host "== Generando el ejecutable nativo (jpackage) ==" -ForegroundColor Cyan
if (Test-Path $distDir) {
    Remove-Item -Recurse -Force $distDir
}
if (Test-Path $jpackageInputDir) {
    Remove-Item -Recurse -Force $jpackageInputDir
}
New-Item -ItemType Directory -Force -Path $jpackageInputDir | Out-Null
Copy-Item (Join-Path $projectDir "target\myInvestorStockProcessor.jar") $jpackageInputDir

& jpackage `
    --type app-image `
    --input $jpackageInputDir `
    --main-jar myInvestorStockProcessor.jar `
    --main-class org.example.myinvestor.Main `
    --name $appName `
    --icon $iconPath `
    --dest $distDir `
    --vendor "Ignacio Murube" `
    --app-version $Version

if ($LASTEXITCODE -ne 0) { throw "jpackage ha fallado." }

Write-Host "== Ensamblando el paquete de distribucion ==" -ForegroundColor Cyan
if (Test-Path $releaseRoot) {
    Remove-Item -Recurse -Force $releaseRoot
}
New-Item -ItemType Directory -Force -Path (Join-Path $releaseDir "app") | Out-Null

Copy-Item -Recurse (Join-Path $distDir $appName) (Join-Path $releaseDir "app\$appName")
Copy-Item (Join-Path $projectDir "installer\Instalar.ps1") $releaseDir
Copy-Item (Join-Path $projectDir "installer\Instalar.bat") $releaseDir
Copy-Item (Join-Path $projectDir "installer\Desinstalar.ps1") $releaseDir
Copy-Item (Join-Path $projectDir "installer\Desinstalar.bat") $releaseDir
Copy-Item (Join-Path $projectDir "GUIA_USO.md") $releaseDir

$zipPath = Join-Path $releaseRoot "$releaseName.zip"
Compress-Archive -Path (Join-Path $releaseDir "*") -DestinationPath $zipPath -Force

Write-Host ""
Write-Host "== Listo ==" -ForegroundColor Green
Write-Host "Carpeta:  $releaseDir"
Write-Host "Zip:      $zipPath"
Write-Host "Sube ese .zip como asset de una Release en GitHub."
