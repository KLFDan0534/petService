@echo off
REM ============================================================
REM  pet-service 一体化镜像构建脚本
REM  作用：构建前端 dist -> 拷贝到构建上下文 -> 构建镜像 pet-service:latest
REM ============================================================
setlocal
cd /d "%~dp0"

echo [1/3] 构建前端 (frontend -> dist) ...
cd ..\frontend
call npm run build || goto :err
cd ..\pet-allinone

echo [2/3] 拷贝 dist 到构建上下文 ...
if exist dist rmdir /s /q dist
xcopy /e /i /q ..\frontend\dist dist >nul || goto :err

echo [3/3] 构建镜像 pet-service:latest ...
docker build -t pet-service:latest . || goto :err

echo.
echo 构建完成：pet-service:latest
echo 启动：docker compose -f docker-compose.allinone.yml up -d
exit /b 0

:err
echo.
echo [错误] 构建失败，请检查上方日志。
exit /b 1
