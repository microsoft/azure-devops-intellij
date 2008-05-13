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

import com.intellij.vcsUtil.VcsUtil;
import org.apache.axis2.databinding.utils.ConverterUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class WorkstationCacheReader extends DefaultHandler {

  private final List<ServerInfo> myServerInfos = new ArrayList<ServerInfo>();

  private ServerInfo myCurrentServerInfo;
  private WorkspaceInfo myCurrentWorkspaceInfo;

  public void error(SAXParseException e) throws SAXException {
    throw e;
  }

  public void fatalError(SAXParseException e) throws SAXException {
    throw e;
  }

  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    if (XmlConstants.SERVER_INFO.equals(qName)) {
      try {
        myCurrentServerInfo =
          new ServerInfo(new URI(attributes.getValue(XmlConstants.URI_ATTR)), attributes.getValue(XmlConstants.GUID_ATTR));
      }
      catch (URISyntaxException e) {
        throw new SAXException(e);
      }
    }
    else if (XmlConstants.WORKSPACE_INFO.equals(qName)) {
      String name = attributes.getValue(XmlConstants.NAME_ATTR);
      String owner = attributes.getValue(XmlConstants.OWNER_NAME_ATTR);
      String computer = attributes.getValue(XmlConstants.COMPUTER_ATTR);
      String comment = attributes.getValue(XmlConstants.COMMENT_ATTR);
      Calendar timestamp = ConverterUtil.convertToDateTime(attributes.getValue(XmlConstants.TIMESTAMP_ATTR));
      myCurrentWorkspaceInfo = new WorkspaceInfo(myCurrentServerInfo, name, owner, computer, comment, timestamp);
    }
    else if (XmlConstants.MAPPED_PATH.equals(qName)) {
      myCurrentWorkspaceInfo.addWorkingFolderInfo(new WorkingFolderInfo(VcsUtil.getFilePath(attributes.getValue(XmlConstants.PATH_ATTR))));
    }
  }

  public void endElement(String uri, String localName, String qName) throws SAXException {
    if (XmlConstants.SERVER_INFO.equals(qName)) {
      myServerInfos.add(myCurrentServerInfo);
      myCurrentServerInfo = null;
    }
    else if (XmlConstants.WORKSPACE_INFO.equals(qName)) {
      myCurrentServerInfo.addWorkspaceInfo(myCurrentWorkspaceInfo);
      //myCurrentWorkspaceInfo.setWorkingFoldersCached(true);
      myCurrentWorkspaceInfo = null;
    }
  }

  public List<ServerInfo> getServers() {
    return myServerInfos;
  }
}
