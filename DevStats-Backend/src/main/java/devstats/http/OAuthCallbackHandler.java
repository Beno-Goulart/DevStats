package devstats.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import devstats.models.DiscordTokenResponse;
import devstats.services.DatabaseService;
import devstats.services.OAuthService;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OAuthCallbackHandler implements HttpHandler {

    private final OAuthService oAuthService;
    private final DatabaseService databaseService;

    public OAuthCallbackHandler(OAuthService oAuthService, DatabaseService databaseService) {
        this.oAuthService = oAuthService;
        this.databaseService = databaseService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            System.out.println("[OAuth] Callback recebido.");
            System.out.println("[OAuth] Método: " + exchange.getRequestMethod());
            System.out.println("[OAuth] URI: " + exchange.getRequestURI());

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Método não permitido.");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI());
            String code = params.get("code");

            if (code == null || code.isBlank()) {
                System.out.println("[OAuth] Erro: Authorization code não encontrado na query string.");
                sendResponse(exchange, 400, "<h1>Erro: Código de autorização não encontrado.</h1>");
                return;
            }

            System.out.println("[OAuth] Authorization code recebido: " + code);
            System.out.println("[OAuth] Trocando code por access_token...");

            DiscordTokenResponse tokenResponse = oAuthService.exchangeCodeForToken(code);

            System.out.println("[OAuth] Access token obtido com sucesso.");
            System.out.println("[OAuth] Obtendo identidade do usuário no Discord...");

            String discordId = oAuthService.getDiscordUser(tokenResponse.getAccessToken());

            System.out.println("[OAuth] Usuário Discord identificado: " + discordId);
            System.out.println("[OAuth] Salvando tokens no banco de dados...");

            databaseService.saveOAuthTokens(discordId, tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());

            System.out.println("[OAuth] Tokens salvos para o usuário: " + discordId);
            System.out.println("[OAuth] OAuth concluído com sucesso.");

            String html = "<html><body style='font-family: sans-serif; text-align: center; margin-top: 80px;'>"
                    + "<h1>DevStats conectado com sucesso!</h1>"
                    + "<p>Você já pode fechar esta janela.</p>"
                    + "</body></html>";

            sendResponse(exchange, 200, html);

        } catch (Exception e) {
            System.err.println("[OAuth] Erro no callback: " + e.getMessage());
            e.printStackTrace();
            String html = "<html><body style='font-family: sans-serif; text-align: center; margin-top: 80px;'>"
                    + "<h1>Erro ao conectar DevStats</h1>"
                    + "<p>" + e.getMessage() + "</p>"
                    + "<p>Tente novamente usando /widget setup no Discord.</p>"
                    + "</body></html>";
            sendResponse(exchange, 500, html);
        }
    }

    private Map<String, String> parseQueryParams(URI uri) {
        String query = uri.getQuery();
        if (query == null || query.isBlank()) {
            return Map.of();
        }
        return Stream.of(query.split("&"))
                .map(param -> param.split("=", 2))
                .collect(Collectors.toMap(
                        parts -> parts[0],
                        parts -> parts.length > 1 ? parts[1] : "",
                        (a, b) -> b
                ));
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
