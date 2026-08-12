$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
if ([string]::IsNullOrWhiteSpace($scriptRoot)) {
    $scriptRoot = (Get-Location).Path
}
Set-Location $scriptRoot

$localRoot = Join-Path $scriptRoot ".local"
$downloadsDir = Join-Path $localRoot "downloads"
$jdkExtractRoot = Join-Path $localRoot "jdk-17"
$zipPath = Join-Path $downloadsDir "temurin-jdk17.zip"
$downloadUrl = "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"

Write-Host "[INFO] 项目根目录: $scriptRoot"
Write-Host "[INFO] 准备本地目录..."
New-Item -ItemType Directory -Force -Path $downloadsDir, $jdkExtractRoot | Out-Null

Write-Host "[INFO] 从 Adoptium API 下载 Temurin JDK 17 ZIP..."
if (Test-Path $zipPath) {
    Remove-Item -Force $zipPath
}

$downloadOk = $false
$primaryError = $null

try {
    Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath -MaximumRedirection 10 -TimeoutSec 600
    $downloadOk = $true
} catch {
    $primaryError = $_.Exception.Message
    Write-Host "[WARN] Invoke-WebRequest 失败: $primaryError"
}

if (-not $downloadOk) {
    $curlCmd = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($curlCmd) {
        Write-Host "[INFO] 使用 curl.exe 重试下载..."
        & curl.exe -L --fail --output $zipPath $downloadUrl
        if ($LASTEXITCODE -eq 0) {
            $downloadOk = $true
        } else {
            Write-Error "[ERROR] curl.exe 下载失败，退出代码 $LASTEXITCODE"
            exit 1
        }
    } else {
        Write-Error "[ERROR] 下载失败，且 curl.exe 不可用。Invoke-WebRequest 错误: $primaryError"
        exit 1
    }
}

if (-not (Test-Path $zipPath)) {
    Write-Error "[ERROR] 下载失败: 未找到文件 $zipPath"
    exit 1
}

$zipFile = Get-Item $zipPath
if ($zipFile.Length -lt 10MB) {
    Write-Error "[ERROR] 下载的文件似乎无效 (大小太小: $($zipFile.Length) bytes): $zipPath"
    exit 1
}

Write-Host "[INFO] 清理旧的 JDK 目录: $jdkExtractRoot ..."
Get-ChildItem -Path $jdkExtractRoot -Force -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force

Write-Host "[INFO] 解压 JDK ZIP..."
try {
    Expand-Archive -Path $zipPath -DestinationPath $jdkExtractRoot -Force
} catch {
    Write-Error "[ERROR] 解压 JDK ZIP 失败: $($_.Exception.Message)"
    exit 1
}

$javaExe = Get-ChildItem -Path $jdkExtractRoot -Recurse -File -Filter "java.exe" |
        Where-Object { $_.FullName -match "\\bin\\java\.exe$" } |
        Select-Object -First 1

if (-not $javaExe) {
    Write-Error "[ERROR] JDK 解压完成但未找到 java.exe: $jdkExtractRoot"
    exit 1
}

$jdkHome = Split-Path -Parent $javaExe.DirectoryName

Write-Host "[INFO] 本地 JDK 17 主目录: $jdkHome"
Write-Host "[INFO] 验证本地 JDK 版本..."
$javaVersionOutput = cmd /c "`"$($javaExe.FullName)`" -version 2>&1"
$javaVersionOutput | ForEach-Object { Write-Host $_ }
if ($LASTEXITCODE -ne 0) {
    Write-Error "[ERROR] 本地 java -version 失败，退出代码 $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "[DONE] 本地 JDK 17 已准备就绪！"
Write-Host "[NEXT] 运行: .\锝烇綋绯荤粺.bat"
