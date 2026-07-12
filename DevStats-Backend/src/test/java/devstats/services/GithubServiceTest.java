package devstats.services;

import devstats.models.GithubProfile;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GithubServiceTest {

    private final GithubService githubService = new GithubService();

    @Test
    void getProfileReturnsEmptyForBlankUsername() {
        Optional<GithubProfile> result = githubService.getProfile("");
        assertTrue(result.isEmpty());
    }

    @Test
    void getProfileReturnsEmptyForNullUsername() {
        Optional<GithubProfile> result = githubService.getProfile(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void getProfileReturnsEmptyForNonexistentUser() {
        Optional<GithubProfile> result = githubService.getProfile("this-user-definitely-does-not-exist-12345678");
        assertTrue(result.isEmpty());
    }
}
