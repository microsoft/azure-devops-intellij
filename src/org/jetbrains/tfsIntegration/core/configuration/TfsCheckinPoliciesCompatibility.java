package org.jetbrains.tfsIntegration.core.configuration;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.tfsIntegration.core.TFSConstants;

import java.io.IOException;

public class TfsCheckinPoliciesCompatibility {

  @NonNls private static final String ENFORCE_TEAM_EXPLORER_EVALUATION_ATTR = "enforceTfs";
  @NonNls private static final String ENFORCE_TEAMPRISE_EVALUATION_ATTR = "enforceTeamprise";
  @NonNls private static final String ENFORCE_NOT_INSTALLED_ATTR = "enforceNotInstalled";


  public boolean teamprise;
  public boolean teamExplorer;
  public boolean nonInstalled;

  public static final TfsCheckinPoliciesCompatibility NO_OVERRIDES = new TfsCheckinPoliciesCompatibility(false, false, false);

  public TfsCheckinPoliciesCompatibility(boolean teamprise, boolean teamExplorer, boolean nonInstalled) {
    this.teamprise = teamprise;
    this.teamExplorer = teamExplorer;
    this.nonInstalled = nonInstalled;
  }

  public static TfsCheckinPoliciesCompatibility fromOverridesAnnotationValue(String s) throws JDOMException, IOException {
    Document doc = JDOMUtil.loadDocument(s);
    boolean enforceTeamprise = Boolean.parseBoolean(doc.getRootElement().getAttributeValue(ENFORCE_TEAMPRISE_EVALUATION_ATTR));
    boolean enforceTeamExplorer = Boolean.parseBoolean(doc.getRootElement().getAttributeValue(ENFORCE_TEAM_EXPLORER_EVALUATION_ATTR));
    boolean enforceNonInstalledWarning = Boolean.parseBoolean(doc.getRootElement().getAttributeValue(ENFORCE_NOT_INSTALLED_ATTR));
    return new TfsCheckinPoliciesCompatibility(enforceTeamprise, enforceTeamExplorer, enforceNonInstalledWarning);
  }

  public String toOverridesAnnotationValue() {
    Document doc = new Document();
    Element element = new Element(TFSConstants.OVERRRIDES_ANNOTATION); // let it be the same as annotation name
    doc.setRootElement(element);
    element.setAttribute(ENFORCE_TEAMPRISE_EVALUATION_ATTR, Boolean.toString(teamprise));
    element.setAttribute(ENFORCE_TEAM_EXPLORER_EVALUATION_ATTR, Boolean.toString(teamExplorer));
    element.setAttribute(ENFORCE_NOT_INSTALLED_ATTR, Boolean.toString(nonInstalled));
    return JDOMUtil.writeDocument(doc, "");
  }

  public TfsCheckinPoliciesCompatibility overrideWith(TfsCheckinPoliciesCompatibility override) {
    return new TfsCheckinPoliciesCompatibility(teamprise | override.teamprise, teamExplorer | override.teamExplorer,
                                               nonInstalled | override.nonInstalled);
  }

  public boolean hasOverrides() {
    return teamprise || teamExplorer;
  }
}
