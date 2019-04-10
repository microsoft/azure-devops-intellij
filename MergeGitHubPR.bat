REM This script helps to merge changes from a GitHub Pull Request on the "mirror" repository to the "master" repository. 
REM Inputs needed for the script are : 
REM 1. Remote URL for the master repository 
REM 2. Remote URL for the mirror repository that was forked. This will be something like https://github.com/userOrOrgName/repoName.git. 
REM 3. The branch name on the fork from the Pull request
REM 4. The github pull request id
REM Example: For this github pull request https://github.com/Microsoft/azure-devops-intellij/pull/3, URL of mirror repository that was worked = https://github.com/yacaovsnc/vso-intellij.git, branch name on the fork = master, pull request id = 3 

IF "%1" == "" (
  echo Master repository URL is not provided
  GOTO USAGE
)

IF "%2" == "" (
  echo Mirror fork repository URL is not provided. 
  GOTO USAGE
)

IF "%3" == "" (
  echo Branch name on fork from the pull request is not provided.
  GOTO USAGE
)

IF "%4" == "" (
  echo GitHub pull request ID is not provided.
  GOTO USAGE
)

SET masterRepoUrl=%1
SET forkRepoURL=%2
SET forkBranch=%3
SET gitHubPRId=%4

IF NOT "%5" == "" (
  cd %5
)

SET mirrorDirName=repoMirror

echo Delete local mirror directory
IF EXIST %mirrorDirName% rd /S /Q %mirrorDirName%

echo Clone master repository to mirror directory
git clone %masterRepoUrl% %mirrorDirName% 
IF NOT EXIST %mirrorDirName% GOTO ERROR
IF NOT %ERRORLEVEL% EQU 0 GOTO ERROR

cd %mirrorDirName%

echo Fetch the branch in the fork for which the PR was created
git fetch %forkRepoURL% %forkBranch%:github/pr/%gitHubPRId%
IF NOT %ERRORLEVEL% EQU 0 GOTO ERROR

echo Checkout the fork branch 
git checkout github/pr/%gitHubPRId%
IF NOT %ERRORLEVEL% EQU 0 GOTO ERROR

echo Push branch to master repo
git push --set-upstream origin github/pr/%gitHubPRId%
IF NOT %ERRORLEVEL% EQU 0 GOTO ERROR
	
echo "Create a pull request for the new branch: " 
echo github/pr/%gitHubPRId%

echo "If PR fails merge, leave comment on GitHub PR for the contributor"
echo "If PR validation build succeeds and any manual verification required is complete, complete the PR." 
echo "This will trigger the CI build in master which if successful will push the changes to the mirror and PR on github will be closed"

:END
exit /b %ERRORLEVEL%

:ERROR
echo Errors during script execution 1>&2
GOTO END

:USAGE
echo Invalid arguments passed to script 1>&2
echo MergeGithubPR.bat MasterRepositoryURL MirrorForkRepositoryURL ForkBranch PullRequestId
echo MasterRepositoryURL is the remote URL of the Git repository which contains the changes to be mirrored
echo MirrorForkRepositoryURL is the remote UrL of the Git repository which was forked from the mirror repository
echo ForkBranch is the branch on the fork repository that user wants to merge into the mirror master
echo PullRequestId is the id of the pull request on GitHub
echo LocalRepoRootDir (Optional) is the root directory where the repository will be cloned. Defaults to the current directory.
GOTO END






