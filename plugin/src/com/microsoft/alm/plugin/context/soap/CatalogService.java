// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context.soap;

import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;

import java.util.List;

public interface CatalogService {

    List<TeamProjectCollectionReference> getProjectCollections();

    TeamProjectCollectionReference getProjectCollection(final String collectionName);
}
