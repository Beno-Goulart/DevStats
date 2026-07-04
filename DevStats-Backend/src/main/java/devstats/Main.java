package devstats;


import devstats.commands.WidgetCommand;
import devstats.models.Config;
import devstats.models.GithubProfile;
import devstats.services.DiscordWidgetService;
import devstats.services.GithubService;
import net.dv8tion.jda.api.JDABuilder;

public class Main {

    public static void main(String[] args) throws Exception {

        JDABuilder.createDefault(Config.BOT_TOKEN)
                .addEventListeners(new WidgetCommand())
                .build();

        GithubService githubService = new GithubService();

        GithubProfile profile = githubService.getProfile();

        DiscordWidgetService discord = new DiscordWidgetService();

        discord.sync(profile);

        System.out.println("SYNC FINALIZADO");

        System.out.println(profile.getFullName());
        System.out.println(profile.getBio());
        System.out.println(profile.getMainLanguage());
        System.out.println(profile.getLastRepository());
        System.out.println(profile.getLastCommit());
        System.out.println(profile.getCommits());

    }



}