@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

title OHCP - 农业病虫害智能问答助手

echo.
echo ╔══════════════════════════════════════════════╗
echo ║     🌾 农业病虫害智能问答助手 (OHCP)         ║
echo ║   Online Pest ^& Herbicide Consultation    ║
echo ╚══════════════════════════════════════════════╝
echo.

:: ── 1. 检查 Java ──
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 java 命令，请确保已安装 JDK 17 并配置 PATH
    pause
    exit /b 1
)

echo [INFO] 检测到的 Java:
java -version 2>&1
echo.

:: 验证 Java 版本是否为 17+
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VER=%%v"
)
set "JAVA_VER=!JAVA_VER:"=!"
echo !JAVA_VER! | findstr /r "^1[7-9]\." >nul
if errorlevel 1 (
    echo !JAVA_VER! | findstr /r "^2[1-9]\." >nul
    if errorlevel 1 (
        echo !JAVA_VER! | findstr /r "^[3-9][0-9]\." >nul
        if errorlevel 1 (
            echo [WARN] 建议使用 JDK 17 或更高版本，当前版本: !JAVA_VER!
            echo [INFO] 继续尝试...
        )
    )
)

:: ── 2. 检查 Maven Wrapper ──
if not exist "%SCRIPT_DIR%mvnw.cmd" (
    echo [ERROR] mvnw.cmd 未找到，请在项目根目录运行此脚本
    pause
    exit /b 1
)

echo [INFO] Maven Wrapper 就绪，将自动管理 Maven 版本
echo.

:: ── 3. 清理之前可能缓存的失败依赖 ──
if exist "%USERPROFILE%\.m2\repository\org\apache\lucene\lucene-smartcn" (
    echo [INFO] 清理旧的 lucene-smartcn 缓存...
    rd /s /q "%USERPROFILE%\.m2\repository\org\apache\lucene\lucene-smartcn" 2>nul
)

:: ── 4. 编译项目 ──
echo [INFO] 正在下载依赖并编译项目 (首次运行可能需要几分钟)...
echo.

call "%SCRIPT_DIR%mvnw.cmd" clean compile -DskipTests -q
if errorlevel 1 (
    echo.
    echo [ERROR] 编译失败！错误详情请查看上方输出。
    echo [HINT] 可尝试手动执行: mvn clean compile 查看详细错误
    pause
    exit /b 1
)

echo.
echo ══════════════════════════════════════════════
echo   ✅ 编译成功，正在启动 Spring Boot 服务...
echo ══════════════════════════════════════════════
echo.
echo   📡 后端API:     http://localhost:8080/api
echo   🖥️  前端页面:     http://localhost:8080
echo   🔧 H2控制台:     http://localhost:8080/h2-console
echo   📖 也可直接打开:  index.html
echo   📊 API 示例:
echo      POST http://localhost:8080/api/qa
echo      POST http://localhost:8080/api/diagnose
echo      GET  http://localhost:8080/api/knowledge/stats
echo.
echo   按 Ctrl+C 停止服务
echo ─────────────────────────────────────────────
echo.

:: ── 5. 启动应用 ──
call "%SCRIPT_DIR%mvnw.cmd" spring-boot:run
pause
