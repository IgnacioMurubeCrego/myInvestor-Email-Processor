#requires -Version 5.1
$ErrorActionPreference = "Stop"

$appName = "myInvestor Email Processor"
$sourceDir = Join-Path $PSScriptRoot "app\$appName"
$installDir = Join-Path $env:LOCALAPPDATA "Programs\$appName"

if (-not (Test-Path $sourceDir)) {
    Write-Host "No se encuentra la carpeta de la aplicacion en:" -ForegroundColor Red
    Write-Host "  $sourceDir"
    Write-Host "Asegurate de haber descomprimido el ZIP completo (con la carpeta 'app' incluida) antes de ejecutar este instalador."
    exit 1
}

Write-Host "Instalando $appName..." -ForegroundColor Cyan

Get-Process -Name $appName -ErrorAction SilentlyContinue | Stop-Process -Force

if (Test-Path $installDir) {
    Remove-Item -Recurse -Force $installDir
}
New-Item -ItemType Directory -Force -Path (Split-Path $installDir) | Out-Null
Copy-Item -Recurse $sourceDir $installDir

$exePath = Join-Path $installDir "$appName.exe"

function New-AppShortcut($shortcutPath) {
    $shell = New-Object -ComObject WScript.Shell
    $shortcut = $shell.CreateShortcut($shortcutPath)
    $shortcut.TargetPath = $exePath
    $shortcut.WorkingDirectory = $installDir
    $shortcut.IconLocation = $exePath
    $shortcut.Description = $appName
    $shortcut.Save()
}

$desktopShortcut = Join-Path ([Environment]::GetFolderPath("Desktop")) "$appName.lnk"
$startMenuShortcut = Join-Path ([Environment]::GetFolderPath("Programs")) "$appName.lnk"
New-AppShortcut $desktopShortcut
New-AppShortcut $startMenuShortcut

Write-Host ""
Write-Host "Listo. Se ha instalado $appName." -ForegroundColor Green
Write-Host "Encontraras un icono en el Escritorio y en el Menu Inicio."
