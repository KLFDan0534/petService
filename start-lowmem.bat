@echo off
REM ============================================================
REM  pet-service 低内存启动脚本（前后端，各占独立窗口）
REM  后端: fat jar + JVM 堆限 320m，端口 8080
REM  前端: vite dev 5173
REM  用法: start-lowmem.bat          (前后端都起)
REM        start-lowmem.bat backend  (只起后端)
REM        start-lowmem.bat frontend (只起前端)
REM ============================================================
chcp 65001 >nul
cd /d D:\code\ideaProject\petService

set MODE=%1
if "%MODE%"=="" set MODE=all

REM 从 .env 加载环境变量（JWT_SECRET / MYSQL_* / TURNSTILE_* 等）
if exist ".env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do (
    set "%%a=%%b"
  )
)

REM 关键：某些宿主/沙箱会注入 SERVER__PORT 覆盖 server.port，
REM Spring 的 relaxed binding 会把它当成 server.port，必须显式覆盖回 8080
set SERVER__PORT=8080

if not "%MODE%"=="frontend" (
  echo [backend] starting :8080  heap=320m ...
  REM 注意：本项目 classpath 涉及 1700+ 依赖 jar，直接 -cp 会超出
  REM Windows 命令行 32KB 上限，必须走 pet-admin 的 fat jar（自带 manifest Class-Path）
  start "pet-backend :8080" cmd /k D:\Other\idea\gameJDK\bin\java.exe ^
    -Xmx320m -Xms96m -XX:MaxMetaspaceSize=192m -XX:MaxDirectMemorySize=64m ^
    -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k ^
    -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 ^
    -Dspring.main.lazy-initialization=true ^
    -Dlogging.level.com.pet=INFO ^
    -jar pet-admin\target\pet-admin-1.0.0.jar
)

if not "%MODE%"=="backend" (
  echo [frontend] starting :5173 ...
  start "pet-frontend :5173" cmd /k "cd /d D:\code\ideaProject\petService\frontend && set NODE_OPTIONS=--max-old-space-size=256 && node node_modules\vite\bin\vite.js --port 5173 --host 0.0.0.0"
)

echo.
echo   backend  : http://localhost:8080   (swagger: /swagger-ui.html)
echo   frontend : http://localhost:5173
echo.
