package devstats;

import devstats.commands.WidgetCommand;
import devstats.http.OAuthServer;
import devstats.models.Config;
import devstats.services.DatabaseService;
import devstats.services.DiscordWidgetService;
import devstats.services.GithubService;
import devstats.services.OAuthService;
import devstats.services.WidgetSyncService;
import net.dv8tion.jda.api.JDABuilder;

public class Main {

    public static void main(String[] args) throws Exception {

        // Inicializar os serviços
        DatabaseService databaseService = new DatabaseService();
        OAuthService oAuthService = new OAuthService();
        GithubService githubService = new GithubService();
        DiscordWidgetService discordWidgetService = new DiscordWidgetService();
        WidgetSyncService widgetSyncService = new WidgetSyncService(githubService, discordWidgetService);

        // Iniciar o JDA Bot e registrar os listeners / comandos
        JDABuilder.createDefault(Config.BOT_TOKEN)
                .addEventListeners(new WidgetCommand(widgetSyncService, oAuthService, databaseService))
                .build();

        System.out.println("DevStats Bot inicializado com sucesso!");

        // Iniciar servidor HTTP para callback OAuth
        OAuthServer oauthServer = new OAuthServer(oAuthService, databaseService);
        oauthServer.start();
    }
}