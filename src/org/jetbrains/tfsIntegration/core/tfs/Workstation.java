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

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.util.SystemProperties;
import org.apache.axis2.databinding.utils.ConverterUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.config.TfsServerConnectionHelper;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;
import org.jetbrains.tfsIntegration.exceptions.DuplicateMappingException;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.exceptions.WorkspaceHasNoMappingException;
import org.jetbrains.tfsIntegration.xmlutil.XmlUtil;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.*;

public class Workstation {

  // to be used in tests
  public static boolean PRESERVE_CONFIG_FILE = false;

  private static File getCacheFileWindows(String version) {
    String path = "Local Settings\\Application Data\\Microsoft\\Team Foundation\\" + version + "\\Cache\\VersionControl.config";
    return new File(SystemProperties.getUserHome(), path);
  }

  @NonNls private static final String CACHE_FILE_LINUX_MAC = "tfs-servers.xml";

  private static final Logger LOG = Logger.getInstance(Workstation.class.getName());

  private final List<ServerInfo> myServerInfos;

  private @Nullable Ref<FilePath> myDuplicateMappedPath;

  private static String ourComputerName;

  private Workstation() {
    myServerInfos = loadCache();
  }

  private static class WorkstationHolder {
    private static final Workstation ourInstance = new Workstation();
  }

  public static Workstation getInstance() {
    return WorkstationHolder.ourInstance;
  }

  public List<ServerInfo> getServers() {
    return Collections.unmodifiableList(myServerInfos);
  }

  @Nullable
  public ServerInfo getServer(URI uri) {
    for (ServerInfo server : getServers()) {
      if (server.getUri().equals(uri)) {
        return server;
      }
    }
    return null;
  }

  @Nullable
  public ServerInfo getServerByInstanceId(final String instanceId) {
    for (ServerInfo server : getServers()) {
      if (server.getGuid().equals(instanceId)) {
        return server;
      }
    }
    return null;
  }

  private List<WorkspaceInfo> getAllWorkspacesForCurrentOwnerAndComputer(boolean showLoginIfNoCredentials) {
    List<WorkspaceInfo> result = new ArrayList<WorkspaceInfo>();
    for (final ServerInfo server : getServers()) {
      if (showLoginIfNoCredentials && server.getQualifiedUsername() == null) {
        try {
          TfsServerConnectionHelper.ensureAuthenticated(null, server.getUri(), false);
        }
        catch (TfsException e) {
          continue;
        }
      }
      result.addAll(server.getWorkspacesForCurrentOwnerAndComputer());
    }
    return result;
  }

  private static List<ServerInfo> loadCache() {
    // TODO: validate against schema
    File cacheFile = getCacheFile(true);
    if (cacheFile != null) {
      try {
        WorkstationCacheReader reader = new WorkstationCacheReader();
        XmlUtil.parseFile(cacheFile, reader);
        return reader.getServers();
      }
      catch (IOException e) {
        LOG.info("Cannot read workspace cache", e);
      }
      catch (SAXException e) {
        LOG.info("Cannot read workspace cache", e);
      }
      catch (RuntimeException e) {
        LOG.info("Cannot read workspace cache", e);
      }
      catch (Exception e) {
        LOG.info("Cannot read workspace cache", e);
      }
    }
    return new ArrayList<ServerInfo>();
  }


  @Nullable
  private static File getCacheFile(boolean existingOnly) {
    if (PRESERVE_CONFIG_FILE) {
      return null;
    }

    File cacheFile;
    String [] versions = new String[] {"4.0", "3.0", "2.0", "1.0"};
    if (SystemInfo.isWindows) {
      int i = 0;
      do {
        cacheFile = getCacheFileWindows(versions[i++]);
      }
      while (!cacheFile.exists() && i < versions.length);
    }
    else {
      cacheFile = new File(PathManager.getOptionsPath(), CACHE_FILE_LINUX_MAC);
    }
    return (cacheFile.exists() || !existingOnly) ? cacheFile : null;
  }

  void update() {
    invalidateDuplicateMappedPath();

    File cacheFile = getCacheFile(false);
    if (cacheFile != null) {
      if (!cacheFile.getParentFile().exists()) {
        cacheFile.getParentFile().mkdirs();
      }
      try {
        XmlUtil.saveFile(cacheFile, new XmlUtil.SaveDelegate() {

          public void doSave(XmlUtil.SavePerformer savePerformer) {
            try {
              savePerformer.startElement(XmlConstants.ROOT);
              savePerformer.startElement(XmlConstants.SERVERS);
              for (ServerInfo serverInfo : getServers()) {
                Map<String, String> serverAttributes = new HashMap<String, String>();
                serverAttributes.put(XmlConstants.URI_ATTR, serverInfo.getUri().toString());
                serverAttributes.put(XmlConstants.GUID_ATTR, serverInfo.getGuid());
                savePerformer.startElement(XmlConstants.SERVER_INFO, serverAttributes);

                for (WorkspaceInfo workspaceInfo : serverInfo.getWorkspaces()) {
                  Map<String, String> workspaceAttributes = new HashMap<String, String>();
                  workspaceAttributes.put(XmlConstants.NAME_ATTR, workspaceInfo.getName());
                  workspaceAttributes.put(XmlConstants.OWNER_NAME_ATTR, workspaceInfo.getOwnerName());
                  workspaceAttributes.put(XmlConstants.COMPUTER_ATTR, workspaceInfo.getComputer());
                  if (workspaceInfo.getComment() != null) {
                    workspaceAttributes.put(XmlConstants.COMMENT_ATTR, workspaceInfo.getComment());
                  }
                  workspaceAttributes.put(XmlConstants.TIMESTAMP_ATTR, ConverterUtil.convertToString(workspaceInfo.getTimestamp()));
                  savePerformer.startElement(XmlConstants.WORKSPACE_INFO, workspaceAttributes);
                  savePerformer.startElement(XmlConstants.MAPPED_PATHS);

                  for (WorkingFolderInfo folderInfo : workspaceInfo.getWorkingFoldersCached()) {
                    Map<String, String> pathAttributes = new HashMap<String, String>();
                    pathAttributes.put(XmlConstants.PATH_ATTR, folderInfo.getLocalPath().getPath());
                    savePerformer.writeElement(XmlConstants.MAPPED_PATH, pathAttributes, "");
                  }
                  savePerformer.endElement(XmlConstants.MAPPED_PATHS);
                  savePerformer.endElement(XmlConstants.WORKSPACE_INFO);
                }
                savePerformer.endElement(XmlConstants.SERVER_INFO);
              }
              savePerformer.endElement(XmlConstants.SERVERS);
              savePerformer.endElement(XmlConstants.ROOT);
            }
            catch (SAXException e) {
              LOG.info("Cannot update workspace cache", e);
            }
            //catch (Exception e) {
            //  LOG.warn("Cannot update workspace cache", e);
            //}
          }
        });
      }
      catch (IOException e) {
        LOG.info("Cannot update workspace cache", e);
      }
      catch (SAXException e) {
        LOG.info("Cannot update workspace cache", e);
      }
    }
  }

  public void addServer(final ServerInfo serverInfo) {
    myServerInfos.add(serverInfo);
    update();
  }

  public void removeServer(final ServerInfo serverInfo) {
    myServerInfos.remove(serverInfo);

    TFSConfigurationManager.getInstance().remove(serverInfo.getUri());
    update();
  }

  public synchronized static String getComputerName() {
    if (ourComputerName == null) {
      try {
        InetAddress address = InetAddress.getLocalHost();
        String hostName = address.getHostName();

        // Ideally, we should return an equivalent of .NET's Environment.MachineName which
        // "Gets the NetBIOS name of this local computer."
        // (see http://msdn.microsoft.com/en-us/library/system.environment.machinename.aspx)
        // All we can do is just strip DNS suffix

        int i = hostName.indexOf('.');
        if (i != -1) {
          hostName = hostName.substring(0, i);
        }
        ourComputerName = hostName;
      }
      catch (UnknownHostException e) {
        // must never happen
        throw new RuntimeException("Cannot retrieve host name.");
      }
    }
    return ourComputerName;
  }

  public Collection<WorkspaceInfo> findWorkspacesCached(final @NotNull FilePath localPath, boolean considerChildMappings) {
    // try cached working folders first
    Collection<WorkspaceInfo> result = new ArrayList<WorkspaceInfo>();
    for (WorkspaceInfo workspace : getAllWorkspacesForCurrentOwnerAndComputer(false)) {
      if (workspace.hasMappingCached(localPath, considerChildMappings)) {
        result.add(workspace);
        if (!considerChildMappings) {
          // optimization: same local path can't be mapped in different workspaces, so don't process other workspaces
          break;
        }
      }
    }
    return result;
  }

  public Collection<WorkspaceInfo> findWorkspaces(final @NotNull FilePath localPath,
                                                  boolean considerChildMappings,
                                                  Object projectOrComponent) throws TfsException {
    checkDuplicateMappings();
    final Collection<WorkspaceInfo> resultCached = findWorkspacesCached(localPath, considerChildMappings);
    if (!resultCached.isEmpty()) {
      // given path is mapped according to cached mapping info -> reload and check with server info
      for (WorkspaceInfo workspace : resultCached) {
        if (!workspace.hasMapping(localPath, considerChildMappings, projectOrComponent)) {
          throw new WorkspaceHasNoMappingException(workspace);
        }
      }
      return resultCached;
    }
    else {
      // TODO: exclude servers that are unavailable during current application run
      // not found in cached info, but workspaces may be out of date -> try to search all the workspaces reloaded
      Collection<WorkspaceInfo> result = new ArrayList<WorkspaceInfo>();
      Collection<ServerInfo> serversToSkip = new ArrayList<ServerInfo>();
      for (WorkspaceInfo workspace : getAllWorkspacesForCurrentOwnerAndComputer(true)) {
        if (serversToSkip.contains(workspace.getServer())) {
          // if server is somehow unavailable, don't try every workspace on it
          continue;
        }
        try {
          if (workspace.hasMapping(localPath, considerChildMappings, projectOrComponent)) {
            result.add(workspace);
            if (!considerChildMappings) {
              // optmimization: same local path can't be mapped in different workspaces, so don't process other workspaces
              return result;
            }
          }
        }
        catch (TfsException e) {
          // if some server failed, try next one, otherwise user will get strange error messages
          serversToSkip.add(workspace.getServer());
        }
      }
      return result;
    }
  }

  public void checkDuplicateMappings() throws DuplicateMappingException {
    if (myDuplicateMappedPath == null) {
      myDuplicateMappedPath = Ref.create(findDuplicateMappedPath());
    }
    //noinspection ConstantConditions
    if (!myDuplicateMappedPath.isNull()) {
      //noinspection ConstantConditions
      throw new DuplicateMappingException(myDuplicateMappedPath.get());
    }
  }

  private void invalidateDuplicateMappedPath() {
    myDuplicateMappedPath = null;
  }

  @Nullable
  private FilePath findDuplicateMappedPath() {
    // don't check duplicate mappings within the same server, server side should take care about this
    Collection<FilePath> otherServersPaths = new ArrayList<FilePath>();
    for (ServerInfo server : getServers()) {
      Collection<FilePath> currentServerPaths = new ArrayList<FilePath>();
      for (WorkspaceInfo workspace : server.getWorkspacesForCurrentOwnerAndComputer()) {
        for (WorkingFolderInfo workingFolder : workspace.getWorkingFoldersCached()) {
          final FilePath currentServerPath = workingFolder.getLocalPath();
          for (FilePath otherServerPath : otherServersPaths) {
            if (currentServerPath.isUnder(otherServerPath, false)) {
              return currentServerPath;
            }
            if (otherServerPath.isUnder(currentServerPath, false)) {
              return otherServerPath;
            }
          }
          currentServerPaths.add(currentServerPath);
        }
      }
      otherServersPaths.addAll(currentServerPaths);
    }
    return null;
  }

}
