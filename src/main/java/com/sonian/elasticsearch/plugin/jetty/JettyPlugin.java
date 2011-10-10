package com.sonian.elasticsearch.plugin.jetty;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;

/**
 * @author imotov
 */
public class JettyPlugin extends AbstractPlugin {

    public JettyPlugin(Settings settings) {
    }

    @Override public String name() {
        return "jetty";
    }

    @Override public String description() {
        return "Jetty Plugin Version: " + Version.number() + " (" + Version.date() + ")";
    }
}
