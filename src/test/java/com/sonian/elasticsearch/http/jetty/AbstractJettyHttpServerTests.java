/*
 * Copyright 2011 Sonian Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sonian.elasticsearch.http.jetty;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.network.NetworkUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalNode;

import java.util.Map;
import java.util.Set;

import static org.elasticsearch.common.collect.Maps.*;
import static org.elasticsearch.common.collect.Sets.*;
import static org.elasticsearch.common.settings.ImmutableSettings.Builder.*;
import static org.elasticsearch.common.settings.ImmutableSettings.*;
import static org.elasticsearch.node.NodeBuilder.*;

/**
 * @author imotov
 */
public class AbstractJettyHttpServerTests {
    protected final ESLogger logger = Loggers.getLogger(getClass());

    private Map<String, Node> nodes = newHashMap();

    private Map<String, Client> clients = newHashMap();

    private Set<TransportClient> transportClients = newHashSet();

    private Settings defaultSettings = ImmutableSettings
            .settingsBuilder()
            .put("cluster.name", "test-cluster-" + NetworkUtils.getLocalAddress().getHostName())
            .put("http.type", JettyHttpServerTransportModule.class.getName())
            .put("sonian.elasticsearch.http.jetty.port", "9290-9300")
            .put("node.local", true)
            .put("gateway.type", "none")
            .put("index.store.type", "memory")
            .build();

    public void putDefaultSettings(Settings.Builder settings) {
        putDefaultSettings(settings.build());
    }

    public void putDefaultSettings(Settings settings) {
        defaultSettings = ImmutableSettings.settingsBuilder().put(defaultSettings).put(settings).build();
    }

    public Node startNode(String id) {
        return buildNode(id).start();
    }

    public Node startNode(String id, Settings.Builder settings) {
        return startNode(id, settings.build());
    }

    public Node startNode(String id, Settings settings) {
        return buildNode(id, settings).start();
    }

    public Node buildNode(String id) {
        return buildNode(id, EMPTY_SETTINGS);
    }

    public Node buildNode(String id, Settings.Builder settings) {
        return buildNode(id, settings.build());
    }

    public Node buildNode(String id, Settings settings) {
        String settingsSource = getClass().getName().replace('.', '/') + ".yml";
        Settings finalSettings = settingsBuilder()
                .loadFromClasspath(settingsSource)
                .put(defaultSettings)
                .put(settings)
                .put("name", id)
                .build();

        if (finalSettings.get("gateway.type") == null) {
            // default to non gateway
            finalSettings = settingsBuilder().put(finalSettings).put("gateway.type", "none").build();
        }

        Node node = nodeBuilder()
                .settings(finalSettings)
                .build();
        nodes.put(id, node);
        clients.put(id, node.client());
        return node;
    }

    public void closeNode(String id) {
        Client client = clients.remove(id);
        if (client != null) {
            client.close();
        }
        Node node = nodes.remove(id);
        if (node != null) {
            node.close();
        }
    }

    public Node node(String id) {
        return nodes.get(id);
    }

    public Client client(String id) {
        return clients.get(id);
    }

    public HttpClient httpClient(String id) {
        return new HttpClient(getHttpServerTransport(id).boundAddress().publishAddress());

    }

    public HttpClient httpClient(String id, String username, String password) {
        return new HttpClient(getHttpServerTransport(id).boundAddress().publishAddress(), username, password);

    }

    public void closeAllNodes() {
        for (TransportClient client : transportClients) {
            client.close();
        }
        transportClients.clear();
        for (Client client : clients.values()) {
            client.close();
        }
        clients.clear();
        for (Node node : nodes.values()) {
            node.close();
        }
        nodes.clear();
    }

    public HttpServerTransport getHttpServerTransport(String id) {
        return ((InternalNode) node(id)).injector().getInstance(HttpServerTransport.class);
    }

    public Map<String, Object> createSearchQuery(String queryString) {
        return MapBuilder.<String, Object>newMapBuilder()
                .put("query", MapBuilder.newMapBuilder()
                        .put("query_string", MapBuilder.newMapBuilder()
                                .put("query", queryString)
                                .immutableMap()
                        ).immutableMap()
                ).immutableMap();
    }

    public void createTestIndex() {
        try {
            client("server1").admin().indices().prepareDelete("test").execute().actionGet();
        } catch (Exception e) {
            // ignore
        }
        client("server1").admin().indices().prepareCreate("test")
                .setSettings(
                        ImmutableSettings.settingsBuilder()
                                .put("number_of_shards", 1)
                                .put("number_of_replicas", 0))
                .execute().actionGet();
        client("server1").admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();
    }

}
