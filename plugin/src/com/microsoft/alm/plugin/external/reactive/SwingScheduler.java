package com.microsoft.alm.plugin.external.reactive;

import com.intellij.openapi.application.ApplicationManager;
import com.jetbrains.rd.util.reactive.IScheduler;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;

public class SwingScheduler implements IScheduler {
    public static SwingScheduler INSTANCE = new SwingScheduler();

    @Override
    public boolean isActive() {
        return SwingUtilities.isEventDispatchThread();
    }

    @Override
    public boolean getOutOfOrderExecution() {
        return false;
    }

    @Override
    public void assertThread(@Nullable Object o) {
        ApplicationManager.getApplication().assertIsDispatchThread();
    }

    @Override
    public void flush() {
        try {
            SwingUtilities.invokeAndWait(() -> {});
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void invokeOrQueue(@NotNull Function0<Unit> action) {
        if (this.isActive()) {
            action.invoke();
        } else {
            queue(action);
        }
    }

    @Override
    public void queue(@NotNull Function0<Unit> action) {
        SwingUtilities.invokeLater(action::invoke);
    }
}
