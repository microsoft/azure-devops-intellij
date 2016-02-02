package org.jetbrains.tfsIntegration.tests.memento;

import com.intellij.openapi.util.ClassLoaderUtil;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.JdomKt;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.util.JDOMCompare;
import junit.framework.TestCase;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.tfsIntegration.checkin.Memento;
import org.jetbrains.tfsIntegration.checkin.XMLMemento;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;

// this test checks that IDEA's Memento implementation is identical to the Teamprise' one,
// so that checkin policy definitions have the same storage format

public class MementoTest extends TestCase {

  // TODO same code for XMLEntities as in JSUmlDataModelDump

  // there's no access to XmlEntities.properties file (rt.jar) from com.intellij.util.lang.UrlClassLoader that is used in tests
  private static final String XML_ENTITIES_URL = "com.sun.org.apache.xml.internal.serializer.XMLEntities";
  private static final String XML_ENTITIES_CONTENT = "lt 60\ngt 62\nquot 34\namp 38";


  private Memento ideaMemento;
  private com.teamprise.core.memento.Memento teampriseMemento;

  @Test
  public void testInitial() {
    Assert.assertEquals(ideaMemento.getBoolean("b"), teampriseMemento.getBoolean("b"));
    Assert.assertEquals(ideaMemento.getInteger("b"), teampriseMemento.getInteger("b"));
    Assert.assertEquals(ideaMemento.getFloat("b"), teampriseMemento.getFloat("b"));
    Assert.assertEquals(ideaMemento.getDouble("b"), teampriseMemento.getDouble("b"));
    Assert.assertEquals(ideaMemento.getString("b"), teampriseMemento.getString("b"));
    Assert.assertEquals(ideaMemento.getTextData(), teampriseMemento.getTextData());
  }

  @Test
  public void testBoolean() {
    ideaMemento.putBoolean("foo", Boolean.TRUE);
    teampriseMemento.putBoolean("foo", Boolean.TRUE);

    Assert.assertEquals(Boolean.TRUE, ideaMemento.getBoolean("foo"));
    Assert.assertEquals(Boolean.TRUE, teampriseMemento.getBoolean("foo"));

    ideaMemento.putBoolean("bar", Boolean.FALSE);
    teampriseMemento.putBoolean("bar", Boolean.FALSE);

    Assert.assertEquals(Boolean.FALSE, ideaMemento.getBoolean("bar"));
    Assert.assertEquals(Boolean.FALSE, teampriseMemento.getBoolean("bar"));
  }

  @Test
  public void testDouble() {
    final Double doubleValue = 1.3445;
    ideaMemento.putDouble("d", doubleValue);
    teampriseMemento.putDouble("d", doubleValue);
    Assert.assertEquals(doubleValue, ideaMemento.getDouble("d"));
    Assert.assertEquals(doubleValue, teampriseMemento.getDouble("d"));
  }

  @Test
  public void testFloat() {
    final Float floatValue = 34.5e3f;
    ideaMemento.putFloat("f", floatValue);
    teampriseMemento.putFloat("f", floatValue);

    Assert.assertEquals(floatValue, ideaMemento.getFloat("f"));
    Assert.assertEquals(floatValue, teampriseMemento.getFloat("f"));
  }

  @Test
  public void testInteger() throws Exception {
    final Integer intValue = 345346;
    ideaMemento.putInteger("i", intValue);
    teampriseMemento.putInteger("i", intValue);

    Assert.assertEquals(intValue, ideaMemento.getInteger("i"));
    Assert.assertEquals(intValue, teampriseMemento.getInteger("i"));

    checkEqual();
  }

  @Test
  public void testString() throws Exception {
    final String value = "sdklfjsdf+3450395634657234+}{<?_)(@+_~@`3-!@#@!#$$%^%$^&(*&<>><";
    ideaMemento.putString("s", value);
    teampriseMemento.putString("s", value);

    Assert.assertEquals(value, ideaMemento.getString("s"));
    Assert.assertEquals(value, teampriseMemento.getString("s"));

    checkEqual();
  }

  @Test
  public void testTextData() throws Exception {
    ideaMemento.putTextData("abcde la la<>3t5465$^#$^&U$%2344[]';?>><");
    teampriseMemento.putTextData("abcde la la<>3t5465$^#$^&U$%2344[]';?>><");
    checkEqual();
  }

  @Test
  public void testChild() throws Exception {
    final String name = "foobar";
    Memento ideaChild = ideaMemento.createChild(name);
    Assert.assertEquals(ideaChild.getName(), name);

    teampriseMemento.createChild(name);
    checkEqual();
  }

  @Test
  public void testChildren() throws Exception {
    final String name = "foobar";
    ideaMemento.createChild(name).putInteger("i", 132);
    ideaMemento.createChild(name).putString("s", "abc");
    final Memento ideaLastChild = ideaMemento.createChild(name);
    ideaLastChild.putFloat("foo", 12345);
    ideaLastChild.putTextData("zz");

    teampriseMemento.createChild(name).putInteger("i", 132);
    teampriseMemento.createChild(name).putString("s", "abc");
    final com.teamprise.core.memento.Memento teampriseLastChild = teampriseMemento.createChild(name);
    teampriseLastChild.putFloat("foo", 12345);
    teampriseLastChild.putTextData("zz");

    Assert.assertEquals(ideaMemento.getChildren(name).length, teampriseMemento.getChildren(name).length);
    checkEqual();
  }

  @Test
  public void testCopyChild() throws Exception {
    final Memento ideaChild = ideaMemento.createChild("asd");
    ideaChild.putString("cc", "vv");
    ideaChild.putTextData("qqq");
    ideaMemento.copyChild(ideaChild);

    final com.teamprise.core.memento.Memento teampriseChild = teampriseMemento.createChild("asd");
    teampriseChild.putString("cc", "vv");
    teampriseChild.putTextData("qqq");
    teampriseMemento.copyChild(teampriseChild);

    checkEqual();
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
    ideaMemento = createIdeaMemento();
    teampriseMemento = createTeampriseMemento();
  }

  @After
  public void tearDown() throws Exception {
    ideaMemento = null;
    teampriseMemento = null;
    super.tearDown();
  }

  protected static Memento createIdeaMemento() {
    return new XMLMemento(new Element("root"));
  }

  protected void checkEqual() throws Exception {
    final String s1 = serialize(ideaMemento);
    final String s2 = serialize(teampriseMemento);
    compareXml(s1, s2);

    final Memento idea = deserializeIdeaMemento(s1);
    final com.teamprise.core.memento.Memento teamprise = deserializeTeampriseMemento(s2);

    compareXml(serialize(idea), serialize(teamprise));
  }

  private static void compareXml(String s1, String s2) throws Exception {
    Element d1 = JdomKt.loadElement(s1);
    Element d2 = JdomKt.loadElement(s2);
    final String difference = JDOMCompare.diffElements(d1, d2);
    if (difference != null) {
      assertEquals(s1, s2); // will fail
    }
  }

  protected static com.teamprise.core.memento.Memento createTeampriseMemento() {
    return com.teamprise.core.memento.XMLMemento.createWriteRoot("root");
  }

  protected static String serialize(Memento ideaMemento) throws IOException {
    Document doc = new Document((Element)((XMLMemento)ideaMemento).getElement().clone());
    return JDOMUtil.writeDocument(doc, "");
  }

  protected static Memento deserializeIdeaMemento(String s) throws JDOMException, IOException {
    return new XMLMemento(JDOMUtil.loadDocument(s).getRootElement());
  }

  protected static com.teamprise.core.memento.Memento deserializeTeampriseMemento(final String s) throws Exception {
    return runWithPatchedClassloader(new ThrowableComputable<com.teamprise.core.memento.Memento, Exception>() {
      public com.teamprise.core.memento.Memento compute() throws Exception {
        // the same way as Teamprise does
        return com.teamprise.core.memento.XMLMemento.createReadRoot(new ByteArrayInputStream(s.getBytes("UTF-8")));
      }
    });
  }

  protected static String serialize(final com.teamprise.core.memento.Memento teampriseMemento) throws Exception {
    // the same way as Teamprise does
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    runWithPatchedClassloader(new ThrowableComputable<Void, Exception>() {
      public Void compute() throws Exception {
        ((com.teamprise.core.memento.XMLMemento)teampriseMemento).save(os);
        //noinspection ConstantConditions
        return null;
      }
    });
    return os.toString("UTF-8");
  }

  private static <T> T runWithPatchedClassloader(ThrowableComputable<T, Exception> computable) throws Exception {
    return ClassLoaderUtil.runWithClassLoader(new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader()) {
      @Override
      public InputStream getResourceAsStream(String name) {
        if (XML_ENTITIES_URL.equals(name)) {
          String resource = XML_ENTITIES_CONTENT;
          try {
            return new ByteArrayInputStream(resource.getBytes("UTF-8"));
          }
          catch (UnsupportedEncodingException e) {
            // should not happen, fallback
          }
        }
        return super.getResourceAsStream(name);
      }
    }, computable);
  }
}
