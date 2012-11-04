package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.vcs.history.VcsRevisionNumber;

/**
 * User: ksafonov
 */
public class TfsRevisionNumber extends VcsRevisionNumber.Int {

  private static final String SEPARATOR = ":";

  public static final int UNDEFINED_ID = Integer.MIN_VALUE;

  private final int myItemId;

  public TfsRevisionNumber(final int value, final int itemId) {
    super(value);
    myItemId = itemId;
  }

  @Override
  public String asString() {
    if (myItemId != UNDEFINED_ID) {
      return String.valueOf(getValue()) + SEPARATOR + String.valueOf(myItemId);
    }
    else {
      return String.valueOf(getValue());
    }
  }

  public TfsRevisionNumber(final int value) {
    this(value, UNDEFINED_ID);
  }

  public int getItemId() {
    return myItemId;
  }

  public static VcsRevisionNumber tryParse(final String s) {
    try {
      int i = s.indexOf(SEPARATOR);
      if (i != -1) {
        String revisionNumberString = s.substring(0, i);
        String itemIdString = s.substring(i + 1);
        int revisionNumber = Integer.parseInt(revisionNumberString);
        int changeset = Integer.parseInt(itemIdString);
        return new TfsRevisionNumber(revisionNumber, changeset);
      }
      else {
        int revisionNumber = Integer.parseInt(s);
        return new TfsRevisionNumber(revisionNumber);
      }
    }
    catch (NumberFormatException e) {
      return null;
    }
  }
}
