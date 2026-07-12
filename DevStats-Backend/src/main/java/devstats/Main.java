package devstats;

import devstats.commands.WidgetCommand;
import devstats.http.OAuthServer;
import devstats.http.WebhookServer;
import devstats.models.Config;
import devstats.services.DatabaseService;
import devstats.services.DiscordWidgetService;
import devstats.services.GithubService;
import devstats.services.OAuthService;
import devstats.services.WidgetSyncService;
import devstats.services.AutoSyncService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static JDA jda;
    private static OAuthServer oauthServer;
    private static WebhookServer webhookServer;
    private static AutoSyncService autoSyncService;
    private static DatabaseService databaseService;

    public static void main(String[] args) throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown));

        log.info("Iniciando DevStats Bot...");

        databaseService = new DatabaseService();
        OAuthService oAuthService = new OAuthService();
        GithubService githubService = new GithubService();
        DiscordWidgetService discordWidgetService = new DiscordWidgetService();
        WidgetSyncService widgetSyncService = new WidgetSyncService(githubService, discordWidgetService);

        jda = JDABuilder.createDefault(Config.BOT_TOKEN)
                .addEventListeners(new WidgetCommand(widgetSyncService, oAuthService, databaseService))
                .build();

        log.info("Bot JDA inicializado com sucesso");

        oauthServer = new OAuthServer(oAuthService, databaseService);
        oauthServer.start();

        webhookServer = new WebhookServer(widgetSyncService, databaseService);
        webhookServer.start();

        autoSyncService = new AutoSyncService(widgetSyncService, databaseService);
        autoSyncService.start();

        log.info("Servidores HTTP iniciados (OAuth: 8080, Webhooks: 8081)");
        log.info("Auto-sync ativo (intervalo: {} min)", Config.REFRESH_MINUTES);
    }

    private static void shutdown() {
        log.info("Desligando DevStats...");

        if (autoSyncService != null) {
            autoSyncService.stop();
        }

        if (webhookServer != null) {
            webhookServer.stop();
        }

        if (oauthServer != null) {
            oauthServer.stop();
        }

        if (jda != null) {
            jda.shutdown();
        }

        if (databaseService != null) {
            databaseService.close();
        }

        log.info("DevStats desligado com sucesso");
    }
}
