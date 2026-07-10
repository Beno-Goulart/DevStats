package devstats.commands;

import devstats.services.DatabaseService;
import devstats.services.OAuthService;
import devstats.services.WidgetSyncService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class WidgetCommand extends ListenerAdapter {

    private static final String COMMAND_NAME = "widget";
    private static final String COMMAND_DESCRIPTION = "Comandos do DevStats Widget";

    private final WidgetSetupCommand setupCommand;
    private final WidgetRefreshCommand refreshCommand;

    public WidgetCommand() {
        this(new WidgetSyncService(), new OAuthService(), new DatabaseService());
    }

    public WidgetCommand(WidgetSyncService widgetSyncService, OAuthService oAuthService, DatabaseService databaseService) {
        this.setupCommand = new WidgetSetupCommand(oAuthService);
        this.refreshCommand = new WidgetRefreshCommand(widgetSyncService, databaseService);
    }

    @Override
    public void onReady(ReadyEvent event) {
        event.getJDA().updateCommands()
                .addCommands(
                        Commands.slash(COMMAND_NAME, COMMAND_DESCRIPTION)
                                .addSubcommands(
                                        new SubcommandData("setup", "Configura seu widget DevStats"),
                                        new SubcommandData("refresh", "Sincroniza seu widget DevStats")
                                )
                )
                .queue();

        System.out.println("Slash Commands do DevStats registrados!");
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

        switch (subcommand) {
            case "setup":
                setupCommand.execute(event);
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
