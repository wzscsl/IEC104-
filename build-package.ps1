# IEC104 测试工具打包脚本
# 使用 jpackage 生成免安装 JDK 的可执行程序
# 要求：JDK 17+（jpackage 在 JDK 14+ 可用，推荐 17+）
#
# 用法:
#   .\build-package.ps1              # 生成 app-image（免安装目录）
#   .\build-package.ps1 -MakeInstaller  # 额外尝试生成 MSI 安装包（需 WiX）
#   .\build-package.ps1 -SkipTests      # 跳过单元测试加速打包

param(
    [switch]$MakeInstaller,
    [switch]$SkipTests
)

Write-Host "===== IEC104 测试工具打包脚本 =====" -ForegroundColor Cyan

# --- 从 pom.xml 读取版本号，避免与脚本硬编码不同步 ---
$projectVersion = $null
try {
    $pomVersionLine = Select-Xml -Path .\pom.xml -XPath "//*[local-name()='project']/*[local-name()='version']" -ErrorAction Stop
    if ($pomVersionLine) {
        $projectVersion = $pomVersionLine.Node.InnerText.Trim()
    }
} catch {}
if (-not $projectVersion) {
    Write-Host "警告: 未能从 pom.xml 读取版本号，使用默认 1.2.0" -ForegroundColor Yellow
    $projectVersion = "1.2.0"
}
Write-Host "应用版本: $projectVersion" -ForegroundColor Green

# --- 检查 Java 版本 ---
$javaVersion = & java -version 2>&1 | Select-Object -First 1
Write-Host "Java 版本: $javaVersion"

# --- 检查 jpackage ---
$jpackagePath = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
if (-not (Test-Path $jpackagePath)) {
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
$mavenArgs = @("clean", "package", "-B")
if ($SkipTests) {
    $mavenArgs += "-DskipTests"
    Write-Host "  (跳过单元测试)" -ForegroundColor DarkGray
}
& mvn @mavenArgs
if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败!" -ForegroundColor Red
    exit 1
}

$jarFile = Get-ChildItem "target\*.jar" |
    Where-Object { $_.Name -notlike "*-sources*" -and $_.Name -notlike "*-javadoc*" -and $_.Name -notlike "original-*" } |
    Select-Object -First 1
if (-not $jarFile) {
    Write-Host "未找到生成的 jar 文件!" -ForegroundColor Red
    exit 1
}
Write-Host "生成 jar: $($jarFile.Name)" -ForegroundColor Green

# Step 2: 使用 jpackage 生成应用镜像
Write-Host "`n[2/3] 使用 jpackage 生成应用镜像..." -ForegroundColor Yellow
$outputDir = "target\jpackage"

if (Test-Path $outputDir) {
    Remove-Item -Recurse -Force $outputDir
}

$jpackageArgs = @(
    "--type", "app-image",
    "--input", "target",
    "--name", "IEC104测试工具",
    "--main-jar", $jarFile.Name,
    "--main-class", "com.iec104tester.Main",
    "--dest", $outputDir,
    "--app-version", $projectVersion,
    "--vendor", "IEC104Tester",
    "--description", "IEC 60870-5-104 Protocol Testing Tool",
    "--java-options", "-Dfile.encoding=UTF-8",
    "--java-options", "-Xms128m",
    "--java-options", "-Xmx512m",
    "--java-options", "-Dsun.java2d.uiScale=1.0"
)

# 应用图标（若存在）
$iconPath = Join-Path $PSScriptRoot "src\main\resources\icons\app-icon.ico"
if (Test-Path $iconPath) {
    $jpackageArgs += @("--icon", $iconPath)
    Write-Host "  使用应用图标: $iconPath" -ForegroundColor DarkGray
} else {
    Write-Host "  提示: 未找到 app-icon.ico，将使用 Java 默认图标" -ForegroundColor DarkGray
    Write-Host "  生成 .ico 方法: 在线 SVG -> ICO 转换，放入 src\main\resources\icons\" -ForegroundColor DarkGray
}

& $jpackagePath @jpackageArgs
if ($LASTEXITCODE -ne 0) {
    Write-Host "jpackage 打包失败!" -ForegroundColor Red
    exit 1
}

Write-Host "应用镜像生成成功" -ForegroundColor Green

# Step 3: 生成安装包（可选，需要 WiX Toolset）
if ($MakeInstaller) {
    Write-Host "`n[3/3] 尝试生成安装包..." -ForegroundColor Yellow
    $appImageDir = Get-ChildItem $outputDir -Directory | Select-Object -First 1

    $wixAvailable = $false
    $candleCmd = Get-Command candle -ErrorAction SilentlyContinue
    if ($candleCmd) {
        $wixAvailable = $true
    } else {
        $wixPaths = @(
            "${env:ProgramFiles(x86)}\WiX Toolset v3.11\bin\candle.exe",
            "${env:ProgramFiles}\WiX Toolset v3.11\bin\candle.exe",
            "${env:ProgramFiles(x86)}\WiX Toolset v3.14\bin\candle.exe",
            "${env:ProgramFiles}\WiX Toolset v3.14\bin\candle.exe"
        )
        foreach ($p in $wixPaths) {
            if (Test-Path $p) {
                $env:Path += ";$(Split-Path $p)"
                $wixAvailable = $true
                break
            }
        }
    }

    if ($appImageDir -and $wixAvailable) {
        $installerDir = "target\installer"
        if (Test-Path $installerDir) {
            Remove-Item -Recurse -Force $installerDir
        }

        & $jpackagePath `
            --type msi `
            --app-image $appImageDir.FullName `
            --name "IEC104测试工具" `
            --app-version $projectVersion `
            --vendor "IEC104Tester" `
            --dest $installerDir

        if ($LASTEXITCODE -eq 0) {
            $msiFile = Get-ChildItem "$installerDir\*.msi" -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($msiFile) {
                Write-Host "生成 MSI 安装包: $($msiFile.FullName)" -ForegroundColor Green
            }
        } else {
            Write-Host "MSI 安装包生成失败，但应用镜像已生成" -ForegroundColor Yellow
        }
    } elseif ($appImageDir -and -not $wixAvailable) {
        Write-Host "未检测到 WiX Toolset，跳过 MSI 安装包生成" -ForegroundColor Yellow
        Write-Host "提示: 安装 WiX Toolset 3.x 后可生成 .msi 安装包" -ForegroundColor Yellow
        Write-Host "应用镜像已生成，可直接运行 exe，无需 MSI 安装包" -ForegroundColor Green
    }
} else {
    Write-Host "`n[3/3] 跳过安装包生成（使用 -MakeInstaller 启用）" -ForegroundColor DarkGray
}

# 完成
Write-Host "`n===== 打包完成 =====" -ForegroundColor Cyan
$appDir = Get-ChildItem $outputDir -Directory | Select-Object -First 1
if ($appDir) {
    $exeFile = Get-ChildItem $appDir.FullName -Filter "*.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($exeFile) {
        Write-Host "可执行文件: $($exeFile.FullName)" -ForegroundColor Green
        Write-Host "应用目录: $($appDir.FullName)" -ForegroundColor Green
        Write-Host "应用版本: $projectVersion" -ForegroundColor Green
        Write-Host "`n可直接运行 exe 文件，无需安装 JDK" -ForegroundColor Cyan
    }
}
