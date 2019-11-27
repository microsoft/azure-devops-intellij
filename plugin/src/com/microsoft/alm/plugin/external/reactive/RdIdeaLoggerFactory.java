// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.reactive;

import com.jetbrains.rd.util.ILoggerFactory;
import com.jetbrains.rd.util.LogLevel;
import com.jetbrains.rd.util.Logger;
import com.jetbrains.rd.util.Statics;
import kotlin.jvm.JvmClassMappingKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RdIdeaLoggerFactory implements ILoggerFactory {

    private static boolean initialized = false;
    private static final RdIdeaLoggerFactory INSTANCE = new RdIdeaLoggerFactory();

    public synchronized static void initialize() {
        if (!initialized) {
            Statics.Companion.of(JvmClassMappingKt.getKotlinClass(ILoggerFactory.class)).push(INSTANCE);
            initialized = true;
        }
    }

    @NotNull
    private static String prepareMessage(@Nullable Object message) {
        return message == null ? "null" : message.toString();
    }

    @NotNull
    private static String prepareMessage(@Nullable Object message, @Nullable Throwable throwable) {
        if (throwable == null) {
            return prepareMessage(message);
        }

        return message == null
                ? throwable.toString()
                : String.format("%s: %s", message.toString(), throwable.toString());
    }

    @NotNull
    @Override
    public Logger getLogger(@NotNull String category) {
        com.intellij.openapi.diagnostic.Logger internalLogger = com.intellij.openapi.diagnostic.Logger.getInstance(category);
        return new Logger() {
            @Override
            public void log(@NotNull LogLevel level, @Nullable Object message, @Nullable Throwable throwable) {
                switch (level) {
                    case Trace:
                        internalLogger.trace(prepareMessage(message, throwable));
                        break;
                    case Debug:
                        internalLogger.debug(prepareMessage(message), throwable);
                        break;
                    case Info:
                        internalLogger.info(prepareMessage(message), throwable);
                        break;
                    case Warn:
                        internalLogger.warn(prepareMessage(message), throwable);
                        break;
                    case Error:
                    case Fatal:
                    default:
                        internalLogger.error(prepareMessage(message), throwable);
                        break;
                }
            }

            @Override
            public boolean isEnabled(@NotNull LogLevel level) {
                switch (level) {
                    case Trace:
                        return internalLogger.isTraceEnabled();
                    case Debug:
                        return internalLogger.isDebugEnabled();
                    case Info:
                    case Warn:
                    case Error:
                    case Fatal:
                    default:
                        return true;
                }
            }
        };
    }
}
