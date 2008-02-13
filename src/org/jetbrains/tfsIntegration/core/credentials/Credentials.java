package org.jetbrains.tfsIntegration.core.credentials;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Credentials {

  private String myUserName;

  private String myDomain;

  private String myPassword;


  public Credentials() {
    this("", "", null);
  }

  public Credentials(String userName, String domain, String password) {
    if (userName == null) {
      throw new IllegalArgumentException("userName is null");
    }
    if (domain == null) {
      throw new IllegalArgumentException("domain is null");
    }
    myUserName = userName;
    myDomain = domain;
    myPassword = password;
  }

  /**
   * @return null is no password set
   */
  @Nullable
  public String getPassword() {
    return myPassword;
  }

  @NotNull
  public String getDomain() {
    return myDomain;
  }

  @NotNull
  public String getUserName() {
    return myUserName;
  }

  void setUserName(String userName) {
    if (userName == null) {
      throw new IllegalArgumentException("userName is null");
    }
    myUserName = userName;
  }

  void setPassword(String password) {
    myPassword = password;
  }

  void setDomain(String domain) {
    if (domain == null) {
      throw new IllegalArgumentException("domain is null");
    }
    myDomain = domain;
  }

  public boolean equalsTo(Credentials c) {
    if (!getUserName().equals(c.getUserName())) {
      return false;
    }
    if (!getDomain().equals(c.getDomain())) {
      return false;
    }

    //noinspection ConstantConditions
    if (getPassword() != null ? !getPassword().equals(c.getPassword()) : c.getPassword() == null) {
      return false;
    }
    return true;
  }

  @NonNls
  public String toString() {
    return "Credentials[username=" + getUserName() + ",domain=" + getDomain() + ",password=" + getPassword() + "]";
  }
}
