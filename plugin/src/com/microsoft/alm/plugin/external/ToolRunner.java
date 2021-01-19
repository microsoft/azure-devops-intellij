// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external;

import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.external.utils.ProcessHelper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This class is used to run an external command line tool and listen to the output.
 * Use the nested class ArgumentBuilder to build up the list of arguments and the nested interface Listener to
 * get callbacks for processing output, exceptions and completion events.
 */
public class ToolRunner {
    private static final Logger logger = LoggerFactory.getLogger(ToolRunner.class);

    private Process toolProcess;
    private final String toolLocation;
    private final String workingDirectory;
    private StreamProcessor standardErrorProcessor;
    private StreamProcessor standardOutProcessor;
    private ProcessWaiter processWaiter;
    private ListenerProxy listenerProxy;

    /**
     * Implement this class to get callbacks on events triggered by the ToolRunner.
     */
    public interface Listener {
        void processStandardOutput(final String line);

        void processStandardError(final String line);

        void processException(final Throwable throwable);

        void completed(int returnCode);
    }

    /**
     * Create an instance of this class to build up the arguments that should be passed to the Tool.
     * These arguments are logged by the tool runner. If an argument should not be written to the log,
     * Call addSecret instead of add on that argument. This will produce ****** in the log in place of the
     * actual argument value.
     */
    public static class ArgumentBuilder {
        private static final String STARS = "********";
        private List<String> arguments = new ArrayList<String>(5);
        private Set<Integer> secretArgumentIndexes = new HashSet<Integer>(5);
        private String workingDirectory;

        public ArgumentBuilder() {
        }

        /**
         * WorkingDirectory is a special argument and is stored and retrieved separately.
         */
        public ArgumentBuilder setWorkingDirectory(final String workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public String getWorkingDirectory() {
            return workingDirectory;
        }

        public ArgumentBuilder add(final String argument) {
            arguments.add(argument);
            return this;
        }

        public ArgumentBuilder addSwitch(final String switchName) {
            return addSwitch(switchName, null, false);
        }

        public ArgumentBuilder addSwitch(final String switchName, final String switchValue) {
            return addSwitch(switchName, switchValue, false);
        }

        public ArgumentBuilder addSwitch(final String switchName, final String switchValue, final boolean isSecret) {
            ArgumentHelper.checkNotEmptyString(switchName, "switchName");
            final String arg;
            if (StringUtils.isEmpty(switchValue)) {
                arg = "-" + switchName;
            } else {
                arg = "-" + switchName + ":" + switchValue;
            }

            if (isSecret) {
                addSecret(arg);
            } else {
                add(arg);
            }

            return this;
        }

        public ArgumentBuilder addSecret(final String argument) {
            add(argument);
            secretArgumentIndexes.add(arguments.size() - 1);
            return this;
        }

        public ArgumentBuilder addAuthInfo(final AuthenticationInfo authInfo) {
            addSwitch("login", authInfo.getUserName() + "," + authInfo.getPassword(), true);
            return this;
        }

        public List<String> build() {
            return Collections.unmodifiableList(arguments);
        }

        /**
         * This method returns an unmodifiable list of arguments.
         * The toolLocation passed in is inserted as the very first argument.
         */
        public List<String> build(final String toolLocation) {
            final List<String> commandLineParts = new ArrayList<String>(arguments);
            commandLineParts.add(0, toolLocation);
            return Collections.unmodifiableList(commandLineParts);
        }

        /**
         * Use this method to easily log all of the arguments.
         * Secret arguments will be shown as *******
         */
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < arguments.size(); i++) {
                final String arg;
                if (secretArgumentIndexes.contains(i)) {
                    arg = STARS;
                } else {
                    arg = arguments.get(i);
                }
                builder.append(arg);
                builder.append(" ");
            }
            return builder.toString().trim();
        }
    }

    public ToolRunner(final String toolLocation, final String workingDirectory) {
        this.toolLocation = toolLocation;
        this.workingDirectory = workingDirectory;
        this.listenerProxy = new ListenerProxy();
    }

    public void addListener(final Listener listener) {
        listenerProxy.addListener(listener);
    }

    public Process start(final ArgumentBuilder argumentBuilder) {
        logger.info("ToolRunner.start: toolLocation = " + toolLocation);
        logger.info("ToolRunner.start: workingDirectory = " + workingDirectory);
        ArgumentHelper.checkNotNull(argumentBuilder, "argumentBuilder");
        logger.info("arguments: " + argumentBuilder.toString());

        try {
            SettableFuture<Boolean> standardOutputFlushed = SettableFuture.create();
            SettableFuture<Boolean> standardErrorFlushed = SettableFuture.create();

            // Create and start the process from the tool location and working directory
            // (it is perfectly okay if working directly is null here. null == not set)
            toolProcess = ProcessHelper.startProcess(workingDirectory, argumentBuilder.build(toolLocation));
            final InputStream stderr = toolProcess.getErrorStream();
            final InputStream stdout = toolProcess.getInputStream();
            standardErrorProcessor = new StreamProcessor(stderr, true, listenerProxy, standardErrorFlushed);
            standardErrorProcessor.start();
            standardOutProcessor = new StreamProcessor(stdout, false, listenerProxy, standardOutputFlushed);
            standardOutProcessor.start();
            processWaiter = new ProcessWaiter(toolProcess, listenerProxy, standardErrorFlushed, standardOutputFlushed);
            processWaiter.start();
            return toolProcess;
        } catch (final IOException e) {
            logger.warn("Failed to start tool process or redirect output.", e);
            listenerProxy.processException(e);
        }

        return null;
    }

    public Process sendArgsViaStandardInput(final ArgumentBuilder argumentBuilder) {
        ArgumentHelper.checkNotNull(toolProcess, "toolProcess");
        ArgumentHelper.checkNotNull(argumentBuilder, "argumentBuilder");
        logger.info("sendArgsViaStandardInput: proceedWithArgs: " + argumentBuilder.toString());
        final OutputStream stdin = toolProcess.getOutputStream();
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
        try {
            for (final String arg : argumentBuilder.build()) {
                final String escapedArg = escapeArgument(arg);
                writer.write(escapedArg);
                writer.write(" ");
            }
            writer.write("\n");
            writer.flush();
        } catch (final Throwable throwable) {
            logger.warn("Error sending args.", throwable);
            listenerProxy.processException(throwable);
        } finally {
            try {
                writer.close();
            } catch (final IOException e) {
                logger.warn("Unable to close the writer.", e);
            }
        }
        return toolProcess;
    }

    /**
     * Command line arguments should have all embedded double quotes repeated to escape them.
     * They should also be surrounded by double quotes if they contain a space.
     */
    private String escapeArgument(final String argument) {
        String escaped = StringUtils.replace(argument, "\"", "\"\"");
        if (StringUtils.contains(escaped, " ")) {
            escaped = "\"" + escaped + "\"";
        }
        return escaped;
    }

    /**
     * Call the dispose method to make sure all threads are cleaned up and disposed of properly.
     */
    public void dispose() {
        try {
            try {
                // Close process' stdin so it could gracefully terminate by itself. Otherwise, process could outlive
                // even processWaiter.cleanUp() call (since it only terminates the root process and not children).
                if (toolProcess != null) {
                    toolProcess.getOutputStream().close();
                }
            } catch (IOException e) {
                logger.warn("Couldn't close the process' stdin", e);
            }

            if (processWaiter != null) {
                processWaiter.cleanUp();
                processWaiter = null;
            }
            if (standardErrorProcessor != null) {
                standardErrorProcessor.cleanUp();
                standardErrorProcessor = null;
            }
            if (standardOutProcessor != null) {
                standardOutProcessor.cleanUp();
                standardOutProcessor = null;
            }
        } catch (final InterruptedException e) {
            logger.warn("Failed to dispose ToolRunner.", e);
        }
    }

    private static class ListenerProxy implements Listener {
        private final List<Listener> listeners = new ArrayList<Listener>(2);

        public ListenerProxy() {
        }

        public void addListener(final Listener listener) {
            listeners.add(listener);
        }

        @Override
        public void processStandardOutput(final String line) {
            for (final Listener l : listeners) {
                l.processStandardOutput(line);
            }
        }

        @Override
        public void processStandardError(final String line) {
            for (final Listener l : listeners) {
                l.processStandardError(line);
            }
        }

        @Override
        public void processException(final Throwable throwable) {
            for (final Listener l : listeners) {
                l.processException(throwable);
            }
        }

        @Override
        public void completed(final int returnCode) {
            for (final Listener l : listeners) {
                l.completed(returnCode);
            }
        }
    }

    /**
     * This internal class is used to manage the thread that waits on the process to finish.
     * It takes in the process to wait on and the listener to issue callbacks to.
     */
    private static class ProcessWaiter extends Thread {
        private boolean processRunning;
        private final Process process;
        private final Listener listener;
        private final SettableFuture<Boolean> errorsFlushed;
        private final SettableFuture<Boolean> outputFlushed;

        public ProcessWaiter(final Process process, final Listener listener, final SettableFuture<Boolean> errorsFlushed, final SettableFuture<Boolean> outputFlushed) {
            ArgumentHelper.checkNotNull(process, "process");
            ArgumentHelper.checkNotNull(listener, "listener");
            this.process = process;
            this.listener = listener;
            this.errorsFlushed = errorsFlushed;
            this.outputFlushed = outputFlushed;
        }

        @Override
        public void run() {
            // Don't let exceptions escape from this top level method
            try {
                processRunning = true;
                // Wait for the process to finish
                process.waitFor();
                // Wait for the output streams to be flushed
                errorsFlushed.get(30, TimeUnit.SECONDS);
                outputFlushed.get(30, TimeUnit.SECONDS);
                // Clear the member variable so we don't try to destroy the process later
                processRunning = false;
                // Call the completed event on the listener with the exit code
                listener.completed(process.exitValue());
            } catch (Throwable e) {
                logger.warn("Failed to wait for process exit.", e);
                listener.processException(e);
            }
        }

        /**
         * This method forces the thread to end by interrupting it and joining with the calling thread.
         *
         * @throws InterruptedException
         */
        public void cleanUp() throws InterruptedException {
            if (processRunning) {
                try {
                    process.destroy();
                } catch (final Throwable t) {
                    logger.warn("Failed to destroy process.", t);
                }
            }
            this.interrupt();
            this.join();
        }
    }

    /**
     * This internal class is used to manage the threads that receive the output from the process.
     * One thread is created to listen for standard output and one is created to listen to standard error.
     * The constructor takes in the stream to listen to, what kind of stream it is, and the listener to
     * issue callbacks to.
     */
    private static class StreamProcessor extends Thread {

        private final InputStream stream;
        private final boolean isStandardError;
        private final Listener listener;
        private final SettableFuture<Boolean> flushed;

        public StreamProcessor(final InputStream stream, final boolean isStandardError, final Listener listener, final SettableFuture<Boolean> flushed) {
            ArgumentHelper.checkNotNull(stream, "stream");
            ArgumentHelper.checkNotNull(listener, "listener");
            ArgumentHelper.checkNotNull(flushed, "flushed");
            this.stream = stream;
            this.isStandardError = isStandardError;
            this.listener = listener;
            this.flushed = flushed;
        }

        @Override
        public void run() {
            BufferedReader bufferedReader = null;

            // Don't let exceptions escape from this top level method
            try {
                // Create a buffered reader so that we can process the output one line at a time
                bufferedReader = new BufferedReader(new InputStreamReader(stream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    // Call the appropriate event with the line that was read
                    if (isStandardError) {
                        listener.processStandardError(line);
                    } else {
                        listener.processStandardOutput(line);
                    }
                }
            } catch (Throwable e) {
                logger.warn("Failed to process output.", e);
                listener.processException(e);
            } finally {
                // Don't let exceptions escape from this top level method
                try {
                    // Make sure to close the buffered reader
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    flushed.set(true);
                } catch (Throwable e) {
                    logger.warn("Failed to close buffer.", e);
                    listener.processException(e);
                }
            }
        }

        /**
         * This method forces the thread to end by interrupting it and joining with the calling thread.
         *
         * @throws InterruptedException
         */
        public void cleanUp() throws InterruptedException {
            this.interrupt();
            this.join();
        }
    }
}
