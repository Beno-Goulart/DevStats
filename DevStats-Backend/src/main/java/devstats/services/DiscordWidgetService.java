package devstats.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import devstats.models.Config;
import devstats.models.DynamicField;
import devstats.models.GithubProfile;
import devstats.models.WidgetData;
import devstats.models.WidgetPayload;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DiscordWidgetService {

    private static final String API = "https://discord.com/api/v9";

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

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public void sync(GithubProfile profile) throws Exception {
        sync(Config.USER_ID, Config.ACCESS_TOKEN, profile);
    }

    public void sync(String userId, String accessToken, GithubProfile profile) throws Exception {

        WidgetPayload payload = buildPayload(profile);

        String json = mapper.writeValueAsString(payload);

        System.out.println("========== DISCORD PAYLOAD ==========");
        System.out.println(json);
        System.out.println("=====================================");

        HttpResponse<String> response = sendPatch(userId, accessToken, json);

        System.out.println("Status : " + response.statusCode());
        System.out.println("Body   : " + response.body());

        validateResponse(userId, response);
    }

    private WidgetPayload buildPayload(GithubProfile profile) {

        WidgetPayload payload = new WidgetPayload();

        payload.setUsername(value(profile.getUsername()));

        List<DynamicField> dynamicFields = buildDynamicFields(profile);

        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) {
            dynamicFields.add(new DynamicField(IMAGE, AVATAR, Map.of("url", profile.getAvatarUrl())));
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

    private static final int MAX_FIELD_LENGTH = 100;

    private String value(String value) {

        if (value == null || value.isBlank()) {
            return "";
        }

        if (value.length() > MAX_FIELD_LENGTH) {
            return value.substring(0, MAX_FIELD_LENGTH);
        }

        return value;
    }

    private HttpResponse<String> sendPatch(String userId, String accessToken, String json) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(profileUrl(userId)))
                .header("Authorization", "Bot " + Config.BOT_TOKEN)
                // .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void validateResponse(String userId, HttpResponse<String> response) throws IOException {

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }

        throw new IOException(
                "Discord Widget API request failed.\n" +
                "URL: " + profileUrl(userId) + "\n" +
                "HTTP: " + response.statusCode() + "\n" +
                response.body()
        );
    }

    private String profileUrl(String userId) {

        return API
                + "/applications/"
                + Config.APPLICATION_ID
                + "/users/"
                + userId
                + "/identities/0/profile";
    }
}