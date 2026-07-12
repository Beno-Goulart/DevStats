package devstats.http;

import com.sun.net.httpserver.HttpServer;
import devstats.services.DatabaseService;
import devstats.services.WidgetSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebhookServer {

    private static final Logger log = LoggerFactory.getLogger(WebhookServer.class);

    private final HttpServer server;

    public WebhookServer(HttpServer server, WidgetSyncService widgetSyncService, DatabaseService databaseService) {
        this.server = server;
        this.server.createContext("/webhook/github", new WebhookHandler(widgetSyncService, databaseService));
        log.info("Rota /webhook/github registrada no servidor HTTP");
    }

    public void start() {
        log.info("Webhook handler ativo (compartilhando servidor HTTP)");
    }

    public void stop() {
        log.info("Webhook handler desativado");
    }
}
