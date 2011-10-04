package com.sonian.elasticsearch.http.filter;

import org.elasticsearch.common.settings.Settings;

/**
 * @author imotov
 */
public interface FilterHttpServerAdapterFactory {

    FilterHttpServerAdapter create(String name, Settings settings);
}
