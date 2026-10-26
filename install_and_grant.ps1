param(
    [string]$ApkPath = ".\app\build\outputs\apk\debug\app-debug.apk"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Simple SMS - ADB Installer & Permission Granter" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Resolve ADB path
$adbCmd = "adb"
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    $sdkAdb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $sdkAdb) {
        $adbCmd = $sdkAdb
        Write-Host "[*] Found Android SDK ADB at: $adbCmd" -ForegroundColor Gray
    }
}

if (-not (Test-Path $ApkPath)) {
    Write-Host "[!] APK not found at: $ApkPath" -ForegroundColor Red
    Write-Host "[*] Please build the project first: .\gradlew assembleDebug" -ForegroundColor Yellow
    exit 1
}

Write-Host "[*] Installing APK with -g flag (auto-grants runtime permissions)..." -ForegroundColor Yellow
& $adbCmd install -r -g $ApkPath

if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Standard adb install failed. Retrying with --bypass-low-target-sdk-block..." -ForegroundColor Red
    & $adbCmd install -r -g --bypass-low-target-sdk-block $ApkPath
}

$packageName = "com.simplesms.app"

Write-Host "[*] Explicitly ensuring carrier SMS & Contacts permissions are granted..." -ForegroundColor Yellow
& $adbCmd shell pm grant $packageName android.permission.SEND_SMS
& $adbCmd shell pm grant $packageName android.permission.READ_PHONE_STATE
& $adbCmd shell pm grant $packageName android.permission.READ_PHONE_NUMBERS
& $adbCmd shell pm grant $packageName android.permission.READ_CONTACTS
& $adbCmd shell pm grant $packageName android.permission.CAMERA
& $adbCmd shell pm grant $packageName android.permission.POST_NOTIFICATIONS

Write-Host "[+] Installation & Permission grant complete!" -ForegroundColor Green
Write-Host "[*] Launching Simple SMS on device..." -ForegroundColor Cyan
& $adbCmd shell monkey -p $packageName -c android.intent.category.LAUNCHER 1
