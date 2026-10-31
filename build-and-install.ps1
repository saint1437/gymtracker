$ErrorActionPreference = "Stop"

$project = Split-Path -Parent $MyInvocation.MyCommand.Path
$sdk = $env:ANDROID_SDK_ROOT
$jdk = $env:JAVA_HOME
$gradleHome = $env:GRADLE_HOME
$gradle = if ($gradleHome) { Join-Path $gradleHome "bin\gradle.bat" } else { "gradle.bat" }
$adb = if ($sdk) { Join-Path $sdk "platform-tools\adb.exe" } else { "adb.exe" }

if ($jdk) { $env:JAVA_HOME = $jdk }
if ($sdk) { $env:ANDROID_SDK_ROOT = $sdk; $env:ANDROID_HOME = $sdk }
if ($gradleHome) { $env:GRADLE_HOME = $gradleHome }

Write-Host "== Build debug APK ==" -ForegroundColor Cyan
Push-Location $project
try {
    & $gradle assembleDebug --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle assembleDebug failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}

$apk = Join-Path $project "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path -LiteralPath $apk)) { throw "APK not found: $apk" }
Write-Host "APK: $apk" -ForegroundColor Green

if (-not (Get-Command $adb -ErrorAction SilentlyContinue) -and -not (Test-Path -LiteralPath $adb)) {
    Write-Host "ADB не найден. Установи APK вручную или настрой ANDROID_SDK_ROOT." -ForegroundColor Yellow
    exit 0
}

$devices = @(& $adb devices | Select-String "`tdevice$")
if ($devices.Count -eq 0) {
    Write-Host "Телефон не подключён или USB debugging не авторизован." -ForegroundColor Yellow
    Write-Host "APK уже собран. Для установки позже запусти этот скрипт снова." -ForegroundColor Yellow
    exit 0
}

Write-Host "== Install update without removing app data ==" -ForegroundColor Cyan
& $adb install -r -d $apk
if ($LASTEXITCODE -ne 0) { throw "adb install failed with exit code $LASTEXITCODE" }
Write-Host "Приложение обновлено без удаления локальной базы Room." -ForegroundColor Green