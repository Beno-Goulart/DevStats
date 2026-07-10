package devstats.services;

import devstats.models.Config;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OAuthService {

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

    public String exchangeCodeForToken(String code) {
        // TODO: Implementar a troca do authorization code pelo access token e refresh token.
        // Deve enviar uma requisição POST para 'https://discord.com/api/v9/oauth2/token'
        // com os seguintes parâmetros form-urlencoded:
        // - client_id
        // - client_secret (a ser lido do Config)
        // - grant_type: "authorization_code"
        // - code: code
        // - redirect_uri: "http://localhost:8080/callback"
        System.out.println("[OAuthService] TODO: Implementar troca do authorization code pelo access token e refresh token.");
        System.out.println("[OAuthService] Código recebido: " + code);
        return "mocked_access_token";
    }

    public String refreshAccessToken(String refreshToken) {
        // TODO: Implementar atualização do token de acesso expirado.
        // Deve enviar uma requisição POST para 'https://discord.com/api/v9/oauth2/token'
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
