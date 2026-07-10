package devstats.services;

import devstats.models.Config;
import devstats.models.GithubProfile;

public class WidgetSyncService {

    private final GithubService githubService;
    private final DiscordWidgetService discordWidgetService;

    public WidgetSyncService() {
        this(new GithubService(), new DiscordWidgetService());
    }

    public WidgetSyncService(GithubService githubService, DiscordWidgetService discordWidgetService) {
        this.githubService = githubService;
        this.discordWidgetService = discordWidgetService;
    }

    public void sync() throws Exception {
        sync(Config.GITHUB_USERNAME, Config.USER_ID, Config.ACCESS_TOKEN);
    }

    public void sync(String githubUsername, String discordId, String discordAccessToken) throws Exception {
        java.util.Optional<GithubProfile> profileOpt = githubService.getProfile(githubUsername);
        if (profileOpt.isPresent()) {
            discordWidgetService.sync(discordId, discordAccessToken, profileOpt.get());
        } else {
            throw new Exception("Não foi possível obter o perfil do GitHub para o usuário: " + githubUsername);
        }
    }
}
