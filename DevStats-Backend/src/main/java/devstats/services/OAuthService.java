package devstats.services;

import devstats.models.Config;
import devstats.models.DiscordTokenResponse;
import devstats.models.DiscordUser;
import devstats.utils.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class OAuthService {

    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);

    private static final String TOKEN_URL = "https://discord.com/api/v10/oauth2/token";
    private static final String USER_URL = "https://discord.com/api/v10/users/@me";
    private static final String REDIRECT_URI = "http://localhost:8080/callback";

    public String generateAuthorizationUrl() {
        String clientId = Config.APPLICATION_ID;
        if (clientId == null || clientId.isBlank()) {
            clientId = "1509844130082062396";
        }

        String encodedRedirect = URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);
        String scope = URLEncoder.encode("identify role_connections.write", StandardCharsets.UTF_8);

        return "https://discord.com/oauth2/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + encodedRedirect +
                "&response_type=code" +
                "&scope=" + scope;
    }

    public DiscordTokenResponse exchangeCodeForToken(String code) throws Exception {
        log.info("Trocando code por access token...");

        Map<String, String> params = new LinkedHashMap<>();
        params.put("client_id", Config.APPLICATION_ID);
        params.put("client_secret", Config.CLIENT_SECRET);
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("redirect_uri", REDIRECT_URI);

        String formBody = HttpUtils.encodeFormBody(params);
        Map<String, String> headers = Map.of("Content-Type", "application/x-www-form-urlencoded");

        DiscordTokenResponse tokenResponse = HttpUtils.postJson(TOKEN_URL, formBody, headers, DiscordTokenResponse.class);

        log.info("Access Token obtido. Expira em: {}s", tokenResponse.getExpiresIn());

        return tokenResponse;
    }

    public DiscordUser getDiscordUser(String accessToken) throws Exception {
        log.debug("Obtendo identidade do usuário no Discord...");

        Map<String, String> headers = Map.of("Authorization", "Bearer " + accessToken);

        DiscordUser user = HttpUtils.getJson(USER_URL, headers, DiscordUser.class);

        log.debug("Usuário identificado: {} ({})", user.getId(), user.getUsername());

        return user;
    }

    public DiscordTokenResponse refreshAccessToken(String refreshToken) throws Exception {
        log.info("Renovando access token...");

        Map<String, String> params = new LinkedHashMap<>();
        params.put("client_id", Config.APPLICATION_ID);
        params.put("client_secret", Config.CLIENT_SECRET);
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);

        String formBody = HttpUtils.encodeFormBody(params);
        Map<String, String> headers = Map.of("Content-Type", "application/x-www-form-urlencoded");

        DiscordTokenResponse tokenResponse = HttpUtils.postJson(TOKEN_URL, formBody, headers, DiscordTokenResponse.class);

        log.info("Access token renovado com sucesso.");

        return tokenResponse;
    }
}
