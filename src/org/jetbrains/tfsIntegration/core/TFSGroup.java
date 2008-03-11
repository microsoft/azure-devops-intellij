package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.actions.StandardVcsGroup;

/**
 * Created by IntelliJ IDEA.
 * Date: 27.01.2008
 * Time: 17:25:40
 */
public class TFSGroup extends StandardVcsGroup {

  public AbstractVcs getVcs(Project project) {
    return TFSVcs.getInstance(project);
  }

  @Override
  public String getVcsName(final Project project) {
    return "TFS";
  }
}
