package devstats.commands;

import devstats.services.WidgetSyncService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.concurrent.CompletableFuture;

public class WidgetCommand extends ListenerAdapter {

    private static final String COMMAND_NAME = "widget";
    private static final String COMMAND_DESCRIPTION = "Atualiza seu DevStats Widget";
    private static final String SYNC_STARTED_MESSAGE = "Atualizando widget...";
    private static final String SYNC_SUCCESS_MESSAGE = "Widget atualizado com sucesso.";
    private static final String SYNC_ERROR_MESSAGE = "Erro ao atualizar o widget";

    private final WidgetSyncService widgetSyncService;

    public WidgetCommand() {

        this(new WidgetSyncService());

    }

    public WidgetCommand(WidgetSyncService widgetSyncService) {

        this.widgetSyncService = widgetSyncService;

    }

    @Override
    public void onReady(ReadyEvent event) {

        event.getJDA().updateCommands()
                .addCommands(
                        Commands.slash(
                                COMMAND_NAME,
                                COMMAND_DESCRIPTION
                        )
                )
                .queue();

        System.out.println("Slash Commands registrados!");

    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        if (!event.getName().equals(COMMAND_NAME))
            return;

        event.reply(SYNC_STARTED_MESSAGE)
                .setEphemeral(true)
                .queue(hook -> CompletableFuture.runAsync(() -> syncWidget(hook)));

    }

    private void syncWidget(InteractionHook hook) {

        try {

            widgetSyncService.sync();

            hook.editOriginal(SYNC_SUCCESS_MESSAGE)
                    .queue();

        } catch (Exception e) {

            hook.editOriginal(SYNC_ERROR_MESSAGE)
                    .queue();

        }

    }

}
