@echo off
chcp 65001 >nul
echo ========================================
echo   PetService 公网访问管理
echo ========================================
echo.
echo [1] 查看隧道状态
echo [2] 重启隧道服务
echo [3] 停止隧道服务
echo [4] 启动隧道服务
echo [5] 查看公网访问地址
echo [6] 测试公网连通性
echo.
set /p choice=请输入选项(1-6):

if "%choice%"=="1" goto status
if "%choice%"=="2" goto restart
if "%choice%"=="3" goto stop
if "%choice%"=="4" goto start
if "%choice%"=="5" goto urls
if "%choice%"=="6" goto test
goto end

:status
echo.
echo === 服务状态 ===
sc query Cloudflared
echo.
echo === 隧道连接 ===
D:\code\pet-web-tunnel\cloudflared.exe tunnel info pet-web-tunnel
goto end

:restart
echo.
echo 正在重启隧道服务...
net stop Cloudflared
timeout /t 2 /nobreak >nul
net start Cloudflared
echo 完成。
goto end

:stop
echo.
net stop Cloudflared
goto end

:start
echo.
net start Cloudflared
goto end

:urls
echo.
echo   前端页面:  https://web.petservice.cc.cd
echo   API 接口:  https://api.petservice.cc.cd/api/
echo   RabbitMQ:  https://api.petservice.cc.cd/rabbitmq/
echo   MinIO:     https://api.petservice.cc.cd/minio/
echo   Chroma:    https://api.petservice.cc.cd/chroma/
echo.
echo   (本地开发前端) http://localhost:5173
goto end

:test
echo.
echo === 测试公网连通性 ===
curl -s -o nul -w "前端 web.petservice.cc.cd : HTTP %%{http_code}\n" --max-time 20 https://web.petservice.cc.cd/
curl -s -o nul -w "API  api.petservice.cc.cd  : HTTP %%{http_code}\n" --max-time 20 -X POST https://api.petservice.cc.cd/api/auth/login -H "Content-Type: application/json" -H "User-Agent: okhttp/4.9.0" -d "{\"username_wsh\":\"admin\",\"password_wsh\":\"123456\"}"
goto end

:end
echo.
pause
