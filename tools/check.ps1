<#
.SYNOPSIS
    Verifies component size and filename constraints, then runs the Kotlin tests and builds the APK.

.DESCRIPTION
    Windows counterpart of tools/check.sh, invoked by `mise run check`.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

$failed = $false
$files = Get-ChildItem -Path 'core/src', 'ui/compose/src', 'app/src' -Recurse -File -Filter '*.kt'
foreach ($file in $files) {
    $lines = (Get-Content -LiteralPath $file.FullName | Measure-Object -Line).Lines
    if ($lines -gt 350) {
        Write-Host "Over 350 lines: $($file.FullName) ($lines)" -ForegroundColor Red
        $failed = $true
    }
    if ($file.Name.Contains('-')) {
        Write-Host "Hyphenated component filename: $($file.FullName)" -ForegroundColor Red
        $failed = $true
    }
}
if ($failed) { exit 1 }

gradle :core:testAndroidHostTest :ui:compose:testAndroidHostTest :app:assembleDebug
exit $LASTEXITCODE
