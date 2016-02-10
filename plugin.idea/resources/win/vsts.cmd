@echo off
:: Script for VSTS protocol handler. Takes in the given arguments and parses them for the correct JetBrains Ide to launch along with the correct args
:: The URI will be: vsoi://checkout/?url=<git_url>&EncFormat=UTF8&IdeType=<ide_type>&IdeExe=<ide_exe>
@setlocal EnableDelayedExpansion

:: Read arg, set it to URI, and escape special chars. If there is no arg just skip to the end.
:: TODO: emit a message or something to user indicating missing or malformed URI
if "%~1"=="" GOTO end

set uri="%~1"
set uri=%uri:&=^&%

:: ----------------------------
::  Parsing URI for IdeType
:: ----------------------------

:: Default the IDE to IntelliJ
set ide=idea

:: If IdeExe is not in URI then skip forward with default IDE
if x%uri:IdeExe=%==x%uri% GOTO findKey

:: Remove the substring before the IdeExe
set "ide=%uri:*IdeExe=%"

::Remove extra quotes
set "ide=%ide:"=%"

:: Remove remaining parameters at the end of the string so only the IdeExe is left
for /F "usebackq delims=^&^ tokens=1" %%a in ('%ide%') do set ide=%%a

:: Removing leading whitespace
for /F "tokens=* delims= " %%a in ('echo %ide%') do set ide=%%a

:: ----------------------------
::  Find Registry Key and Value based on IdeExe
:: ----------------------------
:findKey
set KEY=""
set VALUE=""

if "%ide%"=="studio" (
    set "KEY=HKEY_LOCAL_MACHINE\SOFTWARE\Android Studio"
    set VALUE="Path"
) else (
    set KEY=HKEY_CLASSES_ROOT\Applications\%ide%.exe\shell\open\command
    set VALUE=""
)

for /F "usebackq tokens=2*" %%a in (`REG QUERY "%KEY%" /v %VALUE% 2^>nul`) do set exePath=%%b

:: TODO: check if a path is found and if not look in the Program Files directories

:: ----------------------------
::  Special path configurations
:: ----------------------------
if "%ide%"=="studio" (
    :: default to 32-bit exe
    set "exePath=%exePath%\bin\studio32.exe"
    if /I %Processor_Architecture%==AMD64 set "exePath=%exePath%\bin\studio64.exe"
    if /I "%PROCESSOR_ARCHITEW6432%"=="AMD64" set "exePath=%exePath%\bin\studio64.exe"
) else (
    :: Remove the extra parameter wildcard at the end of registry entry
    set exePath=!exePath:~0,-5!
)

:: ----------------------------
::  Launch Ide with parameters
:: ----------------------------
start "" "%exePath%" vsts %1

:end
@endlocal & exit