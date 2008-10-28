/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorkingFolderInfo {

  public enum Status {
    Active,
    Cloaked
  }

  private @NotNull FilePath myLocalPath;
  private @NotNull String myServerPath;
  private @NotNull Status myStatus;

  public WorkingFolderInfo(final FilePath localPath) {
    this(Status.Active, localPath, "");
  }

  public WorkingFolderInfo(final @NotNull Status status, final @NotNull FilePath localPath, final @NotNull String serverPath) {
    myStatus = status;
    myLocalPath = localPath;
    myServerPath = serverPath;
  }

  @NotNull
  public FilePath getLocalPath() {
    return myLocalPath;
  }

  @NotNull
  public String getServerPath() {
    return myServerPath;
  }

  @NotNull
  public Status getStatus() {
    return myStatus;
  }

  public void setStatus(final @NotNull Status status) {
    myStatus = status;
  }

  public void setServerPath(final @NotNull String serverPath) {
    myServerPath = serverPath;
  }

  public void setLocalPath(final @NotNull FilePath localPath) {
    myLocalPath = localPath;
  }

  public WorkingFolderInfo getCopy() {
    return new WorkingFolderInfo(myStatus, myLocalPath, myServerPath);
  }

  @Nullable
  public String getServerPathByLocalPath(final @NotNull FilePath localPath) {
    if (getServerPath().length() > 0 && localPath.isUnder(getLocalPath(), false)) {
      String localPathString = FileUtil.toSystemIndependentName(localPath.getPath());
      String thisPathString = FileUtil.toSystemIndependentName(getLocalPath().getPath());
      String remainder = localPathString.substring(thisPathString.length());
      return getServerPath() + remainder;
    }
    return null;
  }

  @Nullable
  public FilePath getLocalPathByServerPath(final String serverPath, final boolean isDirectory) {
    if (getServerPath().length() > 0 && VersionControlPath.isUnder(getServerPath(), serverPath)) {
      String remainder = serverPath.substring(getServerPath().length());
      return VcsUtil.getFilePath(getLocalPath().getPath() + remainder, isDirectory);
    }
    return null;
  }

}

