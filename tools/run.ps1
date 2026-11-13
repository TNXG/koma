<#
.SYNOPSIS
    Installs the debug APK on a connected device and launches the app.

.DESCRIPTION
    Windows counterpart of the `mise run run` command chain.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

gradle :app:installDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$root/.tools/android" }
$adb = Join-Path $sdk 'platform-tools/adb.exe'
& $adb shell am start -n moe.tnxg.koma/.MainActivity
exit $LASTEXITCODE
