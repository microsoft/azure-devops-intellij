#!/bin/bash

# This script helps automate mirroring all changes from a "master" repository to a "mirror" repository. 
# Changes should not be made in the mirror but all changes should flow to the mirror repository via the master repository.
# Inputs needed for the script are : 1. Remote URL for the master repository 2. Remote URL for the mirror repository

source ~/.bashrc
set -e
mirrorDirName="repoMirror"
errorMessage="Invalid arguments passed to script 1>&2
MirrorChanges.bat MasterRepositoryURL MirrorRepositoryURL LocalRepoRootDir
MasterRepositoryURL is the remote URL of the Git repository which contains the changes to be mirrored
MirrorRepositoryURL is the remote URL of the Git repository which will mirror the changes in the master repository
LocalRepoRootDir (Optional) is the root directory where the repository will be cloned. Defaults to the current directory."

# check args
if [[ -z "$1" ]]; then
  echo "Master repository URL is not provided"
  echo "${errorMessage}"
  exit 1
fi

if [[ -z "$2" ]]; then
  echo "Mirror repository URL is not provided"
  echo "${errorMessage}"
  exit 1
fi

masterRepoUrl="$1"
mirrorRepoURL="$2"

if [[ -n "$3" ]]; then
  cd "$3"
fi

# remove old directory
echo "Delete local mirror directory"
if [[ -d "${mirrorDirName}" ]]; then
  rm -rf "${mirrorDirName}"
fi

# clone the master repo
echo "Clone master repository to mirror directory"
git clone --no-checkout "${masterRepoUrl}" "${mirrorDirName}" || { echo "clone failed"; exit 1; }

if [[ ! -d "${mirrorDirName}" ]]; then
  echo -e "Errors during script execution 1>&2\n${mirrorDirName} does not exist"
  exit 1
fi

cd "${mirrorDirName}"

# sync mirror
echo "Set remote push url to the mirror repository url"
git remote set-url --push origin "${mirrorRepoURL}"

echo "Push to the mirror repository"
git push --mirror || { echo "push failed"; exit 1; }

# clean up directory
echo "Delete local mirror directory"
cd ..
rm -rf "${mirrorDirName}"
