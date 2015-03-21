package com.sonian.elasticsearch.plugin.jetty;

import com.sonian.elasticsearch.http.filter.FilterHttpServerTransport;
import com.sonian.elasticsearch.http.filter.FilterHttpServerTransportModule;
import com.sonian.elasticsearch.http.jetty.JettyHttpServerTransport;
import com.sonian.elasticsearch.http.jetty.JettyHttpServerTransportModule;
import org.easymock.EasyMock;
import org.elasticsearch.common.inject.Binder;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.binder.AnnotatedBindingBuilder;
import org.elasticsearch.common.inject.binder.ScopedBindingBuilder;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.NoClassSettingsException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.HttpServer;
import org.elasticsearch.http.HttpServerModule;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.http.netty.NettyHttpServerTransport;
import org.testng.annotations.Test;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class JettyPluginTests {

    @Test
    public void testModulesWithNoHttpType() throws Exception {
        Settings settings = ImmutableSettings
                .settingsBuilder()
                .build();
        JettyPlugin plugin = new JettyPlugin(settings);

        // when
        Collection<Class<? extends Module>> modules = plugin.modules();

        assertThat(
                modules.size(),
                equalTo(1)
        );

        Class expectedClass = JettyHttpServerTransportModule.class;
        Class actualClass = (Class)modules.toArray()[0];
        assertThat(
                "Expected class: " + expectedClass.getName() +
                        " Actual class: " + actualClass.getName(),
                modules.contains(expectedClass)
        );
    }

    @Test
    public void testModulesWithHttpTypeNettyOverridesAltType() throws Exception {
        Settings settings = ImmutableSettings
                .settingsBuilder()
                .put("http.type", NettyHttpServerTransport.class.getName())
                .put("sonian.elasticsearch.http.type", JettyHttpServerTransport.class.getName())
                .build();
        JettyPlugin plugin = new JettyPlugin(settings);

        // when
        Collection<Class<? extends Module>> modules = plugin.modules();

        assertThat(
                modules.size(),
                equalTo(0)
        );
    }



    @Test
    public void testModulesWithHttpTypeJetty() throws Exception {
        Settings settings = ImmutableSettings
                .settingsBuilder()
                .put("http.type", JettyHttpServerTransport.class.getName())
                .build();
        JettyPlugin plugin = new JettyPlugin(settings);

        // when
        Collection<Class<? extends Module>> modules = plugin.modules();

        assertThat(
                modules.size(),
                equalTo(1)
        );

        Class expectedClass = JettyHttpServerTransportModule.class;
        Class actualClass = (Class)modules.toArray()[0];
        assertThat(
                "Expected class: " + expectedClass.getName() +
                        " Actual class: " + actualClass.getName(),
                modules.contains(expectedClass)
        );
    }

    @Test
    public void testModulesWithAltHttpTypeJetty() throws Exception {
        Settings settings = ImmutableSettings
                .settingsBuilder()
                .put("sonian.elasticsearch.http.type", JettyHttpServerTransport.class.getName())
                .build();
        JettyPlugin plugin = new JettyPlugin(settings);

        // when
        Collection<Class<? extends Module>> modules = plugin.modules();

        assertThat(
                modules.size(),
                equalTo(1)
        );

        Class expectedClass = JettyHttpServerTransportModule.class;
        Class actualClass = (Class)modules.toArray()[0];
        assertThat(
                "Expected class: " + expectedClass.getName() +
                        " Actual class: " + actualClass.getName(),
                modules.contains(expectedClass)
        );
    }

    @Test
    public void testModulesWithHttpTypeFilter() throws Exception {
        Settings settings = ImmutableSettings
                .settingsBuilder()
                .put("http.type", FilterHttpServerTransport.class.getName())
                .build();
        JettyPlugin plugin = new JettyPlugin(settings);

        // when
        Collection<Class<? extends Module>> modules = plugin.modules();

        assertThat(
                modules.size(),
                equalTo(1)
        );

        Class expectedClass = FilterHttpServerTransportModule.class;
        Class actualClass = (Class)modules.toArray()[0];
        assertThat(
                "Expected class: " + expectedClass.getName() +
                        " Actual class: " + actualClass.getName(),
                modules.contains(expectedClass)
        );
    }

    @Test
    public void testModulesWithAltHttpTypeFilter() throws Exception {
        Settings settings = ImmutableSettings
                .settingsBuilder()
                .put("sonian.elasticsearch.http.type", FilterHttpServerTransport.class.getName())
                .build();
        JettyPlugin plugin = new JettyPlugin(settings);

        // when
        Collection<Class<? extends Module>> modules = plugin.modules();

        assertThat(
                modules.size(),
                equalTo(1)
        );

        Class expectedClass = FilterHttpServerTransportModule.class;
        Class actualClass = (Class)modules.toArray()[0];
        assertThat(
                "Expected class: " + expectedClass.getName() +
                        " Actual class: " + actualClass.getName(),
                modules.contains(expectedClass)
        );
    }

    @Test
    public void testModulesThrowsExceptionWithBadHttpType() throws Exception {
        String badClass = JettyHttpServerTransport.class.getPackage().getName() + ".JttyHttpServerTransport";
        Settings settings = ImmutableSettings
                .settingsBuilder()
                .put("http.type", badClass)
                .build();
        JettyPlugin plugin = new JettyPlugin(settings);

        try {
            plugin.modules();
        } catch (NoClassSettingsException ex) {
            assertThat(ex.getMessage(), equalTo("Failed to load class setting [http.type] with value [" + badClass + "]"));
        }
    }

    @Test
    public void testOnModuleInjectsTransportIntoHttpServerModule() {
        Settings settings = ImmutableSettings
                .settingsBuilder()
                .build();
        JettyPlugin plugin = new JettyPlugin(settings);
        plugin.modules();

        HttpServerModule httpServerModule = new HttpServerModule(settings);
        plugin.onModule(httpServerModule);

        // mock out bind calls
        Class expectedTransportClass = JettyHttpServerTransport.class;
        Binder binderMock = EasyMock.createMock(Binder.class);
        @SuppressWarnings("unchecked")
        AnnotatedBindingBuilder abb = EasyMock.createMock(AnnotatedBindingBuilder.class);
        ScopedBindingBuilder sbb = EasyMock.createMock(ScopedBindingBuilder.class);
        EasyMock.expect(binderMock.bind(HttpServerTransport.class)).andReturn(abb);
        EasyMock.expect(abb.to(expectedTransportClass)).andReturn(sbb);
        sbb.asEagerSingleton();
        EasyMock.expect(binderMock.bind(HttpServer.class)).andReturn(abb);
        abb.asEagerSingleton();
        EasyMock.expectLastCall();

        // call configure
        EasyMock.replay(binderMock);
        EasyMock.replay(abb);
        EasyMock.replay(sbb);
        httpServerModule.configure(binderMock);
    }
}
