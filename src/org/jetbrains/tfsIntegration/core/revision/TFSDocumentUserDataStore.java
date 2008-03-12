package org.jetbrains.tfsIntegration.core.revision;

import com.intellij.openapi.vcs.FilePath;

import java.io.IOException;

// TODO: implement
public class TFSDocumentUserDataStore implements TFSContentStore {

  public static TFSContentStore find(final FilePath path, final int revision) throws IOException {
    return null;
  }

  TFSDocumentUserDataStore(final FilePath path, final int revision) throws IOException {
  }

  public void saveContent(final ContentWriter contentWriter) throws IOException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public String loadContent() throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}