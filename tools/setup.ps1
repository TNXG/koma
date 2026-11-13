<#
.SYNOPSIS
    Installs the Android SDK and the verified OCR model into .tools/ on Windows.

.DESCRIPTION
    Windows counterpart of tools/setup.sh, invoked by `mise run setup`.
    Requires PowerShell 5.1 or newer; Java comes from the toolchain pinned in mise.toml.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

# tools/ -> repository root
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

# sdkmanager and the Android Gradle Plugin keep caches under ANDROID_USER_HOME.
# Keeping that inside the repository stops them from breaking when the home
# directory contains non-ASCII characters.
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = "$root/.tools/android" }
if (-not $env:ANDROID_USER_HOME) { $env:ANDROID_USER_HOME = "$root/.tools/android-user" }
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

$sdk = $env:ANDROID_HOME
$cliUrl = 'https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip'
$modelUrl = 'https://raw.githubusercontent.com/sml2h3/ddddocr/master/ddddocr/common_old.onnx'
$modelPath = 'core/src/androidMain/assets/ocr/captcha.onnx'
$modelHash = 'b8f2ad9cbc1f2e3922a6cb9459e30824e7e2467f3fb4fd61420640e34ea0bf68'

function Save-File([string]$Uri, [string]$Path) {
    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($curl) {
        & $curl.Source -fsSL --retry 2 -o $Path $Uri
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $Uri" }
    } else {
        Invoke-WebRequest -Uri $Uri -OutFile $Path -UseBasicParsing
    }
}

function Get-Sha256([string]$Path) {
    (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

New-Item -ItemType Directory -Force -Path $sdk, $env:ANDROID_USER_HOME, (Split-Path -Parent $modelPath) | Out-Null

$sdkmanager = "$sdk/cmdline-tools/latest/bin/sdkmanager.bat"
if (-not (Test-Path $sdkmanager)) {
    $zip = Join-Path $env:TEMP 'commandlinetools-win.zip'
    $tmp = Join-Path $env:TEMP ('cmdline-tools-' + [guid]::NewGuid().ToString('N'))
    Save-File $cliUrl $zip
    Expand-Archive -LiteralPath $zip -DestinationPath $tmp -Force
    Remove-Item -Recurse -Force "$sdk/cmdline-tools/latest" -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path "$sdk/cmdline-tools" | Out-Null
    Move-Item (Join-Path $tmp 'cmdline-tools') "$sdk/cmdline-tools/latest"
    Remove-Item -Recurse -Force $tmp
    Remove-Item -Force $zip
}

# sdkmanager reads its answers from stdin; feed it enough "y" for every license.
$answers = ("y`n" * 64)
$answers | & $sdkmanager --sdk_root=$sdk --licenses | Out-Null

# build-tools 36.0.0 is what the Android Gradle Plugin resolves by default here;
# 37.0.0 matches compileSdk. Installing both keeps `gradle` offline-clean.
$answers | & $sdkmanager --sdk_root=$sdk 'platform-tools' 'platforms;android-37.0' 'build-tools;37.0.0' 'build-tools;36.0.0'
if ($LASTEXITCODE -ne 0) { throw "sdkmanager failed with exit code $LASTEXITCODE" }

if (-not (Test-Path $modelPath) -or (Get-Sha256 $modelPath) -ne $modelHash) {
    Save-File $modelUrl "$modelPath.tmp"
    if ((Get-Sha256 "$modelPath.tmp") -ne $modelHash) {
        Remove-Item -Force "$modelPath.tmp"
        throw 'OCR model checksum mismatch.'
    }
    Move-Item -Force "$modelPath.tmp" $modelPath
}

Write-Host 'Android SDK and verified OCR model ready.'
