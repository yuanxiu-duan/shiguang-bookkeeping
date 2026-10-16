# 请通过环境变量提供有效的管理员 token，切勿硬编码真实 token（避免泄露）
$token = $env:ADMIN_TOKEN
if (-not $token) { Write-Error "缺少 ADMIN_TOKEN 环境变量，请先设置管理员 token 后重试"; exit 1 }

# 先删除之前的测试数据
$releases = Invoke-RestMethod -Uri 'http://localhost:8070/api/admin/app-releases' -Headers @{Authorization="Bearer $token"}
foreach ($r in $releases.data) {
    if ($r.versionName -in @('1.0.0','9.9.9')) {
        Invoke-RestMethod -Uri "http://localhost:8070/api/admin/app-releases/$($r.id)" -Method Delete -Headers @{Authorization="Bearer $token"}
        Write-Host "Deleted release: $($r.versionName)"
    }
}

# 创建测试文件
$testFile = Join-Path $env:TEMP "test-upload.apk"
[System.IO.File]::WriteAllBytes($testFile, [System.Text.Encoding]::UTF8.GetBytes("fake apk content for testing"))

# 第一步：上传文件
Add-Type -AssemblyName System.Net.Http

$httpClient = New-Object System.Net.Http.HttpClient
$httpClient.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $token)

$content = New-Object System.Net.Http.MultipartFormDataContent

$fileStream = [System.IO.File]::OpenRead($testFile)
$fileContent = New-Object System.Net.Http.StreamContent($fileStream)
$fileContent.Headers.ContentType = New-Object System.Net.Http.Headers.MediaTypeHeaderValue("application/octet-stream")
$content.Add($fileContent, "file", "test.apk")
$content.Add((New-Object System.Net.Http.StringContent("android")), "platform")

try {
    Write-Host "`n--- Step 1: Upload File ---"
    $response = $httpClient.PostAsync("http://localhost:8070/api/admin/app-releases/upload-file", $content).Result
    $body = $response.Content.ReadAsStringAsync().Result
    Write-Host "Status: $($response.StatusCode)"
    Write-Host "Body: $body"

    # 第二步：创建发布记录
    Write-Host "`n--- Step 2: Create Release ---"
    $uploadResult = ($body | ConvertFrom-Json).data
    $releaseBody = @{
        versionName = "2.0.0"
        versionCode = 200
        platform = "android"
        releaseNotes = "test release"
        isForceUpdate = $false
        downloadUrl = $uploadResult.downloadUrl
        fileKey = $uploadResult.fileKey
        fileSize = $uploadResult.fileSize
        fileName = $uploadResult.fileName
    } | ConvertTo-Json

    $releaseResponse = Invoke-RestMethod -Uri 'http://localhost:8070/api/admin/app-releases' -Method Post -Body $releaseBody -ContentType 'application/json' -Headers @{Authorization="Bearer $token"}
    Write-Host "Release created: $($releaseResponse.data.versionName)"
} catch {
    Write-Host "Error: $_"
} finally {
    $fileStream.Close()
}
