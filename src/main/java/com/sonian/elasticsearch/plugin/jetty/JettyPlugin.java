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
package com.sonian.elasticsearch.plugin.jetty;

import com.sonian.elasticsearch.http.filter.FilterHttpServerTransport;
import com.sonian.elasticsearch.http.filter.FilterHttpServerTransportModule;
import com.sonian.elasticsearch.http.jetty.JettyHttpServerTransport;
import com.sonian.elasticsearch.http.jetty.JettyHttpServerTransportModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.http.netty.NettyHttpServerTransport;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;

import static org.elasticsearch.common.collect.Lists.newArrayList;

/**
 * @author imotov
 */
public class JettyPlugin extends AbstractPlugin {

    private final Settings settings;

    public JettyPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override public String name() {
        return "jetty";
    }

    @Override public String description() {
        return "Jetty Plugin Version: " + Version.number() + " (" + Version.date() + ")";
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = newArrayList();
        if (settings.getAsBoolean("http.enabled", true)) {
            Class<? extends HttpServerTransport> defaultHttpServerTransport = NettyHttpServerTransport.class;
            Class<? extends HttpServerTransport> httpServerTransport = settings.getAsClass("http.type", defaultHttpServerTransport, "org.elasticsearch.http.", "HttpServerTransport");
            if (httpServerTransport == JettyHttpServerTransport.class) {
                modules.add(JettyHttpServerTransportModule.class);
            } else if (httpServerTransport == FilterHttpServerTransport.class) {
                modules.add(FilterHttpServerTransportModule.class);
            }

        }
        return modules;
    }
}
