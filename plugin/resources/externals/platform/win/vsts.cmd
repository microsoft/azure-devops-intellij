@echo off
:: Script for VSTS protocol handler. Takes in the given arguments and parses them for the correct JetBrains Ide to launch along with the correct args
:: The URI will be: vsoi://checkout/?url=<git_url>&EncFormat=UTF8&IdeType=<ide_type>&IdeExe=<ide_exe>
@setlocal EnableDelayedExpansion
call:createLogFile

:: Read arg, set it to URI, and escape special chars. If there is no arg just skip to the end.
if "%~1"=="" (
    echo "Error: No args found. Skipping to exit since there is nothing to process" >> %logfile%
    GOTO end
)

set uri="%~1"
set uri=%uri:&=^&%
echo "URI found: %uri%" >> %logfile%
call:getIde

:: ----------------------------
::  Find Registry Key and Value based on IdeExe
:: ----------------------------
:findKey
echo "Searching for registry key for %ide%" >> %logfile%
set KEY=""
set VALUE=""

if "%ide%"=="studio" (
    set "KEY=HKEY_LOCAL_MACHINE\SOFTWARE\Android Studio"
    set VALUE="Path"
) else (
    set KEY=HKEY_CLASSES_ROOT\Applications\%ide%.exe\shell\open\command
    set VALUE=""
)

set exePath=""
for /F "usebackq tokens=2*" %%a in (`REG QUERY "%KEY%" /v %VALUE% 2^>nul`) do set exePath="%%b"
echo "The exe path found in the registry was: %exePath%" >> %logfile%

:: Check if a path is found and if not look in the Program Files directories
if %exePath%=="" GOTO searchPath

:: ----------------------------
::  Special path configurations
:: ----------------------------
if "%ide%"=="studio" (
    GOTO setAndroidExe
) else (
    :: Remove the extra parameter wildcard at the end of registry entry
    set exePath="!exePath:~1,-6!"
)
GOTO launch

:: ----------------------------
::  Search for IDE parent directory
:: ----------------------------
:searchPath
echo "There was nothing found in the registry for this IDE so searching for the exe" >> %logfile%
if "%ide%"=="studio" (
    set "directory=Android\Android Studio"
) else (
    set directory=JetBrains
)

:: Check parent directory in %PROGRAMFILES%, %PROGRAMFILES(x86)%, Program Files, and Program Files (x86)
:: This is done because Firefox doesn't resolve the env variables like expected
if exist "%PROGRAMFILES%\%directory%" (
    set exePath="%PROGRAMFILES%\%directory%"
    GOTO findIdeDirectory
)
if exist "%PROGRAMFILES(x86)%\%directory%" (
     set exePath="%PROGRAMFILES(x86)%\%directory%"
     GOTO findIdeDirectory
 )
if exist "%SYSTEMDRIVE%\Program Files\%directory%" (
    set exePath="%SYSTEMDRIVE%\Program Files\%directory%"
    GOTO findIdeDirectory
)
if exist "%SYSTEMDRIVE%\Program Files (x86)\%directory%" (
    set exePath="%SYSTEMDRIVE%\Program Files ^(x86^)\%directory%"
    GOTO findIdeDirectory
)
:: IDE parent directory was not found. Can't find exe to run so exit and log an error
echo "Error: The IDE could not be found on this machine in the default $directory% directory and therefore couldn't be opened." >> %logfile%
GOTO end

:: ----------------------------
::  Find IDE directory
:: ----------------------------
:findIdeDirectory
if "%ide%"=="studio" (
    GOTO setAndroidExe
)

call:getType
echo "IDE type found was: %type%" >> %logfile%
set literalPath=%exePath:~1,-1%
for /d %%d in ("%literalPath%\%type%*") do set "exePath=%%~sfd"
set exePath=%exePath%\bin\%ide%

:: RubyMine does not have a 64bit exe
if %type%==RubyMine (
    set exePath=%exePath%.exe
    GOTO launch
)

:: Get 64bit exe name for other IDEs if appropriate
call:is64
if %is64% == 1 set exePath=%exePath%64
set exePath=%exePath%.exe
GOTO launch

:: ----------------------------
::  Sets correct Android exe then launches
:: ----------------------------
:setAndroidExe
set exe=studio32.exe
call:is64
if %is64% == 1 set exe=studio64.exe
set exePath="%exePath:~1,-1%\bin\%exe%"
GOTO launch

:: ----------------------------
::  Launch IDE with parameters
:: ----------------------------
:launch
echo "Launching %exePath% with args" >> %logfile%
start "" %exePath% vsts %1
GOTO end

:: ----------------------------
::  Checks if the machine is 64 bit or not
:: ----------------------------
:is64
set is64=0
if /I %Processor_Architecture%==AMD64 set is64=1
if /I "%PROCESSOR_ARCHITEW6432%"=="AMD64" set is64=1
echo "System is 64 bit: %is64%" >> %logfile%
GOTO:eof

:: ----------------------------
::  Parsing URI for IdeExe
::
:: Constant quotations around the string is because of the special chars that are in the URI and will cause the script to fail otherwise
:: ----------------------------
:getIde
:: Default the IDE to IntelliJ
set ide=idea
:: If IdeExe is not in URI then skip forward with default IDE
if x%uri:IdeExe=%==x%uri% GOTO:eof
:: Remove the substring before the IdeExe
set "ide=%uri:*IdeExe=%"
::Remove extra quotes
set "ide=%ide:"=%"
:: Remove remaining parameters at the end of the string so only the IdeExe is left
for /F "usebackq delims=^&^ tokens=1" %%a in ('%ide%') do set ide=%%a
:: Removing leading whitespace
for /F "tokens=* delims= " %%a in ('echo %ide%') do set ide=%%a
GOTO:eof

:: ----------------------------
::  Parsing URI for IdeType
::
:: Constant quotations around the string is because of the special chars that are in the URI and will cause the script to fail otherwise
:: ----------------------------
:getType
:: Default the IDE to IntelliJ
set type=IntelliJ
:: If IdeType is not in URI then skip forward with default IDE
if x%uri:IdeType=%==x%uri% GOTO:eof
:: Remove the substring before the IdeExe
set "type=%uri:*IdeType=%"
::Remove extra quotes
set "type=%type:"=%"
:: Remove remaining parameters at the end of the string so only the IdeType is left
for /F "usebackq delims=^&^ tokens=1" %%a in ('%type%') do set type=%%a
:: Removing leading whitespace
for /F "tokens=* delims= " %%a in ('echo %type%') do set type=%%a
GOTO:eof

:: ----------------------------
:: Create log file
:: ----------------------------
:createLogFile
set shortDate=%date:~4%
for /F "delims=/ tokens=1,2,3" %%a in ('echo %shortDate%') do (
    set month=%%a
    set day=%%b
    set year=%%c
)

set shortTime=%time:~0,-3%
for /F "delims=: tokens=1,2,3" %%a in ('echo %shortTime%') do (
    set hour=%%a
    set minute=%%b
    set second=%%c
)
set logfile="%TEMP%\vsts_protocol_handler_%year%.%month%.%day%_%hour%.%minute%.%second%.txt"
echo.>%logfile%
GOTO:eof

:end
echo "Exiting script..." >> %logfile%
@endlocal & exit