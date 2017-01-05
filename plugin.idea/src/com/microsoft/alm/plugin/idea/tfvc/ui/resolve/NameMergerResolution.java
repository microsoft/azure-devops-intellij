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

import org.apache.commons.lang.StringUtils;

public class NameMergerResolution {
    final String theirNameChoice;
    final String myNameChoice;
    final String userSelection;
    String resolvedLocalPath;

    public NameMergerResolution(final String theirNameChoice, final String myNameChoice, final String userSelection) {
        this.theirNameChoice = theirNameChoice;
        this.myNameChoice = myNameChoice;
        this.userSelection = userSelection;
    }

    public String getMyNameChoice() {
        return myNameChoice;
    }

    public String getTheirNameChoice() {
        return theirNameChoice;
    }

    public String getUserSelection() {
        return userSelection;
    }

    public boolean userChoseTheirs() {
        return StringUtils.equalsIgnoreCase(theirNameChoice, userSelection);
    }

    public void setResolvedLocalPath(String resolvedLocalPath) {
        this.resolvedLocalPath = resolvedLocalPath;
    }

    public String getResolvedLocalPath() {
        return resolvedLocalPath;
    }
}
