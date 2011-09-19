package org.elasticsearch.http.jetty.handler;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.http.jetty.JettyHttpServerTransport;

/**
 * @author imotov
 */
public abstract class AbstractJettyHttpServerTransportHandler extends AbstractHandler {

    private volatile JettyHttpServerTransport transport;

    protected volatile ESLogger logger;

    @Override
    public void setServer(Server server) {
        super.setServer(server);
        setTransport((JettyHttpServerTransport)server.getAttribute(JettyHttpServerTransport.TRANSPORT_ATTRIBUTE));
    }

    public JettyHttpServerTransport getTransport() {
        return transport;
    }

    public void setTransport(JettyHttpServerTransport transport) {
        this.transport = transport;
        this.logger = Loggers.getLogger(getClass(), transport.settings());
    }
}
