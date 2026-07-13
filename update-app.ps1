#requires -Version 5.1
$ErrorActionPreference = "Stop"

$projectDir = $PSScriptRoot
$appName = "myInvestor Email Processor"
$iconPath = Join-Path $projectDir "icon.ico"
$distDir = Join-Path $projectDir "dist"
$jpackageInputDir = Join-Path $projectDir "jpackage-input"
$installDir = Join-Path $env:LOCALAPPDATA "Programs\$appName"

function Ensure-Shortcut($shortcutPath, $exePath, $workingDir) {
    if (-not (Test-Path $shortcutPath)) {
        $shell = New-Object -ComObject WScript.Shell
        $shortcut = $shell.CreateShortcut($shortcutPath)
        $shortcut.TargetPath = $exePath
        $shortcut.WorkingDirectory = $workingDir
        $shortcut.IconLocation = $exePath
        $shortcut.Description = $appName
        $shortcut.Save()
        Write-Host "Acceso directo creado: $shortcutPath"
    }
}

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
    --app-version 1.0.0

if ($LASTEXITCODE -ne 0) { throw "jpackage ha fallado." }

# La app debe cerrarse antes de sobrescribir sus ficheros, o la copia falla con "en uso".
Write-Host "== Cerrando la aplicacion si esta en ejecucion ==" -ForegroundColor Cyan
Get-Process -Name $appName -ErrorAction SilentlyContinue | Stop-Process -Force

Write-Host "== Instalando la nueva version en $installDir ==" -ForegroundColor Cyan
if (Test-Path $installDir) {
    Remove-Item -Recurse -Force $installDir
}
Copy-Item -Recurse (Join-Path $distDir $appName) $installDir

# Los accesos directos apuntan a esta misma ruta, asi que sobreviven a la actualizacion sin tocarlos.
$exePath = Join-Path $installDir "$appName.exe"
$desktopShortcut = Join-Path ([Environment]::GetFolderPath("Desktop")) "$appName.lnk"
$startMenuShortcut = Join-Path ([Environment]::GetFolderPath("Programs")) "$appName.lnk"
Ensure-Shortcut $desktopShortcut $exePath $installDir
Ensure-Shortcut $startMenuShortcut $exePath $installDir

Write-Host "== Listo. Version actualizada instalada en: $installDir ==" -ForegroundColor Green
