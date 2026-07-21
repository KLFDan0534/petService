@echo off
cd /d D:\code\ideaProject\petService

setlocal enabledelayedexpansion

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
