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

import org.jetbrains.annotations.NonNls;

final class XmlConstants {

  @NonNls public static final String ROOT = "VersionControlServer";
  @NonNls public static final String SERVERS = "Servers";
  @NonNls public static final String SERVER_INFO = "ServerInfo";
  @NonNls public static final String WORKSPACE_INFO = "WorkspaceInfo";
  @NonNls public static final String MAPPED_PATH = "MappedPath";
  @NonNls public static final String MAPPED_PATHS = "MappedPaths";

  @NonNls public static final String NAME_ATTR = "name";
  @NonNls public static final String OWNER_NAME_ATTR = "ownerName";
  @NonNls public static final String COMPUTER_ATTR = "computer";
  @NonNls public static final String COMMENT_ATTR = "comment";
  @NonNls public static final String TIMESTAMP_ATTR = "LastSavedCheckinTimeStamp";
  @NonNls public static final String URI_ATTR = "uri";
  @NonNls public static final String GUID_ATTR = "repositoryGuid";
  @NonNls public static final String PATH_ATTR = "path";

}