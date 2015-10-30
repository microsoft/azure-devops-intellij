// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

import com.microsoft.alm.plugin.context.ServerContext;

import java.net.URI;
import java.util.List;

/**
 * This interface represents the service that allows us to load/save/forget server context information.
 */
public interface ServerContextStore {

    static class Key {
        private final String host;

        private Key(final String host) {
            assert host != null;
            this.host = host;
        }

        public static Key create(final URI uri) {
            return new Key(uri.getHost());
        }

        public static Key create(final ServerContext context) {
            return create(context.getUri());
        }

        public String stringValue() {
            return host;
        }

        public String toString() {
            return "host: " + host;
        }
    }

    void forgetServerContext(final Key key);

    List<ServerContext> restoreServerContexts();

    void saveServerContext(final ServerContext context);
}
