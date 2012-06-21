package com.sonian.elasticsearch.http.jetty.security;

import org.eclipse.jetty.http.security.Credential;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.log.Log;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;

import java.util.*;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

/**
 * @author drewr
 */
public class ESLoginService extends MappedLoginService {
    private volatile String authIndex;

    private volatile int cacheTime = -1;

    private volatile long lastHashPurge;

    private volatile Client client;

    public ESLoginService() {
    }

    public ESLoginService(String name) {
        setName(name);
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setAuthIndex(String idx) {
        authIndex = idx;
    }

    public void setCacheTime(String t) {
        cacheTime = Integer.parseInt(t);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        lastHashPurge = 0;
      }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    @Override
    public UserIdentity login(String username, Object credentials)
    {
        if (cacheTime >= 0) {
            long now = System.currentTimeMillis();

            if (now - lastHashPurge > cacheTime || cacheTime == 0) {
                _users.clear();
                lastHashPurge = now;
            }
        }

        return super.login(username,credentials);
    }

    @Override
    public UserIdentity loadUser(String user) {
        SearchResponse res = null;
        String pass = null;
        String[] roles = null;

        try {
            res = client.prepareSearch(authIndex)
                    .setQuery(termQuery("user", user))
                    .addField("password")
                    .addField("roles")
                    .execute().actionGet();
            if (res != null && res.hits().totalHits() > 0) {
                pass = res.hits().getAt(0).field("password").value();
                List<Object> rs = res.hits().getAt(0).field("roles").value();
                roles = rs.toArray(new String[rs.size()]);
            }

            if (pass == null) {
                return null;
            }
            return putUser(user, Credential.getCredential(pass), roles);
        } catch (IndexMissingException e) {
            Log.warn("no auth index [{}]", authIndex);
        } catch (Exception e) {
            Log.warn("error finding user [" + user + "] in [" + authIndex + "]", e);
        }
        return null;
    }

    @Override
    public void loadUsers() {
    }
}
