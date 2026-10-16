@echo off
chcp 65001 >nul
setlocal

echo ========================================
echo  Backend Start
echo ========================================
echo.

echo [1/2] Killing process on port 8070...
call :KILL_PORT 8070
echo Port 8070 released.
echo.

echo [2/2] Starting Spring Boot...
cd /d "%~dp0jz-server"
set JAVA_HOME=C:\Program Files\Java\jdk-21
REM 如果 JDK 安装在其他路径，请修改上面的 JAVA_HOME
set PATH=%JAVA_HOME%\bin;%PATH%
mvn spring-boot:run

pause
exit /b

:KILL_PORT
set "PORT=%~1"
for /f "tokens=5" %%P in ('netstat -ano -p tcp ^| findstr "LISTENING" ^| findstr ":%PORT% "') do (
    if not "%%P"=="0" (
        echo Killing PID %%P
        taskkill /F /PID %%P >nul 2>&1
    )
)
exit /b