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
import java.util.LinkedHashMap;
import java.util.Map;

public class DiscordWidgetService {

    private static final Logger log = LoggerFactory.getLogger(DiscordWidgetService.class);

    private static final String API = "https://discord.com/api/v10";
    private static final String CONFIG_PATH = "widget-config.json";

    private static final int MAX_FIELD_LENGTH = 100;

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonNode widgetConfig;

    public DiscordWidgetService() {
        this.widgetConfig = loadConfig();
    }

    public void sync(GithubProfile profile) throws Exception {
        sync(Config.USER_ID, Config.ACCESS_TOKEN, profile);
    }

    public void sync(String userId, String accessToken, GithubProfile profile) throws Exception {
        ObjectNode payload = buildPayload(profile);

        String json = JsonUtils.toJson(payload);

        log.debug("Discord payload para {}: {}", userId, json);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bot " + Config.BOT_TOKEN);
        if (accessToken != null && !accessToken.isBlank()) {
            headers.put("X-Access-Token", accessToken);
        }
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        try {
            String url = profileUrl(userId);
            String response = HttpUtils.patch(url, json, headers);
            log.info("Widget sincronizado para {} com sucesso", userId);
            log.debug("Resposta Discord: {}", response);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("40106")) {
                log.warn("Identity já existe para outro usuário, deletando identity 0 para {}...", userId);
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
        payload.put("username", truncate(profile.getUsername()));

        if (widgetConfig != null) {
            ObjectNode data = buildDataNode(profile);
            payload.set("data", data);

            if (widgetConfig.has("surfaces")) {
                payload.set("surfaces", widgetConfig.get("surfaces"));
            }
        } else {
            log.warn("Widget config não encontrado, usando payload flat");
            payload.set("data", buildFlatPayload(profile));
        }

        return payload;
    }

    private ObjectNode buildDataNode(GithubProfile profile) {
        ObjectNode data = mapper.createObjectNode();

        data.put("full_name", truncate(profile.getFullName()));
        data.put("bio", truncate(profile.getBio()));
        data.put("language", truncate(profile.getMainLanguage()));
        data.put("active", String.valueOf(profile.getActiveRepos()));
        data.put("last_repo", truncate(profile.getLastRepository()));
        data.put("last_commit", truncate(profile.getLastCommit()));
        data.put("commits_today", String.valueOf(profile.getCommitsToday()));

        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) {
            ObjectNode avatarObj = mapper.createObjectNode();
            avatarObj.put("url", profile.getAvatarUrl());
            data.set("profile_img", avatarObj);
        }

        ArrayNode dynamic = mapper.createArrayNode();
        dynamic.add(dynamicField("full_name", profile.getFullName()));
        dynamic.add(dynamicField("bio", profile.getBio()));
        dynamic.add(dynamicField("language", profile.getMainLanguage()));
        dynamic.add(dynamicField("active", String.valueOf(profile.getActiveRepos())));
        dynamic.add(dynamicField("last_repo", profile.getLastRepository()));
        dynamic.add(dynamicField("last_commit", profile.getLastCommit()));
        dynamic.add(dynamicField("commits_today", String.valueOf(profile.getCommitsToday())));
        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) {
            ObjectNode imgField = mapper.createObjectNode();
            imgField.put("type", 3);
            imgField.put("name", "profile_img");
            ObjectNode imgValue = mapper.createObjectNode();
            imgValue.put("url", profile.getAvatarUrl());
            imgField.set("value", imgValue);
            dynamic.add(imgField);
        }
        data.set("dynamic", dynamic);

        return data;
    }

    private ObjectNode buildFlatPayload(GithubProfile profile) {
        ArrayNode dynamic = mapper.createArrayNode();

        dynamic.add(dynamicField("full_name", profile.getFullName()));
        dynamic.add(dynamicField("bio", profile.getBio()));
        dynamic.add(dynamicField("language", profile.getMainLanguage()));
        dynamic.add(dynamicField("active", String.valueOf(profile.getActiveRepos())));
        dynamic.add(dynamicField("last_repo", profile.getLastRepository()));
        dynamic.add(dynamicField("last_commit", profile.getLastCommit()));
        dynamic.add(dynamicField("commits_today", String.valueOf(profile.getCommitsToday())));

        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) {
            ObjectNode imgField = mapper.createObjectNode();
            imgField.put("type", 3);
            imgField.put("name", "profile_img");
            ObjectNode imgValue = mapper.createObjectNode();
            imgValue.put("url", profile.getAvatarUrl());
            imgField.set("value", imgValue);
            dynamic.add(imgField);
        }

        ObjectNode data = mapper.createObjectNode();
        data.set("dynamic", dynamic);
        return data;
    }

    private ObjectNode dynamicField(String name, String value) {
        ObjectNode field = mapper.createObjectNode();
        field.put("type", 1);
        field.put("name", name);
        field.put("value", truncate(value));
        return field;
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() > MAX_FIELD_LENGTH) {
            return value.substring(0, MAX_FIELD_LENGTH);
        }
        return value;
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
                log.info("Widget config carregado com {} surfaces", config.get("surfaces").size());
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
