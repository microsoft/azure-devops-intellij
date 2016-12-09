// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.ToolRunnerCache;
import com.microsoft.alm.plugin.external.exceptions.ToolException;
import com.microsoft.alm.plugin.external.exceptions.ToolParseFailureException;
import com.microsoft.alm.plugin.external.tools.TfTool;
import jersey.repackaged.com.google.common.util.concurrent.SettableFuture;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import sun.security.util.Debug;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern CHANGESET_NUMBER_PATTERN = Pattern.compile("#(\\d+)");

    public static final int OUTPUT_TYPE_INFO = 0;
    public static final int OUTPUT_TYPE_WARNING = 1;
    public static final int OUTPUT_TYPE_ERROR = 2;

    private static final String WARNING_PREFIX = "WARN ";
    private static final String XML_PREFIX = "<?xml ";

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
                .addSwitch("noprompt");
        if (context != null && context.getCollectionURI() != null) {
            // decode URI since CLC does not expect encoded collection urls
            try {
                builder.addSwitch("collection", URLDecoder.decode(context.getCollectionURI().toString(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                logger.warn("Error while decoding collection url. Using encoded url instead", e);
                builder.addSwitch("collection", context.getCollectionURI().toString());
            }
            if (context.getAuthenticationInfo() != null) {
                builder.addSwitch("login", context.getAuthenticationInfo().getUserName() + "," + context.getAuthenticationInfo().getPassword(), true);
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
        final ToolRunner runner = ToolRunnerCache.getRunningToolRunner(TfTool.getValidLocation(),
                getArgumentBuilder(), new ToolRunner.Listener() {
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

                        Throwable error = null;
                        T result = null;
                        try {
                            //TODO there are some commands that write errors to stdout and simply return a non-zero exit code (i.e. when a workspace is not found by name)
                            //TODO we may want to pass in the return code to the parse method or something like that to allow the command to inspect this info as well.
                            result = parseOutput(stdout.toString(), stderr.toString());
                            TfTool.throwBadExitCode(interpretReturnCode(returnCode));
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
        final long startTime = System.nanoTime();
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
        } finally {
            final long endTime = System.nanoTime();
            logger.info(Long.toString(endTime - startTime) + "(ns) - elapsed time for " + this.getArgumentBuilder().toString());
            Debug.println("", Long.toString(endTime - startTime) + "(ns) - elapsed time for " + this.getArgumentBuilder().toString());
        }
    }

    public abstract T parseOutput(final String stdout, final String stderr);

    /**
     * Default method for parsing return code that can be overridden if need be
     *
     * @param returnCode
     * @return returnCode
     */
    public int interpretReturnCode(final int returnCode) {
        return returnCode;
    }

    protected NodeList evaluateXPath(final String stdout, final String xpathQuery) {
        if (StringUtils.isEmpty(stdout)) {
            return null;
        }

        // Skip over any lines (like WARNing lines) that come before the xml tag
        // Example:
        // WARN -- Unable to construct Telemetry Client
        // <?xml ...
        final InputSource xmlInput;
        final int xmlStart = stdout.indexOf(XML_PREFIX);
        if (xmlStart > 0) {
            xmlInput = new InputSource(new StringReader(stdout.substring(xmlStart)));
        } else {
            xmlInput = new InputSource(new StringReader(stdout));
        }

        final XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            final Object result = xpath.evaluate(xpathQuery, xmlInput, XPathConstants.NODESET);
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
        return getLines(buffer, true);
    }

    protected String[] getLines(final String buffer, final boolean skipWarnings) {
        final List<String> lines = new ArrayList<String>(Arrays.asList(buffer.replace("\r\n", "\n").split("\n")));
        if (skipWarnings) {
            while (lines.size() > 0 && StringUtils.startsWithIgnoreCase(lines.get(0), WARNING_PREFIX)) {
                lines.remove(0);
            }
        }
        return lines.toArray(new String[lines.size()]);
    }

    /**
     * This method is used by Checkin and CreateBranch to parse out the changeset number.
     *
     * @param buffer
     * @return
     */
    protected String getChangesetNumber(final String buffer) {
        // parse output for changeset number
        String changesetNumber = StringUtils.EMPTY;
        final Matcher matcher = CHANGESET_NUMBER_PATTERN.matcher(buffer);
        if (matcher.find()) {
            changesetNumber = matcher.group(1);
            logger.info("Changeset '" + changesetNumber + "' was created");
        } else {
            logger.info("Changeset pattern not found in buffer: " + buffer);
        }
        return changesetNumber;
    }

    /**
     * This method evaluates a line of output to see if it contains something that is expected.
     * If not, it returns false.
     * There are 3 kinds of expected lines:
     * 1) lines that begin with an expected prefix
     * 2) lines that contain a folder path (/path/path:)
     * 3) empty lines
     * All other types of lines are unexpected.
     */
    protected boolean isOutputLineExpected(final String line, final String[] expectedPrefixes, final boolean filePathsAreExpected) {
        final String trimmed = line != null ? line.trim() : null;
        if (StringUtils.isNotEmpty(trimmed)) {
            // If we are expecting file paths, check for a file path pattern (ex. /path/path2:)
            if (filePathsAreExpected && isFilePath(line)) {
                // This matched our file path pattern, so it is expected
                return true;
            }

            // Next, check for one of the expected prefixes
            if (expectedPrefixes != null) {
                for (final String prefix : expectedPrefixes) {
                    if (StringUtils.startsWithIgnoreCase(line, prefix)) {
                        // The line starts with an expected prefix so it is expected
                        return true;
                    }
                }
            }

            // The line is not empty and does not contain anything we expect
            // So, it is probably an error.
            return false;
        }

        // Just return true for empty lines
        return true;
    }

    protected boolean isFilePath(final String line) {
        if (StringUtils.endsWith(line, ":")) {
            // File paths are different on different OSes
            if (StringUtils.containsAny(line, "\\/")) {
                return true;
            }
        }
        return false;
    }

    protected String getFilePath(final String path, final String filename, final String pathRoot) {
        // If the path still has a ':' at the end, remove it
        String folderPath = StringUtils.removeEnd(path, ":");
        // If the path isn't rooted, add in the root
        if (!Path.isAbsolute(folderPath) && StringUtils.isNotEmpty(pathRoot)) {
            folderPath = Path.combine(pathRoot, folderPath);
        }

        return Path.combine(folderPath, filename);
    }

    protected void throwIfError(final String stderr) {
        if (StringUtils.isNotEmpty(stderr)) {
            //TODO what kind of exception should this be?
            throw new RuntimeException(stderr);
        }
    }

}