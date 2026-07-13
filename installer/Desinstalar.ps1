#requires -Version 5.1
$ErrorActionPreference = "Stop"

$appName = "myInvestor Email Processor"
$installDir = Join-Path $env:LOCALAPPDATA "Programs\$appName"

Get-Process -Name $appName -ErrorAction SilentlyContinue | Stop-Process -Force

if (Test-Path $installDir) {
    Remove-Item -Recurse -Force $installDir
    Write-Host "Aplicacion desinstalada de: $installDir" -ForegroundColor Green
} else {
    Write-Host "No se ha encontrado una instalacion en: $installDir" -ForegroundColor Yellow
}

$desktopShortcut = Join-Path ([Environment]::GetFolderPath("Desktop")) "$appName.lnk"
$startMenuShortcut = Join-Path ([Environment]::GetFolderPath("Programs")) "$appName.lnk"
Remove-Item -Force -ErrorAction SilentlyContinue $desktopShortcut
Remove-Item -Force -ErrorAction SilentlyContinue $startMenuShortcut

Write-Host "Accesos directos eliminados." -ForegroundColor Green
