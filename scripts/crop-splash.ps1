<#
.SYNOPSIS
    拾光账本 APP 启动图裁剪脚本
.DESCRIPTION
    从 static/shiguang.png 按比例居中裁剪生成 4 个分辨率的启动图
    所有图片统一为 9:16 比例 (0.5625)，避免变形
.NOTES
    源图: jz-app/static/shiguang.png
    输出: jz-app/unpackage/res/splash/{hdpi,xhdpi,xxhdpi,xxxhdpi}.png
#>

param(
    # 自动取仓库根目录（脚本位于 <root>/scripts/ 下），避免硬编码个人路径
    $RepoRoot = Split-Path $PSScriptRoot -Parent
    [string]$Source = Join-Path $RepoRoot "jz-app\static\shiguang.png",
    [string]$OutputDir = Join-Path $RepoRoot "jz-app\unpackage\res\splash"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

# 创建输出目录
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
}

# 加载源图
$src = [System.Drawing.Image]::FromFile($Source)
$srcW = $src.Width
$srcH = $src.Height
$srcRatio = [math]::Round($srcW / $srcH, 4)
Write-Host ""
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "  拾光账本 启动图裁剪" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "源图: $Source"
Write-Host "源图尺寸: $srcW x $srcH (比例 $srcRatio)"
Write-Host ""

# 目标比例 9:16 = 0.5625
$targetRatio = 9.0 / 16.0

# 计算裁剪区域（居中裁剪为 9:16）
if ($srcRatio -gt $targetRatio) {
    # 源图偏宽，左右裁剪
    $newH = $srcH
    $newW = [int]($srcH * $targetRatio)
} else {
    # 源图偏高，上下裁剪
    $newW = $srcW
    $newH = [int]($srcW / $targetRatio)
}
$cropX = [int](($srcW - $newW) / 2)
$cropY = [int](($srcH - $newH) / 2)
Write-Host "目标比例: 9:16 (0.5625)"
Write-Host "裁剪区域: $newW x $newH (从 $cropX, $cropY 开始)"
Write-Host ""

# 4 个分辨率配置（均为 9:16 比例）
$sizes = [ordered]@{
    "hdpi"    = @{ W = 450;  H = 800  }
    "xhdpi"   = @{ W = 720;  H = 1280 }
    "xxhdpi"  = @{ W = 1080; H = 1920 }
    "xxxhdpi" = @{ W = 1440; H = 2560 }
}

Write-Host "-------------------------------------------" -ForegroundColor Gray
Write-Host "开始生成..."
Write-Host "-------------------------------------------" -ForegroundColor Gray

foreach ($entry in $sizes.GetEnumerator()) {
    $name = $entry.Key
    $w = $entry.Value.W
    $h = $entry.Value.H

    # 创建目标位图
    $bmp = New-Object System.Drawing.Bitmap $w, $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    # 绘制（从源图裁剪区域缩放到目标尺寸）
    $dstRect = New-Object System.Drawing.Rectangle 0, 0, $w, $h
    $srcRect = New-Object System.Drawing.Rectangle $cropX, $cropY, $newW, $newH
    $g.DrawImage($src, $dstRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose()

    # 保存
    $outPath = Join-Path $OutputDir "$name.png"
    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    $sizeKB = [math]::Round((Get-Item $outPath).Length / 1KB, 1)
    $ratio = [math]::Round($w / $h, 4)
    Write-Host ("  [{0,-7}] {1}x{2}  比例={3}  大小={4}KB  -> {5}" -f $name, $w, $h, $ratio, $sizeKB, $outPath) -ForegroundColor Green
}

$src.Dispose()

Write-Host "-------------------------------------------" -ForegroundColor Gray
Write-Host "完成! 共生成 $($sizes.Count) 个启动图" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""
