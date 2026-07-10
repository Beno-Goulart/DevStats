package devstats.commands;

import devstats.services.OAuthService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class WidgetSetupCommand {

    private final OAuthService oAuthService;

    public WidgetSetupCommand(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    public void execute(SlashCommandInteractionEvent event) {
        String oauthUrl = oAuthService.generateAuthorizationUrl();
        Button authButton = Button.link(oauthUrl, "Autorizar Discord");

        event.reply("Clique no botão abaixo para autorizar o DevStats no seu Discord:")
                .addActionRow(authButton)
                .setEphemeral(true)
                .queue();
    }
}
