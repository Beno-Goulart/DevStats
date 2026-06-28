package devstats;


import devstats.commands.WidgetCommand;
import devstats.models.Config;
import net.dv8tion.jda.api.JDABuilder;

public class Main {

    public static void main(String[] args) throws Exception {

        JDABuilder.createDefault(Config.BOT_TOKEN)
                .addEventListeners(new WidgetCommand())
                .build();

    }

}