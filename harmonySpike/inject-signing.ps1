# 检查 harmonyApp/build-profile.json5 的签名配置。
# 签名材料是机器本地数据（DevEco Studio 自动签名生成），不入库。
# 本地缺失时：用 DevEco Studio 打开 harmonyApp -> File > Project Structure > Signing Configs
#             勾选 Automatically generate signature 并登录华为账号，即可重新生成。
$ErrorActionPreference = "Stop"
$profile = Join-Path (Split-Path -Parent $PSScriptRoot) "harmonyApp\build-profile.json5"
$content = Get-Content $profile -Raw
if ($content -match "signingConfigs") {
    Write-Host "OK: build-profile.json5 已含签名配置。"
} else {
    Write-Warning "build-profile.json5 缺少签名配置。请在 DevEco Studio 中完成自动签名，"
    Write-Warning "或手动向 app 节点添加 signingConfigs 并给 product 挂 signingConfig: \"default\"。"
    exit 1
}
