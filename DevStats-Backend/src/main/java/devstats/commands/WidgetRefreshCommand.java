package devstats.commands;

import devstats.models.UserData;
import devstats.services.DatabaseService;
import devstats.services.WidgetSyncService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.concurrent.CompletableFuture;

public class WidgetRefreshCommand {

    private final WidgetSyncService widgetSyncService;
    private final DatabaseService databaseService;

    public WidgetRefreshCommand(WidgetSyncService widgetSyncService, DatabaseService databaseService) {
        this.widgetSyncService = widgetSyncService;
        this.databaseService = databaseService;
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

        event.reply("Sincronizando widget...")
                .setEphemeral(true)
                .queue(hook -> CompletableFuture.runAsync(() -> {
                    try {
                        String githubUsername = user.getGithubUsername();
                        String accessToken = user.getDiscordAccessToken();

                        widgetSyncService.sync(githubUsername, discordId, accessToken);

                        long now = System.currentTimeMillis();
                        databaseService.updateLastSync(discordId, now);

                        hook.editOriginal("Widget sincronizado.")
                                .queue();
                    } catch (Exception e) {
                        e.printStackTrace();
                        hook.editOriginal("Erro ao sincronizar o widget: " + e.getMessage())
                                .queue();
                    }
                }));
    }
}
