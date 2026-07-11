package devstats.models;

public class GithubProfile {

    private String fullName;

    private String username;

    private String avatarUrl;

    private String bio;

    private String mainLanguage;

    private int activeRepos;

    private int commitsToday;

    private String lastCommit;

    private String lastRepository;

    public GithubProfile() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getMainLanguage() {
        return mainLanguage;
    }

    public void setMainLanguage(String mainLanguage) {
        this.mainLanguage = mainLanguage;
    }

    public int getActiveRepos() {
        return activeRepos;
    }

    public void setActiveRepos(int activeRepos) {
        this.activeRepos = activeRepos;
    }

    public int getCommitsToday() {
        return commitsToday;
    }

    public void setCommitsToday(int commitsToday) {
        this.commitsToday = commitsToday;
    }

    public String getLastCommit() {
        return lastCommit;
    }

    public void setLastCommit(String lastCommit) {
        this.lastCommit = lastCommit;
    }

    public String getLastRepository() {
        return lastRepository;
    }

    public void setLastRepository(String lastRepository) {
        this.lastRepository = lastRepository;
    }
}