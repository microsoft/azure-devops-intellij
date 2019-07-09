// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.versioncontrol.path;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.common.utils.FileHelper;
import org.apache.commons.lang.StringUtils;

import java.io.File;

/**
 * Taken from team-explorer-everywhere: source/com.microsoft.tfs.core/src/com/microsoft/tfs/core/clients/versioncontrol/path/LocalPath.java
 */
public abstract class LocalPath {
    public final static char TFS_PREFERRED_LOCAL_PATH_SEPARATOR = '\\';

    public final static String GENERAL_LOCAL_PATH_SEPARATOR = "/";

    /**
     * <p>
     * Tests the given paths for a parent-child relationship. A path is a child
     * of another if the object it describes would reside below the object
     * described by the parent path in the local filesystem. Case is respected
     * on a per-platform basis.
     * <p>
     * </p>
     * A possible child that is equivalent to the parent path (both refer to the
     * same object) is considered a child. This is compatible with Visual
     * Studio's behavior.
     * </p>
     * <p>
     * The given paths are not canonicalized before testing.
     * </p>
     * <p>
     * The given paths must be in the OS's native path format.
     * </p>
     *
     * @param parentPath    the local path to the parent item (not null).
     * @param possibleChild the local path of the possible child item (not null).
     * @return true if possibleChild is a child of parentPath, false otherwise
     * (including I/O errors accessing either path).
     */
    public final static boolean isChild(final String parentPath, final String possibleChild) {
        ArgumentHelper.checkNotNull(parentPath, "parentPath");
        ArgumentHelper.checkNotNull(possibleChild, "possibleChild");

        // See this methods Javadoc for why this is true.
        if ((FileHelper.doesFileSystemIgnoreCase() && parentPath.equalsIgnoreCase(possibleChild))
                || parentPath.equals(possibleChild)) {
            return true;
        }

        final File parent = new File(parentPath);
        final File child = new File(possibleChild);

        /*
         * This may be less efficient than is otherwise possible, but it should
         * respect all the platform rules about file and directory naming. We
         * walk up the possible child's path, testing each parent directory
         * along the way. If it matches the given parentPath, the possible child
         * is indeed a child.
         */

        File tmp = child.getParentFile();
        while (tmp != null) {
            // If this temp parent is equal to the given parent, we have a
            // match.
            if (tmp.equals(parent)) {
                return true;
            }

            // Keep walking back up.
            tmp = tmp.getParentFile();
        }

        return false;
    }

    /**
     * Combines the two given paths into one path string, using this platform's
     * preferred path separator character. If relative is an absolute path, it
     * is returned as the entire return value (parent is discarded).
     *
     * @param parent   the first (left-side) path component (must not be
     *                 <code>null</code>)
     * @param relative the second (right-side) path component (must not be
     *                 <code>null</code>)
     */
    public final static String combine(final String parent, final String relative) {
        ArgumentHelper.checkNotNull(parent, "parent");
        ArgumentHelper.checkNotNull(relative, "relative");

        /*
         * Return the relative path if it's already absolute, since I'm not sure
         * Java's File does this in the same way.
         */
        final File relativeFile = new File(relative);
        if (relativeFile.isAbsolute()) {
            return relative;
        }

        /*
         * Let Java combine the paths.
         */
        return new File(parent, relative).getAbsolutePath();
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
     * @param localPath  the path to the local item to describe (must not be
     *                   <code>null</code>)
     * @param relativeTo the path that the first parameter will be described relative to
     *                   (must not be <code>null</code>)
     * @return the relative path, or the unaltered given local path if it could
     * not be made relative to the second path.
     */
    public static String makeRelative(final String localPath, final String relativeTo) {
        ArgumentHelper.checkNotNull(localPath, "localPath");
        ArgumentHelper.checkNotNull(relativeTo, "relativeTo");

        /*
         * The absolute path must start with the relativeTo path, else it's not
         * relative. We're using a case-insensitve compare because any working
         * folder items won't be allowed to conflict in case only (because the
         * server would not allow it).
         *
         * Use regionMatches() for a locale-invariant "starts with" test.
         */
        if (localPath.regionMatches(true, 0, relativeTo, 0, relativeTo.length())) {
            if (localPath.length() == relativeTo.length()) {
                return StringUtils.EMPTY;
            }

            /*
             * If the relativeTo path ends in a separator, we have a relative
             * path to express.
             */
            if (relativeTo.length() > 0 && relativeTo.charAt(relativeTo.length() - 1) == File.separatorChar) {
                return localPath.substring(relativeTo.length());
            }

            /*
             * If the given path's last character is a separator, then we also
             * have a relative path.
             */
            if (localPath.charAt(relativeTo.length()) == File.separatorChar) {
                return localPath.substring(relativeTo.length() + 1);
            }
        }

        /*
         * Return the local path unaltered.
         */
        return localPath;
    }

    /**
     * <p>
     * Maps a local path to a server path, given a parent local path of the path
     * to be mapped, and a server path that corresponds to the parent.
     * </p>
     * <p>
     * Character case is ignored during string comparison, so strings with
     * mismatched-in-case common elements will still succeed in being made
     * relative.
     * </p>
     * <p>
     * Paths are not normalized (for ending separators, case, etc.). It is the
     * caller's responsibility to make sure the relativeToLocalPath path can be
     * matched.
     * </p>
     *
     * @param localPath           the local path to convert to a server path (must not be
     *                            <code>null</code>)
     * @param relativeToLocalPath the parent local path (must not be <code>null</code> and must be a
     *                            parent of <code>localPath</code>)
     * @param serverRoot          the server path that corresponds to
     *                            <code>relativeToLocalPath</code> (must not be <code>null</code>)
     * @return the corresponding server path (never <code>null</code>)
     */
    public static String makeServer(final String localPath, final String relativeToLocalPath, final String serverRoot) {
        ArgumentHelper.checkNotNull(localPath, "localPath");
        ArgumentHelper.checkNotNull(relativeToLocalPath, "relativeToLocalPath");
        ArgumentHelper.checkNotNull(serverRoot, "serverRoot");

        final String relativePart = LocalPath.makeRelative(localPath, relativeToLocalPath);

        /*
         * Convert this platform's separator characters into TFS's separator
         * character.
         */
        final StringBuilder relativeBuffer = new StringBuilder(relativePart);
        for (int k = 0; k < relativeBuffer.length(); k++) {
            if (relativeBuffer.charAt(k) == File.separatorChar) {
                relativeBuffer.setCharAt(k, ServerPath.PREFERRED_SEPARATOR_CHARACTER);
            }
        }

        /*
         * If the relative path begins with a separator, remove it, so we can
         * always add one later.
         */
        if (relativeBuffer.length() > 0 && relativeBuffer.charAt(0) == ServerPath.PREFERRED_SEPARATOR_CHARACTER) {
            relativeBuffer.deleteCharAt(0);
        }

        /*
         * ServerPath.combine() would work to combine these parts, but it
         * doesn't check for illegal dollars in the path (it specifically
         * permits the relative part to start with a dollar; other dollars are
         * caught--legacy behavior).
         *
         * We've enusred the relative part doesn't start with a separator, so
         * combine here.
         */

        // Checks for illegal dollar
        return ServerPath.canonicalize(
                serverRoot + ServerPath.PREFERRED_SEPARATOR_CHARACTER + relativeBuffer.toString(), true);
    }

    /**
     * Returns the given path string without trailing separators (as specified
     * by File.separator).
     *
     * @param path the string to strip trailing separators from.
     * @return the given string with all trailing separators removed.
     */
    public static String removeTrailingSeparators(final String path) {
        ArgumentHelper.checkNotNull(path, "path");

        final int length = path.length();
        int index = path.length() - 1;

        while (index > 0 && path.charAt(index) == File.separatorChar) {
            index--;
        }

        return index < length - 1 ? path.substring(0, index + 1) : path;
    }
}