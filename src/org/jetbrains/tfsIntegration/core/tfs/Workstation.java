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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import org.apache.axis2.databinding.utils.ConverterUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

  // TODO: where is the file if we're not on Windows?
  @NonNls private static final String CONFIG_FILE =
    "Local Settings\\Application Data\\Microsoft\\Team Foundation\\1.0\\Cache\\VersionControl.config";

  private static Workstation ourInstance;

  private static final Logger LOG = Logger.getInstance(Workstation.class.getName());

  private List<ServerInfo> myServerInfos;

  private Workstation() {
    myServerInfos = loadCache();
  }

  public static Workstation getInstance() {
    if (ourInstance == null) {
      ourInstance = new Workstation();
    }
    return ourInstance;
  }

  public List<ServerInfo> getServers() {
    return Collections.unmodifiableList(myServerInfos);
  }

  @Nullable
  public ServerInfo getServer(URI uri) {
    for (ServerInfo server : myServerInfos) {
      if (server.getUri().equals(uri)) {
        return server;
      }
    }
    return null;
  }

  public List<WorkspaceInfo> getAllWorkspacesForCurrentOwner() {
    List<WorkspaceInfo> result = new ArrayList<WorkspaceInfo>();
    for (ServerInfo serverInfo : getServers()) {
      result.addAll(serverInfo.getWorkspacesForCurrentOwner());
    }
    return result;
  }

  private static List<ServerInfo> loadCache() {
    // TODO: validate against schema
    File cacheFile = getExistingCacheFile();
    if (cacheFile != null) {
      try {
        WorkstationCacheReader reader = new WorkstationCacheReader();
        XmlUtil.parseFile(cacheFile, reader);
        return reader.getServers();
      }
      catch (IOException e) {
        LOG.info("Failed to read workspaces cache file", e);
      }
      catch (SAXException e) {
        LOG.info("Failed to read workspaces cache file", e);
      }
      catch (RuntimeException e) {
        LOG.info("Failed to read workspaces cache file", e);
      }
      catch (Exception e) {
        LOG.info("Failed to read workspaces cache file", e);
      }
    }
    return new ArrayList<ServerInfo>();
  }


  /**
   * @return not null if file exists now
   */
  @Nullable
  private static File getExistingCacheFile() {
    //noinspection HardCodedStringLiteral
    File file = new File(System.getProperty("user.home"), CONFIG_FILE);
    if (!file.exists()) {
      file.getParentFile().mkdirs();
      LOG.info("WorkspaceInfo cache file " + file + " not exists, creating");
      try {
        XmlUtil.saveFile(file, new XmlUtil.SaveDelegate() {

          public void doSave(XmlUtil.SavePerformer savePerformer) {
            try {
              savePerformer.startElement(XmlConstants.ROOT);
              savePerformer.writeElement(XmlConstants.SERVERS, "");
              savePerformer.endElement(XmlConstants.ROOT);
            }
            catch (SAXException e) {
              LOG.warn("Failed to create workspaces cache file", e);
            }
          }
        });
      }
      catch (IOException e) {
        LOG.warn("Failed to create workspaces cache file", e);
      }
      catch (SAXException e) {
        LOG.warn("Failed to create workspaces cache file", e);
      }
    }
    return file;
  }

  void updateCacheFile() {
    File cacheFile = getExistingCacheFile();
    if (cacheFile != null) {
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
                  List<WorkingFolderInfo> workingFolders;
                  try {
                    workingFolders = workspaceInfo.getWorkingFolders();
                  }
                  catch (TfsException e) {
                    LOG.info("Failed to update workspace " + workspaceInfo.getName(), e);
                    continue;
                  }

                  Map<String, String> workspaceAttributes = new HashMap<String, String>();
                  workspaceAttributes.put(XmlConstants.NAME_ATTR, workspaceInfo.getName());
                  workspaceAttributes.put(XmlConstants.OWNER_NAME_ATTR, workspaceInfo.getOwnerName());
                  workspaceAttributes.put(XmlConstants.COMPUTER_ATTR, workspaceInfo.getComputer());
                  workspaceAttributes.put(XmlConstants.COMMENT_ATTR, workspaceInfo.getComment());
                  workspaceAttributes.put(XmlConstants.TIMESTAMP_ATTR, ConverterUtil.convertToString(workspaceInfo.getTimestamp()));
                  savePerformer.startElement(XmlConstants.WORKSPACE_INFO, workspaceAttributes);
                  savePerformer.startElement(XmlConstants.MAPPED_PATHS);

                  for (WorkingFolderInfo folderInfo : workingFolders) {
                    Map<String, String> pathAttributes = new HashMap<String, String>();
                    pathAttributes.put(XmlConstants.PATH_ATTR, VersionControlPath.toTfsRepresentation(folderInfo.getLocalPath()));
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
              LOG.info("Failed to update workspaces cache file", e);
            }
            //catch (Exception e) {
            //  LOG.warn("Failed to update workspaces cache file", e);
            //}
          }
        });
      }
      catch (IOException e) {
        LOG.info("Failed to update workspaces cache file", e);
      }
      catch (SAXException e) {
        LOG.info("Failed to update workspaces cache file", e);
      }
    }
  }

  public void addServer(final ServerInfo serverInfo) {
    myServerInfos.add(serverInfo);
    updateCacheFile();
  }

  public void removeServer(final ServerInfo serverInfo) {
    myServerInfos.remove(serverInfo);
    updateCacheFile();
  }

  public static String getComputerName() {
    try {
      InetAddress address = InetAddress.getLocalHost();
      return address.getHostName();
    }
    catch (UnknownHostException e) {
      // must never happen
      throw new RuntimeException("Cannot get hostname!");
    }
  }

  public Collection<WorkspaceInfo> findWorkspace(final @NotNull FilePath localPath, boolean considerChildMappings) throws TfsException {
    // try cached working folders first
    Collection<WorkspaceInfo> resultCached = new ArrayList<WorkspaceInfo>();
    for (WorkspaceInfo workspace : getAllWorkspacesForCurrentOwner()) {
      if (workspace.hasMappingCached(localPath, considerChildMappings)) {
        resultCached.add(workspace);
        if (!considerChildMappings) {
          // optmimization: same local path can't be mapped in different workspaces, so don't process other workspaces
          break;
        }
      }
    }

    if (!resultCached.isEmpty()) {
      // given path is mapped according to cached mapping info -> reload and check with server info
      for (WorkspaceInfo workspace : resultCached) {
        if (!workspace.hasMapping(localPath, considerChildMappings)) {
          throw new WorkspaceHasNoMappingException(workspace);
        }
      }
      return resultCached;
    }
    else {
      // TODO: if server is unavailable, don't try every workspace on it
      // TODO: exclude servers that are unavailable during current application run
      // not found in cached info, but workspaces may be out of date -> try to search all the workspaces reloaded
      Collection<WorkspaceInfo> result = new ArrayList<WorkspaceInfo>();
      for (WorkspaceInfo workspace : getAllWorkspacesForCurrentOwner()) {
        try {
          if (workspace.hasMapping(localPath, considerChildMappings)) {
            result.add(workspace);
            if (!considerChildMappings) {
              // optmimization: same local path can't be mapped in different workspaces, so don't process other workspaces
              return result;
            }
          }
        }
        catch (TfsException e) {
          // skip
        }
      }
      return result;
    }
  }

}
