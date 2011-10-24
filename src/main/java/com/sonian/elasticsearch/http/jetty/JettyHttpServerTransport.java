package com.sonian.elasticsearch.http.jetty;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.BoundTransportAddress;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.PortsRange;
import org.elasticsearch.env.Environment;
import org.elasticsearch.http.BindHttpException;
import org.elasticsearch.http.HttpServerAdapter;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.http.HttpStats;
import org.elasticsearch.transport.BindTransportException;

import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author imotov
 */
public class JettyHttpServerTransport extends AbstractLifecycleComponent<HttpServerTransport> implements HttpServerTransport {

    public static final String TRANSPORT_ATTRIBUTE = "com.sonian.elasticsearch.http.jetty.transport";

    private final NetworkService networkService;

    private final String port;

    private final String bindHost;

    private final String publishHost;

    private final String jettyConfig;

    private final Environment environment;

    private final ESLoggerWrapper loggerWrapper;

    private final ClusterName clusterName;

    private volatile BoundTransportAddress boundAddress;

    private volatile Server jettyServer;

    private volatile HttpServerAdapter httpServerAdapter;


    @Inject
    public JettyHttpServerTransport(Settings settings, Environment environment, NetworkService networkService, ESLoggerWrapper loggerWrapper, ClusterName clusterName) {
        super(settings);
        this.environment = environment;
        this.networkService = networkService;
        this.port = componentSettings.get("port", settings.get("http.port", "9200-9300"));
        this.bindHost = componentSettings.get("bind_host", settings.get("http.bind_host", settings.get("http.host")));
        this.publishHost = componentSettings.get("publish_host", settings.get("http.publish_host", settings.get("http.host")));
        this.jettyConfig = componentSettings.get("config", "jetty.xml");
        this.loggerWrapper = loggerWrapper;
        this.clusterName = clusterName;
    }

    @Override
    protected void doStart() throws ElasticSearchException {
        PortsRange portsRange = new PortsRange(port);
        final AtomicReference<Exception> lastException = new AtomicReference<Exception>();

        Log.setLog(loggerWrapper);

        portsRange.iterate(new PortsRange.PortCallback() {
            @Override
            public boolean onPortNumber(int portNumber) {
                try {
                    URL config = environment.resolveConfig(jettyConfig);
                    XmlConfiguration xmlConfiguration = new XmlConfiguration(config);
                    xmlConfiguration.getProperties().putAll(jettySettings(bindHost, portNumber));
                    Server server = (Server) xmlConfiguration.configure();

                    server.setAttribute(TRANSPORT_ATTRIBUTE, JettyHttpServerTransport.this);

                    server.start();

                    jettyServer = server;
                    lastException.set(null);
                } catch (BindException e) {
                    lastException.set(e);
                    return false;
                } catch (Exception e) {
                    logger.error("Jetty Startup Failed ", e);
                    lastException.set(e);
                    return true;
                }
                return true;
            }
        });
        if (lastException.get() != null) {
            throw new BindHttpException("Failed to bind to [" + port + "]", lastException.get());
        }
        InetSocketAddress jettyBoundAddress = findFirstInetConnector(jettyServer);
        if (jettyBoundAddress != null) {
            InetSocketAddress publishAddress;
            try {
                publishAddress = new InetSocketAddress(networkService.resolvePublishHostAddress(publishHost), jettyBoundAddress.getPort());
            } catch (Exception e) {
                throw new BindTransportException("Failed to resolve publish address", e);
            }
            this.boundAddress = new BoundTransportAddress(new InetSocketTransportAddress(jettyBoundAddress), new InetSocketTransportAddress(publishAddress));
        } else {
            throw new BindHttpException("Failed to find a jetty connector with Inet transport");
        }
    }

    private InetSocketAddress findFirstInetConnector(Server server){
        Connector[] connectors = server.getConnectors();
        if(connectors != null) {
            for(Connector connector : connectors) {
                Object connection =  connector.getConnection();
                if (connection instanceof ServerSocketChannel) {
                    SocketAddress address = ((ServerSocketChannel) connector.getConnection()).socket().getLocalSocketAddress();
                    if (address instanceof InetSocketAddress) {
                        return (InetSocketAddress) address;
                    }
                } else if(connection instanceof ServerSocket) {
                    SocketAddress address = ((ServerSocket) connector.getConnection()).getLocalSocketAddress();
                    if (address instanceof InetSocketAddress) {
                        return (InetSocketAddress) address;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void doStop() throws ElasticSearchException {
        if (jettyServer != null) {
            try {
                jettyServer.stop();
            } catch (Exception ex) {
                throw new ElasticSearchException("Cannot stop jetty server", ex);
            }
            jettyServer = null;
        }
    }

    @Override
    protected void doClose() throws ElasticSearchException {
    }

    @Override
    public BoundTransportAddress boundAddress() {
        return this.boundAddress;
    }

    @Override
    public HttpStats stats() {
        return new HttpStats(0);
    }

    @Override
    public void httpServerAdapter(HttpServerAdapter httpServerAdapter) {
        this.httpServerAdapter = httpServerAdapter;
    }

    public HttpServerAdapter httpServerAdapter() {
        return httpServerAdapter;
    }

    public Settings settings() {
        return settings;
    }

    public Settings componentSettings() {
        return componentSettings;
    }

    private Map<String, String> jettySettings(String hostAddress, int port) {
        MapBuilder<String, String> jettySettings = MapBuilder.newMapBuilder();
        jettySettings.put("es.home", environment.homeFile().getAbsolutePath());
        jettySettings.put("es.config", environment.configFile().getAbsolutePath());
        jettySettings.put("es.data", environment.dataFile().getAbsolutePath());
        jettySettings.put("es.cluster.data", environment.dataWithClusterFile().getAbsolutePath());
        jettySettings.put("es.cluster", clusterName.value());
        if(hostAddress != null) {
            jettySettings.put("jetty.bind_host", hostAddress);
        }
        for(Map.Entry<String, String> entry : componentSettings.getAsMap().entrySet()) {
            jettySettings.put("jetty." + entry.getKey(), entry.getValue());
        }
        // Override jetty port in case we have a port-range
        jettySettings.put("jetty.port", String.valueOf(port));
        return jettySettings.immutableMap();
    }

}
