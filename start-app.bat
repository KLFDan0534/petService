@echo off
cd /d D:\code\ideaProject\petService

setlocal enabledelayedexpansion

REM Load environment variables from .env (KEY=VALUE lines, # comments ignored)
if exist ".env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do (
    set "%%a=%%b"
  )
)

REM Build classpath from project module target/classes
set CP=pet-admin\target\classes
set CP=%CP%;pet-common\target\classes
set CP=%CP%;pet-framework\target\classes
set CP=%CP%;pet-system\target\classes
set CP=%CP%;pet-security\target\classes
set CP=%CP%;pet-business\target\classes
set CP=%CP%;pet-ai\target\classes

REM Add all Maven JARs from local repo
for /r "%USERPROFILE%\.m2\repository" %%f in (*.jar) do set CP=%CP%;%%f

REM Start the application
D:\Other\idea\gameJDK\bin\java.exe -cp "%CP%" com.pet.admin.PetApplication
