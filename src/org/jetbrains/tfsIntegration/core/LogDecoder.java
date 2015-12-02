package org.jetbrains.tfsIntegration.core;

import org.apache.commons.httpclient.ChunkedInputStream;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@SuppressWarnings("ALL")
public class LogDecoder {

  private LogDecoder() { }

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.out.println("Usage: LogDecoder idea.log output.log");
      return;
    }

    FileInputStream is = new FileInputStream(args[0]);
    try {
      OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(args[1]));
      try {
        List<Entry> entries = decode(is);
        for (Entry entry : entries) {
          os.write(entry.getText());
        }
      }
      finally {
        os.close();
      }
    }
    finally {
      is.close();
    }
  }

  public static List<Entry> decode(InputStream inputStream) throws IOException {
    List<Entry> result = new ArrayList<Entry>();

    BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
    String line;
    Map<MessageType, String> session = null;
    MessageType currentType = null;
    StringBuilder accumulated = new StringBuilder();
    while ((line = r.readLine()) != null) {
      MessageType lineType = null;
      for (MessageType type : MessageType.values()) {
        if (line.contains(type.getPrefix())) {
          lineType = type;
          int beginIndex = line.indexOf(type.getPrefix()) + type.getPrefix().length();
          int endIndex = line.lastIndexOf(type.getSuffix());
          line = line.substring(beginIndex, endIndex);
          break;
        }
      }
      if (lineType == null) {
        result.add(new UnknownEntry(line));
        continue;
      }

      if (session == null) {
        session = new HashMap<MessageType, String>();
      }
      else if (session.containsKey(currentType)) {
        result.add(new SessionEntry(session));
        session = new HashMap<MessageType, String>();
      }

      if (currentType != null && lineType != currentType) {
        byte[] unescaped = unescape(accumulated.toString());
        String unzipped = null;
        if (currentType == MessageType.ContentIn) {
          unzipped = tryUnzip(unescaped);
        }
        String display = unzipped != null ? unzipped : new String(unescaped);
        display = tryPrettyPrintXml(display);
        session.put(currentType, display);
        accumulated = new StringBuilder();
      }

      accumulated.append(line);
      currentType = lineType;
    }
    if (session != null) {
      result.add(new SessionEntry(session));
    }
    return result;
  }

  private static byte[] unescape(String escaped) throws IOException {
    // see org.apache.commons.httpclient.Wire.wire()

    ByteArrayOutputStream os = new ByteArrayOutputStream();

    for (int i = 0; i < escaped.length(); ) {
      if (escaped.startsWith("[\\r]", i)) {
        os.write(13);
        i += "[\\r]".length();
      }
      else if (escaped.startsWith("[\\n]", i)) {
        os.write(10);
        i += "[\\n]".length();
      }
      else if (escaped.startsWith("[0x", i)) {
        int closingBracket = escaped.indexOf(']', i);
        String hex = escaped.substring(i + 3, closingBracket);
        byte ch = (byte)(Integer.parseInt(hex, 16));
        os.write(ch);
        i = closingBracket + 1;
      }
      else {
        os.write(escaped.charAt(i));
        i++;
      }
    }
    return os.toByteArray();
  }

  private static boolean equal(byte[] a1, int a1start, byte[] a2, int length) {
    for (int i = 0; i < length; i++) {
      if (a1[a1start + i] != a2[i]) {
        return false;
      }
    }
    return true;
  }

  private static String tryUnzip(byte[] zipped) {
    try {
      ByteArrayInputStream is = new ByteArrayInputStream(zipped);
      ChunkedInputStream cs = new ChunkedInputStream(is);
      GZIPInputStream zs = new GZIPInputStream(cs);
      BufferedReader r = new BufferedReader(new InputStreamReader(zs));
      StringBuilder result = new StringBuilder();
      String line;
      while ((line = r.readLine()) != null) {
        result.append(line).append("\n");
      }
      return result.toString();
    }
    catch (IOException e) {
      if (!"Not in GZIP format".equals(e.getMessage())) {
        e.printStackTrace();
      }
      return null;
    }
  }

  private static void write(byte[] bytes, String file) {
    FileOutputStream f = null;
    try {
      f = new FileOutputStream(file);
      f.write(bytes);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    finally {
      if (f != null) {
        try {
          f.close();
        }
        catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }


  private static String tryPrettyPrintXml(String xml) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setValidating(true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      InputSource sourceXML = new InputSource(new StringReader(xml));
      Document xmlDoc = db.parse(sourceXML);
      Element e = xmlDoc.getDocumentElement();
      e.normalize();

      StringWriter result = new StringWriter();

      OutputFormat format = new OutputFormat(xmlDoc);
      format.setIndenting(true);
      format.setIndent(2);
      XMLSerializer serializer = new XMLSerializer(result, format);
      serializer.serialize(xmlDoc);

      return result.toString();
    }
    catch (Exception e) {
      return xml;
    }
  }

  private interface Entry {
    String getText();
  }

  private enum MessageType {
    HeaderOut("header - >> \"", "\""), ContentOut("content - >> \"", "\""), HeaderIn("header - << \"", "\""), ContentIn("content - << \"",
                                                                                                                        "\"");

    private final String myPrefix;
    private final String mySuffix;

    MessageType(String prefix, String suffix) {
      this.myPrefix = prefix;
      this.mySuffix = suffix;
    }

    public String getPrefix() {
      return myPrefix;
    }

    public String getSuffix() {
      return mySuffix;
    }

  }

  private static class SessionEntry implements Entry {

    private final Map<MessageType, String> mySession;

    public SessionEntry(Map<MessageType, String> session) {
      mySession = session;
    }

    public String getText() {
      StringBuilder s = new StringBuilder();
      for (MessageType t : MessageType.values()) {
        if (mySession.containsKey(t)) {
          String[] lines = mySession.get(t).split("\n");
          for (String line : lines) {
            if (line.length() > 0 && !"\n".equals(line.trim())) {
              s.append(t.getPrefix()).append(line).append("\n");
            }
          }
        }
      }
      return s.toString();
    }
  }

  private static class UnknownEntry implements Entry {
    private final String myText;

    public UnknownEntry(String text) {
      myText = text;
    }

    public String getText() {
      return myText + "\n";
    }
  }
}