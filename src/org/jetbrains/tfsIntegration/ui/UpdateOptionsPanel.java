package org.jetbrains.tfsIntegration.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.tfsIntegration.core.TFSProjectConfiguration;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.TfsPanel;
import org.jetbrains.tfsIntegration.core.tfs.UpdateWorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UpdateOptionsPanel implements TfsPanel {
    private JPanel myPanel;
    private JRadioButton latestRadioButton;
    private JRadioButton dateRadioButton;
    private JRadioButton changesetRadioButton;
    private JRadioButton labelRadioButton;
    private JRadioButton workspaceRadioButton;
    private TextFieldWithBrowseButton labelVersionText;
    private TextFieldWithBrowseButton changesetVersionText;
    private JLabel workspaceNameLabel;
    private JTextField dateText;

    private WorkspaceInfo workspace;
    private TFSVcs myTfsVcs;

    public UpdateOptionsPanel(WorkspaceInfo workspace, TFSVcs vcs) {
        this.workspace = workspace;
        this.myTfsVcs = vcs;

        latestRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disableAllVersionInfo();
            }
        });
        dateRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disableAllVersionInfo();
                if (dateRadioButton.isSelected()) {
                    setDateTimePickerEnabled(true);
                    if ("".equals(dateText.getText())) {
                        setDate(new Date(System.currentTimeMillis()));
                    }
                }
            }
        });
        changesetRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disableAllVersionInfo();
                if (changesetRadioButton.isSelected()) {
                    changesetVersionText.setEnabled(true);
                }
            }
        });
        labelRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disableAllVersionInfo();
                if (labelRadioButton.isSelected()) {
                    labelVersionText.setEnabled(true);
                }
            }
        });
        workspaceRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disableAllVersionInfo();
            }
        });

        changesetVersionText.getButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // TODO: implementation for changesetVersionText browse button
            }
        });

        labelVersionText.getButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SelectLabelDailog dialog = new SelectLabelDailog(myTfsVcs, UpdateOptionsPanel.this.workspace);
                dialog.show();
                if (dialog.isOK()) {
                    labelVersionText.setText(dialog.getLabelString());
                }
            }
        });
        disableAllVersionInfo();
    }

    private void setDateTimePickerEnabled(boolean enabled) {
        dateText.setEnabled(enabled);
    }

    private void disableAllVersionInfo() {
        setDateTimePickerEnabled(false);
        labelVersionText.setEnabled(false);
        changesetVersionText.setEnabled(false);
    }

    private void createUIComponents() {
    }

    public void reset(TFSProjectConfiguration configuration) {
        final UpdateWorkspaceInfo info = configuration.getUpdateWorkspaceInfo(workspace);
        VersionSpecBase version = info.getVersion();
        if (version instanceof LatestVersionSpec) {
            latestRadioButton.setSelected(true);
        }
        else if (version instanceof ChangesetVersionSpec) {
            int changeset = ((ChangesetVersionSpec)version).getChangeSetId();
            changesetRadioButton.setSelected(true);
            changesetVersionText.setEnabled(true);
            changesetVersionText.setText(Integer.toString(changeset));
        }
        else if (version instanceof WorkspaceVersionSpec) {
            String workspaceName = ((WorkspaceVersionSpec) version).getWorkspaceName();
            workspaceRadioButton.setSelected(true);
            workspaceNameLabel.setText(workspaceName);
        }
        else if (version instanceof DateVersionSpec) {
            Date date = ((DateVersionSpec) version).getDate();
            dateRadioButton.setSelected(true);
            setDateTimePickerEnabled(true);
            setDate(date);
        }
        else if (version instanceof LabelVersionSpec) {
            String label = ((LabelVersionSpec) version).getLabel();
            labelRadioButton.setSelected(true);
            labelVersionText.setEnabled(true); 
            labelVersionText.setText(label); // TODO: what about label scope?
        }
    }

    public void apply(TFSProjectConfiguration configuration) throws ConfigurationException {
        final UpdateWorkspaceInfo info = configuration.getUpdateWorkspaceInfo(workspace);
        if (latestRadioButton.isSelected()) {
            info.setVersion(LatestVersionSpec.INSTANCE);
        }
        else if (changesetRadioButton.isSelected()) {
            try {
                int changeset = Integer.parseInt(changesetVersionText.getText());
                info.setVersion(new ChangesetVersionSpec(changeset));
            }
            catch (NumberFormatException e) {
                throw new ConfigurationException("Invalid change set id '" + changesetVersionText.getText() + "'");
            }
        }
        else if (workspaceRadioButton.isSelected()) {
            workspaceRadioButton.setSelected(true);
            info.setVersion(new WorkspaceVersionSpec(workspace.getName(), workspace.getOwnerName()));
        }
        else if (dateRadioButton.isSelected()) {
            Date date = getDate();
            info.setVersion(new DateVersionSpec(date));
        }
        else if (labelRadioButton.isSelected()) {
            String labelString = labelVersionText.getText();
            String label = LabelVersionSpec.getLabel(labelString);
            String scope = LabelVersionSpec.getScope(labelString);
            info.setVersion(new LabelVersionSpec(label, scope));
        }
    }

    public boolean canApply() {
        return true;
    }

    public JPanel getPanel() {
        return myPanel;
    }

    public void setDate(Date date) {
        if (date == null)  {
            return;
        }
        dateText.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date));    }

    public Date getDate() throws ConfigurationException {
        Date date;
        try {
            date = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).parse(dateText.getText());
        } catch (ParseException e) {
            throw new ConfigurationException("Incorrect date: '" + dateText.getText() + "'");
        }
        return date;
    }

}

