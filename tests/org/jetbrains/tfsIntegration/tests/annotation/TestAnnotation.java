package org.jetbrains.tfsIntegration.tests.annotation;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.annotate.LineAnnotationAspect;
import com.intellij.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import junit.framework.Assert;
import org.jetbrains.tfsIntegration.core.TFSChangeList;
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

    FilePath file = getChildPath(mySandboxRoot, "file.txt");
    createFileInCommand(file, contents[0]);
    commit();

    for (int i = 1; i < contents.length; i++) {
      editFiles(file);
      setFileContent(file, contents[i]);
      commit();
    }

    file.refresh();
    final FileAnnotation fileAnnotation = createTestAnnotation(getVcs().getAnnotationProvider(), file.getVirtualFile());
    Assert.assertEquals(fileAnnotation.getAnnotatedContent(), contents[contents.length - 1]);

    LineAnnotationAspect[] aspects = fileAnnotation.getAspects();
    Assert.assertEquals(aspects.length, 3);

    final RepositoryLocation location = getVcs().getCommittedChangesProvider().getLocationFor(file);
    final List<TFSChangeList> historyList =
      getVcs().getCommittedChangesProvider().getCommittedChanges(new ChangeBrowserSettings(), location, 0);

    for (int line = 0; line < expectedRevisions.length; line++) {
      Assert.assertEquals(aspects[0].getValue(line),
                          String.valueOf(historyList.get(contents.length - 1 - expectedRevisions[line]).getNumber()));
    }
    Assert.assertEquals(aspects[0].getValue(expectedRevisions.length), "");
  }

}
