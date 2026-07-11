package devstats.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import devstats.models.Config;
import devstats.models.DiscordTokenResponse;
import devstats.models.DiscordUser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class OAuthService {

    private static final String TOKEN_URL = "https://discord.com/api/v10/oauth2/token";
    private static final String USER_URL = "https://discord.com/api/v10/users/@me";
    private static final String REDIRECT_URI = "http://localhost:8080/callback";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

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

        System.out.println("[OAuth] Trocando code por access token...");

        String form = Map.of(
                "client_id", Config.APPLICATION_ID,
                "client_secret", Config.CLIENT_SECRET,
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", REDIRECT_URI
        ).entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Falha na troca do authorization code. HTTP " + response.statusCode() + ": " + response.body());
        }

        DiscordTokenResponse tokenResponse = mapper.readValue(response.body(), DiscordTokenResponse.class);

        System.out.println("[OAuth] Access Token obtido. Expira em: " + tokenResponse.getExpiresIn() + "s");

        return tokenResponse;
    }

    public DiscordUser getDiscordUser(String accessToken) throws Exception {

        System.out.println("[OAuth] Obtendo identidade do usuário no Discord...");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Falha ao obter identidade do usuário Discord. HTTP " + response.statusCode() + ": " + response.body());
        }

        DiscordUser user = mapper.readValue(response.body(), DiscordUser.class);

        System.out.println("[OAuth] Usuário identificado: " + user.getId() + " (" + user.getUsername() + ")");

        return user;
    }

    public DiscordTokenResponse refreshAccessToken(String refreshToken) throws Exception {

        System.out.println("[OAuth] Renovando access token...");

        String form = Map.of(
                "client_id", Config.APPLICATION_ID,
                "client_secret", Config.CLIENT_SECRET,
                "grant_type", "refresh_token",
                "refresh_token", refreshToken
        ).entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Falha ao renovar access token. HTTP " + response.statusCode() + ": " + response.body());
        }

        DiscordTokenResponse tokenResponse = mapper.readValue(response.body(), DiscordTokenResponse.class);

        System.out.println("[OAuth] Access token renovado com sucesso.");

        return tokenResponse;
    }
}
