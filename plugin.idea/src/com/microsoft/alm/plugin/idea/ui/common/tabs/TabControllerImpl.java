// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.tabs;

import com.microsoft.alm.plugin.events.ServerEvent;
import com.microsoft.alm.plugin.events.ServerEventListener;
import com.microsoft.alm.plugin.events.ServerEventManager;
import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.idea.utils.EventContextHelper;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * Generic controller for the VC tab
 */
public abstract class TabControllerImpl<T extends TabModel> implements TabController, Observer, ActionListener {
    protected Tab tab;
    protected T model;

    public TabControllerImpl(@NotNull final Tab tab, @NotNull T model, @NotNull final ServerEvent[] eventFilters) {
        this.tab = tab;
        this.model = model;

        setupTab();

        // Initialize the form with the current values from the model
        update(null, null);

        // add the observer and action listener after we are fully initialized, otherwise we will just get called
        // in the middle
        model.addObserver(this);

        //load the items
        model.loadData();

        // Hook up to the server changed events so that we can auto update when the server changes
        ServerEventManager.getInstance().addListener(new ServerEventListener() {
            @Override
            public void serverChanged(final ServerEvent event, final Map<String, Object> contextMap) {
                for (final ServerEvent filter : eventFilters) {
                    if (filter == event) {
                        handleServerChangedEvent(event, contextMap);
                        break;
                    }
                }
            }
        });
    }

    /**
     * The default behavior is to simply refresh the tab. If a subclass needs to do additional work or change the
     * behavior, they should override this method.
     * Note that this method will not be called except for events that are in the eventFilters passed into the constructor.
     *
     * @param event      the event that was triggered.
     * @param contextMap the map of name value pairs that make up the context of this event.
     */
    protected void handleServerChangedEvent(ServerEvent event, Map<String, Object> contextMap) {
        if (EventContextHelper.isProjectClosing(contextMap)) {
            // Nothing to do in this case, just ignore it
            return;
        }

        // Push back onto the UI thread and refresh the model
        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                model.loadData();
            }
        });
    }

    private void setupTab() {
        tab.addActionListener(this);
        tab.addObserver(this);
    }

    public void actionPerformed(final ActionEvent e) {
        updateModel();
        performAction(e);
    }

    /**
     * Perform action based on event
     *
     * @param e
     */
    protected void performAction(final ActionEvent e) {
        if (TabForm.CMD_STATUS_LINK.equals(e.getActionCommand())) {
            if (model.getTabStatus() == VcsTabStatus.NOT_TF_GIT_REPO) {
                //import into team services git
                model.importIntoTeamServicesGit();
            } else if (model.getTabStatus() == VcsTabStatus.NO_AUTH_INFO) {
                //prompt for credentials and load items
                model.loadData();
            } else {
                //open current repository in web
                model.openGitRepoLink();
            }
        }
    }

    public JComponent getPanel() {
        return tab.getPanel();
    }

    @Override
    public void update(final Observable observable, final Object arg) {
        if (arg == null
                || TabModel.PROP_TAB_STATUS.equals(arg)) {
            tab.setStatus(model.getTabStatus());
        }
        if (arg == null) {
            tab.setViewModel(model.getModelForView());
        }
        if (arg == null || arg.equals(TabModel.PROP_FILTER)) {
            tab.setFilter(model.getFilter());
        }

        //actions from the form
        if (TabForm.CMD_CREATE_NEW_ITEM.equals(arg)) {
            model.createNewItem();
        }
        if (TabForm.CMD_REFRESH.equals(arg)) {
            model.loadData();
            tab.refresh();
        }
        if (TabForm.CMD_OPEN_SELECTED_ITEM_IN_BROWSER.equals(arg)) {
            model.openSelectedItemsLink();
        }
    }

    protected void updateModel() {
        model.setFilter(tab.getFilter());
        model.setOperationInputs(tab.getOperationInputs());
    }

    public void dispose() {
        model.dispose();
    }
}
