package devstats.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import devstats.models.Config;
import devstats.models.GithubProfile;
import devstats.utils.HttpUtils;
import devstats.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

public class DiscordWidgetService {

    private static final Logger log = LoggerFactory.getLogger(DiscordWidgetService.class);

    private static final String API = "https://discord.com/api/v10";
    private static final String CONFIG_PATH = "widget-config.json";

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonNode widgetConfig;

    public DiscordWidgetService() {
        this.widgetConfig = loadConfig();
    }

    public void sync() throws Exception {
        sync(Config.USER_ID, Config.ACCESS_TOKEN, null);
    }

    public void sync(String userId, String accessToken, GithubProfile profile) throws Exception {
        ObjectNode payload = buildPayload(profile);
        String json = JsonUtils.toJson(payload);

        log.debug("Discord payload para {}: {}", userId, json);

        Map<String, String> headers = buildHeaders(accessToken);

        try {
            String url = profileUrl(userId);
            String response = HttpUtils.patch(url, json, headers);
            log.info("Widget sincronizado para {} com sucesso", userId);
            log.debug("Resposta Discord: {}", response);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("40106")) {
                log.warn("Identity já existe, deletando identity 0 para {}...", userId);
                deleteIdentity(userId, headers);
                String url = profileUrl(userId);
                String response = HttpUtils.patch(url, json, headers);
                log.info("Widget sincronizado para {} após delete+recreate", userId);
                log.debug("Resposta Discord: {}", response);
            } else {
                throw e;
            }
        }
    }

    private void deleteIdentity(String userId, Map<String, String> headers) {
        try {
            String url = identitiesUrl(userId);
            HttpUtils.delete(url, headers);
            log.info("Identity deletada para {}", userId);
        } catch (Exception e) {
            log.warn("Falha ao deletar identity para {}: {}", userId, e.getMessage());
        }
    }

    private ObjectNode buildPayload(GithubProfile profile) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("username", profile != null ? profile.getUsername() : "");

        ObjectNode data = mapper.createObjectNode();
        ArrayNode dynamic = mapper.createArrayNode();

        if (profile != null) {
            addTextField(dynamic, "full_name", profile.getFullName());
            addTextField(dynamic, "bio", profile.getBio());
            addImageField(dynamic, "profile_img", profile.getAvatarUrl());
            addTextField(dynamic, "language", profile.getMainLanguage());
            addTextField(dynamic, "active", String.valueOf(profile.getActiveRepos()));
            addTextField(dynamic, "commits_today", String.valueOf(profile.getCommitsToday()));
            addTextField(dynamic, "last_commit", profile.getLastCommit());
            addTextField(dynamic, "last_repo", profile.getLastRepository());
        }

        data.set("dynamic", dynamic);
        payload.set("data", data);

        if (widgetConfig != null && widgetConfig.has("surfaces")) {
            payload.set("surfaces", widgetConfig.get("surfaces"));
        }

        return payload;
    }

    private void addTextField(ArrayNode dynamic, String name, String value) {
        if (value == null || value.isBlank()) return;
        ObjectNode field = mapper.createObjectNode();
        field.put("type", 1);
        field.put("name", name);
        field.put("value", value);
        dynamic.add(field);
    }

    private void addImageField(ArrayNode dynamic, String name, String url) {
        if (url == null || url.isBlank()) return;
        ObjectNode field = mapper.createObjectNode();
        field.put("type", 3);
        field.put("name", name);
        ObjectNode value = mapper.createObjectNode();
        value.put("url", url);
        field.set("value", value);
        dynamic.add(field);
    }

    private Map<String, String> buildHeaders(String accessToken) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bot " + Config.BOT_TOKEN);
        if (accessToken != null && !accessToken.isBlank()) {
            headers.put("X-Access-Token", accessToken);
        }
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        return headers;
    }

    private JsonNode loadConfig() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_PATH);
            if (is == null) {
                log.warn("Widget config não encontrado: {}", CONFIG_PATH);
                return null;
            }
            try (is) {
                JsonNode config = mapper.readTree(is);
                log.info("Widget config carregado: {} surfaces", config.get("surfaces").size());
                return config;
            }
        } catch (Exception e) {
            log.error("Erro ao carregar widget config: {}", e.getMessage());
            return null;
        }
    }

    private String profileUrl(String userId) {
        return API
                + "/applications/"
                + Config.APPLICATION_ID
                + "/users/"
                + userId
                + "/identities/0/profile";
    }

    private String identitiesUrl(String userId) {
        return API
                + "/applications/"
                + Config.APPLICATION_ID
                + "/users/"
                + userId
                + "/identities/0";
    }
}
