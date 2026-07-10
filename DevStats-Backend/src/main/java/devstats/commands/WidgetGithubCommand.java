package devstats.commands;

import devstats.services.DatabaseService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class WidgetGithubCommand {

    private final DatabaseService databaseService;

    public WidgetGithubCommand(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public void execute(SlashCommandInteractionEvent event) {
        String discordId = event.getUser().getId();
        OptionMapping option = event.getOption("username");

        if (option == null) {
            event.reply("Você precisa informar seu nome de usuário do GitHub.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String githubUsername = option.getAsString();

        databaseService.updateGithub(discordId, githubUsername);

        event.reply("GitHub username definido como: **" + githubUsername + "**")
                .setEphemeral(true)
                .queue();
    }
}
