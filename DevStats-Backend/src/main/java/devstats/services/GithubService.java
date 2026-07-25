package devstats.services;

import com.fasterxml.jackson.databind.JsonNode;
import devstats.models.Config;
import devstats.models.GithubProfile;
import devstats.utils.DateUtils;
import devstats.utils.HttpUtils;
import devstats.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class GithubService {

    private static final Logger log = LoggerFactory.getLogger(GithubService.class);

    private static final String API = "https://api.github.com";
    private static final int REPOSITORIES_PER_PAGE = 100;
    private static final int COMMITS_PER_PAGE = 100;

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

            log.debug("Perfil GitHub carregado para {}: lang={}, repos={}, commits={}",
                    githubUsername, profile.getMainLanguage(), profile.getActiveRepos(), profile.getCommitsToday());

            return Optional.of(profile);
        } catch (Exception e) {
            log.error("Erro ao obter perfil do GitHub para {}: {}", githubUsername, e.getMessage());
            return Optional.empty();
        }
    }

    private JsonNode fetchUserDetails(String username) throws Exception {
        return fetchJson("/users/" + encodePathSegment(username));
    }

    private JsonNode fetchUserRepositories(String username, int page) throws Exception {
        return fetchJson(
                "/users/" + encodePathSegment(username) + "/repos?per_page=" + REPOSITORIES_PER_PAGE + "&page=" + page
        );
    }

    private void loadRepositoryStats(GithubProfile profile, String githubUsername) throws Exception {
        Map<String, Long> languageBytes = new HashMap<>();
        JsonNode latestRepository = null;
        int activeReposCount = 0;
        var thirtyDaysAgo = DateUtils.daysAgo(30);

        int page = 1;

        while (true) {
            JsonNode repositories = fetchUserRepositories(githubUsername, page);

            if (!repositories.isArray() || repositories.isEmpty()) {
                break;
            }

            for (JsonNode repository : repositories) {
                addRepositoryLanguages(repository, languageBytes);

                String pushedAt = textField(repository, "pushed_at");
                if (DateUtils.isAfter(pushedAt, thirtyDaysAgo)) {
                    activeReposCount++;
                }

                if (latestRepository == null ||
                        textField(repository, "updated_at").compareTo(textField(latestRepository, "updated_at")) > 0) {
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
        loadAllCommitsToday(profile, githubUsername);
    }

    private void addRepositoryLanguages(JsonNode repository, Map<String, Long> languageBytes) throws Exception {
        String owner = textField(repository.path("owner"), "login");
        String repositoryName = textField(repository, "name");

        if (owner.isBlank() || repositoryName.isBlank()) {
            return;
        }

        JsonNode languages = fetchJson(
                "/repos/" + encodePathSegment(owner) + "/" + encodePathSegment(repositoryName) + "/languages"
        );

        languages.properties().forEach(entry ->
                languageBytes.merge(entry.getKey(), entry.getValue().asLong(), Long::sum)
        );
    }

    private void loadCommits(GithubProfile profile, JsonNode repository) throws Exception {
        String owner = textField(repository.path("owner"), "login");
        String repositoryName = textField(repository, "name");

        JsonNode commits = fetchJson(
                "/repos/" + encodePathSegment(owner) + "/" + encodePathSegment(repositoryName) + "/commits?per_page=" + COMMITS_PER_PAGE
        );

        if (!commits.isArray() || commits.isEmpty()) {
            return;
        }

        JsonNode latestCommit = commits.get(0);
        String message = textField(latestCommit.path("commit"), "message");
        profile.setLastCommit(message.isBlank() ? textField(latestCommit, "sha") : message);
    }

    private void loadAllCommitsToday(GithubProfile profile, String githubUsername) throws Exception {
        String path = (Config.GITHUB_TOKEN != null && !Config.GITHUB_TOKEN.isBlank())
                ? "/user/events?per_page=100"
                : "/users/" + encodePathSegment(githubUsername) + "/events?per_page=100";

        JsonNode events = fetchJson(path);

        int commitsToday = 0;
        var startOfDay = DateUtils.startOfTodayUtc();

        if (events.isArray()) {
            for (JsonNode event : events) {
                if (!"PushEvent".equals(textField(event, "type"))) {
                    continue;
                }

                String createdAt = textField(event, "created_at");
                if (!DateUtils.isAfter(createdAt, startOfDay)) {
                    continue;
                }

                JsonNode payload = event.path("payload");
                JsonNode commitList = payload.path("commits");
                if (commitList.isArray()) {
                    commitsToday += commitList.size();
                }
            }
        }

        profile.setCommitsToday(commitsToday);
    }

    private JsonNode fetchJson(String path) throws Exception {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/vnd.github+json");
        headers.put("User-Agent", "DevStats");
        headers.put("X-GitHub-Api-Version", "2022-11-28");

        if (Config.GITHUB_TOKEN != null && !Config.GITHUB_TOKEN.isBlank()) {
            headers.put("Authorization", "Bearer " + Config.GITHUB_TOKEN);
        }

        String body = HttpUtils.get(API + path, headers);
        return JsonUtils.parseTree(body);
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
