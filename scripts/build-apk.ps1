<#
.SYNOPSIS
    拾光账本 uni-app 本地打包 APK 自动化脚本
.DESCRIPTION
    1. 读取 manifest.json 版本信息
    2. 生成/复用 uni-app App 本地资源
    3. 同步资源 + 配置到 Android 离线 SDK 工程
    4. Gradle 构建 release/debug APK
    5. 输出 APK 到 android-app/output/
.PARAMETER SkipBuildResources
    跳过 HBuilderX 资源生成步骤（使用已有资源）
.PARAMETER Debug
    构建 debug 版本（默认 release）
.PARAMETER SdkProjectPath
    Android 离线 SDK 工程路径（含 HBuilder-Integrate-AS）
.EXAMPLE
    .\build-apk.ps1
    .\build-apk.ps1 -Debug
    .\build-apk.ps1 -SkipBuildResources
#>
param(
    [switch]$SkipBuildResources,
    [switch]$Debug,
    # 请修改为你的 Android 离线 SDK 工程路径（含 HBuilder-Integrate-AS）
    [string]$SdkProjectPath = "D:\path\to\uniapp-android-sdk\HBuilder-Integrate-AS"
)

$ErrorActionPreference = "Stop"

# ============================================================
# 配置区（按实际环境修改）
# ============================================================
# 自动取仓库根目录（脚本位于 <root>/scripts/ 下），避免硬编码个人路径
$ProjectRoot    = Split-Path $PSScriptRoot -Parent
$JzAppPath      = Join-Path $ProjectRoot "jz-app"
$ConfigPath     = Join-Path $ProjectRoot "android-app\config"
$KeystorePath   = Join-Path $ProjectRoot "android-app\keystore\shiguang.jks"
$OutputDir      = Join-Path $ProjectRoot "android-app\output"
$AppId          = "__UNI__1060BB1"
$AppName        = "拾光账本"
$ApplicationId  = "com.moneykeeper.app"

# 以下路径请按你的本机环境修改
$HBuilderXPath  = "D:\path\to\HBuilderX"
$AndroidSdkPath = "C:\Users\<your-user>\AppData\Local\Android\Sdk"
$JdkPath        = "C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot"

# 签名证书
$KeyAlias       = "shiguang"
# 签名口令请勿硬编码，运行时通过环境变量注入（避免密钥泄露）：
#   $env:KEYSTORE_KEY_PASSWORD = "你的key密码"; $env:KEYSTORE_STORE_PASSWORD = "你的store密码"
$KeyPassword    = if ($env:KEYSTORE_KEY_PASSWORD) { $env:KEYSTORE_KEY_PASSWORD } else { "change-me-key-password" }
$StorePassword  = if ($env:KEYSTORE_STORE_PASSWORD) { $env:KEYSTORE_STORE_PASSWORD } else { "change-me-key-password" }

# ============================================================
# 工具函数
# ============================================================
function Write-Step([string]$msg) {
    Write-Host "`n[*] $msg" -ForegroundColor Cyan
}
function Write-Ok([string]$msg) {
    Write-Host "    [OK] $msg" -ForegroundColor Green
}
function Write-Warn([string]$msg) {
    Write-Host "    [!] $msg" -ForegroundColor Yellow
}
function Write-Err([string]$msg) {
    Write-Host "    [X] $msg" -ForegroundColor Red
}

# ============================================================
# 0. 前置检查
# ============================================================
Write-Step "前置检查"

if (-not (Test-Path $KeystorePath)) {
    Write-Err "签名证书不存在: $KeystorePath"
    Write-Err "请先运行: keytool -genkey -alias shiguang -keyalg RSA -validity 9125 -keystore `"$KeystorePath`" -storepass $StorePassword -keypass $KeyPassword -dname `"CN=Shiguang, OU=App, O=MoneyKeeper, L=Beijing, ST=Beijing, C=CN`""
    exit 1
}
Write-Ok "签名证书: $KeystorePath"

if (-not (Test-Path $JzAppPath)) {
    Write-Err "uni-app 项目不存在: $JzAppPath"
    exit 1
}
Write-Ok "uni-app 项目: $JzAppPath"

if (-not (Test-Path $SdkProjectPath)) {
    Write-Err "Android SDK 工程不存在: $SdkProjectPath"
    Write-Err "请先下载 uni-app Android 离线 SDK 并解压，确保路径下含 HBuilder-Integrate-AS 目录"
    Write-Err "下载地址: https://nativesupport.dcloud.net.cn/AppDocs/download/android.html"
    exit 1
}
Write-Ok "Android SDK 工程: $SdkProjectPath"

if (-not (Test-Path $JdkPath)) {
    Write-Err "JDK 不存在: $JdkPath"
    exit 1
}
Write-Ok "JDK: $JdkPath"

if (-not (Test-Path $AndroidSdkPath)) {
    Write-Err "Android SDK 不存在: $AndroidSdkPath"
    exit 1
}
Write-Ok "Android SDK: $AndroidSdkPath"

# ============================================================
# 1. 读取版本信息
# ============================================================
Write-Step "读取 manifest.json 版本信息"

$manifestFile = Join-Path $JzAppPath "manifest.json"
$manifest = Get-Content $manifestFile -Raw -Encoding UTF8 | ConvertFrom-Json
$versionName = $manifest.versionName
$versionCode = $manifest.versionCode
Write-Ok "版本: $versionName (versionCode=$versionCode)"

# ============================================================
# 2. 生成/复用 uni-app App 资源
# ============================================================
$resourcesPath = Join-Path $JzAppPath "unpackage\resources\$AppId\www"

if ($SkipBuildResources) {
    Write-Step "跳过 uni-app 资源生成（-SkipBuildResources）"
} elseif (Test-Path $resourcesPath) {
    Write-Step "检测到已有 uni-app App 资源"
    Write-Warn "如需重新生成，请删除: $resourcesPath"
    Write-Warn "或在 HBuilderX 中: 发行 → 原生App-本地打包 → 生成本地打包App资源"
} else {
    Write-Step "生成 uni-app App 资源"

    $cliExe = Join-Path $HBuilderXPath "cli.exe"
    if (Test-Path $cliExe) {
        Write-Host "    尝试通过 HBuilderX CLI 生成资源..." -ForegroundColor Gray
        $projectName = (Get-Item $JzAppPath).Name
        & $cliExe publish --platform app --project $projectName 2>&1 | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
        if ($LASTEXITCODE -ne 0) {
            Write-Warn "HBuilderX CLI 生成失败（退出码 $LASTEXITCODE）"
            Write-Warn "请在 HBuilderX 中手动生成: 发行 → 原生App-本地打包 → 生成本地打包App资源"
        }
    } else {
        Write-Warn "未找到 HBuilderX CLI: $cliExe"
        Write-Warn "请在 HBuilderX 中手动生成: 发行 → 原生App-本地打包 → 生成本地打包App资源"
    }

    if (-not (Test-Path $resourcesPath)) {
        Write-Host ""
        Write-Host "    ====================================================" -ForegroundColor Yellow
        Write-Host "    请在 HBuilderX 中执行以下操作后重新运行本脚本:" -ForegroundColor Yellow
        Write-Host "    1. 打开 HBuilderX，导入 jz-app 项目" -ForegroundColor Yellow
        Write-Host "    2. 菜单: 发行 → 原生App-本地打包 → 生成本地打包App资源" -ForegroundColor Yellow
        Write-Host "    3. 等待生成完成（控制台提示 '导出成功'）" -ForegroundColor Yellow
        Write-Host "    4. 重新运行: .\build-apk.ps1 -SkipBuildResources" -ForegroundColor Yellow
        Write-Host "    ====================================================" -ForegroundColor Yellow
        exit 0
    }
}

if (-not (Test-Path $resourcesPath)) {
    Write-Err "uni-app App 资源不存在: $resourcesPath"
    Write-Err "请先在 HBuilderX 中生成本地打包App资源"
    exit 1
}
Write-Ok "App 资源: $resourcesPath"

# ============================================================
# 3. 同步资源到 Android SDK 工程
# ============================================================
Write-Step "同步 uni-app 资源到 Android 工程"

$appAssetsDir = Join-Path $SdkProjectPath "app\src\main\assets\apps\$AppId\www"
$dcloudDataDir = Join-Path $SdkProjectPath "app\src\main\assets\data"

# 清理旧资源
if (Test-Path $appAssetsDir) {
    Remove-Item $appAssetsDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $appAssetsDir | Out-Null
New-Item -ItemType Directory -Force -Path $dcloudDataDir | Out-Null

# 复制 www 资源
Copy-Item -Path "$resourcesPath\*" -Destination $appAssetsDir -Recurse -Force
$resourceCount = (Get-ChildItem $appAssetsDir -Recurse -File).Count
Write-Ok "复制 App 资源: $resourceCount 个文件"

# ============================================================
# 4. 同步配置文件
# ============================================================
Write-Step "同步配置文件到 Android 工程"

# dcloud_control.xml（更新 appid 和版本）
$dcontrolFile = Join-Path $dcloudDataDir "dcloud_control.xml"
@"
<?xml version="1.0" encoding="utf-8"?>
<hbuilder>
    <apps>
        <app appid="$AppId" appver="$versionName"/>
    </apps>
</hbuilder>
"@ | Set-Content $dcontrolFile -Encoding UTF8
Write-Ok "dcloud_control.xml (appid=$AppId, appver=$versionName)"

# AndroidManifest.xml
$manifestFile = Join-Path $ConfigPath "AndroidManifest.xml"
if (Test-Path $manifestFile) {
    Copy-Item $manifestFile -Destination (Join-Path $SdkProjectPath "app\src\main\AndroidManifest.xml") -Force
    Write-Ok "AndroidManifest.xml"
}

# strings.xml（应用名称）
$resValuesDir = Join-Path $SdkProjectPath "app\src\main\res\values"
New-Item -ItemType Directory -Force -Path $resValuesDir | Out-Null
$stringsFile = Join-Path $resValuesDir "strings.xml"
@"
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">$AppName</string>
</resources>
"@ | Set-Content $stringsFile -Encoding UTF8
Write-Ok "strings.xml (app_name=$AppName)"

# proguard-rules.pro
$proguardFile = Join-Path $ConfigPath "proguard-rules.pro"
if (Test-Path $proguardFile) {
    Copy-Item $proguardFile -Destination (Join-Path $SdkProjectPath "app\proguard-rules.pro") -Force
    Write-Ok "proguard-rules.pro"
}

# ============================================================
# 5. 生成 build.gradle（含签名配置 + 版本号）
# ============================================================
Write-Step "生成 build.gradle"

$buildGradleFile = Join-Path $SdkProjectPath "app\build.gradle"
$buildGradleContent = @"
apply plugin: 'com.android.application'

android {
    compileSdk 33
    namespace "$ApplicationId"

    defaultConfig {
        applicationId "$ApplicationId"
        minSdkVersion 21
        targetSdkVersion 33
        versionCode $versionCode
        versionName "$versionName"
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86'
        }
    }

    signingConfigs {
        shiguang {
            keyAlias '$KeyAlias'
            keyPassword '$KeyPassword'
            storeFile file('${($KeystorePath -replace '\\','\\')}')
            storePassword '$StorePassword'
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.shiguang
            minifyEnabled false
            shrinkResources false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            signingConfig signingConfigs.shiguang
            minifyEnabled false
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    aaptOptions {
        additionalParameters '--auto-add-overlay'
        ignoreAssetsPattern "!.svn:!.git:.*:!CVS:!thumbs.db:!picasa.ini:!*.scc:*~"
    }
}

dependencies {
    implementation fileTree(include: ['*.jar'], dir: 'libs')
    implementation fileTree(include: ['*.aar'], dir: 'libs')
}
"@
$buildGradleContent | Set-Content $buildGradleFile -Encoding UTF8
Write-Ok "build.gradle (versionCode=$versionCode, versionName=$versionName)"

# ============================================================
# 6. 生成 local.properties
# ============================================================
Write-Step "生成 local.properties"

$localPropsFile = Join-Path $SdkProjectPath "local.properties"
$androidSdkPathEscaped = $AndroidSdkPath -replace '\\', '\\'
@"
sdk.dir=$androidSdkPathEscaped
"@ | Set-Content $localPropsFile -Encoding UTF8
Write-Ok "local.properties (sdk.dir=$AndroidSdkPath)"

# ============================================================
# 7. 复制应用图标
# ============================================================
Write-Step "同步应用图标"

$iconsSrcDir = Join-Path $JzAppPath "unpackage\res\icons"
if (Test-Path $iconsSrcDir) {
    $iconMap = @{
        "mipmap-mdpi"    = "72x72.png"
        "mipmap-hdpi"    = "72x72.png"
        "mipmap-xhdpi"   = "96x96.png"
        "mipmap-xxhdpi"  = "144x144.png"
        "mipmap-xxxhdpi" = "192x192.png"
    }
    foreach ($entry in $iconMap.GetEnumerator()) {
        $srcIcon = Join-Path $iconsSrcDir $entry.Value
        $dstDir = Join-Path $SdkProjectPath "app\src\main\res\$($entry.Key)"
        if (Test-Path $srcIcon) {
            New-Item -ItemType Directory -Force -Path $dstDir | Out-Null
            Copy-Item $srcIcon -Destination (Join-Path $dstDir "ic_launcher.png") -Force
            Copy-Item $srcIcon -Destination (Join-Path $dstDir "ic_launcher_round.png") -Force
        }
    }
    Write-Ok "应用图标已同步"
} else {
    Write-Warn "图标源目录不存在: $iconsSrcDir（使用 SDK 默认图标）"
}

# ============================================================
# 8. Gradle 构建 APK
# ============================================================
Write-Step "Gradle 构建 APK"

$buildType = if ($Debug) { "assembleDebug" } else { "assembleRelease" }
$gradlew = Join-Path $SdkProjectPath "gradlew.bat"

if (-not (Test-Path $gradlew)) {
    Write-Err "gradlew.bat 不存在: $gradlew"
    exit 1
}

# 设置 Java 环境
$env:JAVA_HOME = $JdkPath
$env:PATH = "$JdkPath\bin;$env:PATH"

Write-Host "    构建类型: $buildType" -ForegroundColor Gray
Write-Host "    JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Gray

Push-Location $SdkProjectPath
try {
    & $gradlew $buildType --no-daemon 2>&1 | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
    $buildExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($buildExitCode -ne 0) {
    Write-Err "Gradle 构建失败（退出码 $buildExitCode）"
    Write-Warn "常见原因:"
    Write-Warn "  1. JAVA_HOME 版本不兼容 → 尝试 JDK 17"
    Write-Warn "  2. SDK 版本与 HBuilderX 版本不匹配"
    Write-Warn "  3. Android SDK 缺少组件 → 打开 Android Studio SDK Manager 安装"
    exit 1
}

# ============================================================
# 9. 复制 APK 到输出目录
# ============================================================
Write-Step "复制 APK 到输出目录"

$apkDir = if ($Debug) {
    Join-Path $SdkProjectPath "app\build\outputs\apk\debug"
} else {
    Join-Path $SdkProjectPath "app\build\outputs\apk\release"
}

$apkFile = Get-ChildItem $apkDir -Filter "*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $apkFile) {
    Write-Err "未找到生成的 APK: $apkDir"
    exit 1
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$outputApkName = "${AppName}_v${versionName}_code${versionCode}_$(if($Debug){'debug'}else{'release'}).apk"
$outputApkPath = Join-Path $OutputDir $outputApkName
Copy-Item $apkFile.FullName -Destination $outputApkPath -Force

$apkSize = [math]::Round((Get-Item $outputApkPath).Length / 1MB, 2)
Write-Ok "APK 构建成功!"
Write-Host ""
Write-Host "    ====================================================" -ForegroundColor Green
Write-Host "    APK: $outputApkPath" -ForegroundColor Green
Write-Host "    大小: ${apkSize} MB" -ForegroundColor Green
Write-Host "    版本: $versionName (code=$versionCode)" -ForegroundColor Green
Write-Host "    签名: $KeyAlias (shiguang.jks)" -ForegroundColor Green
Write-Host "    ====================================================" -ForegroundColor Green
Write-Host ""
Write-Host "    下一步: 登录 jz-web 管理端 → APP发布管理 → 新建版本" -ForegroundColor Cyan
Write-Host "    上传此 APK 并发布，APP 端即可检测到更新" -ForegroundColor Cyan
