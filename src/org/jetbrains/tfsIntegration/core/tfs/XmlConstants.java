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