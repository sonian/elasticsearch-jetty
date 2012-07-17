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
package com.sonian.elasticsearch.http.jetty.security;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;
import org.eclipse.jetty.util.security.Constraint;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import sun.misc.BASE64Encoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static javax.servlet.http.HttpServletResponse.*;
import static org.hamcrest.Matchers.startsWith;

/**
 * @author imotov
 */
public class RestConstraintSecurityHandlerTests {

    private Server server;

    private LocalConnector connector;

    @BeforeMethod
    public void createServer() {
        server = new Server();
        connector = new LocalConnector();
        server.addConnector(connector);
    }

    @AfterMethod
    public void stopServer() throws Exception {
        server.stop();
        server.join();
    }

    protected HttpTester execute(HttpTester request) throws Exception {
        HttpTester response = new HttpTester();
        response.parse(connector.getResponses(request.generate()));
        return response;
    }

    protected HttpTester request(String method, String url) {
        HttpTester request = new HttpTester();
        request.setMethod(method);
        request.setURI(url);
        request.setVersion("HTTP/1.0");
        return request;
    }

    protected HttpTester request(String method, String url, String username, String password) {
        HttpTester request = new HttpTester();
        request.setMethod(method);
        request.setURI(url);
        request.setVersion("HTTP/1.0");
        BASE64Encoder enc = new sun.misc.BASE64Encoder();
        String userPassword = username + ":" + password;
        request.addHeader("Authorization", "Basic " + enc.encode(userPassword.getBytes()));
        return request;
    }

    protected ConstraintMapping constraintMapping(String method, String url, String... roles) {
        ConstraintMapping constraintMapping = new ConstraintMapping();
        Constraint constraint = new Constraint();
        constraintMapping.setMethod(method);
        constraintMapping.setPathSpec(url);
        if (roles.length > 0) {
            constraint.setAuthenticate(true);
            constraint.setRoles(roles);
        }
        constraintMapping.setConstraint(constraint);
        return constraintMapping;
    }

    protected ConstraintMapping forbidden(String method, String url) {
        ConstraintMapping constraintMapping = new ConstraintMapping();
        Constraint constraint = new Constraint();
        constraintMapping.setMethod(method);
        constraintMapping.setPathSpec(url);
        constraint.setAuthenticate(true);
        constraintMapping.setConstraint(constraint);
        return constraintMapping;
    }

    protected LoginService loginService() {
        HashLoginService loginService = new HashLoginService();
        loginService.setName("DefaultRealm");
        loginService.setConfig("config/realm.properties");
        return loginService;
    }

    @Test
    public void testServerRequest() throws Exception {
        RequestCollectingHandler handler = new RequestCollectingHandler();
        server.setHandler(handler);
        server.start();
        assertThat(execute(request("GET", "/some-page")).getStatus(), equalTo(SC_OK));
        assertThat(handler.requests().size(), equalTo(1));
        assertThat(handler.requests().get(0), equalTo("GET:/some-page"));
    }

    @Test
    public void testBasicPermissions() throws Exception {
        RestConstraintSecurityHandler securityHandler = new RestConstraintSecurityHandler();
        securityHandler.setLoginService(loginService());
        securityHandler.addConstraintMapping(constraintMapping("GET", "/admin-page", "admin"));
        RequestCollectingHandler handler = new RequestCollectingHandler();
        securityHandler.setHandler(handler);
        server.setHandler(securityHandler);
        server.start();
        assertThat(execute(request("GET", "/some-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(handler.requests().size(), equalTo(1));
    }

    @Test
    public void testMethodPermissions() throws Exception {
        RestConstraintSecurityHandler securityHandler = new RestConstraintSecurityHandler();
        securityHandler.setLoginService(loginService());
        securityHandler.addConstraintMapping(constraintMapping("GET", "/admin-page"));
        securityHandler.addConstraintMapping(constraintMapping("POST", "/admin-page", "admin"));
        RequestCollectingHandler handler = new RequestCollectingHandler();
        securityHandler.setHandler(handler);
        server.setHandler(securityHandler);
        server.start();
        assertThat(execute(request("GET", "/some-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("POST", "/admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(execute(request("GET", "/admin-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("PUT", "/admin-page")).getStatus(), equalTo(SC_OK));
        assertThat(handler.requests().size(), equalTo(3));
    }

    @Test
    public void testDefaultConstraint() throws Exception {
        RestConstraintSecurityHandler securityHandler = new RestConstraintSecurityHandler();
        securityHandler.setLoginService(loginService());
        securityHandler.addConstraintMapping(constraintMapping("GET", "/admin-page"));
        securityHandler.addConstraintMapping(constraintMapping("POST", "/admin-page", "admin"));
        securityHandler.addConstraintMapping(constraintMapping(null, "*", "admin"));
        RequestCollectingHandler handler = new RequestCollectingHandler();
        securityHandler.setHandler(handler);
        server.setHandler(securityHandler);
        server.start();
        assertThat(execute(request("GET", "/some-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(execute(request("POST", "/admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(execute(request("GET", "/admin-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("PUT", "/admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(handler.requests().size(), equalTo(1));
    }

    @Test
    public void testDefaultConstraintForbidden() throws Exception {
        RestConstraintSecurityHandler securityHandler = new RestConstraintSecurityHandler();
        securityHandler.setLoginService(loginService());
        securityHandler.addConstraintMapping(constraintMapping("GET", "/admin-page"));
        securityHandler.addConstraintMapping(constraintMapping("POST", "/admin-page", "admin"));
        securityHandler.addConstraintMapping(forbidden(null, "*"));
        RequestCollectingHandler handler = new RequestCollectingHandler();
        securityHandler.setHandler(handler);
        server.setHandler(securityHandler);
        server.start();
        assertThat(execute(request("GET", "/some-page")).getStatus(), equalTo(SC_FORBIDDEN));
        assertThat(execute(request("POST", "/admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(execute(request("GET", "/admin-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("PUT", "/admin-page")).getStatus(), equalTo(SC_FORBIDDEN));
        assertThat(handler.requests().size(), equalTo(1));
    }

    @Test
    public void testConstraintForbidden() throws Exception {
        RestConstraintSecurityHandler securityHandler = new RestConstraintSecurityHandler();
        securityHandler.setLoginService(loginService());
        securityHandler.addConstraintMapping(constraintMapping("GET", "/admin-page"));
        securityHandler.addConstraintMapping(constraintMapping("POST", "/admin-page", "admin"));
        securityHandler.addConstraintMapping(forbidden("PUT", "/admin-page"));
        securityHandler.addConstraintMapping(forbidden("DELETE", "*"));
        RequestCollectingHandler handler = new RequestCollectingHandler();
        securityHandler.setHandler(handler);
        server.setHandler(securityHandler);
        server.start();
        assertThat(execute(request("GET", "/some-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("POST", "/admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(execute(request("GET", "/admin-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("PUT", "/admin-page")).getStatus(), equalTo(SC_FORBIDDEN));
        assertThat(execute(request("DELETE", "/admin-page")).getStatus(), equalTo(SC_FORBIDDEN));
        assertThat(execute(request("DELETE", "/some-page")).getStatus(), equalTo(SC_FORBIDDEN));
        assertThat(handler.requests().size(), equalTo(2));
    }

    @Test
    public void testRestrictingConstraintsOnTheFly() throws Exception {
        RestConstraintSecurityHandler securityHandler = new RestConstraintSecurityHandler();
        securityHandler.setLoginService(loginService());
        securityHandler.addConstraintMapping(constraintMapping("GET", "/non-admin-page"));
        securityHandler.addConstraintMapping(constraintMapping("GET", "/admin-page", "admin"));
        RequestCollectingHandler handler = new RequestCollectingHandler();
        securityHandler.setHandler(handler);
        server.setHandler(securityHandler);
        server.start();
        assertThat(execute(request("GET", "/some-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/non-admin-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        securityHandler.addConstraintMapping(constraintMapping("GET", "/non-admin-page", "admin"));
        assertThat(execute(request("GET", "/some-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/non-admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(execute(request("GET", "/admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
    }

    @Test
    public void testRemovingConstraintsOnTheFly() throws Exception {
        RestConstraintSecurityHandler securityHandler = new RestConstraintSecurityHandler();
        securityHandler.setLoginService(loginService());
        securityHandler.addConstraintMapping(constraintMapping("GET", "/non-admin-page"));
        securityHandler.addConstraintMapping(constraintMapping("GET", "/admin-page", "admin"));
        RequestCollectingHandler handler = new RequestCollectingHandler();
        securityHandler.setHandler(handler);
        server.setHandler(securityHandler);
        server.start();
        assertThat(execute(request("GET", "/some-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/non-admin-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        // Remove authentication requirement from the page
        securityHandler.addConstraintMapping(constraintMapping("GET", "/admin-page"));
        assertThat(execute(request("GET", "/some-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/non-admin-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/admin-page")).getStatus(), equalTo(SC_OK));
    }

    @Test
    public void testRelaxingConstraintsOnTheFly() throws Exception {
        RestConstraintSecurityHandler securityHandler = new RestConstraintSecurityHandler();
        securityHandler.setLoginService(loginService());
        securityHandler.addConstraintMapping(constraintMapping("GET", "/non-admin-page", "readwrite"));
        securityHandler.addConstraintMapping(constraintMapping("GET", "/admin-page", "admin"));
        RequestCollectingHandler handler = new RequestCollectingHandler();
        securityHandler.setHandler(handler);
        server.setHandler(securityHandler);
        server.start();
        assertThat(execute(request("GET", "/some-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/non-admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(execute(request("GET", "/non-admin-page", "user", "Passw0rd")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(execute(request("GET", "/admin-page", "user", "Passw0rd")).getStatus(), equalTo(SC_FORBIDDEN));
        assertThat(execute(request("GET", "/admin-page", "superuser", "Adm1n")).getStatus(), equalTo(SC_OK));
        securityHandler.addConstraintMapping(constraintMapping("GET", "/admin-page", "readwrite"));
        assertThat(execute(request("GET", "/some-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/non-admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(execute(request("GET", "/non-admin-page", "user", "Passw0rd")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(execute(request("GET", "/admin-page", "user", "Passw0rd")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/admin-page", "superuser", "Adm1n")).getStatus(), equalTo(SC_OK));
    }

    @Test
    public void testAnyRoleNoneStrict() throws Exception {
        RestConstraintSecurityHandler securityHandler = new RestConstraintSecurityHandler();
        securityHandler.setLoginService(loginService());
        securityHandler.addConstraintMapping(constraintMapping("GET", "/non-admin-page", "*"));
        RequestCollectingHandler handler = new RequestCollectingHandler();
        securityHandler.setHandler(handler);
        securityHandler.setStrict(false);
        server.setHandler(securityHandler);
        server.start();
        assertThat(execute(request("GET", "/non-admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(execute(request("GET", "/non-admin-page", "user", "Passw0rd")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/non-admin-page", "superuser", "Adm1n")).getStatus(), equalTo(SC_OK));
    }

    @Test
    public void testAnyRoleStrict() throws Exception {
        RestConstraintSecurityHandler securityHandler = new RestConstraintSecurityHandler();
        securityHandler.setLoginService(loginService());
        securityHandler.addConstraintMapping(constraintMapping("GET", "/admin-page", "*"));
        securityHandler.addRole("admin");
        RequestCollectingHandler handler = new RequestCollectingHandler();
        securityHandler.setHandler(handler);
        securityHandler.setStrict(true);
        server.setHandler(securityHandler);
        server.start();
        assertThat(execute(request("GET", "/admin-page")).getStatus(), equalTo(SC_UNAUTHORIZED));
        // Readwrite wasn't defined - should be forbidden
        assertThat(execute(request("GET", "/admin-page", "user", "Passw0rd")).getStatus(), equalTo(SC_FORBIDDEN));
        assertThat(execute(request("GET", "/admin-page", "superuser", "Adm1n")).getStatus(), equalTo(SC_OK));
    }

    @Test
    public void testMultiplePathSpecs() throws Exception {
        RestConstraintSecurityHandler securityHandler = new RestConstraintSecurityHandler();
        securityHandler.setLoginService(loginService());
        securityHandler.addConstraintMapping(constraintMapping("GET", "/page1,/page2,\n   /page3,/page4  ", "readwrite"));
        RequestCollectingHandler handler = new RequestCollectingHandler();
        securityHandler.setHandler(handler);
        server.setHandler(securityHandler);
        server.start();
        assertThat(execute(request("GET", "/some-page")).getStatus(), equalTo(SC_OK));
        assertThat(execute(request("GET", "/page1")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(execute(request("GET", "/page2")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(execute(request("GET", "/page3")).getStatus(), equalTo(SC_UNAUTHORIZED));
        assertThat(execute(request("GET", "/page4")).getStatus(), equalTo(SC_UNAUTHORIZED));
    }

    @Test
    public void testPageWithoutMethod() throws Exception {
        RestConstraintSecurityHandler securityHandler = new RestConstraintSecurityHandler();
        securityHandler.setLoginService(loginService());
        securityHandler.addConstraintMapping(constraintMapping(null, "/page"));
        RequestCollectingHandler handler = new RequestCollectingHandler();
        securityHandler.setHandler(handler);
        server.setHandler(securityHandler);
        try {
            server.start();
            assertThat("Server shouldn't start", false);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), startsWith("No method specified"));
        }
    }


    private class RequestCollectingHandler extends DefaultHandler {

        private List<String> requests = new ArrayList<String>();

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (response.isCommitted() || baseRequest.isHandled())
                return;

            baseRequest.setHandled(true);

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MimeTypes.TEXT_PLAIN);

            ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(1500);

            String requestStr = request.getMethod() + ":" + request.getRequestURI();
            requests.add(requestStr);
            writer.write(requestStr);
            writer.flush();
            response.setContentLength(writer.size());
            OutputStream out = response.getOutputStream();
            writer.writeTo(out);
            out.close();
        }

        public List<String> requests() {
            return requests;
        }
    }
}
