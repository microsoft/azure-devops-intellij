/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.tfsIntegration.core.credentials;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;

public class Credentials {

  private String myUserName;

  private String myDomain;

  private String myPassword;


  public Credentials() {
    this("", "", null);
  }

  public Credentials(final @NotNull String userName, final @NotNull String domain, final String password) {
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

  void setUserName(final @NotNull String userName) {
    myUserName = userName;
  }

  void setPassword(String password) {
    myPassword = password;
  }

  void setDomain(final @NotNull String domain) {
    myDomain = domain;
  }

  @NotNull
  public String getQualifiedUsername() {
    if (getDomain().length() > 0) {
      return MessageFormat.format("{0}\\{1}", getDomain(), getUserName());
    }
    else {
      return getUserName();
    }
  }

  public boolean equalsTo(Credentials c) {
    if (!getUserName().equals(c.getUserName())) {
      return false;
    }
    if (!getDomain().equals(c.getDomain())) {
      return false;
    }

    //noinspection ConstantConditions
    if (getPassword() != null ? !getPassword().equals(c.getPassword()) : c.getPassword() != null) {
      return false;
    }
    return true;
  }

  @NonNls
  public String toString() {
    return "Credentials[username=" + getUserName() + ",domain=" + getDomain() + ",password=" + getPassword() + "]";
  }

}
