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

package org.jetbrains.tfsIntegration.core.configuration;

import com.intellij.openapi.util.PasswordUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;

/*
  type = NtlmNative and password = "" means that IDE should not ask for credentials
  type = NtlmNative and *null* password means that password has been reset
 */

@Tag(value = "credentials")
@SuppressWarnings("UnusedDeclaration")
public class Credentials {

  private enum UseNative {Yes, No, Reset}

  public static Credentials createNative() {
    return new Credentials("", "", "", true, Type.NtlmNative);
  }

  public enum Type {NtlmExplicit, NtlmNative, Alternate;

    public String getPresentableText() {
      return TFSBundle.message("credentials.type." + name());
    }
  }

  private @NotNull String myUserName;

  private @NotNull String myDomain;

  private @Nullable String myPassword;

  private boolean myStorePassword;

  private Type myType;

  public Credentials() {
    this("", "", null, false, Type.NtlmExplicit);
  }

  public Credentials(final @NotNull String userName,
                     final @NotNull String domain,
                     final @Nullable String password,
                     final boolean storePassword,
                     final Type type) {
    myUserName = userName;
    myDomain = domain;
    myPassword = password;
    myStorePassword = storePassword;
    myType = type;
  }

  public Credentials(final @NotNull String credentials,
                     final @Nullable String password,
                     final boolean storePassword,
                     final Type type) {
    int i = credentials.indexOf('\\');
    myDomain = i != -1 ? credentials.substring(0, i) : "";
    myUserName = i != -1 ? credentials.substring(i + 1) : credentials;
    myPassword = password;
    myStorePassword = storePassword;
    myType = type;
  }

  /**
   * @return null is no password set
   */
  @Nullable
  public String getPassword() {
    return myPassword;
  }

  public void resetPassword() {
    myPassword = null;
    myStorePassword = false;
  }

  @Nullable
  @Tag(value = "password")
  public String getEncodedPassword() {
    return myStorePassword && myPassword != null ? PasswordUtil.encodePassword(myPassword) : null;
  }

  public void setEncodedPassword(String encodedPassword) {
    myPassword = encodedPassword != null ? PasswordUtil.decodePassword(encodedPassword) : null;
    myStorePassword = true;
  }

  public boolean isStorePassword() {
    return myStorePassword;
  }

  @NotNull
  @Tag(value = "domain")
  public String getDomain() {
    return myDomain;
  }

  public void setDomain(final @NotNull String domain) {
    myDomain = domain;
  }

  @NotNull
  @Tag(value = "username")
  public String getUserName() {
    return myUserName;
  }

  public void setUserName(final @NotNull String userName) {
    myUserName = userName;
  }

  public Type getType() {
    return myType;
  }

  // backward compatibility
  @Attribute("nativeAuth")
  public String getUseNativeSerialized() {
    return null;
  }

  public void setUseNativeSerialized(String useNative) {
    if (UseNative.Yes.name().equals(useNative)) {
      myType = Type.NtlmNative;
      myPassword = "";
    }
    else if (UseNative.Reset.name().equals(useNative)) {
      myType = Type.NtlmNative;
      myPassword = null;
    }
    else {
      myType = Type.NtlmExplicit;
    }
  }

  @Attribute("type")
  public String getTypeSerialized() {
    return myType.name();
  }

  public void setTypeSerialized(String s) {
    try {
      myType = Type.valueOf(s);
    }
    catch (IllegalArgumentException e) {
      myType = Type.NtlmExplicit;
    }
  }

  @NotNull
  public String getQualifiedUsername() {
    if (getDomain().length() > 0) {
      return TfsUtil.getQualifiedUsername(getDomain(), getUserName());
    }
    else {
      return getUserName();
    }
  }

  @NonNls
  public String toString() {
    //noinspection ConstantConditions
    return myType.name() +
           ": " +
           getQualifiedUsername() +
           "," +
           (getPassword() != null ? getPassword().replaceAll(".", "x") : "(no password)");
  }

  public boolean shouldShowLoginDialog() {
    return myPassword == null;
  }
}
