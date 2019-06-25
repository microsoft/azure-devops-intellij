// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.utils;

import com.sun.jna.Platform;

/**
 * Taken from team-explorer-everywhere: source/com.microsoft.tfs.util/src/com/microsoft/tfs/util/FileHelpers.java
 */
public final class FileHelper {

    /**
     * Truth table based on INVALID_NTFS_FILE_NAME_CHARACTERS.
     */
    // @formatter:off
    public final static boolean[] VALID_NTFS_FILE_NAME_CHAR_TABLE = {
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            true, true, false, true, true, true, true, true, true, true, true, true, true, true, true, false,
            true, true, true, true, true, true, true, true, true, true, false, true, false, true, false, true,
            true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, true, true, false, true, true, true,
            true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, true, true, false, true, true, true
    };
    // @formatter:on

    /**
     * All entries in this list have length 3, and the implementation of
     * isReservedName depends upon this If you change the contents of this list
     * you will likely need to modify IsReservedName
     */
    private final static String[] RESERVED_NAMES_LENGTH3 = {
            "CON",
            "PRN",
            "AUX",
            "NUL"
    };

    private static final String FORCE_HONOR_CASE_SYSPROP = "com.microsoft.tfs.util.FileHelpers.force-honor-case";

    private static final String FORCE_IGNORE_CASE_SYSPROP = "com.microsoft.tfs.util.FileHelpers.force-ignore-case";

    /**
     * Statically initialized with the result of a filesystem case sensitivity
     * test.
     */
    private static boolean fileSystemIgnoresCase;

    static {
        /*
         * I dont know of a great way to test the running "platform" for
         * case-sensitivity in filesystems, because that behavior is usually a
         * property of the filesystem in use (and there may be many of those at
         * once).
         *
         * Sun/Oracle's Java File class simply hard-codes case-insensitive path
         * compares on Windows, case-sensitive on Unix, even though these
         * systems could be using filesystems that do the opposite.
         *
         * So here's a simple hard-coded test.
         */
        if (System.getProperty(FORCE_IGNORE_CASE_SYSPROP) != null) {
            fileSystemIgnoresCase = true;
        } else if (System.getProperty(FORCE_HONOR_CASE_SYSPROP) != null) {
            fileSystemIgnoresCase = false;
        } else if (Platform.isWindows() || Platform.isMac()) {
            fileSystemIgnoresCase = true;
        } else {
            // Generic Unix and unknown
            fileSystemIgnoresCase = false;
        }
    }

    /**
     * @return true if the filesystem this JVM is running on ignores case when
     * comparing file names (new File("A").equals(new File("a"))), false
     * if it does not.
     */
    public static boolean doesFileSystemIgnoreCase() {
        return fileSystemIgnoresCase;
    }

    /**
     * Check if the specified name is in the list of reserved NTFS names.
     *
     * @param name the file name to check
     * @return true if name is a reserved NTFS file name
     */
    public static boolean isReservedName(final String name) {
        /*
         * This method gets called *often* and is written for speed, even to the
         * point of being fragile with respect to changes to the reservedNames
         * and reservedNamesLength3 lists. Changes to the list of reserved names
         * will likely require code changes here.
         */

        // LPT1 -> LPT9, COM1 -> COM9 are reserved names.
        // LPT0 and COM0 are NOT reserved names.
        if (name.length() == 4 && Character.isDigit(name.charAt(3)) && name.charAt(3) != '0') {
            final String firstThree = name.substring(0, 3);
            if (firstThree.equalsIgnoreCase("LPT") || firstThree.equalsIgnoreCase("COM")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                return true;
            }
        }

        // All of the strings in reservedNamesLength3 are length 3.
        if (name.length() == 3) {
            for (int i = 0; i < RESERVED_NAMES_LENGTH3.length; i++) {
                if (name.equalsIgnoreCase(RESERVED_NAMES_LENGTH3[i])) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Tests whether the given character is valid in an NTFS file name.
     *
     * @param c the character to test
     * @return true if the character is allowed in NTFS file names, false if it
     * is disallowed
     */
    public static boolean isValidNTFSFileNameCharacter(final char c) {
        // All of our illegal characters are in the ASCII range (0x00 -> 0x7f),
        // so if this character has a code point higher than 0x7f, it must be
        // valid.
        if (c > '\u007f') {
            return true;
        }

        // This character is in our truth table.
        return VALID_NTFS_FILE_NAME_CHAR_TABLE[c];
    }
}