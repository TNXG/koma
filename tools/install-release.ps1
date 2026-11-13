<#
.SYNOPSIS
    Builds the release APK and installs it on a connected device.

.DESCRIPTION
    Windows counterpart of the `mise run install-release` command chain.
    The release build is signed with the key named by KOMA_KEYSTORE, so the
    install only works where that keystore and its passwords are configured.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

gradle :app:assembleRelease
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# ABI splits produce koma-<version>-<abi>.apk; the universal one installs anywhere.
$apk = @(Get-ChildItem -LiteralPath "$root/app/build/outputs/apk/release" -Filter '*-universal.apk' -File)
if ($apk.Count -ne 1) { throw "Expected one universal release APK, found $($apk.Count)." }

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$root/.tools/android" }
$adb = Join-Path $sdk 'platform-tools/adb.exe'
& $adb install -r $apk[0].FullName
exit $LASTEXITCODE
