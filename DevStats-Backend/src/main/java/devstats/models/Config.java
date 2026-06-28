package devstats.models;

import java.io.FileInputStream;
import java.util.Properties;

public class Config {

    public static String BOT_TOKEN;

    public static String APPLICATION_ID;

    public static String USER_ID;

    public static String GITHUB_USERNAME;

    public static String GITHUB_TOKEN;

    static {

        try {

            Properties properties = new Properties();

            properties.load(new FileInputStream("application.properties"));

            BOT_TOKEN = properties.getProperty("discord.bot.token");

            APPLICATION_ID = properties.getProperty("discord.application.id");

            USER_ID = properties.getProperty("discord.user.id");

            GITHUB_USERNAME = properties.getProperty("github.username");

            GITHUB_TOKEN = properties.getProperty("github.token");

        } catch (Exception e) {

            throw new RuntimeException(e);

        }

    }

}