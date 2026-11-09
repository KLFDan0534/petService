@echo off
REM ============================================================
REM  pet-web 前端镜像构建脚本
REM  作用：构建前端 dist -> 拷贝到构建上下文 -> 构建镜像 pet-web:latest
REM ============================================================
setlocal
cd /d "%~dp0"

echo [1/3] 构建前端 (frontend -> dist) ...
cd ..\frontend
call npm run build || goto :err
cd ..\pet-web

echo [2/3] 拷贝 dist 到构建上下文 ...
if exist dist rmdir /s /q dist
xcopy /e /i /q ..\frontend\dist dist >nul || goto :err

echo [3/3] 构建镜像 pet-web:latest ...
docker build -t pet-web:latest . || goto :err

echo.
echo 构建完成：pet-web:latest
echo 启动：docker compose -f docker-compose.web.yml up -d
echo 停止：docker compose -f docker-compose.web.yml down
exit /b 0

:err
echo.
echo [错误] 构建失败，请检查上方日志。
exit /b 1
