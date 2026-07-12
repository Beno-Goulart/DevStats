package devstats.services;

import com.fasterxml.jackson.databind.JsonNode;
import devstats.models.Config;
import devstats.models.DynamicField;
import devstats.models.GithubProfile;
import devstats.models.ImageField;
import devstats.models.WidgetData;
import devstats.models.WidgetPayload;
import devstats.utils.HttpUtils;
import devstats.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DiscordWidgetService {

    private static final Logger log = LoggerFactory.getLogger(DiscordWidgetService.class);

    private static final String API = "https://discord.com/api/v10";

    private static final int TEXT = 1;
    private static final int IMAGE = 3;

    private static final String FULL_NAME = "full_name";
    private static final String ROLE = "bio";
    private static final String LANGUAGE = "language";
    private static final String ACTIVE_REPOS = "active";
    private static final String LAST_REPO = "last_repo";
    private static final String LAST_COMMIT = "last_commit";
    private static final String COMMITS_TODAY = "commits_today";
    private static final String AVATAR = "profile_img";

    private static final int MAX_FIELD_LENGTH = 100;

    public void sync(GithubProfile profile) throws Exception {
        sync(Config.USER_ID, Config.ACCESS_TOKEN, profile);
    }

    public void sync(String userId, String accessToken, GithubProfile profile) throws Exception {
        WidgetPayload payload = buildPayload(profile);
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

    private WidgetPayload buildPayload(GithubProfile profile) {
        WidgetPayload payload = new WidgetPayload();
        payload.setUsername(value(profile.getUsername()));

        List<DynamicField> dynamicFields = buildDynamicFields(profile);

        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) {
            dynamicFields.add(new ImageField(IMAGE, AVATAR, profile.getAvatarUrl()));
        }

        payload.setData(new WidgetData(dynamicFields));
        return payload;
    }

    private List<DynamicField> buildDynamicFields(GithubProfile profile) {
        List<DynamicField> fields = new ArrayList<>();
        fields.add(field(FULL_NAME, profile.getFullName()));
        fields.add(field(ROLE, profile.getBio()));
        fields.add(field(LANGUAGE, profile.getMainLanguage()));
        fields.add(field(ACTIVE_REPOS, String.valueOf(profile.getActiveRepos())));
        fields.add(field(LAST_REPO, profile.getLastRepository()));
        fields.add(field(LAST_COMMIT, profile.getLastCommit()));
        fields.add(field(COMMITS_TODAY, String.valueOf(profile.getCommitsToday())));
        return fields;
    }

    private DynamicField field(String name, String value) {
        return new DynamicField(TEXT, name, value(value));
    }

    private String value(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() > MAX_FIELD_LENGTH) {
            return value.substring(0, MAX_FIELD_LENGTH);
        }
        return value;
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
