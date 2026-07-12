# Plan: Implement Complete Discord OAuth2 Flow

## Context

DevStats already has a partial OAuth2 implementation. The core pieces exist but have gaps:
- `OAuthService.exchangeCodeForToken()` works (real HTTP POST)
- `OAuthService.getDiscordUser()` works but only returns the user ID
- `OAuthService.refreshAccessToken()` is **mocked** (returns hardcoded string)
- `OAuthCallbackHandler` handles the callback but has a URL-decode bug
- `DatabaseService` has all needed methods
- `UserData` has all needed fields (except `discord_username`)
- `Config.ACCESS_TOKEN` is declared but **never assigned** in the static block

The goal is to fix these gaps and add proper logging, without changing architecture.

---

## Changes by File

### 1. `Config.java` — Fix missing ACCESS_TOKEN assignment

**Path:** `DevStats-Backend/src/main/java/devstats/models/Config.java`

Add the missing `resolve()` call for `ACCESS_TOKEN` in the static block:

```java
ACCESS_TOKEN = resolve("discord.access.token", properties, envFile);
```

Also add `ACCESS_TOKEN` to `application.properties`:
```
discord.access.token=${DISCORD_ACCESS_TOKEN}
```

---

### 2. `OAuthService.java` — Fix getDiscordUser, implement refreshAccessToken

**Path:** `DevStats-Backend/src/main/java/devstats/services/OAuthService.java`

**2a. Create a `DiscordUser` model** (new file: `models/DiscordUser.java`):
```java
public class DiscordUser {
    private String id;
    private String username;
    private String globalName;
    // getters/setters
}
```

**2b. Update `getDiscordUser(String accessToken)`:**
- Return `DiscordUser` instead of `String`
- Extract `id`, `username`, `global_name` from the JSON response

**2c. Implement `refreshAccessToken(String refreshToken)` for real:**
- POST to `https://discord.com/api/v10/oauth2/token`
- Send: `client_id`, `client_secret`, `grant_type=refresh_token`, `refresh_token`
- Return deserialized `DiscordTokenResponse`
- Remove the mock

**2d. Add constant for redirect URI** to avoid duplication:
```java
private static final String REDIRECT_URI = "http://localhost:8080/callback";
```

---

### 3. `OAuthCallbackHandler.java` — Fix URL decoding, add logging

**Path:** `DevStats-Backend/src/main/java/devstats/http/OAuthCallbackHandler.java`

- Fix `parseQueryParams` to URL-decode values (`URLDecoder.decode(value, StandardCharsets.UTF_8)`)
- Add `System.out.println` logs at each step:
  - "Code recebido"
  - "Access Token obtido"
  - "Usuário identificado: {id} ({username})"
  - "Tokens salvos no banco"
  - "Fluxo OAuth concluído"

---

### 4. `UserData.java` — Add `discordUsername` field

**Path:** `DevStats-Backend/src/main/java/devstats/models/UserData.java`

Add field:
```java
private String discordUsername;
```
With getter/setter.

---

### 5. `DatabaseService.java` — Add `discord_username` column, update save logic

**Path:** `DevStats-Backend/src/main/java/devstats/services/DatabaseService.java`

- Add `discord_username TEXT` column to CREATE TABLE (with IF NOT EXISTS handling for existing DBs)
- Update `saveOAuthTokens()` to also save `discord_username`
- Update `findUser()` to read `discord_username`
- Update `saveUser()` to include `discord_username`

**Note:** Since the DB already exists, use `ALTER TABLE users ADD COLUMN discord_username TEXT` with try-catch for the "column already exists" case.

---

### 6. `OAuthCallbackHandler.java` — Pass discordUsername to database save

Update the callback to also save the `discord_username` from the `DiscordUser` response.

---

### 7. Logging — Add consistent logs across the OAuth flow

Add `System.out.println` logs (matching the project's existing style) at key points:

| Location | Log Message |
|---|---|
| `OAuthCallbackHandler` start | `[OAuth] Code recebido` |
| After token exchange | `[OAuth] Access Token obtido` |
| After user lookup | `[OAuth] Usuário identificado: {id} ({username})` |
| After DB save | `[OAuth] Tokens salvos no banco` |
| Callback complete | `[OAuth] Fluxo concluído com sucesso` |
| `OAuthService.refreshAccessToken` | `[OAuth] Renovando access token...` |
| After refresh | `[OAuth] Access token renovado` |
| Errors | `[OAuth] Erro: {message}` |

---

## Files Modified (summary)

| File | Action |
|---|---|
| `models/Config.java` | Add ACCESS_TOKEN resolve call |
| `resources/application.properties` | Add discord.access.token property |
| `services/OAuthService.java` | Fix getDiscordUser, implement refreshAccessToken, add REDIRECT_URI constant |
| `models/DiscordUser.java` | **NEW** — model for Discord user info |
| `http/OAuthCallbackHandler.java` | Fix URL decoding, add logging, pass discordUsername |
| `models/UserData.java` | Add discordUsername field |
| `services/DatabaseService.java` | Add discord_username column, update queries |

---

## Verification

1. Run `mvn clean compile` — must pass with zero errors
2. Manual test flow:
   - Run the bot
   - Execute `/widget setup` in Discord
   - Click "Autorizar Discord" button
   - Browser redirects to localhost:8080/callback
   - Console shows logs: Code received → Token obtained → User identified → Tokens saved
   - Browser shows "DevStats conectado com sucesso!"
   - Check SQLite: user record has discord_id, discord_username, access_token, refresh_token
