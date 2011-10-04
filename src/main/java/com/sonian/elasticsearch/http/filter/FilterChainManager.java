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
