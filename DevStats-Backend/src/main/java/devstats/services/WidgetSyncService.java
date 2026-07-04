package devstats.services;

import devstats.models.GithubProfile;

public class WidgetSyncService {

    private final GithubService githubService = new GithubService();

    private final DiscordWidgetService discordWidgetService = new DiscordWidgetService();

    public void sync() throws Exception {

        GithubProfile profile = githubService.getProfile();

        discordWidgetService.sync(profile);

    }

}
