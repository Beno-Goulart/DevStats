package devstats.services;

import devstats.models.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseServiceTest {

    @TempDir
    Path tempDir;

    private DatabaseService db;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        db = new DatabaseService(url);
    }

    @Test
    void saveAndFindUser() {
        UserData user = new UserData("123", "testuser", "ghuser", "token", "refresh", 1000L, 2000L);
        db.saveUser(user);

        UserData found = db.findUser("123");
        assertNotNull(found);
        assertEquals("123", found.getDiscordId());
        assertEquals("testuser", found.getDiscordUsername());
        assertEquals("ghuser", found.getGithubUsername());
        assertEquals("token", found.getDiscordAccessToken());
        assertEquals("refresh", found.getRefreshToken());
        assertEquals(1000L, found.getTokenExpiresAt());
        assertEquals(2000L, found.getLastSync());
    }

    @Test
    void findUserReturnsNullForNonexistent() {
        assertNull(db.findUser("nonexistent"));
    }

    @Test
    void updateGithubUsername() {
        UserData user = new UserData("123", "user", null, null, null, null, null);
        db.saveUser(user);

        db.updateGithub("123", "newghuser");

        UserData found = db.findUser("123");
        assertEquals("newghuser", found.getGithubUsername());
    }

    @Test
    void updateToken() {
        UserData user = new UserData("123", "user", "gh", "old_token", "old_refresh", 100L, null);
        db.saveUser(user);

        db.updateToken("123", "new_token", "new_refresh", 3600);

        UserData found = db.findUser("123");
        assertEquals("new_token", found.getDiscordAccessToken());
        assertEquals("new_refresh", found.getRefreshToken());
        assertNotNull(found.getTokenExpiresAt());
        assertTrue(found.getTokenExpiresAt() > System.currentTimeMillis());
    }

    @Test
    void updateLastSync() {
        UserData user = new UserData("123", "user", null, null, null, null, null);
        db.saveUser(user);

        db.updateLastSync("123", 9999L);

        UserData found = db.findUser("123");
        assertEquals(9999L, found.getLastSync());
    }

    @Test
    void saveOAuthTokens() {
        db.saveOAuthTokens("456", "oauthuser", "access_token", "refresh_token", 3600);

        UserData found = db.findUser("456");
        assertNotNull(found);
        assertEquals("456", found.getDiscordId());
        assertEquals("oauthuser", found.getDiscordUsername());
        assertEquals("access_token", found.getDiscordAccessToken());
        assertEquals("refresh_token", found.getRefreshToken());
        assertNotNull(found.getTokenExpiresAt());
        assertNotNull(found.getLastSync());
    }

    @Test
    void findAllUsers() {
        db.saveOAuthTokens("u1", "user1", "t1", "r1", 3600);
        db.saveOAuthTokens("u2", "user2", "t2", "r2", 3600);

        var users = db.findAllUsers();
        assertEquals(2, users.size());
    }

    @Test
    void findAllUsersReturnsEmptyForNoUsers() {
        var users = db.findAllUsers();
        assertTrue(users.isEmpty());
    }

    @Test
    void upsertUpdatesOnConflict() {
        db.saveOAuthTokens("789", "user", "token1", "refresh1", 3600);
        db.saveOAuthTokens("789", "user_updated", "token2", "refresh2", 7200);

        UserData found = db.findUser("789");
        assertEquals("user_updated", found.getDiscordUsername());
        assertEquals("token2", found.getDiscordAccessToken());
    }

    @Test
    void findUserByGithubUsername() {
        db.saveOAuthTokens("999", "ghuser", "token", "refresh", 3600);
        db.updateGithub("999", "mygithub");

        UserData found = db.findUserByGithubUsername("mygithub");
        assertNotNull(found);
        assertEquals("999", found.getDiscordId());
    }

    @Test
    void findUserByGithubUsernameReturnsNullForNonexistent() {
        assertNull(db.findUserByGithubUsername("nobody"));
    }
}
