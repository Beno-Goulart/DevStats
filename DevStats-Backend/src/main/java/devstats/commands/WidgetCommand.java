package devstats.commands;

import devstats.services.DatabaseService;
import devstats.services.OAuthService;
import devstats.services.WidgetSyncService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WidgetCommand extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(WidgetCommand.class);

    private static final String COMMAND_NAME = "widget";
    private static final String COMMAND_DESCRIPTION = "Comandos do DevStats Widget";

    private final WidgetSetupCommand setupCommand;
    private final WidgetRefreshCommand refreshCommand;
    private final WidgetGithubCommand githubCommand;

    private boolean commandsRegistered = false;

    public WidgetCommand() {
        this(new WidgetSyncService(), new OAuthService(), new DatabaseService());
    }

    public WidgetCommand(WidgetSyncService widgetSyncService, OAuthService oAuthService, DatabaseService databaseService) {
        this.setupCommand = new WidgetSetupCommand(oAuthService);
        this.refreshCommand = new WidgetRefreshCommand(widgetSyncService, databaseService, oAuthService);
        this.githubCommand = new WidgetGithubCommand(databaseService);
    }

    @Override
    public void onReady(ReadyEvent event) {
        if (commandsRegistered) {
            return;
        }

        event.getJDA().updateCommands()
                .addCommands(
                        Commands.slash(COMMAND_NAME, COMMAND_DESCRIPTION)
                                .addSubcommands(
                                        new SubcommandData("setup", "Configura seu widget DevStats"),
                                        new SubcommandData("github", "Define seu nome de usuário do GitHub")
                                                .addOptions(new OptionData(OptionType.STRING, "username", "Seu nome de usuário do GitHub", true)),
                                        new SubcommandData("refresh", "Sincroniza seu widget DevStats")
                                )
                )
                .queue(success -> {
                    commandsRegistered = true;
                    log.info("Slash commands registrados com sucesso");
                });
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals(COMMAND_NAME)) {
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            return;
        }

        log.debug("Comando recebido: /{} {} por {}", COMMAND_NAME, subcommand, event.getUser().getId());

        switch (subcommand) {
            case "setup":
                setupCommand.execute(event);
                break;
            case "github":
                githubCommand.execute(event);
                break;
            case "refresh":
                refreshCommand.execute(event);
                break;
            default:
                event.reply("Subcomando desconhecido.")
                        .setEphemeral(true)
                        .queue();
                break;
        }
    }
}
