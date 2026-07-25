package devstats.commands;

import devstats.models.DiscordTokenResponse;
import devstats.models.UserData;
import devstats.services.DatabaseService;
import devstats.services.OAuthService;
import devstats.services.WidgetSyncService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class WidgetRefreshCommand {

    private static final Logger log = LoggerFactory.getLogger(WidgetRefreshCommand.class);

    private final WidgetSyncService widgetSyncService;
    private final DatabaseService databaseService;
    private final OAuthService oAuthService;

    public WidgetRefreshCommand(WidgetSyncService widgetSyncService, DatabaseService databaseService, OAuthService oAuthService) {
        this.widgetSyncService = widgetSyncService;
        this.databaseService = databaseService;
        this.oAuthService = oAuthService;
    }

    public void execute(SlashCommandInteractionEvent event) {
        String discordId = event.getUser().getId();
        UserData user = databaseService.findUser(discordId);

        if (user == null) {
            event.reply("Você precisa executar /widget setup primeiro.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (user.getGithubUsername() == null || user.getGithubUsername().isBlank()) {
            event.reply("Você precisa configurar sua conta primeiro.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.reply("Sincronizando widget...")
                .setEphemeral(true)
                .queue(hook -> CompletableFuture.runAsync(() -> {
                    try {
                        String accessToken = ensureValidToken(user, discordId);

                        widgetSyncService.sync(discordId, accessToken, user.getGithubUsername());

                        databaseService.updateLastSync(discordId, System.currentTimeMillis());

                        hook.editOriginal("Widget sincronizado com sucesso!")
                                .queue();
                    } catch (Exception e) {
                        log.error("Erro ao sincronizar widget para {}: {}", discordId, e.getMessage(), e);
                        hook.editOriginal("Erro ao sincronizar o widget: " + e.getMessage())
                                .queue();
                    }
                }));
    }

    private String ensureValidToken(UserData user, String discordId) throws Exception {
        if (!user.isTokenExpired()) {
            return user.getDiscordAccessToken();
        }

        if (user.getRefreshToken() != null && !user.getRefreshToken().isBlank()) {
            log.info("Token expirado para {}, renovando...", discordId);
            DiscordTokenResponse tokenResponse = oAuthService.refreshAccessToken(user.getRefreshToken());
            databaseService.updateToken(discordId, tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), tokenResponse.getExpiresIn());
            return tokenResponse.getAccessToken();
        }

        throw new RuntimeException("Token expirado e sem refresh token. Execute /widget setup novamente.");
    }
}
