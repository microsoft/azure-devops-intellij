REM This script helps automate mirroring all changes from a "master" repository to a "mirror" repository. 
REM Changes should not be made in the mirror but all changes should flow to the mirror repository via the master repository.
REM Inputs needed for the script are : 1. Remote URL for the master repository 2. Remote URL for the mirror repository

IF "%1" == "" (
  echo Master repository URL is not provided
  GOTO USAGE
)

IF "%2" == "" (
  echo Mirror repository URL is not provided
  GOTO USAGE
)

SET masterRepoUrl=%1
SET mirrorRepoURL=%2

IF NOT "%3" == "" (
  cd %3
)

SET mirrorDirName=repoMirror

echo Delete local mirror directory
IF EXIST %mirrorDirName% rd /S /Q %mirrorDirName%

echo Clone master repository to mirror directory
git clone --mirror %masterRepoUrl% %mirrorDirName% 
IF NOT EXIST %mirrorDirName% GOTO ERROR
IF NOT %ERRORLEVEL% EQU 0 GOTO ERROR

cd %mirrorDirName%

echo Set remote push url to the mirror repository url
git remote set-url --push origin %mirrorRepoURL%

echo Fetch and prune refs from master repository
git fetch -p origin
IF NOT %ERRORLEVEL% EQU 0 GOTO ERROR

echo Push to the mirror repository
git push --mirror
IF NOT %ERRORLEVEL% EQU 0 GOTO ERROR

echo Delete local mirror directory
cd ..
rd /S /Q %mirrorDirName%

:END
exit /b %ERRORLEVEL%

:ERROR
echo Errors during script execution 1>&2
GOTO END

:USAGE
echo Invalid arguments passed to script 1>&2
echo MirrorChanges.bat MasterRepositoryURL MirrorRepositoryURL LocalRepoRootDir
echo MasterRepositoryURL is the remote URL of the Git repository which contains the changes to be mirrored
echo MirrorRepositoryURL is the remote UL of the Git repository which will mirror the changes in the master repository
echo LocalRepoRootDir (Optional) is the root directory where the repository will be cloned. Defaults to the current directory.
GOTO END
