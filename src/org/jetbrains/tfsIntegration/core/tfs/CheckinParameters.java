/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.CheckinNoteFieldDefinition;

import java.text.MessageFormat;
import java.util.*;

public class CheckinParameters {

  @Nullable
  public static String validate(Map<ServerInfo, CheckinParameters> params, Condition<ServerInfo> accept) {
    StringBuilder message = null;
    for (Map.Entry<ServerInfo, CheckinParameters> entry : params.entrySet()) {
      if (!accept.value(entry.getKey())) {
        continue;
      }

      final Collection<String> msgs = entry.getValue().validate();
      if (!msgs.isEmpty()) {
        if (message == null) {
          message = new StringBuilder();
        }
        else if (message.length() > 0) {
          message.append("\n");
        }

        if (params.size() > 1) { // show server address if original file set affected several servers  
          message.append(entry.getKey().getUri()).append(":");
        }
        for (String msg : msgs) {
          if (message.length() > 0) {
            message.append("\n");
          }
          message.append(msg);
        }
      }
    }
    return message != null ? message.toString() : null;
  }

  public static class CheckinNote {
    public @NotNull final String name;
    public final boolean required;
    public @Nullable String value;

    private CheckinNote(String name, boolean required) {
      this.name = name;
      this.required = required;
    }
  }

  private final List<CheckinNote> myCheckinNotes;

  private final WorkItemsCheckinParameters myWorkItems;

  public CheckinParameters(List<CheckinNoteFieldDefinition> checkinNotes, WorkItemsCheckinParameters workItems) {
    myWorkItems = workItems;
    myCheckinNotes = new ArrayList<CheckinNote>(checkinNotes.size());
    for (CheckinNoteFieldDefinition checkinNote : checkinNotes) {
      myCheckinNotes.add(new CheckinNote(checkinNote.getName(), checkinNote.getReq()));
    }
  }

  private CheckinParameters(WorkItemsCheckinParameters workItems, List<CheckinNote> checkinNotesData) {
    myWorkItems = workItems;
    myCheckinNotes = checkinNotesData;
  }

  public List<CheckinNote> getCheckinNotes() {
    return Collections.unmodifiableList(myCheckinNotes);
  }

  public WorkItemsCheckinParameters getWorkItems() {
    return myWorkItems;
  }

  public Collection<String> validate() {
    return validateCheckinNotes();
  }

  public Collection<String> validateCheckinNotes() {
    Collection<String> emptyNotes = new ArrayList<String>();
    for (CheckinNote checkinNote : myCheckinNotes) {
      if (checkinNote.required && StringUtil.isEmptyOrSpaces(checkinNote.value)) {
        emptyNotes.add(checkinNote.name);
      }
    }

    final String error;
    if (emptyNotes.size() > 1) {
      error = MessageFormat.format("Check In notes ''{0}'' are required to commit",
                                   StringUtil.join(emptyNotes.toArray(new String[emptyNotes.size()]), "', '"));

    }
    else if (emptyNotes.size() == 1) {
      error = MessageFormat.format("Check In note ''{0}'' is required to commit", emptyNotes.iterator().next());
    }
    else {
      error = null;
    }
    return error != null ? Collections.singletonList(error) : Collections.<String>emptyList();
  }

  public CheckinParameters createCopy() {
    List<CheckinNote> notesCopy = new ArrayList<CheckinNote>(myCheckinNotes.size());
    for (CheckinNote original : myCheckinNotes) {
      CheckinNote copy = new CheckinNote(original.name, original.required);
      copy.value = original.value;
      notesCopy.add(copy);
    }
    return new CheckinParameters(myWorkItems.createCopy(), notesCopy);
  }

}
