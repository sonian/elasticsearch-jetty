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
package com.sonian.elasticsearch.http.filter;

import com.sonian.elasticsearch.http.jetty.ESLoggerWrapper;
import com.sonian.elasticsearch.http.jetty.JettyHttpServerTransport;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Scopes;
import org.elasticsearch.common.inject.assistedinject.FactoryProvider;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.common.settings.NoClassSettingsException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.HttpServerTransport;

import java.util.Map;

/**
 * @author imotov
 */
public class FilterHttpServerTransportModule extends AbstractModule {

    private final Settings componentSettings;

    public FilterHttpServerTransportModule(Settings settings) {
        componentSettings = settings.getComponentSettings(this.getClass());
    }


    @Override
    protected void configure() {
        bind(HttpServerTransport.class)
                .to(FilterHttpServerTransport.class).asEagerSingleton();
        Class<? extends HttpServerTransport> transport;
        // This is a hack for debugging. It allows switching back to NettyHttpServer if needed.
        // HttpServerTransportModule should be loaded instead of just binding HttpServerTransport
        // directly. Unfortunately, it's not possible to override annotation then.
        transport = componentSettings.getAsClass("transport_type", JettyHttpServerTransport.class,
                "org.elasticsearch.http.", "HttpServerTransport");
        bind(HttpServerTransport.class)
                .annotatedWith(FilteredHttpServerTransport.class)
                .to(transport).asEagerSingleton();

        bind(ESLoggerWrapper.class).asEagerSingleton();
        configureFilters();
    }

    private void configureFilters() {
        MapBinder<String, FilterHttpServerAdapterFactory> filterBinder
                = MapBinder.newMapBinder(binder(), String.class, FilterHttpServerAdapterFactory.class);
        Map<String, Settings> filtersSettings = componentSettings.getGroups("http_filter");

        for (Map.Entry<String, Settings> entry : filtersSettings.entrySet()) {
            String filterName = entry.getKey();
            Settings filterSettings = entry.getValue();

            Class<? extends FilterHttpServerAdapter> type = null;
            try {
                type = filterSettings.getAsClass("type", null, "com.sonian.elasticsearch.http.filter.", "FilterHttpServerAdapter");
            } catch (NoClassSettingsException e) {
                // Ignore
            }
            if (type == null) {
                throw new ElasticsearchIllegalArgumentException("Http Filter [" + filterName + "] must have a type associated with it");
            }
            filterBinder.addBinding(filterName)
                    .toProvider(FactoryProvider.newFactory(FilterHttpServerAdapterFactory.class, type))
                    .in(Scopes.SINGLETON);
        }
    }
}
