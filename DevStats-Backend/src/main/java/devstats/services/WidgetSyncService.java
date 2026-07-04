package devstats.services;

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

        GithubProfile profile = githubService.getProfile();

        discordWidgetService.syncProfile(profile);

    }

}
