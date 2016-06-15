@echo off
@echo Applying environment configuration...
set CURRENT_DIR=%cd%
cd ..
set PROJECT_HOME=%cd%
cd %CURRENT_DIR%

set ENV_NAME=%USERNAME%

rem Set path to the TOOLS_HOME
setlocal enabledelayedexpansion
set aliev_TOOLS_HOME=C:\_Projects\_upc2-____-infra\trunk\tools
set grebenjuk_TOOLS_HOME=C:\Work\projects\upc2\upc2-infra\tools
set kkolesnikova_TOOLS_HOME=D:\_Projects\upc-upc2-trunk\tools
set usova_TOOLS_HOME=D:\Projects\upc-upc2-infra-git\tools
set afedorov_TOOLS_HOME=D:\projects\upc-upc2-infra\tools
set fedorov_TOOLS_HOME=D:\OPK\trunk\tools
set akhmetov_TOOLS_HOME=D:\upc2-____-infra\trunk\tools
set estartseva_TOOLS_HOME=D:\workspace\upc2\upc-upc2-infra\trunk\tools
set kipenko_TOOLS_HOME=C:\projects\_upc2-____-infra\trunk\tools
set plizga_TOOLS_HOME=E:\Projects\upc2\upc-upc2-infra\trunk\tools
rem Add your path here.

set TOOLS_HOME=!%ENV_NAME%_TOOLS_HOME!
endlocal & set TOOLS_HOME=%TOOLS_HOME%
if not defined TOOLS_HOME (
    set TOOLS_HOME= 
    echo Error: TOOL_HOME is NOT set for the %ENV_NAME% environment. Append the "%ENV_NAME%_TOOLS_HOME=<path to the tools on the local machine>" to the setenv.cmd  
)

if exist %TOOLS_HOME% (
    call %TOOLS_HOME%\set-tool-env.cmd
) else (
    echo Error: TOOLS_HOME NOT exist %TOOLS_HOME%
)

rem Set path to the JAVA_HOME
setlocal enabledelayedexpansion
set aliev_JAVA_HOME=C:\java\jdk6-37-b32
set grebenjuk_JAVA_HOME=C:\Work\jdk\jdk1.6.0_37
set kkolesnikova_JAVA_HOME=D:\java\jdk6_0_38
set usova_JAVA_HOME=C:\Work\Java\jdk1.6.0_32
set afedorov_JAVA_HOME=C:\Program Files\Java\jdk1.6.0_25
set fedorov_JAVA_HOME=c:\Program Files\Java\jdk1.6.0_37
set akhmetov_JAVA_HOME=D:\jdk1.6.0_45
set estartseva_JAVA_HOME=C:\Program Files\Java\jdk1.6.0_45
set kipenko_JAVA_HOME=C:\Program Files\Java\jdk1.6.0_37
set plizga_JAVA_HOME=E:\Dist\jdk1.8.0_92_patched
rem Add your path here.

set JAVA_HOME=!%ENV_NAME%_JAVA_HOME!
endlocal & set JAVA_HOME=%JAVA_HOME%
if not defined JAVA_HOME (
    set JAVA_HOME= 
    echo Error: JAVA_HOME is NOT set for the %ENV_NAME% environment. Append the "%ENV_NAME%_JAVA_HOME=<path to the Java on the local machine>" to the setenv.cmd  
)

if not exist %JAVA_HOME% (
    echo Error: JAVA_HOME NOT exist %JAVA_HOME%
)

set PATH=%PATH%;%JAVA_HOME%/bin

@echo             ENV_NAME:    %ENV_NAME%
@echo         PROJECT_HOME:    %PROJECT_HOME%
@echo           TOOLS_HOME:    %TOOLS_HOME%
@echo            JAVA_HOME:    %JAVA_HOME%
@echo                 PATH:    %PATH%

@echo done. (Applying environment configuration)

@echo Applying execution environment configuration...
call setenv.%ENV_NAME%.cmd
@echo done. (Applying execution environment configuration)
