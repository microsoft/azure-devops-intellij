/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jetbrains.tfsIntegration.checkin;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StatefulPolicyParser {
  private static final String VERSION = "version";
  private static final String POLICY_ANNOTATION = "policy-annotation";


  private static final String CURRENT_VERSION = "1";
  private static final String POLICY_DEFINITION = "policy-definition";
  private static final String ENABLED = "enabled";
  private static final String PRIORITY = "priority";
  private static final String POLICY_TYPE = "policy-type";
  private static final String ID = "id";
  private static final String NAME = "name";
  private static final String SHORT_DESCRIPTION = "short-description";
  private static final String LONG_DESCRIPTION = "long-description";
  private static final String INSTALLATION_INSTRUCTIONS = "installation-instructions";
  private static final String CONFIGURATION_DATA = "configuration-data";
  private static final String SCOPE = "scope";

  public static List<StatefulPolicyDescriptor> parseDescriptors(String input) throws PolicyParseException {
    final Document document;
    try {
      document = JDOMUtil.loadDocument(input);
    }
    catch (IOException e) {
      throw new PolicyParseException(e);
    }
    catch (JDOMException e) {
      throw new PolicyParseException(e);
    }
    if (!POLICY_ANNOTATION.equals(document.getRootElement().getName())) {
      throw new PolicyParseException("Element expected: " + POLICY_ANNOTATION);
    }

    //if (!CURRENT_VERSION.equals(document.getRootElement().getAttributeValue(VERSION))) {
    //  throw new PolicyParseException("Unsupported version");
    //}

    List<StatefulPolicyDescriptor> result = new ArrayList<StatefulPolicyDescriptor>();
    for (Object o : document.getRootElement().getChildren(POLICY_DEFINITION)) {
      Element definitionElement = (Element)o;
      //if (!CURRENT_VERSION.equals(definitionElement.getAttributeValue(VERSION))) {
      //  throw new PolicyParseException("Unsupported version");
      //}
      final String enabled = definitionElement.getAttributeValue(ENABLED);
      checkNotNull(enabled, ENABLED);
      final String priority = definitionElement.getAttributeValue(PRIORITY);
      checkNotNull(priority, PRIORITY);
      Element typeElement = definitionElement.getChild(POLICY_TYPE);
      checkNotNull(typeElement, POLICY_TYPE);
      String id = typeElement.getAttributeValue(ID);
      checkNotNull(id, ID);
      String name = typeElement.getAttributeValue(NAME);
      checkNotNull(name, NAME);
      String shortDescription = typeElement.getAttributeValue(SHORT_DESCRIPTION);
      String longDescription = typeElement.getAttributeValue(LONG_DESCRIPTION);
      String installationInstructions = typeElement.getAttributeValue(INSTALLATION_INSTRUCTIONS);
      PolicyType type = new PolicyType(id, name, shortDescription, installationInstructions);
      Element configurationElement = definitionElement.getChild(CONFIGURATION_DATA);
      checkNotNull(configurationElement, CONFIGURATION_DATA);
      List<String> scope = new ArrayList<String>();
      for (Object o2 : definitionElement.getChildren(SCOPE)) {
        Element scopeElement = (Element)o2;
        scope.add(scopeElement.getText());
      }
      result.add(new StatefulPolicyDescriptor(type, Boolean.parseBoolean(enabled), configurationElement, scope, priority, longDescription));
    }
    return result;
  }

  public static Element createEmptyConfiguration() {
    return new Element(CONFIGURATION_DATA);
  }

  public static String saveDescriptors(List<StatefulPolicyDescriptor> value) {
    Element root = new Element(POLICY_ANNOTATION);
    root.setAttribute(VERSION, CURRENT_VERSION);
    for (StatefulPolicyDescriptor descriptor : value) {
      Element descriptorElement = new Element(POLICY_DEFINITION);
      root.addContent(descriptorElement);

      descriptorElement.setAttribute(ENABLED, String.valueOf(descriptor.isEnabled()));
      descriptorElement.setAttribute(PRIORITY, descriptor.getPriority());
      descriptorElement.setAttribute(VERSION, CURRENT_VERSION);

      for (String scope : descriptor.getScope()) {
        Element scopeElement = new Element(SCOPE);
        scopeElement.setText(scope);
        descriptorElement.addContent(scopeElement);
      }

      Element typeElement = new Element(POLICY_TYPE);
      typeElement.setAttribute(ID, descriptor.getType().getId());
      typeElement.setAttribute(INSTALLATION_INSTRUCTIONS, descriptor.getType().getInstallationInstructions());
      typeElement.setAttribute(LONG_DESCRIPTION, descriptor.getLongDescription());
      typeElement.setAttribute(NAME, descriptor.getType().getName());
      typeElement.setAttribute(SHORT_DESCRIPTION, descriptor.getType().getDescription());
      descriptorElement.addContent(typeElement);

      descriptorElement.addContent((Element)descriptor.getConfiguration().clone());
    }
    Document document = new Document(root);
    return JDOMUtil.writeDocument(document, "");
  }

  private static void checkNotNull(Element element, String expectedElementName) throws PolicyParseException {
    if (element == null) {
      throw new PolicyParseException("Element expected: " + expectedElementName);
    }
  }

  private static void checkNotNull(String value, String expectedElementName) throws PolicyParseException {
    if (value == null) {
      throw new PolicyParseException("Attribute expected: " + expectedElementName);
    }
  }
}
