package org.elasticsearch.http.jetty;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.HttpServerTransport;

/**
 * @author imotov
 */
public class JettyHttpServerTransportModule extends AbstractModule {

    @Override protected void configure() {
        bind(HttpServerTransport.class).to(JettyHttpServerTransport.class).asEagerSingleton();
        bind(ESLoggerWrapper.class).asEagerSingleton();
    }
}
