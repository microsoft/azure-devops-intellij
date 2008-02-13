package org.jetbrains.tfsIntegration.xmlutil;

import org.jetbrains.annotations.NonNls;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Collections;
import java.util.Map;

@SuppressWarnings({"CallToPrintStackTrace", "ThrowFromFinallyBlock", "CaughtExceptionImmediatelyRethrown"})
public class XmlUtil {

  @NonNls protected static final String STRING_ATTRIBUTE_TYPE = "CDATA";

  private XmlUtil() {
  }

  public interface SavePerformer {
    void startElement(String name) throws SAXException;

    void startElement(String name, Map<String, String> attributes) throws SAXException;

    void endElement(String name) throws SAXException;

    void writeText(String text) throws SAXException;

    void writeElement(String name, Map<String, String> attributes, String innerText) throws SAXException;

    void writeElement(String name, String innerText) throws SAXException;
  }

  public interface SaveDelegate {
    void doSave(SavePerformer savePerformer);
  }

  public static void parseFile(File file, DefaultHandler handler) throws IOException, SAXException {
    InputStream is = null;
    boolean read = false;
    try {
      is = new BufferedInputStream(new FileInputStream(file));
      SAXParser p = SAXParserFactory.newInstance().newSAXParser();
      p.parse(new InputSource(is), handler);
      read = true;
    }
    catch (ParserConfigurationException e) {
      e.printStackTrace();
    }
    finally {
      if (is != null) {
        try {
          is.close();
        }
        catch (IOException e) {
          if (read) {
            throw e;
          }
        }
      }
    }
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public static void saveFile(File file, SaveDelegate saveDelegate) throws IOException, SAXException {
    OutputStream os = null;
    boolean saved = false;
    try {
      os = new BufferedOutputStream(new FileOutputStream(file));
      SAXTransformerFactory transformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
      TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();
      Transformer serializer = transformerHandler.getTransformer();
      serializer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformerHandler.setResult(new StreamResult(os));
      transformerHandler.startDocument();
      saveDelegate.doSave(new SavePerformerImpl(transformerHandler));
      transformerHandler.endDocument();
      saved = true;
    }
    catch (TransformerConfigurationException e) {
      e.printStackTrace();
    }
    finally {
      if (os != null) {
        try {
          os.close();
        }
        catch (IOException e) {
          if (saved) {
            throw e;
          }
        }
      }
    }
  }

  private static class SavePerformerImpl implements SavePerformer {

    private final ContentHandler myContentHandler;

    public SavePerformerImpl(ContentHandler contentHandler) {
      myContentHandler = contentHandler;
    }

    public void startElement(String name) throws SAXException {
      startElement(name, Collections.<String, String>emptyMap());
    }

    public void startElement(String name, Map<String, String> attributes) throws SAXException {
      AttributesImpl attrs = new AttributesImpl();
      for (Map.Entry<String, String> e : attributes.entrySet()) {
        attrs.addAttribute("", "", e.getKey(), STRING_ATTRIBUTE_TYPE, e.getValue());
      }
      myContentHandler.startElement("", "", name, attrs);
    }

    public void endElement(String name) throws SAXException {
      myContentHandler.endElement("", "", name);
    }

    public void writeText(String text) throws SAXException {
      final char[] chars = text.toCharArray();
      myContentHandler.characters(chars, 0, chars.length);
    }

    public void writeElement(String name, Map<String, String> attributes, String innerText) throws SAXException {
      startElement(name, attributes);
      writeText(innerText);
      endElement(name);
    }

    public void writeElement(String name, String innerText) throws SAXException {
      writeElement(name, Collections.<String, String>emptyMap(), innerText);
    }


  }

}
