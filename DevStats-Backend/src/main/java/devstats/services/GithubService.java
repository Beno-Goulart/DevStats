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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GithubService {

    private static final String API = "https://api.github.com";
    private static final int REPOSITORIES_PER_PAGE = 100;
    private static final int COMMITS_PER_PAGE = 100;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public Optional<GithubProfile> getProfile() {
        return getProfile(Config.GITHUB_USERNAME);
    }

    public Optional<GithubProfile> getProfile(String githubUsername) {
        if (githubUsername == null || githubUsername.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode user = fetchUserDetails(githubUsername);
            if (user == null || user.isMissingNode() || user.isNull()) {
                return Optional.empty();
            }

            GithubProfile profile = new GithubProfile();
            profile.setUsername(textField(user, "login"));
            profile.setFullName(textField(user, "name"));
            profile.setBio(textField(user, "bio"));
            profile.setAvatarUrl(textField(user, "avatar_url"));

            loadRepositoryStats(profile, githubUsername);

            return Optional.of(profile);
        } catch (Exception e) {
            System.err.println("Erro ao obter perfil do GitHub para " + githubUsername + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private JsonNode fetchUserDetails(String username) throws Exception {
        return getJsonNode("/users/" + encodePathSegment(username));
    }

    private JsonNode fetchUserRepositories(String username, int page) throws Exception {
        return getJsonNode(
                "/users/" +
                        encodePathSegment(username) +
                        "/repos?per_page=" +
                        REPOSITORIES_PER_PAGE +
                        "&page=" +
                        page
        );
    }

    private void loadRepositoryStats(GithubProfile profile, String githubUsername) throws Exception {

        Map<String, Long> languageBytes = new HashMap<>();
        JsonNode latestRepository = null;
        int activeReposCount = 0;
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        int page = 1;

        while (true) {

            JsonNode repositories = fetchUserRepositories(githubUsername, page);

            if (!repositories.isArray() || repositories.isEmpty()) {
                break;
            }

            for (JsonNode repository : repositories) {

                addRepositoryLanguages(repository, languageBytes);

                String pushedAt = textField(repository, "pushed_at");
                if (!pushedAt.isBlank() && Instant.parse(pushedAt).isAfter(thirtyDaysAgo)) {
                    activeReposCount++;
                }

                if (
                        latestRepository == null ||
                                textField(repository, "updated_at").compareTo(textField(latestRepository, "updated_at")) > 0
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
        profile.setActiveRepos(activeReposCount);

        if (latestRepository == null) {
            return;
        }

        profile.setLastRepository(textField(latestRepository, "name"));
        loadCommits(profile, latestRepository);

    }

    private void addRepositoryLanguages(JsonNode repository, Map<String, Long> languageBytes) throws Exception {

        String owner = textField(repository.path("owner"), "login");
        String repositoryName = textField(repository, "name");

        if (owner.isBlank() || repositoryName.isBlank()) {
            return;
        }

        JsonNode languages = getJsonNode(
                "/repos/" +
                        encodePathSegment(owner) +
                        "/" +
                        encodePathSegment(repositoryName) +
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

        String owner = textField(repository.path("owner"), "login");
        String repositoryName = textField(repository, "name");

        JsonNode commits = getJsonNode(
                "/repos/" +
                        encodePathSegment(owner) +
                        "/" +
                        encodePathSegment(repositoryName) +
                        "/commits?per_page=" +
                        COMMITS_PER_PAGE
        );

        if (!commits.isArray() || commits.isEmpty()) {
            return;
        }

        int commitsToday = 0;
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();

        for (JsonNode commit : commits) {
            String dateStr = commit.path("commit").path("author").path("date").asText("");
            if (!dateStr.isBlank() && Instant.parse(dateStr).isAfter(startOfDay)) {
                commitsToday++;
            }
        }

        JsonNode latestCommit = commits.get(0);
        String message = textField(latestCommit.path("commit"), "message");
        profile.setLastCommit(message.isBlank() ? textField(latestCommit, "sha") : message);
        profile.setCommitsToday(commitsToday);

    }

    private JsonNode getJsonNode(String path) throws Exception {

        HttpResponse<String> response = sendGetRequest(path);
        return mapper.readTree(response.body());

    }

    private HttpResponse<String> sendGetRequest(String path) throws Exception {

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

    private String textField(JsonNode node, String field) {

        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText();

    }

    private String encodePathSegment(String value) {

        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");

    }

}
