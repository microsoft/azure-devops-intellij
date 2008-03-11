package org.jetbrains.tfsIntegration.core.revision;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * Date: 10.03.2008
 * Time: 14:42:11
 */
public class TFSContentStoreFactory {

  public static TFSContentStore create(final FilePath path, final VcsRevisionNumber.Int revision) throws IOException {
    return new TFSTmpFileStore(path, revision.getValue());
  }

  public static TFSContentStore find(final FilePath path, final VcsRevisionNumber.Int revision) throws IOException {
    return TFSTmpFileStore.find(path, revision.getValue());
  }
}

