// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

public class MergeNameDialog extends DialogWrapper {
    private MergeNameForm myMergeNameForm;
    private final String myLocalName;
    private final String myServerName;
    private final Project myProject;

    public MergeNameDialog(final String yourName, String theirsName, Project project) {
        super(project, false);
        myLocalName = yourName;
        myServerName = theirsName;
        myProject = project;
        setTitle(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_NAME_DIALOG));
        setResizable(true);
        init();
    }

    @Nullable
    protected JComponent createCenterPanel() {
        myMergeNameForm = new MergeNameForm(myLocalName, myServerName);

        // TODO comment back in when allowing user input
//        myMergeNameForm.addListener(new MergeNameForm.Listener() {
//            public void selectedPathChanged() {
//                String errorMessage = validate(myMergeNameForm.getSelectedPath());
//                myMergeNameForm.setErrorText(errorMessage);
//                setOKActionEnabled(errorMessage == null);
//            }
//        });
        return myMergeNameForm.getPanel();
    }

    @NotNull
    public String getSelectedPath() {
        return myMergeNameForm.getSelectedPath();
    }

    // TODO comment back in when using user input
//    @Nullable
//    private String validate(String path) {
//        if (path == null || path.length() == 0) {
//            return "Path is empty";
//        }
//        return null;
//    }
}
