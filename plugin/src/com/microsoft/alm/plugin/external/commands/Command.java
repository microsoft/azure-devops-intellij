// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.exceptions.ToolException;
import com.microsoft.alm.plugin.external.exceptions.ToolParseFailureException;
import com.microsoft.alm.plugin.external.tools.TfTool;
import jersey.repackaged.com.google.common.util.concurrent.SettableFuture;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.concurrent.ExecutionException;

/**
 * Base class for all TF commands. This class provides an argument builder for the common arguments for all commmands:
 * name of the command, no prompt, collection url, and login info.
 * If no context is provided the collection and login parameters are simply left off (this should be fine if the command
 * is a local command).
 * All commands must also provide an implementation of the parseOutput method. This method takes the output readers for
 * command and converts them into an instance of T
 *
 * @param <T>
 */
public abstract class Command<T> {
    private static final Logger logger = LoggerFactory.getLogger(Command.class);

    public static final int OUTPUT_TYPE_INFO = 0;
    public static final int OUTPUT_TYPE_WARNING = 1;
    public static final int OUTPUT_TYPE_ERROR = 2;

    private final String name;

    // The server context provides the server url and credentials for commands
    // Note that this may be null in some cases
    private final ServerContext context;

    public interface Listener<T> {
        /**
         * This method is called to notify the owner of progress made by the command process.
         *
         * @param output
         * @param outputType
         * @param percentComplete
         */
        void progress(final String output, final int outputType, final int percentComplete);

        /**
         * This method is called to notify the owner that the command process has completed.
         * Check the error parameter first to see if anything went wrong. If the error is
         * set, the result parameter may be null.
         *
         * @param result
         * @param error
         */
        void completed(final T result, final Throwable error);
    }

    public Command(final String name, final ServerContext context) {
        this.name = name;
        this.context = context;
    }

    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = new ToolRunner.ArgumentBuilder()
                .add(name)
                .add("-noprompt");
        if (context != null && context.getCollectionURI() != null) {
            builder.add("-collection:" + context.getCollectionURI().toString());
            if (context.getAuthenticationInfo() != null) {
                builder.add("-login:" + context.getAuthenticationInfo().getUserName() + "," + context.getAuthenticationInfo().getPassword());
            }
        }

        return builder;
    }

    /**
     * This method starts the command after hooking up the listener. It returns immediately and calls the listener
     * when the command process finishes.
     *
     * @param listener
     */
    public void run(final Listener<T> listener) {
        final StringBuilder stdout = new StringBuilder();
        final StringBuilder stderr = new StringBuilder();
        ArgumentHelper.checkNotNull(listener, "listener");
        final ToolRunner runner = new ToolRunner(TfTool.getLocation(), getArgumentBuilder());
        runner.start(new ToolRunner.Listener() {
            @Override
            public void processStandardOutput(final String line) {
                logger.info("CMD: " + line);
                stdout.append(line + "\n");
                listener.progress(line, OUTPUT_TYPE_INFO, 50);
            }

            @Override
            public void processStandardError(final String line) {
                logger.info("ERROR: " + line);
                stderr.append(line + "\n");
                listener.progress(line, OUTPUT_TYPE_ERROR, 50);
            }

            @Override
            public void processException(final Throwable throwable) {
                logger.info("ERROR: " + throwable.toString());
                listener.progress("", OUTPUT_TYPE_INFO, 100);
                listener.completed(null, throwable);
            }

            @Override
            public void completed(final int returnCode) {
                listener.progress("Parsing command output", OUTPUT_TYPE_INFO, 99);

                // TODO wait for streams to finish (there is a timing issue right now where completed is called before the streams are finished)
                Throwable error = null;
                T result = null;
                try {
                    //TODO there are some commands that write errors to stdout and simply return a non-zero exit code (i.e. when a workspace is not found by name)
                    //TODO we may want to pass in the return code to the parse method or something like that to allow the command to inspect this info as well.
                    result = parseOutput(stdout.toString(), stderr.toString());
                    TfTool.throwBadExitCode(returnCode);
                } catch (Throwable throwable) {
                    logger.warn("CMD: parsing output failed", throwable);
                    error = throwable;
                }
                listener.progress("", OUTPUT_TYPE_INFO, 100);
                listener.completed(result, error);
            }
        });
    }

    /**
     * This method is provided to allow callers to run the command and wait on the result.
     * You should probably not call this method on the main thread.
     * You should also limit this to fast local commands.
     *
     * @return
     */
    public T runSynchronously() {
        final SettableFuture<T> syncResult = SettableFuture.create();
        final SettableFuture<Throwable> syncError = SettableFuture.create();

        run(new Listener<T>() {
            @Override
            public void progress(String output, int outputType, int percentComplete) {
                // Do nothing
            }

            @Override
            public void completed(T result, Throwable error) {
                syncResult.set(result);
                syncError.set(error);
            }
        });

        try {
            Throwable error = syncError.get();
            if (error != null) {
                if (error instanceof RuntimeException) {
                    throw (RuntimeException) error;
                } else {
                    // Wrap the exception
                    throw new ToolException(ToolException.KEY_TF_BAD_EXIT_CODE, error);
                }
            } else {
                return syncResult.get();
            }
        } catch (InterruptedException e) {
            logger.error("CMD: failure", e);
            throw new ToolException(ToolException.KEY_TF_BAD_EXIT_CODE, e);
        } catch (ExecutionException e) {
            logger.error("CMD: failure", e);
            throw new ToolException(ToolException.KEY_TF_BAD_EXIT_CODE, e);
        }
    }

    public abstract T parseOutput(final String stdout, final String stderr);

    protected NodeList evaluateXPath(final String stdout, final String xpathQuery) {
        final XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            final Object result = xpath.evaluate(xpathQuery, new InputSource(new StringReader(stdout)), XPathConstants.NODESET);
            if (result != null && result instanceof NodeList) {
                return (NodeList) result;
            }
        } catch (final XPathExpressionException inner) {
            throw new ToolParseFailureException(inner);
        }

        throw new ToolParseFailureException();
    }

    protected String getXPathAttributeValue(final NamedNodeMap attributeMap, final String attributeName) {
        String value = StringUtils.EMPTY;
        if (attributeMap != null) {
            final Node node = attributeMap.getNamedItem(attributeName);
            if (node != null) {
                value = node.getNodeValue();
            }
        }

        return value;
    }

    protected String[] getLines(final String buffer) {
        final String[] lines = buffer.replace("\r\n", "\n").split("\n");
        return lines;
    }

    protected void throwIfError(final String stderr) {
        if (StringUtils.isNotEmpty(stderr)) {
            //TODO what kind of exception should this be?
            throw new RuntimeException(stderr);
        }
    }

}