$ErrorActionPreference = "Stop"

$project = Split-Path -Parent $MyInvocation.MyCommand.Path
$avdName = if ($env:GYMTRACKER_AVD) { $env:GYMTRACKER_AVD } else { "GymTracker_S24_Ultra" }
$sdk = $env:ANDROID_SDK_ROOT
$jdk = $env:JAVA_HOME
$gradleHome = $env:GRADLE_HOME
$emulator = if ($sdk) { Join-Path $sdk "emulator\emulator.exe" } else { "emulator.exe" }
$adb = if ($sdk) { Join-Path $sdk "platform-tools\adb.exe" } else { "adb.exe" }
$gradle = if ($gradleHome) { Join-Path $gradleHome "bin\gradle.bat" } else { "gradle.bat" }

if ($jdk) { $env:JAVA_HOME = $jdk }
if ($sdk) { $env:ANDROID_SDK_ROOT = $sdk; $env:ANDROID_HOME = $sdk }
if ($gradleHome) { $env:GRADLE_HOME = $gradleHome }

if (-not (Test-Path -LiteralPath $emulator)) { throw "Android Emulator not found: $emulator" }
if (-not (Test-Path -LiteralPath $adb)) { throw "ADB not found: $adb" }

$existing = & $adb devices | Select-String "emulator-.*`tdevice$"
if (-not $existing) {
    Write-Host "== Starting $avdName ==" -ForegroundColor Cyan
    Start-Process -FilePath $emulator -ArgumentList @(
        "-avd", $avdName,
        "-no-snapshot",
        "-no-boot-anim",
        "-gpu", "swiftshader_indirect",
        "-memory", "1536"
    ) -WindowStyle Normal | Out-Null
}

Write-Host "== Waiting for emulator ==" -ForegroundColor Cyan
& $adb wait-for-device
$deadline = (Get-Date).AddMinutes(5)
do {
    Start-Sleep -Seconds 3
    $boot = (& $adb shell getprop sys.boot_completed 2>$null).Trim()
    Write-Host "." -NoNewline
} while ($boot -ne "1" -and (Get-Date) -lt $deadline)
Write-Host ""
if ($boot -ne "1") { throw "Emulator did not finish booting in 5 minutes" }

Write-Host "== Build debug APK ==" -ForegroundColor Cyan
Push-Location $project
try {
    & $gradle assembleDebug --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle assembleDebug failed with exit code $LASTEXITCODE" }
} finally { Pop-Location }

$apk = Join-Path $project "app\build\outputs\apk\debug\app-debug.apk"
Write-Host "== Install/update APK in emulator ==" -ForegroundColor Cyan
& $adb install -r -d $apk
if ($LASTEXITCODE -ne 0) { throw "adb install failed with exit code $LASTEXITCODE" }

& $adb shell am force-stop com.sol.gymtracker
& $adb shell monkey -p com.sol.gymtracker 1 | Out-Null
Write-Host "GymTracker is running in the Android Emulator. The phone is not needed." -ForegroundColor Green