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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSProgressUtil;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.exceptions.UserCancelledException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TfsExecutionUtil {

  public static class ResultWithErrors<T> {
    public final List<VcsException> errors;
    @Nullable public final T result;
    @NotNull private final Project project;
    public final boolean cancelled;

    private ResultWithErrors(List<VcsException> errors, T result, Project project, boolean cancelled) {
      this.errors = errors;
      this.result = result;
      this.project = project;
      this.cancelled = cancelled;
    }

    public void throwIfErrors() throws VcsException {
      if (errors.isEmpty()) {
        return;
      }

      if (errors.size() == 1) {
        throw errors.iterator().next();
      }

      Collection<String> messages = new ArrayList<String>(errors.size());
      for (VcsException error : errors) {
        messages.add(error.getMessage());
      }
      throw new VcsException(messages);
    }

    public boolean showTabIfErrors() {
      if (errors.isEmpty()) {
        return false;
      }
      AbstractVcsHelper.getInstance(project).showErrors(errors, TFSVcs.TFS_NAME);
      return true;
    }

    public boolean showDialogIfErrors(String title, String prefix) {
      if (errors.isEmpty()) {
        return false;
      }
      StringBuilder errorMessage = new StringBuilder(prefix + ":\n\n");
      for (VcsException e : errors) {
        errorMessage.append(e.getMessage()).append("\n");
      }
      Messages.showErrorDialog(project, errorMessage.toString(), title);
      return true;
    }

  }

  public static class ResultWithError<T> {
    @Nullable public final VcsException error;
    @Nullable public final T result;
    @NotNull private final Project project;
    public final boolean cancelled;

    private ResultWithError(@Nullable VcsException error, @Nullable T result, Project project, boolean cancelled) {
      this.error = error;
      this.result = result;
      this.project = project;
      this.cancelled = cancelled;
    }

    public void throwIfError() throws VcsException {
      if (error != null) {
        throw new VcsException(error);
      }
    }

    public boolean showTabIfError() {
      if (error != null) {
        AbstractVcsHelper.getInstance(project).showError(error, TFSVcs.TFS_NAME);
        return true;
      }
      return false;
    }

    public boolean showDialogIfError(String title) {
      if (error == null) {
        return false;
      }
      Messages.showErrorDialog(project, error.getMessage(), title);
      return true;
    }

  }

  public interface ProcessWithErrors<T> {
    @Nullable
    T run(final Collection<VcsException> errorsHolder) throws TfsException, VcsException;
  }

  public interface VoidProcessWithErrors {
    void run(final Collection<VcsException> errorsHolder) throws TfsException, VcsException;
  }

  public interface Process<T> {
    @Nullable
    T run() throws TfsException, VcsException;
  }

  public interface VoidProcess {
    void run() throws TfsException, VcsException;
  }

  public static <T> ResultWithErrors<T> executeInBackground(String progressText, Project project, final ProcessWithErrors<T> process) {
    final Ref<T> result = new Ref<T>();
    final List<VcsException> errors = new ArrayList<VcsException>();
    final Ref<Boolean> explicitlyCancelled = new Ref<Boolean>();
    Runnable runnable = new Runnable() {
      public void run() {
        TFSProgressUtil.setIndeterminate(ProgressManager.getInstance().getProgressIndicator(), true);
        try {
          result.set(process.run(errors));
        }
        catch (UserCancelledException e) {
          explicitlyCancelled.set(true);
        }
        catch (TfsException e) {
          //noinspection ThrowableInstanceNeverThrown
          errors.add(new VcsException(e.getMessage(), e));
        }
        catch (VcsException e) {
          errors.add(e);
        }
      }
    };

    final boolean completed;
    if (ApplicationManager.getApplication().isDispatchThread()) {
      completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, progressText, true, project);
    }
    else {
      runnable.run();
      completed = true;
    }

    return new ResultWithErrors<T>(errors, result.get(), project,
                                   !completed || (!explicitlyCancelled.isNull() && explicitlyCancelled.get()));
  }

  public static ResultWithErrors<Void> executeInBackground(String progressText, Project project, final VoidProcessWithErrors process) {
    return executeInBackground(progressText, project, new ProcessWithErrors<Void>() {
      public Void run(Collection<VcsException> errorsHolder) throws TfsException, VcsException {
        process.run(errorsHolder);
        return null;
      }
    });
  }

  public static <T> ResultWithError<T> executeInBackground(String progressText, Project project, final Process<T> process) {
    ResultWithErrors<T> result = executeInBackground(progressText, project, new ProcessWithErrors<T>() {
      public T run(Collection<VcsException> errorsHolder) throws TfsException, VcsException {
        return process.run();
      }
    });
    assert result.errors.size() < 2;
    //noinspection ThrowableResultOfMethodCallIgnored
    return new ResultWithError<T>(ContainerUtil.getFirstItem(result.errors, null), result.result, project, result.cancelled);
  }

  public static ResultWithError<Void> executeInBackground(String progressText, Project project, final VoidProcess process) {
    return executeInBackground(progressText, project, new Process<Void>() {
      public Void run() throws TfsException, VcsException {
        process.run();
        return null;
      }
    });
  }

}
