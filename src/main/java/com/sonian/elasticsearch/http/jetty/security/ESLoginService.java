package com.sonian.elasticsearch.http.jetty.security;

import org.eclipse.jetty.http.security.Credential;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.indices.IndexMissingException;

import java.io.IOException;
import java.util.*;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

/**
 * @author drewr
 */
public class ESLoginService extends MappedLoginService {
    private String config;

    private String authIndex;

    private String authHost;

    private int authPort;

    private String authCluster;

    private int cacheTime;

    private long lastHashPurge;

    public ESLoginService() {
    }

    public ESLoginService(String name) {
        setName(name);
    }

    public ESLoginService(String name, String config) {
        setName(name);
        setConfig(config);
    }

    public String getConfig() {
        return this.config;
    }

    public void setConfig(String config) {
        if (isRunning())
            throw new IllegalStateException("Running");
        this.config = config;
    }

    public Client getClient() {
        TransportAddress addr = new InetSocketTransportAddress(authHost, authPort);
        TransportClient cli = new TransportClient(ImmutableSettings.settingsBuilder()
                      .put("cluster.name", authCluster)
                      .build());
        cli.addTransportAddress(addr);
        return cli;
    }

    public void setAuthHost(String host) {
        authHost = host;
    }

    public void setAuthPort(String port) {
        authPort = Integer.parseInt(port);
    }

    public void setAuthCluster(String cluster) {
        authCluster = cluster;
    }

    public void setAuthIndex(String idx) {
        authIndex = idx;
    }

    @Override
    protected void doStart() throws Exception {
//        Properties properties = new Properties();
//        Resource resource = Resource.newResource(config);
//        properties.load(resource.getInputStream());

        super.doStart();
    }

    @Override
    public UserIdentity login(String username, Object credentials)
    {
        long now = System.currentTimeMillis();

        if (now - lastHashPurge > cacheTime || cacheTime == 0) {
            _users.clear();
            lastHashPurge = now;
        }

        return super.login(username,credentials);
    }

    @Override
    public UserIdentity loadUser(String user) {
        Client client = getClient();
        SearchResponse res = null;
        String pass = null;
        String[] roles = null;

        try {
            res = client.prepareSearch(authIndex)
                    .setQuery(termQuery("user", user))
                    .addField("password")
                    .addField("roles")
                    .execute().actionGet();
        } catch (IndexMissingException e) {
            Log.warn("no auth index [{}]", authIndex);
        }


        // hello new visitor
        try{
            if (res != null && res.hits().totalHits() > 0) {
                pass = res.hits().getAt(0).field("password").value();
                List<Object> rs = res.hits().getAt(0).field("roles").value();
                roles = rs.toArray(new String[rs.size()]);
            }
        } catch (Exception e) {
            throw new ElasticSearchException("", e);
        }

        if (pass == null) {
            return null;
        }

        UserIdentity userI = putUser(user, Credential.getCredential(pass), roles);
        return userI;
    }

    @Override
    public void loadUsers() {
    }
}
