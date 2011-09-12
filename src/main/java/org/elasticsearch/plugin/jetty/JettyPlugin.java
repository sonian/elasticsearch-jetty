package org.elasticsearch.plugin.jetty;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;

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
        return "Jetty Plugin";
    }
}
