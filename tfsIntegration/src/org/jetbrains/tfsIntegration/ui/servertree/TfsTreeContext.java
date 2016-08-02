package org.jetbrains.tfsIntegration.ui.servertree;

import com.intellij.openapi.util.Condition;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Item;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.util.List;

public class TfsTreeContext {

  public final ServerInfo myServer;

  private final boolean myFoldersOnly;
  private final Object myProjectOrComponent;
  @Nullable
  private final Condition<String> myFilter;

  public TfsTreeContext(ServerInfo server, boolean foldersOnly, Object projectOrComponent, Condition<String> filter) {
    myServer = server;
    myFoldersOnly = foldersOnly;
    myFilter = filter;
    myProjectOrComponent = projectOrComponent;
  }

  public boolean isAccepted(String path) {
    return myFilter == null || myFilter.value(path);
  }

  public List<Item> getChildItems(String path) throws TfsException {
    return myServer.getVCS().getChildItems(path, myFoldersOnly, myProjectOrComponent, TFSBundle.message("loading.items"));
  }
}
