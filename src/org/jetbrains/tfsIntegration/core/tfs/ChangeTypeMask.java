package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ChangeType;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ChangeType_type0;
import org.jetbrains.annotations.Nullable;

public class ChangeTypeMask {
  @Nullable
  private ChangeType_type0[] myValues;

  public ChangeTypeMask(ChangeType changeType) {
    myValues = changeType != null ? changeType.getChangeType_type0() : null;
  }

  public boolean containsAll(final ChangeType_type0... values) {
    if (myValues == null) {
      return false;
    }

    for (ChangeType_type0 value : values) {
      if (!ArrayUtil.contains(value, myValues)) {
        return false;
      }
    }
    return true;
  }

  public boolean contains(final ChangeType_type0 value) {
    return containsAny(value);
  }

  public boolean containsAny(final ChangeType_type0... values) {
    if (myValues == null) {
      return false;
    }

    for (ChangeType_type0 value : values) {
      if (ArrayUtil.contains(value, myValues)) {
        return true;
      }
    }
    return false;
  }

  public boolean containsOnly(final ChangeType_type0... values) {
    return myValues != null && myValues.length == values.length && containsAll(values);
  }


  public void remove(ChangeType_type0... values) {
    if (myValues == null) {
      return;
    }

    for (ChangeType_type0 value : values) {
      myValues = ArrayUtil.remove(myValues, value);
    }
  }

  public boolean isEmpty() {
    return myValues == null || myValues.length == 0;
  }

  public int size() {
    return myValues != null ? myValues.length : 0;
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "(empty)";
    }
    else {
      //noinspection ConstantConditions
      return StringUtil.join(myValues, new Function<ChangeType_type0, String>() {
        @Override
        public String fun(ChangeType_type0 changeType_type0) {
          return changeType_type0.getValue();
        }
      }, ",");
    }
  }
}
