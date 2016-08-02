package org.jetbrains.tfsIntegration.checkin;

/**
 * This interface and its implementation, {@link XMLMemento}, are intended to follow an API and behaviour
 * of their namesakes classes from <a href="http://labs.teamprise.com/policysdk/">Teamprise checkin policy SDK</a>.
 * <p/>
 * Original class javadoc:
 * <p/>
 * This class has the same design as Eclipse's Memento interface.
 * A Memento can be used for saving the state of an object into a hierarchical form that easily be persisted (via XML, or any other means).
 * Mementos form trees, and each tree node can have data attached.
 * <p/>
 * In short, things you can do with a Memento:
 * <ul>
 * <li>Add data to it by a String key</li>
 * <li>Get data by String key</li>
 * <li>Create child Memento objects</li>
 * <li>Get its child Memento objects</li>
 * <ul>
 *
 * @see MementoStoredPolicyBase
 */
public interface Memento {

  /**
   * Creates a new child of this memento with the given name.
   *
   * @param nodeName the name of the memento node
   * @return a new child memento
   */
  Memento createChild(String nodeName);

  /**
   * Copies a child into a new memento.
   *
   * @param child the child to copy
   * @return a copy of the child.
   */
  Memento copyChild(Memento child);

  /**
   * Returns the first child with the given node name
   *
   * @param nodeName the nodename of the memento node to get
   * @return the first child with the given node name
   */
  Memento getChild(String nodeName);

  /**
   * Returns all children with the given node name
   *
   * @param nodeName the name of the memento nodes to get
   * @return an array of children with the given node name
   */
  Memento[] getChildren(String nodeName);

  /**
   * @return this memento's name (the nodeName it was created with).
   */
  String getName();


  /**
   * Returns the double floating point value of the given key.
   *
   * @param key the key
   * @return the value, or <code>null</code> if the key was not found or was found but was not a floating point number
   */
  Double getDouble(String key);

  /**
   * Returns the floating point value of the given key.
   *
   * @param key the key
   * @return the value, or <code>null</code> if the key was not found or was found but was not a floating point number
   */
  Float getFloat(String key);

  /**
   * Returns the integer value of the given key.
   *
   * @param key the key
   * @return the value, or <code>null</code> if the key was not found or was found but was not an integer
   */
  Integer getInteger(String key);

  /**
   * Returns the long integer value of the given key.
   *
   * @param key the key
   * @return the value, or <code>null</code> if the key was not found or was found but was not an integer
   */
  Long getLong(String key);

  /**
   * Returns the string value of the given key.
   *
   * @param key the key
   * @return the value, or <code>null</code> if the key was not found
   */
  String getString(String key);

  /**
   * Returns the Boolean value of the given key
   *
   * @param key the key
   * @return the value, or <code>null</code> if the key was not found
   */
  Boolean getBoolean(String key);

  /**
   * Returns the data of the Text node of the memento. Each memento is allowed only one Text node.
   *
   * @return the data of the Text node of the memento, or <code>null</code> if the memento has no Text node.
   */
  String getTextData();

  /**
   * Sets the value of the given key to the given double floating point number.
   *
   * @param key   the key
   * @param value the value
   */
  void putDouble(String key, double value);

  /**
   * Sets the value of the given key to the given floating point number.
   *
   * @param key   the key
   * @param value the value
   */
  void putFloat(String key, float value);

  /**
   * Sets the value of the given key to the given integer.
   *
   * @param key   the key
   * @param value the value
   */
  void putInteger(String key, int value);

  /**
   * Sets the value of the given key to the given long integer.
   *
   * @param key   the key
   * @param value the value
   */
  void putLong(String key, long value);

  /**
   * Copy the attributes and children from <code>memento</code> to the receiver.
   *
   * @param memento the Memento to be copied.
   */
  void putMemento(Memento memento);

  /**
   * Sets the value of the given key to the given string.
   *
   * @param key   the key
   * @param value the value
   */
  void putString(String key, String value);

  /**
   * Sets the value of the given key to the given boolean value.
   *
   * @param key   the key
   * @param value the value
   */
  void putBoolean(String key, boolean value);

  /**
   * Sets the memento's Text node to contain the given data. Creates the Text node if none exists. If a Text node does exist, it's current contents are replaced. Each memento is allowed only one text node.
   *
   * @param data the data to be placed on the Text node
   */
  void putTextData(String data);
}
