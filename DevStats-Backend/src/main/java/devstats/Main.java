package devstats;

import devstats.models.Config;
import devstats.services.DiscordWidgetService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static JDA jda;

    public static void main(String[] args) throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown));

        log.info("Iniciando Grokix Widget Bot...");

        DiscordWidgetService widgetService = new DiscordWidgetService();

        jda = JDABuilder.createDefault(Config.BOT_TOKEN).build();
        jda.awaitReady();

        log.info("Bot conectado, aplicando widget...");

        widgetService.sync();

        log.info("Widget aplicado com sucesso! Bot ativo.");
    }

    private static void shutdown() {
        log.info("Desligando...");
        if (jda != null) {
            jda.shutdown();
        }
    }
}
