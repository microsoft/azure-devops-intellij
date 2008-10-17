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

package org.jetbrains.tfsIntegration.core.tfs.version;

import org.apache.axiom.om.OMFactory;
import org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

public class WorkspaceVersionSpec extends VersionSpecBase {

  private String workspaceName;

  private String workspaceOwnerName;

  public WorkspaceVersionSpec(final String workspaceName, final String workspaceOwnerName) {
    this.workspaceName = workspaceName;
    this.workspaceOwnerName = workspaceOwnerName;
  }

  protected void writeAttributes(final QName parentQName, final OMFactory factory, final MTOMAwareXMLStreamWriter xmlWriter)
    throws XMLStreamException {
    writeVersionAttribute("", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance", xmlWriter);
    writeVersionAttribute("", "xsi:type", "WorkspaceVersionSpec", xmlWriter);
    writeVersionAttribute("", "name", workspaceName, xmlWriter);
    writeVersionAttribute("", "owner", workspaceOwnerName, xmlWriter);
  }

  public String getWorkspaceName() {
    return workspaceName;
  }

  public String getWorkspaceOwnerName() {
    return workspaceOwnerName;
  }

  public String getPresentableString() {
    return workspaceName + ';' + workspaceOwnerName;
  }

}