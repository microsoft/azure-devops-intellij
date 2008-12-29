/*
 * Copyright 2000-2008 JetBrains s.r.o.
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

package org.jetbrains.tfsIntegration.core.tfs.operations;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vcs.rollback.RollbackProgressListener;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public interface ApplyProgress {

  void setText(String text);

  boolean isCancelled();

  void setFraction(double fraction);


  ApplyProgress EMPTY = new ApplyProgress() {

    public void setText(String text) {
    }

    public boolean isCancelled() {
      return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setFraction(double fraction) {
    }
  };


  class ProgressIndicatorWrapper implements ApplyProgress {

    private final @NotNull ProgressIndicator myProgressIndicator;

    public ProgressIndicatorWrapper(ProgressIndicator progressIndicator) {
      myProgressIndicator = progressIndicator;
    }

    public void setText(String text) {
      myProgressIndicator.setText(text);
    }

    public boolean isCancelled() {
      return myProgressIndicator.isCanceled();
    }

    public void setFraction(double fraction) {
      myProgressIndicator.setFraction(fraction);
    }
  }

  class RollbackProgressWrapper implements ApplyProgress {

    private final @NotNull RollbackProgressListener myListener;

    public RollbackProgressWrapper(RollbackProgressListener listener) {
      myListener = listener;
    }

    public void setText(String text) {
      myListener.accept(new File(text));
    }

    public boolean isCancelled() {
      return false;
    }

    public void setFraction(double fraction) {
    }
  }

}
