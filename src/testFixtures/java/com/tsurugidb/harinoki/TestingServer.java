package com.tsurugidb.harinoki;

import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

/**
 * A simple embedded HTTP server for testing.
 */
public class TestingServer implements AutoCloseable {

    private final Server server;

    private final WebAppContext context;

    private final UserStore userStore;

    /**
     * Creates a new instance.
     * @param port the port number
     */
    public TestingServer(int port) {
        server = new Server(port);
        context = new WebAppContext();
        context.setResourceBase("src/main/webapp");
        context.setContextPath("/");
        context.setConfigurations(new Configuration[] {
                new WebAppConfiguration(),
                new WebXmlConfiguration(),
        });

        var securityHandler = new ConstraintSecurityHandler();
        var loginService = new HashLoginService();
        userStore = new UserStore();
        loginService.setUserStore(userStore);
        securityHandler.setLoginService(loginService);
        context.setSecurityHandler(securityHandler);

        server.setHandler(context);
    }

    /**
     * Returns the current context.
     * @return the current context
     */
    public WebAppContext getContext() {
        return context;
    }

    /**
     * Adds a new authorized user.
     * @param user the user name
     * @param password the password
     * @param roles the role names
     */
    public void addUser(String user, String password, String... roles) {
        userStore.addUser(user, new Password(password), roles);
    }

    /**
     * Starts the HTTP server.
     * @throws Exception if failed
     */
    public void start() throws Exception {
        server.start();
    }

    /**
     * Stops the HTTP server.
     */
    @Override
    public void close() throws Exception {
        server.stop();
    }
}
