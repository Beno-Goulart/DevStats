package devstats.http;

import com.sun.net.httpserver.HttpServer;
import devstats.services.DatabaseService;
import devstats.services.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class OAuthServer {

    private static final Logger log = LoggerFactory.getLogger(OAuthServer.class);

    private final HttpServer server;

    public OAuthServer(OAuthService oAuthService, DatabaseService databaseService) throws Exception {
        this.server = HttpServer.create(new InetSocketAddress(8080), 0);
        this.server.createContext("/callback", new OAuthCallbackHandler(oAuthService, databaseService));
        this.server.setExecutor(null);
    }

    public void start() {
        this.server.start();
        log.info("Servidor OAuth iniciado na porta 8080");
    }

    public void stop() {
        this.server.stop(0);
        log.info("Servidor OAuth parado");
    }
}
