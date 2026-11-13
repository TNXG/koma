<#
.SYNOPSIS
    Builds the release APK and installs it on a connected device.

.DESCRIPTION
    Windows counterpart of the `mise run install-release` command chain.
    The release variant reuses the debug signing key so it can be installed over adb.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

gradle :app:assembleRelease
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$root/.tools/android" }
$adb = Join-Path $sdk 'platform-tools/adb.exe'
& $adb install -r "$root/app/build/outputs/apk/release/app-release.apk"
exit $LASTEXITCODE
