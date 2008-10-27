package org.jetbrains.tfsIntegration.tests.annotation;

import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.annotate.LineAnnotationAspect;
import com.intellij.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import com.intellij.openapi.vfs.VirtualFile;
import junit.framework.Assert;
import org.jetbrains.tfsIntegration.core.TFSChangeList;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.tests.TFSTestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

@SuppressWarnings({"HardCodedStringLiteral"})
public class TestAnnotation extends TFSTestCase {

  @Test
  public void testAnnotations() throws VcsException, IOException {
    final String[] contents = {"", "1\r1\r1\r1\r1\n", "\r\n2\r\n2\n2\r\n1\r1\n2\n1\r\n\r", "\r\n\n2\r\n2\n2\r\n1\r1\r\n3\r3\n\r\n"};
    final int[] expectedRevisions = {2, 3, 2, 2, 2, 1, 1, 3, 3, 2};

    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);

    VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", contents[0]);
    commit(getChanges().getChanges(), "initial revision");

    for (int i = 1; i < contents.length; i++) {
      editFiles(file);
      setFileContent(file, contents[i]);
      commit(getChanges().getChanges(), "content " + i);
    }

    final RepositoryLocation location = getVcs().getCommittedChangesProvider().getLocationFor(TfsFileUtil.getFilePath(file));
    final List<TFSChangeList> historyList =
      getVcs().getCommittedChangesProvider().getCommittedChanges(new ChangeBrowserSettings(), location, 0);

    final FileAnnotation fileAnnotation = getVcs().getAnnotationProvider().annotate(file);

    Assert.assertEquals(fileAnnotation.getAnnotatedContent(), contents[contents.length - 1]);

    LineAnnotationAspect[] aspects = fileAnnotation.getAspects();
    Assert.assertEquals(aspects.length, 3);

    for (int line = 0; line < expectedRevisions.length; line++) {
      Assert.assertEquals(aspects[0].getValue(line),
                          String.valueOf(historyList.get(contents.length - 1 - expectedRevisions[line]).getNumber()));
    }
    Assert.assertEquals(aspects[0].getValue(expectedRevisions.length), "");
  }

}
