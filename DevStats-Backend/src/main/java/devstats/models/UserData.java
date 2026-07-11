package devstats.models;

public class UserData {

    private String discordId;
    private String discordUsername;
    private String githubUsername;
    private String discordAccessToken;
    private String refreshToken;
    private Long lastSync;

    public UserData() {
    }

    public UserData(String discordId, String discordUsername, String githubUsername, String discordAccessToken, String refreshToken, Long lastSync) {
        this.discordId = discordId;
        this.discordUsername = discordUsername;
        this.githubUsername = githubUsername;
        this.discordAccessToken = discordAccessToken;
        this.refreshToken = refreshToken;
        this.lastSync = lastSync;
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public String getDiscordUsername() {
        return discordUsername;
    }

    public void setDiscordUsername(String discordUsername) {
        this.discordUsername = discordUsername;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    public String getDiscordAccessToken() {
        return discordAccessToken;
    }

    public void setDiscordAccessToken(String discordAccessToken) {
        this.discordAccessToken = discordAccessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getLastSync() {
        return lastSync;
    }

    public void setLastSync(Long lastSync) {
        this.lastSync = lastSync;
    }
}
