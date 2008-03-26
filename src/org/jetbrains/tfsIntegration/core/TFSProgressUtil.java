package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.Nullable;

public class TFSProgressUtil {

  public static void checkCanceled(final @Nullable ProgressIndicator progressIndicator) {
    if (progressIndicator != null && progressIndicator.isCanceled()) {
      throw new ProcessCanceledException();
    }
  }

  public static boolean isCanceled(final @Nullable ProgressIndicator progressIndicator) {
    return progressIndicator != null && progressIndicator.isCanceled();
  }

  public static void setProgressText(final @Nullable ProgressIndicator progressIndicator, String text) {
    if (progressIndicator != null) {
      progressIndicator.setText(text);
    }
  }

}
