// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.versioncontrol.path;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.common.utils.FileHelper;
import com.microsoft.alm.plugin.exceptions.ServerPathFormatException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Taken from team-explorer-everywhere: source/com.microsoft.tfs.core/src/com/microsoft/tfs/core/clients/versioncontrol/path/ServerPath.java
 */
public abstract class ServerPath {
    private static final Logger logger = LoggerFactory.getLogger(ServerPath.class);

    /**
     * Occasionally code must format a path using the root without the slash.
     */
    public static final String ROOT_NAME_ONLY = "$";

    /**
     * All server paths begin with this string. The slash is important (the
     * server seems to always reject access to "$").
     */
    public static final String ROOT = "$/";

    /**
     * Allowed path separator characters in repository paths. All characters are
     * equivalent. Forward slash ('/') is the preferred character.
     */
    public static final char[] SEPARATOR_CHARACTERS = {
            '/',
            '\\'
    };

    /**
     * The preferred separator character.
     */
    public static final char PREFERRED_SEPARATOR_CHARACTER = '/';

    /**
     * Maximum length of a repository path in characters since TFS2012_QU_2.
     * Includes $/ at beginning.
     */
    public final static int MAX_SERVER_PATH_SIZE = 399;

    /**
     * Longest allowable TFS directory component part
     */
    public static final int MAXIMUM_COMPONENT_LENGTH = 256;

    /**
     * <p>
     * Tests the given paths for a parent-child relationship. A path is a child
     * of another if the object it describes would reside below the object
     * described by the parent path in the TFS repository. Case is ignored.
     * </p>
     * <p>
     * A possible child that is equivalent to the parent path (both refer to the
     * same object) is considered a child. This is compatible with Visual
     * Studio's implementation.
     * </p>
     *
     * @param parentPath    the server path to the parent item (must not be <code>null</code>)
     * @param possibleChild the server path of the possible child item (must not be
     *                      <code>null</code>)
     * @return true if possibleChild is a child of parentPath.
     */
    public final static boolean isChild(String parentPath, String possibleChild) throws ServerPathFormatException {
        ArgumentHelper.checkNotNull(parentPath, "parentPath");
        ArgumentHelper.checkNotNull(possibleChild, "possibleChild");

        // Canonicalize the paths for easy comparison.
        parentPath = ServerPath.canonicalize(parentPath);
        possibleChild = ServerPath.canonicalize(possibleChild);

        // Ignoring case, if the parent matches all the way up to the length of
        // the child...
        if (parentPath.regionMatches(true, 0, possibleChild, 0, parentPath.length())) {
            // If the paths are the same length, they are equal, and therefore
            // one is a child (see method JDoc).
            if (parentPath.length() == possibleChild.length()) {
                return true;
            }

            // If the parent ends with a separator (then the child also has one
            // in the right place), so it's a match.
            if (ServerPath.isSeparator(parentPath.charAt(parentPath.length() - 1))) {
                return true;
            }

            // If the child has a separator right beyond where we just did the
            // compare,
            // then it is a child.
            if (ServerPath.isSeparator(possibleChild.charAt(parentPath.length()))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a new version of the given repository path that is fully rooted
     * and canonicalized. Use this function to sanitize user input or to expand
     * partial paths encountered in server data. Note: strips trailing slashes
     * from directories, except $/.
     *
     * @param serverPath the repository path string to clean up.
     * @return the cleaned up path.
     * @throws ServerPathFormatException when the path cannot be cleaned up.
     */
    public final static String canonicalize(String serverPath) throws ServerPathFormatException {
        ArgumentHelper.checkNotNull(serverPath, "serverPath");

        int serverPathLength = serverPath.length();

        // empty string is not valid
        if (serverPathLength == 0) {
            logger.warn("Server path is empty");
            throw new ServerPathFormatException(serverPath);
        }

        /*
         * Do a quicker check up front to see if the path is OK. If it's not,
         * we'll try to fix it up and/or throw the appropriate exception.
         *
         * This is a huge win, since almost all of the server paths we ever
         * encounter in the program (and especially at large scale, like
         * translating hundreds of thousands of server paths to local paths) are
         * already canonicalized.
         */
        if (isCanonicalizedPath(serverPath, true)) {
            return serverPath;
        }

        final List<String> newComponents = new ArrayList<String>();
        final StringBuilder currentComponent = new StringBuilder(serverPathLength);
        int position = 0;

        // A simple conversion for $ to $/
        if (serverPath.equals(ServerPath.ROOT_NAME_ONLY)) {
            serverPath = ServerPath.ROOT;
            serverPathLength = 2;
        }

        // prepend a $ if necessary (next check will take care of the following /)
        if (serverPath.charAt(0) == '$') {
            position++;
        }

        newComponents.add("$");

        // the path must begin with one of: /, $/, \, $\
        if (position >= serverPathLength || ServerPath.isSeparator(serverPath.charAt(position)) == false) {
            logger.warn("The server path is not absolute: ", serverPath);
            throw new ServerPathFormatException(serverPath);
        }

        boolean illegalDollarInPath = false;

        // walk the rest of the given path
        for (; position <= serverPathLength; position++) {
            // end of the string or directory separators, append the current
            // component to the list
            if (position == serverPathLength || ServerPath.isSeparator(serverPath.charAt(position))) {
                // squash multiple concurrent separators
                if (currentComponent.length() == 0) {
                    // Ignore
                }
                // single dot components are thrown away
                else if (currentComponent.toString().equals(".")) {
                    // Ignore
                }
                // double dot components mean to pop the last off the stack
                else if (currentComponent.toString().equals("..")) {
                    if (newComponents.size() <= 1) {
                        logger.warn(String.format("Server path %s refers to invalid path outside %s", serverPath, ROOT));
                        throw new ServerPathFormatException(serverPath);
                    }

                    newComponents.remove(newComponents.size() - 1);
                }
                // otherwise, pop this bad boy on the directory stack
                else {
                    final String cleaned = ServerPath.cleanupComponent(currentComponent).toString();

                    /*
                     * Match Visual Studio behavior of ignoring directories that
                     * contain nothing but spaces and dots.
                     */
                    if (cleaned.length() == 0) {
                        // Ignore
                    } else if (cleaned.length() > MAXIMUM_COMPONENT_LENGTH) {
                        logger.warn(String.format("Server path component %s is longer than the maximum character length %s", cleaned, MAXIMUM_COMPONENT_LENGTH));
                        throw new ServerPathFormatException(cleaned);
                    } else if (FileHelper.isReservedName(cleaned)) {
                        logger.warn(String.format("Server path %s contains an invalid directory component: %s", serverPath, cleaned));
                        throw new ServerPathFormatException(cleaned);
                    } else {
                        if (cleaned.charAt(0) == '$') {
                            illegalDollarInPath = true;
                        }

                        // Good component, just add.
                        newComponents.add(cleaned);
                    }
                }

                currentComponent.setLength(0);
            }
            // a non-directory separator, non-dot, but valid in NTFS filenames
            else if (ServerPath.isValidPathCharacter(serverPath.charAt(position)) == true) {
                currentComponent.append(serverPath.charAt(position));
            }
            // invalid character
            else {
                char c = serverPath.charAt(position);
                char c_safe = c < ' ' ? '?' : c;
                logger.warn(String.format("At position %s, the character 0x%s (%s) is not permitted in server paths %s.",
                        String.format("%04x", (int) c), c_safe, serverPath.replace(c, c_safe)));
                throw new ServerPathFormatException(serverPath.replace(c, c_safe));
            }
        }

        if (newComponents.size() == 1) {
            return ServerPath.ROOT;
        }

        // join components with a slash
        final StringBuilder newPath = new StringBuilder();
        for (int i = 0; i < newComponents.size(); i++) {
            if (i > 0) {
                newPath.append(ServerPath.PREFERRED_SEPARATOR_CHARACTER);
            }

            newPath.append(newComponents.get(i));
        }

        /*
         * We were checking for illegal dollar in the path during the loop
         * through the string. Throw the same exception as checkForIllegalDollar
         * would, if the flag was raised stating we had an illegal dollar
         * somewhere.
         */
        if (illegalDollarInPath) {
            logger.warn(String.format("TF10122: The path %s contains a $ at the beginning of a path component.", newPath.toString()));
            throw new ServerPathFormatException(newPath.toString());
        }

        return newPath.toString();
    }

    /**
     * Returns true if the path is canonicalized. The path must not contain a $
     * at the beginning of a path part, or any illegal characters.
     */
    public static boolean isCanonicalizedPath(final String serverItem, final boolean allowSemicolon) {
        if (serverItem.length() > MAX_SERVER_PATH_SIZE) {
            return false;
        }

        // The path is not legal if it does not start with $/.
        if (!serverItem.startsWith(ROOT)) {
            return false;
        }

        // If the path is $/, it is legal.
        if (2 == serverItem.length()) {
            return true;
        }

        // The path is not legal if it ends with a separator character.
        if (serverItem.length() > 2 && serverItem.charAt(serverItem.length() - 1) == PREFERRED_SEPARATOR_CHARACTER) {
            return false;
        }

        int pathPartLength = 0;

        for (int i = 2; i < serverItem.length(); i++) {
            final char c = serverItem.charAt(i);

            if (c == PREFERRED_SEPARATOR_CHARACTER) {
                if (!isCanonicalizedPathPart(serverItem, i, pathPartLength)) {
                    return false;
                }

                pathPartLength = 0;
                continue;
            }

            // The $ character is not permitted to lead a path part.
            if (0 == pathPartLength && c == ROOT.charAt(0)) {
                return false;
            }

            // Look up each character in the NTFS valid characters truth table.
            if (!FileHelper.isValidNTFSFileNameCharacter(c)) {
                return false;
            }

            // The semicolon character is not legal anywhere in a version
            // control path.
            if (!allowSemicolon && c == ';') {
                return false;
            }

            // Wildcard characters are not legal in a version control path.
            if (c == '*' || c == '?') {
                return false;
            }

            pathPartLength++;
        }

        // Check the last path part.
        if (!isCanonicalizedPathPart(serverItem, serverItem.length(), pathPartLength)) {
            return false;
        }

        return true;
    }

    private static boolean isCanonicalizedPathPart(final String serverItem, final int i, final int pathPartLength) {
        // It's not legal to have two separators next to each other.
        if (0 == pathPartLength) {
            return false;
        } else if (2 == pathPartLength) {
            // It's not legal to have a path part which is just '..'
            if (serverItem.charAt(i - 1) == '.' && serverItem.charAt(i - 2) == '.') {
                return false;
            }
        } else if (3 == pathPartLength || 4 == pathPartLength) {
            // All the reserved names are of length 3 or 4 (NUL, COM1, etc.)
            if (FileHelper.isReservedName(serverItem.substring(i - pathPartLength, i))) {
                return false;
            }
        } else if (pathPartLength > MAXIMUM_COMPONENT_LENGTH) {
            return false;
        }

        if (serverItem.charAt(i - 1) == '.' || Character.isWhitespace(serverItem.charAt(i - 1))) {
            // It is not legal to end a path part with whitespace or a dot.
            return false;
        }

        return true;
    }

    /**
     * <p>
     * Returns a new string describing the first given path made relative to the
     * second given path.
     * </p>
     * <p>
     * Character case is ignored during string comparison, so strings with
     * mismatched-in-case common elements will still succeed in being made
     * relative.
     * </p>
     * <p>
     * Paths are not normalized (for ending separators, case, etc.). It is the
     * caller's responsibility to make sure the relativeTo path can be matched.
     * </p>
     *
     * @param serverPath the path to the server item to describe (must not be
     *                   <code>null</code>)
     * @param relativeTo the path that the first parameter will be described relative to
     *                   (must not be <code>null</code>)
     * @return the relative path, or the unaltered given server path if it could
     * not be made relative to the second path.
     */
    public static String makeRelative(final String serverPath, final String relativeTo) {
        ArgumentHelper.checkNotNull(serverPath, "serverPath");
        ArgumentHelper.checkNotNull(relativeTo, "relativeTo");

        /*
         * Use regionMatches() for a locale-invariant "starts with" test.
         */
        if (serverPath.regionMatches(true, 0, relativeTo, 0, relativeTo.length())) {
            /*
             * Compare lengths of canonical strings.
             */
            if (serverPath.length() == relativeTo.length()) {
                return StringUtils.EMPTY;
            }

            /*
             * If the relativeTo path ends in a separator, we have a relative
             * path to express.
             */
            if (relativeTo.length() > 0 && ServerPath.isSeparator(relativeTo.charAt(relativeTo.length() - 1))) {
                return serverPath.substring(relativeTo.length());
            }

            /*
             * If the given path's last character is a separator, then we also
             * have a relative path.
             */
            if (ServerPath.isSeparator(serverPath.charAt(relativeTo.length()))) {
                return serverPath.substring(relativeTo.length() + 1);
            }
        }

        return serverPath;
    }

    /**
     * <p>
     * Maps a server path to a local path, given a parent server path of the
     * path to be mapped, and a local path that corresponds to the parent.
     * </p>
     * <p>
     * Character case is ignored during string comparison, so strings with
     * mismatched-in-case common elements will still succeed in being made
     * relative.
     * </p>
     * <p>
     * Paths are not normalized (for ending separators, case, etc.). It is the
     * caller's responsibility to make sure the relativeToServerPath path can be
     * matched.
     * </p>
     *
     * @param serverPath           the server path to convert to a local path (must not be
     *                             <code>null</code>)
     * @param relativeToServerPath the parent server path (must not be <code>null</code> and must be
     *                             a parent of <code>serverPath</code>)
     * @param localRoot            the local path that corresponds to
     *                             <code>relativeToServerPath</code> (must not be <code>null</code>)
     * @return the corresponding local path (never <code>null</code>)
     */
    public static String makeLocal(final String serverPath, final String relativeToServerPath, final String localRoot) {
        ArgumentHelper.checkNotNull(serverPath, "serverPath");
        ArgumentHelper.checkNotNull(relativeToServerPath, "relativeToServerPath");
        ArgumentHelper.checkNotNull(localRoot, "localRoot");

        // ServerPath.canonicalize() checks for illegal dollar
        final String relativePart = ServerPath.makeRelative(ServerPath.canonicalize(serverPath), relativeToServerPath);

        /*
         * Convert any allowed separator characters into this platform's
         * preferred separator.
         */
        final StringBuilder relativeBuffer = new StringBuilder(relativePart);
        for (int j = 0; j < ServerPath.SEPARATOR_CHARACTERS.length; j++) {
            for (int k = 0; k < relativeBuffer.length(); k++) {
                if (relativeBuffer.charAt(k) == ServerPath.SEPARATOR_CHARACTERS[j]) {
                    relativeBuffer.setCharAt(k, File.separatorChar);
                }
            }
        }

        return LocalPath.combine(localRoot, relativeBuffer.toString());
    }

    /**
     * Strips trailing spaces and dots from a pathname, for canonicalization.
     *
     * @param s {@link StringBuilder} to strip
     * @return Stripped {@link StringBuilder} (may be empty)
     */
    private final static StringBuilder cleanupComponent(final StringBuilder s) {
        while (s.length() > 0
                && (s.charAt(s.length() - 1) == '.' || Character.isWhitespace(s.charAt(s.length() - 1)))) {
            s.setLength(s.length() - 1);
        }

        return s;
    }

    /**
     * Tests whether the given character is valid in a repository path component
     * (file/folder name).
     *
     * @param c the character to test.
     * @return true if the character is allowed in a path component
     * (file/folder), false if not.
     */
    public final static boolean isValidPathCharacter(final char c) {
        final char[] invalidCharacters = {
                '"',
                '/',
                ':',
                '<',
                '>',
                '\\',
                '|'
        };

        // All the control characters are not allowed.
        if (c <= 31) {
            return false;
        }

        for (int i = 0; i < invalidCharacters.length; i++) {
            if (invalidCharacters[i] == c) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests whether the given character is a valid repository path separator
     * character (as defined by {@link ServerPath#SEPARATOR_CHARACTERS}).
     *
     * @param c the character to test.
     * @return true if the character is a valid separator character, false
     * otherwise.
     */
    public final static boolean isSeparator(final char c) {
        for (int i = 0; i < ServerPath.SEPARATOR_CHARACTERS.length; i++) {
            if (ServerPath.SEPARATOR_CHARACTERS[i] == c) {
                return true;
            }
        }

        return false;
    }
}