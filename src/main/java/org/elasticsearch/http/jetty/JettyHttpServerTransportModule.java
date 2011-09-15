package org.elasticsearch.http.jetty;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.http.jetty.logging.RequestLogger;

/**
 * @author imotov
 */
public class JettyHttpServerTransportModule extends AbstractModule {

    private final Settings settings;

    public JettyHttpServerTransportModule(Settings settings) {
        this.settings = settings;
    }

    @Override protected void configure() {
        bind(HttpServerTransport.class).to(JettyHttpServerTransport.class).asEagerSingleton();
        if (settings.getComponentSettings(getClass()).getAsBoolean("request_log.enabled", false)) {
            bind(RequestLogger.class).asEagerSingleton();
        }
    }
}
