package devstats.http;

import com.sun.net.httpserver.HttpServer;
import devstats.services.DatabaseService;
import devstats.services.WidgetSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class WebhookServer {

    private static final Logger log = LoggerFactory.getLogger(WebhookServer.class);

    private final HttpServer server;

    public WebhookServer(WidgetSyncService widgetSyncService, DatabaseService databaseService) throws Exception {
        this.server = HttpServer.create(new InetSocketAddress(8081), 0);
        this.server.createContext("/webhook/github", new WebhookHandler(widgetSyncService, databaseService));
        this.server.setExecutor(null);
    }

    public void start() {
        this.server.start();
        log.info("Servidor de Webhooks iniciado na porta 8081");
    }

    public void stop() {
        this.server.stop(0);
        log.info("Servidor de Webhooks parado");
    }
}
