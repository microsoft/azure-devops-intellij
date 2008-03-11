package org.jetbrains.tfsIntegration.core.revision;

import java.io.IOException;

public interface TFSContentStore {
  void saveContent(final String content) throws IOException;

  String loadContent() throws IOException;
}
