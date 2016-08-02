package org.jetbrains.tfsIntegration.core.configuration;

import com.intellij.ide.ui.OptionsTopHitProvider;
import com.intellij.ide.ui.PublicMethodBasedOptionDescription;
import com.intellij.ide.ui.search.BooleanOptionDescription;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.impl.VcsDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Sergey.Malenkov
 */
public final class TFSOptionsTopHitProvider extends OptionsTopHitProvider {
  @Override
  public String getId() {
    return "vcs";
  }

  @NotNull
  @Override
  public Collection<BooleanOptionDescription> getOptions(@Nullable Project project) {
    if (project != null) {
      for (VcsDescriptor descriptor : ProjectLevelVcsManager.getInstance(project).getAllVcss()) {
        if ("TFS".equals(descriptor.getDisplayName())) {
          return Collections.unmodifiableCollection(Arrays.<BooleanOptionDescription>asList(
            new Option("TFS: Use HTTP Proxy settings", null, "useIdeaHttpProxy", "setUseIdeaHttpProxy"),
            new Option("TFS: Evaluate Team Explorer policies", "teamExplorer", null, "setSupportTfsCheckinPolicies"),
            new Option("TFS: Evaluate Teamprise policies", "teamprise", null, "setSupportStatefulCheckinPolicies"),
            new Option("TFS: Warn about not installed policies", "nonInstalled", null, "setReportNotInstalledCheckinPolicies")));
        }
      }
    }
    return Collections.emptyList();
  }

  private static final class Option extends PublicMethodBasedOptionDescription {
    private final String myField;

    Option(String option, String field, String getterName, String setterName) {
      super(option, "vcs.TFS", getterName, setterName);
      myField = field;
    }

    @Override
    public TFSConfigurationManager getInstance() {
      return TFSConfigurationManager.getInstance();
    }

    @Override
    public boolean isOptionEnabled() {
      if (myField == null) {
        return super.isOptionEnabled();
      }
      try {
        Object instance = getInstance().getCheckinPoliciesCompatibility();
        final Field field = instance.getClass().getField(myField);
        return field.getBoolean(instance);
      }
      catch (NoSuchFieldException ignore) {
      }
      catch (IllegalAccessException ignore) {
      }
      return false;
    }
  }
}
