package devstats.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import devstats.models.Config;
import devstats.models.DiscordTokenResponse;
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

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String generateAuthorizationUrl() {
        String clientId = Config.APPLICATION_ID;
        if (clientId == null || clientId.isBlank()) {
            clientId = "1509844130082062396";
        }

        String redirectUri = "http://localhost:8080/callback";
        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String scope = URLEncoder.encode("identify role_connections.write", StandardCharsets.UTF_8);

        return "https://discord.com/oauth2/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + encodedRedirect +
                "&response_type=code" +
                "&scope=" + scope;
    }

    public DiscordTokenResponse exchangeCodeForToken(String code) throws Exception {
        String redirectUri = "http://localhost:8080/callback";

        String form = Map.of(
                "client_id", Config.APPLICATION_ID,
                "client_secret", Config.CLIENT_SECRET,
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", redirectUri
        ).entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[OAuthService] Token exchange response status: " + response.statusCode());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Falha na troca do authorization code. HTTP " + response.statusCode() + ": " + response.body());
        }

        DiscordTokenResponse tokenResponse = mapper.readValue(response.body(), DiscordTokenResponse.class);
        System.out.println("[OAuthService] Access token obtido. Expira em: " + tokenResponse.getExpiresIn() + "s");
        System.out.println("[OAuthService] Scope: " + tokenResponse.getScope());

        return tokenResponse;
    }

    public String getDiscordUser(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Falha ao obter identidade do usuário Discord. HTTP " + response.statusCode() + ": " + response.body());
        }

        String discordId = mapper.readTree(response.body()).get("id").asText();
        System.out.println("[OAuthService] Usuário Discord identificado: " + discordId);

        return discordId;
    }

    public String refreshAccessToken(String refreshToken) {
        // TODO: Implementar atualização do token de acesso expirado.
        // Deve enviar uma requisição POST para 'https://discord.com/api/v10/oauth2/token'
        // com os seguintes parâmetros form-urlencoded:
        // - client_id
        // - client_secret (a ser lido do Config)
        // - grant_type: "refresh_token"
        // - refresh_token: refreshToken
        System.out.println("[OAuthService] TODO: Implementar atualização de access token via refresh token.");
        System.out.println("[OAuthService] Refresh Token utilizado: " + refreshToken);
        return "mocked_refreshed_access_token";
    }
}
