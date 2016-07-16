package ubongo.server;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class UbongoHTTPServer {

    private static final UbongoHTTPServer INSTANCE = new UbongoHTTPServer();
    private final URI baseUri;
    private static final Logger logger = LogManager.getLogger(UbongoHTTPServer.class);
    private HttpServer server;

    public static void main(String[] args) throws Exception {
        getInstance().startServer();
    }

    private UbongoHTTPServer() {
        baseUri = getBaseURI();
    }

    public static UbongoHTTPServer getInstance() {
        return INSTANCE;
    }

    private int getPort(int defaultPort) {
        // TODO grab port from environment, otherwise fall back to default port 9997
        String httpPort = System.getProperty("jersey.test.port");
        if (null != httpPort) {
            try {
                return Integer.parseInt(httpPort);
            } catch (NumberFormatException e) {
                // TODO log
            }
        }
        return defaultPort;
    }

    private String getHost(String defaultHost) {
        String host = System.getProperty("jersey.test.host"); // TODO
        if (host == null || host.isEmpty()) {
            host = defaultHost;
        }

        return host;
    }

    // TODO
    private String getStaticRoot() throws IOException {
        String staticRoot = System.getProperty("jersey.static.root"); // TODO
        if (null == staticRoot || staticRoot.isEmpty()) {
            Path pathToClass = Paths.get(
                    new File(UbongoHTTPServer.class.getProtectionDomain()
                            .getCodeSource().getLocation().getPath()).getPath());
            staticRoot = pathToClass.resolve(Paths.get("./../../../ui")).toRealPath().toString();
        }
        return staticRoot;
    }

    private URI getBaseURI() {
        return UriBuilder.fromUri("http://" + getHost("localhost") + "/").port(getPort(9997)).path("ubongo").build();
    }

    public final void startServer() throws Exception {
        ResourceConfig rc = new ResourceConfig();
        rc.register(JacksonFeature.class);
        rc.packages(true, "ubongo.server");

        logger.info(String.format("Starting Ubongo Server on %s:", baseUri));
        server = GrizzlyHttpServerFactory.createHttpServer(baseUri, rc, false);
        server.getServerConfiguration().addHttpHandler(new StaticHttpHandler(getStaticRoot()), "/");
        server.start();
        logger.info("Server successfully started!");
    }

    public final void stopServer() {
        if (server != null) {
            logger.info("Shutting down server");
            server.shutdownNow();
            logger.info("Server shutdown successfully!");
        }
    }

    public final boolean isServerRunning() {
        return server != null && server.isStarted();
    }

}
