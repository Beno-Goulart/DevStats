package devstats.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devstats.models.Config;
import devstats.models.GithubProfile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GithubService {

    private static final String API = "https://api.github.com";

    private final HttpClient client = HttpClient.newHttpClient();

    private final ObjectMapper mapper = new ObjectMapper();

    public GithubProfile getProfile() throws Exception {

        GithubProfile profile = new GithubProfile();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API + "/users/" + Config.GITHUB_USERNAME))
                .header("Authorization", "Bearer " + Config.GITHUB_TOKEN)
                .header("Accept", "application/vnd.github+json")
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode json = mapper.readTree(response.body());

        profile.setUsername(json.get("login").asText());

        profile.setFullName(json.get("name").asText());

        profile.setAvatarUrl(json.get("avatar_url").asText());

        profile.setBio(json.get("bio").asText());

        loadRepositories(profile);

        return profile;

    }

    private void loadRepositories(GithubProfile profile) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(
                        URI.create(
                                API +
                                        "/users/" +
                                        Config.GITHUB_USERNAME +
                                        "/repos?sort=updated&per_page=1"
                        )
                )
                .header("Authorization", "Bearer " + Config.GITHUB_TOKEN)
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode repo = mapper.readTree(response.body()).get(0);

        profile.setLastRepository(repo.get("name").asText());

        if (repo.hasNonNull("language")) {

            profile.setMainLanguage(
                    repo.get("language").asText()
            );

        } else {

            profile.setMainLanguage("Unknown");

        }

        loadCommits(profile);

    }

    private void loadCommits(GithubProfile profile) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(
                        URI.create(
                                API +
                                        "/repos/" +
                                        Config.GITHUB_USERNAME +
                                        "/" +
                                        profile.getLastRepository() +
                                        "/commits?per_page=1"
                        )
                )
                .header("Authorization", "Bearer " + Config.GITHUB_TOKEN)
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode commit = mapper.readTree(response.body()).get(0);

        profile.setLastCommit(
                commit
                        .get("commit")
                        .get("author")
                        .get("date")
                        .asText()
        );

        profile.setCommits(1);

        profile.setStreak(0);

    }

}