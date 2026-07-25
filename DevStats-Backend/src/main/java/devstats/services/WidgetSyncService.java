package devstats.services;

import devstats.models.Config;
import devstats.models.GithubProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class WidgetSyncService {

    private static final Logger log = LoggerFactory.getLogger(WidgetSyncService.class);

    private final DiscordWidgetService discordWidgetService;
    private final GithubService githubService;

    public WidgetSyncService() {
        this(new DiscordWidgetService(), new GithubService());
    }

    public WidgetSyncService(DiscordWidgetService discordWidgetService, GithubService githubService) {
        this.discordWidgetService = discordWidgetService;
        this.githubService = githubService;
    }

    public void sync() throws Exception {
        sync(Config.USER_ID, Config.ACCESS_TOKEN, Config.GITHUB_USERNAME);
    }

    public void sync(String discordId, String discordAccessToken, String githubUsername) throws Exception {
        log.debug("Sincronizando widget para Discord={}, GitHub={}", discordId, githubUsername);

        Optional<GithubProfile> profileOpt = githubService.getProfile(githubUsername);
        if (profileOpt.isEmpty()) {
            log.warn("Perfil GitHub não encontrado para {}, sincronizando vazio", githubUsername);
        }

        GithubProfile profile = profileOpt.orElse(null);
        discordWidgetService.sync(discordId, discordAccessToken, profile);
    }
}
