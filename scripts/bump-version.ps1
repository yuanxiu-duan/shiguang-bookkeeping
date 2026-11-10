<#
.SYNOPSIS
    拾光账本 APP 版本号自动 +1（唯一版本源：jz-app/manifest.json）

.DESCRIPTION
    规则（与 AGENTS.md 一致）：
      versionName  = major.minor.patch，patch +1（patch 超过 9 时进位到 minor）
      versionCode  = major*100 + minor*10 + patch

    APP 能否收到更新，取决于两处版本号同时满足：
      1) 打进 APK 的 jz-app/manifest.json 版本号（本脚本改这里）
      2) 服务端 app_release 表里 is_published=true 且版本更高的发布记录（AdminManage → APP发布管理）
    只改一处都不会触发更新。

.PARAMETER DryRun
    只打印将要变成什么，不写入文件

.EXAMPLE
    .\bump-version.ps1
    .\bump-version.ps1 -DryRun
#>
param(
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ManifestFile = Join-Path $ProjectRoot "jz-app\manifest.json"

if (-not (Test-Path $ManifestFile)) {
    Write-Host "[X] 找不到 manifest.json: $ManifestFile" -ForegroundColor Red
    exit 1
}

# 保留原始格式（manifest.json 由 HBuilderX 生成，缩进 4 空格）
$raw = Get-Content $ManifestFile -Raw -Encoding UTF8
$manifest = $raw | ConvertFrom-Json

$oldName = [string]$manifest.versionName
$oldCode = [int]$manifest.versionCode
Write-Host "当前版本: $oldName (versionCode=$oldCode)" -ForegroundColor Cyan

$parts = $oldName.Split('.')
if ($parts.Count -ne 3) {
    Write-Host "[X] versionName 不是 major.minor.patch 格式: $oldName" -ForegroundColor Red
    exit 1
}

$major = [int]$parts[0]
$minor = [int]$parts[1]
$patch = [int]$parts[2]

# patch +1，超过 9 进位（因为 versionCode 用 minor*10+patch，patch 只能 0-9）
$patch++
if ($patch -gt 9) {
    $patch = 0
    $minor++
}

$newName = "$major.$minor.$patch"
$newCode = $major * 100 + $minor * 10 + $patch

Write-Host "新版本  : $newName (versionCode=$newCode)" -ForegroundColor Green

if ($DryRun) {
    Write-Host "(DryRun，未写入)" -ForegroundColor Yellow
    exit 0
}

$newRaw = $raw `
    -replace '("versionName"\s*:\s*")[^"]*(")', "`${1}$newName`${2}" `
    -replace '("versionCode"\s*:\s*)\d+', "`${1}$newCode"

if ($newRaw -eq $raw) {
    Write-Host "[!] 没有发生替换，请检查 manifest.json 中 versionName / versionCode 的写法" -ForegroundColor Yellow
    exit 1
}

# UTF8 无 BOM，避免 HBuilderX 解析异常
[System.IO.File]::WriteAllText($ManifestFile, $newRaw, (New-Object System.Text.UTF8Encoding($false)))

$check = (Get-Content $ManifestFile -Raw -Encoding UTF8 | ConvertFrom-Json)
Write-Host "[OK] 已写入: $($check.versionName) (versionCode=$($check.versionCode))" -ForegroundColor Green
Write-Host ""
Write-Host "下一步（缺一不可）：" -ForegroundColor Cyan
Write-Host "  1. HBuilderX 重新打开 jz-app 项目，确认 manifest 里版本是 $newName / $newCode，再云打包"
Write-Host "  2. 打好包到 jz-web → AdminManage → APP发布管理，创建 $newName / $newCode 的记录并【发布】"
