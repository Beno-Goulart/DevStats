package devstats.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devstats.models.Config;
import devstats.models.GithubProfile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class GithubService {

    private static final String API = "https://api.github.com";
    private static final int REPOSITORIES_PER_PAGE = 100;
    private static final int COMMITS_PER_PAGE = 100;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public GithubProfile getProfile() throws Exception {

        GithubProfile profile = new GithubProfile();

        JsonNode user = getJson("/users/" + path(Config.GITHUB_USERNAME));

        profile.setUsername(text(user, "login"));
        profile.setFullName(text(user, "name"));
        profile.setBio(text(user, "bio"));
        profile.setAvatarUrl(text(user, "avatar_url"));
        profile.setStreak(0);

        loadRepositories(profile);

        return profile;

    }

    private void loadRepositories(GithubProfile profile) throws Exception {

        Map<String, Long> languageBytes = new HashMap<>();
        JsonNode latestRepository = null;

        int page = 1;

        while (true) {

            JsonNode repositories = getJson(
                    "/users/" +
                            path(Config.GITHUB_USERNAME) +
                            "/repos?per_page=" +
                            REPOSITORIES_PER_PAGE +
                            "&page=" +
                            page
            );

            if (!repositories.isArray() || repositories.isEmpty()) {
                break;
            }

            for (JsonNode repository : repositories) {

                addRepositoryLanguages(repository, languageBytes);

                if (
                        latestRepository == null ||
                                text(repository, "updated_at").compareTo(text(latestRepository, "updated_at")) > 0
                ) {
                    latestRepository = repository;
                }

            }

            if (repositories.size() < REPOSITORIES_PER_PAGE) {
                break;
            }

            page++;

        }

        profile.setMainLanguage(findMainLanguage(languageBytes));

        if (latestRepository == null) {
            profile.setCommits(0);
            return;
        }

        profile.setLastRepository(text(latestRepository, "name"));
        loadCommits(profile, latestRepository);

    }

    private void addRepositoryLanguages(JsonNode repository, Map<String, Long> languageBytes) throws Exception {

        String owner = text(repository.path("owner"), "login");
        String repositoryName = text(repository, "name");

        if (owner.isBlank() || repositoryName.isBlank()) {
            return;
        }

        JsonNode languages = getJson(
                "/repos/" +
                        path(owner) +
                        "/" +
                        path(repositoryName) +
                        "/languages"
        );

        languages.properties().forEach(entry ->
                languageBytes.merge(
                        entry.getKey(),
                        entry.getValue().asLong(),
                        Long::sum
                )
        );

    }

    private void loadCommits(GithubProfile profile, JsonNode repository) throws Exception {

        String owner = text(repository.path("owner"), "login");
        String repositoryName = text(repository, "name");

        JsonNode commits = getJson(
                "/repos/" +
                        path(owner) +
                        "/" +
                        path(repositoryName) +
                        "/commits?per_page=" +
                        COMMITS_PER_PAGE
        );

        if (!commits.isArray() || commits.isEmpty()) {
            profile.setCommits(0);
            return;
        }

        JsonNode latestCommit = commits.get(0);
        String message = text(latestCommit.path("commit"), "message");

        profile.setLastCommit(message.isBlank() ? text(latestCommit, "sha") : message);
        profile.setCommits(commits.size());

    }

    private JsonNode getJson(String path) throws Exception {

        HttpResponse<String> response = sendGet(path);
        return mapper.readTree(response.body());

    }

    private HttpResponse<String> sendGet(String path) throws Exception {

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(API + path))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "DevStats")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET();

        if (Config.GITHUB_TOKEN != null && !Config.GITHUB_TOKEN.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + Config.GITHUB_TOKEN);
        }

        HttpResponse<String> response = client.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "GitHub API request failed: GET " +
                            path +
                            " returned HTTP " +
                            response.statusCode() +
                            " - " +
                            response.body()
            );
        }

        return response;

    }

    private String findMainLanguage(Map<String, Long> languageBytes) {

        return languageBytes.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

    }

    private String text(JsonNode node, String field) {

        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText();

    }

    private String path(String value) {

        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");

    }

}
