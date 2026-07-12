package devstats.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDataTest {

    @Test
    void isTokenExpiredReturnsTrueWhenExpiresAtIsNull() {
        UserData user = new UserData();
        user.setTokenExpiresAt(null);
        assertTrue(user.isTokenExpired());
    }

    @Test
    void isTokenExpiredReturnsTrueWhenExpired() {
        UserData user = new UserData();
        user.setTokenExpiresAt(System.currentTimeMillis() - 1000);
        assertTrue(user.isTokenExpired());
    }

    @Test
    void isTokenExpiredReturnsFalseWhenNotExpired() {
        UserData user = new UserData();
        user.setTokenExpiresAt(System.currentTimeMillis() + 60000);
        assertFalse(user.isTokenExpired());
    }

    @Test
    void constructorSetsAllFields() {
        UserData user = new UserData("id1", "user1", "ghuser", "token1", "refresh1", 1000L, 2000L);
        assertEquals("id1", user.getDiscordId());
        assertEquals("user1", user.getDiscordUsername());
        assertEquals("ghuser", user.getGithubUsername());
        assertEquals("token1", user.getDiscordAccessToken());
        assertEquals("refresh1", user.getRefreshToken());
        assertEquals(1000L, user.getTokenExpiresAt());
        assertEquals(2000L, user.getLastSync());
    }

    @Test
    void defaultConstructorCreatesEmptyObject() {
        UserData user = new UserData();
        assertNull(user.getDiscordId());
        assertNull(user.getGithubUsername());
        assertNull(user.getDiscordAccessToken());
        assertNull(user.getRefreshToken());
        assertNull(user.getTokenExpiresAt());
        assertNull(user.getLastSync());
    }
}
