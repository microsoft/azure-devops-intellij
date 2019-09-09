// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.settings;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.external.commands.ToolEulaNotAcceptedException;
import com.microsoft.alm.plugin.external.tools.TfTool;
import com.microsoft.alm.plugin.external.utils.ProcessHelper;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.jar.JarFile;

public class EULADialog extends DialogWrapper {
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(EULADialog.class);
    private static boolean myWasShow = false;
    private final JBScrollPane myScrollPane;
    private final Boolean myEulaTextFound;

    private EULADialog(@Nullable Project project) {
        super(project);
        String eulaText = getEULAText();
        myEulaTextFound = eulaText != null;

        JTextArea textArea = new JTextArea();
        textArea.setText(eulaText);
        textArea.setMinimumSize(null);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        textArea.setBackground(null);
        textArea.setBorder(JBUI.Borders.empty());

        myScrollPane = new JBScrollPane(textArea);
        myScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        setModal(true);
        setTitle("Team Foundation Command-Line Client EULA");

        setOKButtonText("Accept");
        getOKAction().putValue(DEFAULT_ACTION, null);

        setCancelButtonText("Decline");
        getCancelAction().putValue(DEFAULT_ACTION, Boolean.TRUE);

        init();
    }

    @Nullable
    private String getEULAText() {
        JarFile jarFile = null;
        InputStream eulaStream = null;
        String jarName = "com.microsoft.tfs.client.common.jar";
        try {
            String tfLocation = TfTool.getLocation();
            if (tfLocation == null) {
                return null;
            }

            String path = Path.combine(Path.combine(Path.getDirectoryName(tfLocation), "lib"), jarName);
            jarFile = new JarFile(path);
            eulaStream = jarFile.getInputStream(jarFile.getEntry("eula.txt"));

            return StreamUtil.readText(eulaStream, Charsets.UTF_8);
        } catch (IOException e) {
            logger.error("Cannot read EULA text from: " + jarName, e);
        } finally {
            if (eulaStream != null) {
                try {
                    eulaStream.close();

                } catch (IOException e) {
                    logger.error("Cannot eulaStream properly", e);
                }
            }
            if (jarFile != null) {
                try {
                    jarFile.close();

                } catch (IOException e) {
                    logger.error("Cannot jarFile properly", e);
                }
            }
        }
        return null;
    }

    @Override
    public void show() {
        if (myEulaTextFound) {
            super.show();
        }
    }

    public static void acceptEula() throws IOException, InterruptedException {
        String tfLocation = TfTool.getLocation();
        Process process = ProcessHelper.startProcess(
                Path.getDirectoryName(tfLocation), Arrays.asList(tfLocation, "eula", SystemInfo.isWindows ? "/accept" : "-accept"));
        process.waitFor();
    }

    @Override
    public void doOKAction() {
        try {
            acceptEula();
            close(OK_EXIT_CODE);
        } catch (Exception e) {
            logger.error("Can't accept EULA", e);
            doCancelAction();
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return myScrollPane;
    }

    /**
     * Shows the EULA dialog if it wasn't previously shown in the current session.
     *
     * @param project project for which the dialog should be shown.
     * @return null if dialog was previously shown; true if dialog was shown and the user clicked "Accept"; false if the
     * dialog was shown and the user clicked "Decline".
     */
    @Nullable
    public static synchronized Boolean showDialogIfNeeded(final Project project) {
        if (!myWasShow) {
            boolean result = new EULADialog(project).showAndGet();
            myWasShow = true;
            return result;
        }

        return null;
    }

    /**
     * Executes the activity and shows the EULA dialog if necessary.
     *
     * @param activity activity to execute on the current thread. May be executed twice if it throws a
     *                 {@link ToolEulaNotAcceptedException}.
     * @throws ToolEulaNotAcceptedException will be thrown if the user was presented by the EULA dialog and didn't
     *                                      accepted it.
     */
    public static <T> T executeWithGuard(@NotNull Project project, @NotNull Supplier<T> activity) {
        T result;
        try {
            result = activity.get();
        } catch (ToolEulaNotAcceptedException ex) {
            logger.warn("EULA not accepted; showing EULA dialog");

            AtomicBoolean isAccepted = new AtomicBoolean();

            IdeaHelper.runOnUIThread(() -> {
                Boolean wasAccepted = EULADialog.showDialogIfNeeded(project);
                if (wasAccepted != null) {
                    logger.info("EULADialog.showDialogIfNeeded result: {}", wasAccepted);
                    isAccepted.set(wasAccepted);
                }
            }, true);

            if (isAccepted.get()) {
                logger.info("EULA accepted; repeating the operation");
                result = activity.get();
            } else {
                logger.error("EULA was declined by the user");
                throw ex;
            }
        }

        return result;
    }
}
