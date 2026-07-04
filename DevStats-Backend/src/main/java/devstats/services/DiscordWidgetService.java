package devstats.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import devstats.models.Config;
import devstats.models.DynamicField;
import devstats.models.GithubProfile;
import devstats.models.ImageField;
import devstats.models.WidgetData;
import devstats.models.WidgetPayload;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class DiscordWidgetService {

    private static final String API = "https://discord.com/api/v9";
    private static final int TEXT_FIELD_TYPE = 1;
    private static final int IMAGE_FIELD_TYPE = 2;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public void sync(GithubProfile profile) throws Exception {

        updateProfile(profile);

    }

    public void updateProfile(GithubProfile profile) throws Exception {

        WidgetPayload payload = buildPayload(profile);
        String json = mapper.writeValueAsString(payload);

        HttpResponse<String> response = sendPatch(json);
        validateResponse(response);

    }

    private WidgetPayload buildPayload(GithubProfile profile) {

        WidgetPayload payload = new WidgetPayload();

        payload.setUsername(profile.getUsername());
        payload.setData(new WidgetData(buildDynamicFields(profile)));

        return payload;

    }

    private List<DynamicField> buildDynamicFields(GithubProfile profile) {

        List<DynamicField> fields = new ArrayList<>();

        fields.add(textField("full_name", profile.getFullName()));
        fields.add(textField("role", profile.getBio()));
        fields.add(imageField("profile_img", profile.getAvatarUrl()));
        fields.add(textField("language", profile.getMainLanguage()));
        fields.add(textField("commits", String.valueOf(profile.getCommits())));
        fields.add(textField("last_commit", profile.getLastCommit()));
        fields.add(textField("last_repo", profile.getLastRepository()));

        return fields;

    }

    private DynamicField textField(String name, String value) {

        return new DynamicField(TEXT_FIELD_TYPE, name, value);

    }

    private ImageField imageField(String name, String value) {

        return new ImageField(IMAGE_FIELD_TYPE, name, value);

    }

    private HttpResponse<String> sendPatch(String json) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(profileUrl()))
                .header("Authorization", "Bot " + Config.BOT_TOKEN)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());

    }

    private void validateResponse(HttpResponse<String> response) throws IOException {

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "Discord Widget API request failed: PATCH " +
                            profileUrl() +
                            " returned HTTP " +
                            response.statusCode() +
                            " - " +
                            response.body()
            );
        }

    }

    private String profileUrl() {

        return API +
                "/applications/" +
                Config.APPLICATION_ID +
                "/users/" +
                Config.USER_ID +
                "/identities/0/profile";

    }

}
