# IEC104 测试工具打包脚本
# 使用 jpackage 生成免安装 JDK 的可执行程序
# 要求：JDK 14+（推荐 JDK 17+）

Write-Host "===== IEC104 测试工具打包脚本 =====" -ForegroundColor Cyan

# 检查 Java 版本
$javaVersion = & java -version 2>&1 | Select-Object -First 1
Write-Host "Java 版本: $javaVersion"

# 检查是否支持 jpackage
$jpackagePath = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
if (-not (Test-Path $jpackagePath)) {
    # 尝试从 java 命令路径推断
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $javaDir = Split-Path $javaCmd.Source
        $jpackagePath = Join-Path $javaDir "jpackage.exe"
    }
}

if (-not (Test-Path $jpackagePath)) {
    Write-Host "错误: 未找到 jpackage，请使用 JDK 14+ 并设置 JAVA_HOME" -ForegroundColor Red
    Write-Host "当前 JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Yellow
    Write-Host "提示: 安装 JDK 17+ 后重新运行此脚本" -ForegroundColor Yellow
    exit 1
}

Write-Host "jpackage 路径: $jpackagePath" -ForegroundColor Green

# Step 1: 编译并生成 fat jar
Write-Host "`n[1/3] 编译并生成 fat jar..." -ForegroundColor Yellow
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败!" -ForegroundColor Red
    exit 1
}

$jarFile = Get-ChildItem "target\*.jar" | Where-Object { $_.Name -notlike "*-sources*" -and $_.Name -notlike "*-javadoc*" } | Select-Object -First 1
if (-not $jarFile) {
    Write-Host "未找到生成的 jar 文件!" -ForegroundColor Red
    exit 1
}
Write-Host "生成 jar: $($jarFile.Name)" -ForegroundColor Green

# Step 2: 使用 jpackage 生成应用镜像
Write-Host "`n[2/3] 使用 jpackage 生成应用镜像..." -ForegroundColor Yellow
$outputDir = "target\jpackage"

# 清理旧输出
if (Test-Path $outputDir) {
    Remove-Item -Recurse -Force $outputDir
}

& $jpackagePath `
    --type app-image `
    --input "target" `
    --name "IEC104测试工具" `
    --main-jar $jarFile.Name `
    --main-class "com.iec104tester.Main" `
    --dest $outputDir `
    --app-version "1.0.0" `
    --vendor "IEC104Tester" `
    --description "IEC 60870-5-104 Protocol Testing Tool" `
    --java-options "-Dfile.encoding=UTF-8" `
    --java-options "-Xms128m" `
    --java-options "-Xmx512m"

if ($LASTEXITCODE -ne 0) {
    Write-Host "jpackage 打包失败!" -ForegroundColor Red
    exit 1
}

# Step 3: 生成安装包（可选）
Write-Host "`n[3/3] 尝试生成安装包..." -ForegroundColor Yellow
$appImageDir = Get-ChildItem $outputDir -Directory | Select-Object -First 1

if ($appImageDir) {
    $installerDir = "target\installer"
    if (Test-Path $installerDir) {
        Remove-Item -Recurse -Force $installerDir
    }

    & $jpackagePath `
        --type msi `
        --app-image $appImageDir.FullName `
        --name "IEC104测试工具" `
        --app-version "1.0.0" `
        --vendor "IEC104Tester" `
        --dest $installerDir

    if ($LASTEXITCODE -eq 0) {
        $msiFile = Get-ChildItem "$installerDir\*.msi" -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($msiFile) {
            Write-Host "生成 MSI 安装包: $($msiFile.FullName)" -ForegroundColor Green
        }
    } else {
        Write-Host "MSI 安装包生成失败（需要 WiX Toolset），但应用镜像已生成" -ForegroundColor Yellow
        Write-Host "提示: 安装 WiX Toolset 3.x 后可生成 .msi 安装包" -ForegroundColor Yellow
    }
}

# 完成
Write-Host "`n===== 打包完成 =====" -ForegroundColor Cyan
$appDir = Get-ChildItem $outputDir -Directory | Select-Object -First 1
if ($appDir) {
    $exeFile = Get-ChildItem $appDir.FullName -Filter "*.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($exeFile) {
        Write-Host "可执行文件: $($exeFile.FullName)" -ForegroundColor Green
        Write-Host "应用目录: $($appDir.FullName)" -ForegroundColor Green
        Write-Host "`n可直接运行 exe 文件，无需安装 JDK" -ForegroundColor Cyan
    }
}
