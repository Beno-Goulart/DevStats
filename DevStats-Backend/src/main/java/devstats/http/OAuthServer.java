package devstats.http;

import com.sun.net.httpserver.HttpServer;
import devstats.services.DatabaseService;
import devstats.services.OAuthService;
import java.net.InetSocketAddress;

public class OAuthServer {

    private final HttpServer server;

    public OAuthServer(OAuthService oAuthService, DatabaseService databaseService) throws Exception {
        this.server = HttpServer.create(new InetSocketAddress(8080), 0);
        this.server.createContext("/callback", new OAuthCallbackHandler(oAuthService, databaseService));
        this.server.setExecutor(null);
    }

    public void start() {
        this.server.start();
        System.out.println("[OAuthServer] Servidor HTTP iniciado na porta 8080");
        System.out.println("[OAuthServer] URL de callback: http://localhost:8080/callback");
    }

    public void stop() {
        this.server.stop(0);
    }
}
