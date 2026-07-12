package devstats.models;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    public static String BOT_TOKEN;
    public static String APPLICATION_ID;
    public static String USER_ID;
    public static String GITHUB_USERNAME;
    public static String GITHUB_TOKEN;
    public static String ACCESS_TOKEN;
    public static String CLIENT_SECRET;
    public static String GITHUB_WEBHOOK_SECRET;
    public static int REFRESH_MINUTES = 1;
    public static String DATABASE_URL;
    public static String OAUTH_REDIRECT_URI;

    static {
        try {
            Properties properties = new Properties();
            Map<String, String> envFile = loadEnvFile();

            try (InputStream input = openProperties()) {
                properties.load(input);
            }

            BOT_TOKEN = resolve("discord.bot.token", properties, envFile);
            APPLICATION_ID = resolve("discord.application.id", properties, envFile);
            USER_ID = resolve("discord.user.id", properties, envFile);
            GITHUB_USERNAME = resolve("github.username", properties, envFile);
            GITHUB_TOKEN = resolve("github.token", properties, envFile);
            CLIENT_SECRET = resolve("discord.client.secret", properties, envFile);
            ACCESS_TOKEN = resolve("discord.access.token", properties, envFile);
            GITHUB_WEBHOOK_SECRET = resolve("github.webhook.secret", properties, envFile);

            String refreshMinutes = resolve("widget.refresh.minutes", properties, envFile);
            if (refreshMinutes != null && !refreshMinutes.isBlank()) {
                try {
                    REFRESH_MINUTES = Integer.parseInt(refreshMinutes);
                } catch (NumberFormatException ignored) {
                }
            }

            DATABASE_URL = System.getenv("DATABASE_URL");
            if (DATABASE_URL == null || DATABASE_URL.isBlank()) {
                DATABASE_URL = resolve("database.url", properties, envFile);
            }

            OAUTH_REDIRECT_URI = resolve("oauth.redirect.uri", properties, envFile);
            if (OAUTH_REDIRECT_URI == null || OAUTH_REDIRECT_URI.isBlank()) {
                OAUTH_REDIRECT_URI = System.getenv("OAUTH_REDIRECT_URI");
            }
            if (OAUTH_REDIRECT_URI == null || OAUTH_REDIRECT_URI.isBlank()) {
                OAUTH_REDIRECT_URI = "http://localhost:8080/callback";
            }

            log.info("Configurações carregadas com sucesso");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static InputStream openProperties() throws Exception {
        InputStream resource = Config.class
                .getClassLoader()
                .getResourceAsStream("application.properties");

        if (resource != null) {
            return resource;
        }

        Path localFile = Path.of("application.properties");
        if (Files.exists(localFile)) {
            return new FileInputStream(localFile.toFile());
        }

        Path resourceFile = Path.of("src", "main", "resources", "application.properties");
        if (Files.exists(resourceFile)) {
            return new FileInputStream(resourceFile.toFile());
        }

        return InputStream.nullInputStream();
    }

    private static Map<String, String> loadEnvFile() throws Exception {
        Map<String, String> values = new HashMap<>();
        Path envPath = findEnvFile();

        if (envPath == null) {
            return values;
        }

        for (String line : Files.readAllLines(envPath)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                continue;
            }
            String[] parts = trimmed.split("=", 2);
            values.put(parts[0].trim(), unquote(parts[1].trim()));
        }

        log.debug("Arquivo .env carregado: {} variáveis", values.size());
        return values;
    }

    private static Path findEnvFile() {
        Path localFile = Path.of(".env");
        if (Files.exists(localFile)) {
            return localFile;
        }

        Path backendFile = Path.of("DevStats-Backend", ".env");
        if (Files.exists(backendFile)) {
            return backendFile;
        }

        return null;
    }

    private static String resolve(String propertyKey, Properties properties, Map<String, String> envFile) {
        String value = properties.getProperty(propertyKey);

        if (value != null) {
            if (!value.startsWith("${") || !value.endsWith("}")) {
                return value;
            }

            String environmentKey = value.substring(2, value.length() - 1);
            String environmentValue = System.getenv(environmentKey);

            if (environmentValue != null && !environmentValue.isBlank()) {
                return environmentValue;
            }

            String envFileValue = envFile.get(environmentKey);

            if (envFileValue != null && !envFileValue.isBlank()) {
                return envFileValue;
            }
        }

        String envKey = propertyKey.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return envFile.getOrDefault(propertyKey, "");
    }

    private static String unquote(String value) {
        if (
                value.length() >= 2 &&
                        (
                                value.startsWith("\"") && value.endsWith("\"") ||
                                        value.startsWith("'") && value.endsWith("'")
                        )
        ) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }
}
