package org.jetbrains.tfsIntegration.core.revision;

import java.io.IOException;
import java.io.OutputStream;

public interface TFSContentStore {
  interface ContentWriter {
    void write(OutputStream outputStream);
  }
  
  void saveContent(ContentWriter contentWriter) throws IOException;

  String loadContent() throws IOException;
}
