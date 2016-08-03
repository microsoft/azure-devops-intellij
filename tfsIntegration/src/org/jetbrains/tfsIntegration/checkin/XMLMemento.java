package org.jetbrains.tfsIntegration.checkin;

import org.jdom.Element;

import java.util.List;

/**
 * An XML-based implementation of {@link Memento} interface. It just decorates a JDOM implementation.
 */
public class XMLMemento implements Memento {

  private Element myElement;

  public XMLMemento(Element element) {
    myElement = element;
  }

  public Memento createChild(String nodeName) {
    Element element = new Element(nodeName);
    myElement.addContent(element);
    return new XMLMemento(element);
  }

  public Memento copyChild(Memento child) {
    Element element = (Element)((XMLMemento)child).myElement.clone();
    myElement.addContent(element);
    return new XMLMemento(element);
  }

  public Memento getChild(String nodeName) {
    final Element element = myElement.getChild(nodeName);
    return element != null ? new XMLMemento(element) : null;
  }

  public Memento[] getChildren(String nodeName) {
    final List elements = myElement.getChildren(nodeName);
    Memento result[] = new Memento[elements.size()];
    for (int i = 0; i < elements.size(); i++) {
      result[i] = new XMLMemento((Element)elements.get(i));
    }

    return result;
  }

  public String getName() {
    return myElement.getName();
  }

  public Double getDouble(String key) {
    final String s = myElement.getAttributeValue(key);
    if (s != null) {
      try {
        return new Double(s);
      }
      catch (NumberFormatException e) {
        // fallback to null
      }
    }
    return null;
  }

  public Float getFloat(String key) {
    final String s = myElement.getAttributeValue(key);
    if (s != null) {
      try {
        return new Float(s);
      }
      catch (NumberFormatException e) {
        // fallback to null
      }
    }
    return null;
  }

  public Integer getInteger(String key) {
    final String s = myElement.getAttributeValue(key);
    if (s != null) {
      try {
        return new Integer(s);
      }
      catch (NumberFormatException e) {
        // fallback to null
      }
    }
    return null;
  }

  public Long getLong(String key) {
    final String s = myElement.getAttributeValue(key);
    if (s != null) {
      try {
        return new Long(s);
      }
      catch (NumberFormatException e) {
        // fallback to null
      }
    }
    return null;
  }

  public String getString(String key) {
    return myElement.getAttributeValue(key);
  }

  public Boolean getBoolean(String key) {
    final String s = myElement.getAttributeValue(key);
    if (s != null) {
      return Boolean.valueOf(s);
    }
    return null;
  }

  public String getTextData() {
    String text = myElement.getText();
    if (text != null && text.length() == 0) {
      text = null;
    }
    return text;
  }

  public void putDouble(String key, double value) {
    myElement.setAttribute(key, String.valueOf(value));
  }

  public void putFloat(String key, float value) {
    myElement.setAttribute(key, String.valueOf(value));
  }

  public void putInteger(String key, int value) {
    myElement.setAttribute(key, String.valueOf(value));
  }

  public void putLong(String key, long value) {
    myElement.setAttribute(key, String.valueOf(value));
  }

  public void putMemento(Memento memento) {
    myElement = (Element)((XMLMemento)memento).myElement.clone();
  }

  public void putString(String key, String value) {
    myElement.setAttribute(key, value);
  }

  public void putBoolean(String key, boolean value) {
    myElement.setAttribute(key, String.valueOf(value));
  }

  public void putTextData(String data) {
    myElement.setText(data);
  }

  // used in tests
  public Element getElement() {
    return myElement;
  }

}
