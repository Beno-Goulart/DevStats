package devstats.commands;

import devstats.models.Config;
import devstats.models.UserData;
import devstats.services.DatabaseService;
import devstats.utils.HttpUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WidgetUnlinkCommand {

    private static final Logger log = LoggerFactory.getLogger(WidgetUnlinkCommand.class);

    private final DatabaseService databaseService;

    public WidgetUnlinkCommand(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public void execute(SlashCommandInteractionEvent event) {
        String discordId = event.getUser().getId();
        UserData user = databaseService.findUser(discordId);

        if (user == null) {
            event.reply("Você não possui dados vinculados.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.reply("Desvinculando sua conta...")
                .setEphemeral(true)
                .queue(hook -> CompletableFuture.runAsync(() -> {
                    try {
                        Map<String, String> headers = new LinkedHashMap<>();
                        headers.put("Authorization", "Bot " + Config.BOT_TOKEN);
                        if (user.getDiscordAccessToken() != null && !user.getDiscordAccessToken().isBlank()) {
                            headers.put("X-Access-Token", user.getDiscordAccessToken());
                        }

                        String deleteUrl = "https://discord.com/api/v10/applications/"
                                + Config.APPLICATION_ID
                                + "/users/" + discordId
                                + "/identities/0";

                        HttpUtils.delete(deleteUrl, headers);
                        log.info("Identity deletada no Discord para {}", discordId);

                        databaseService.deleteUser(discordId);
                        log.info("Dados deletados no DB para {}", discordId);

                        hook.editOriginal("Conta desvinculada com sucesso! Você pode executar `/widget setup` para vincular novamente.")
                                .queue();
                    } catch (Exception e) {
                        log.error("Erro ao desvincular {}: {}", discordId, e.getMessage(), e);

                        databaseService.deleteUser(discordId);

                        hook.editOriginal("Identity removida do Discord. Dados locais limpos. Execute `/widget setup` para recomeçar.")
                                .queue();
                    }
                }));
    }
}
