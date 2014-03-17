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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.BoundTransportAddress;
import org.elasticsearch.http.HttpInfo;
import org.elasticsearch.http.HttpServerAdapter;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.http.HttpStats;

import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.collect.Lists.newArrayList;

/**
 * @author imotov
 */
public class FilterHttpServerTransport extends AbstractLifecycleComponent<HttpServerTransport> implements HttpServerTransport {

    private final HttpServerTransport filteredHttpServerTransport;

    private final FilterHttpServerAdapter[] filters;

    private Map<String, FilterHttpServerAdapter> filterMap;

    private List<String> filterNames;

    @Inject
    public FilterHttpServerTransport(Settings settings, @FilteredHttpServerTransport HttpServerTransport filteredHttpServerTransport,
                                     @Nullable Map<String, FilterHttpServerAdapterFactory> filterHttpServerAdapterFactoryMap) {
        super(settings);
        this.filteredHttpServerTransport = filteredHttpServerTransport;

        MapBuilder<String, FilterHttpServerAdapter> filters = MapBuilder.newMapBuilder();

        if (filterHttpServerAdapterFactoryMap != null) {
            Map<String, Settings> filtersSettings = componentSettings.getGroups("http_filter");

            for (Map.Entry<String, FilterHttpServerAdapterFactory> entry : filterHttpServerAdapterFactoryMap.entrySet()) {
                String filterName = entry.getKey();
                FilterHttpServerAdapterFactory filterFactory = entry.getValue();
                Settings filterSettings = filtersSettings.get(filterName);
                if (filterSettings == null) {
                    filterSettings = ImmutableSettings.Builder.EMPTY_SETTINGS;
                }
                filters.put(filterName, filterFactory.create(filterName, filterSettings));
            }

        }

        filterMap = filters.immutableMap();

        String[] filterNames = componentSettings.getAsArray("http_filter_chain");
        List<FilterHttpServerAdapter> filterList = newArrayList();

        for (String filterName : filterNames) {
            FilterHttpServerAdapter filter = filters.get(filterName);
            if (filter == null) {
                throw new IllegalArgumentException("Failed to find http_filter under name [" + filterName + "]");
            }
            filterList.add(filter);
        }
        this.filters = filterList.toArray(new FilterHttpServerAdapter[filterList.size()]);
        this.filterNames = ImmutableList.copyOf(filterNames);
    }

    @Override
    protected void doStart
            () throws ElasticsearchException {
        filteredHttpServerTransport.start();
    }

    @Override
    protected void doStop
            () throws ElasticsearchException {
        filteredHttpServerTransport.stop();
    }

    @Override
    protected void doClose
            () throws ElasticsearchException {
        filteredHttpServerTransport.close();
    }

    @Override
    public BoundTransportAddress boundAddress
            () {
        return filteredHttpServerTransport.boundAddress();
    }

    @Override
    public HttpInfo info() {
        return new HttpInfo(boundAddress(), 0);
    }

    @Override
    public HttpStats stats
            () {
        return filteredHttpServerTransport.stats();
    }

    @Override
    public void httpServerAdapter
            (HttpServerAdapter
                     httpServerAdapter) {
        filteredHttpServerTransport.httpServerAdapter(new FilterChainManager(filters, httpServerAdapter));
    }

    public List<String> filterNames() {
        return filterNames;
    }

    public FilterHttpServerAdapter filter(String name) {
        return filterMap.get(name);
    }
}
