package org.jetbrains.tfsIntegration.exceptions;

import org.apache.axis2.AxisFault;

import javax.xml.namespace.QName;

public class WorkspaceNotFoundException extends TfsException {

  private static final long serialVersionUID = 1L;

  public static final String CODE = "WorkspaceNotFoundException";

  private final String myWorkspaceOwner;

  private final String myWorkspaceName;

  public WorkspaceNotFoundException(final AxisFault cause) {
    super(cause);

    if (cause.getDetail() != null) {
      myWorkspaceOwner = cause.getDetail().getAttributeValue(new QName("WorkspaceOwner"));
      myWorkspaceName = cause.getDetail().getAttributeValue(new QName("WorkspaceName"));
    }
    else {
      myWorkspaceOwner = null;
      myWorkspaceName = null;
    }

  }

  public String getWorkspaceName() {
    return myWorkspaceName;
  }

  public String getWorkspaceOwner() {
    return myWorkspaceOwner;
  }
}
