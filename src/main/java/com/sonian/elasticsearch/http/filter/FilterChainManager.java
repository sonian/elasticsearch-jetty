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

import org.elasticsearch.http.HttpChannel;
import org.elasticsearch.http.HttpRequest;
import org.elasticsearch.http.HttpServerAdapter;

/**
 * @author imotov
 */
public class FilterChainManager implements HttpServerAdapter {

    private final FilterHttpServerAdapter[] filters;

    private final HttpServerAdapter adapter;

    public FilterChainManager(FilterHttpServerAdapter[] filters, HttpServerAdapter adapter) {
        this.filters = filters;
        this.adapter = adapter;
    }

    @Override
    public void dispatchRequest(HttpRequest request, HttpChannel channel) {
        FilterChain filterChain = new FilterChainImpl();
        filterChain.doFilter(request, channel);
    }

    private class FilterChainImpl implements FilterChain {
        private final int currentFilter;

        FilterChainImpl() {
            this(0);
        }

        public FilterChainImpl(int currentFilter) {
            this.currentFilter = currentFilter;
        }

        @Override
        public void doFilter(HttpRequest request, HttpChannel channel) {
            if (currentFilter < filters.length) {
                filters[currentFilter].doFilter(request, channel, new FilterChainImpl(currentFilter + 1));
            } else {
                adapter.dispatchRequest(request, channel);
            }
        }
    }
}
