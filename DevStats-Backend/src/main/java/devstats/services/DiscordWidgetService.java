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
    private static final String FIELD_FULL_NAME = "full_name";
    private static final String FIELD_ROLE = "role";
    private static final String FIELD_PROFILE_IMAGE = "profile_img";
    private static final String FIELD_LANGUAGE = "language";
    private static final String FIELD_COMMITS = "commits";
    private static final String FIELD_LAST_COMMIT = "last_commit";
    private static final String FIELD_LAST_REPOSITORY = "last_repo";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public void sync(GithubProfile profile) throws Exception {

        syncProfile(profile);

    }

    public void syncProfile(GithubProfile profile) throws Exception {

        WidgetPayload payload = buildPayload(profile);
        String json = mapper.writeValueAsString(payload);

        HttpResponse<String> response = sendPatch(json);
        System.out.println(json);

        System.out.println(response.statusCode());

        System.out.println(response.body());
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

        fields.add(textField(FIELD_FULL_NAME, profile.getFullName()));
        fields.add(textField(FIELD_ROLE, profile.getBio()));
        fields.add(imageField(FIELD_PROFILE_IMAGE, profile.getAvatarUrl()));
        fields.add(textField(FIELD_LANGUAGE, profile.getMainLanguage()));
        fields.add(textField(FIELD_COMMITS, String.valueOf(profile.getCommits())));
        fields.add(textField(FIELD_LAST_COMMIT, profile.getLastCommit()));
        fields.add(textField(FIELD_LAST_REPOSITORY, profile.getLastRepository()));

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
