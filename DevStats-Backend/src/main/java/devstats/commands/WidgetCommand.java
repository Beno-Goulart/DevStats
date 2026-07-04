package devstats.commands;

import devstats.services.WidgetSyncService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.concurrent.CompletableFuture;

public class WidgetCommand extends ListenerAdapter {

    private final WidgetSyncService widgetSyncService = new WidgetSyncService();

    @Override
    public void onReady(ReadyEvent event) {

        event.getJDA().updateCommands()
                .addCommands(
                        Commands.slash(
                                "widget",
                                "Atualiza seu DevStats Widget"
                        )
                )
                .queue();

        System.out.println("Slash Commands registrados!");

    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        if (!event.getName().equals("widget"))
            return;

        event.reply("Atualizando widget...")
                .setEphemeral(true)
                .queue(hook ->
                        CompletableFuture.runAsync(() -> {

                            try {

                                widgetSyncService.sync();

                                hook.editOriginal("Widget atualizado com sucesso.")
                                        .queue();

                            } catch (Exception e) {

                                hook.editOriginal("Erro ao atualizar o widget")
                                        .queue();

                            }

                        })
                );

    }

}
