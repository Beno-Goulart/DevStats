package devstats.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import devstats.models.DiscordUser;
import devstats.services.DatabaseService;
import devstats.services.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OAuthCallbackHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuthCallbackHandler.class);

    private final OAuthService oAuthService;
    private final DatabaseService databaseService;

    public OAuthCallbackHandler(OAuthService oAuthService, DatabaseService databaseService) {
        this.oAuthService = oAuthService;
        this.databaseService = databaseService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            log.debug("Callback OAuth recebido");

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Método não permitido.");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI());
            String code = params.get("code");

            if (code == null || code.isBlank()) {
                log.warn("Authorization code não encontrado no callback");
                sendResponse(exchange, 400, "<h1>Erro: Código de autorização não encontrado.</h1>");
                return;
            }

            log.debug("Code recebido, trocando por token...");

            var tokenResponse = oAuthService.exchangeCodeForToken(code);

            DiscordUser user = oAuthService.getDiscordUser(tokenResponse.getAccessToken());

            log.debug("Salvando tokens para {} ({})", user.getId(), user.getUsername());

            databaseService.saveOAuthTokens(
                    user.getId(),
                    user.getUsername(),
                    tokenResponse.getAccessToken(),
                    tokenResponse.getRefreshToken(),
                    tokenResponse.getExpiresIn()
            );

            log.info("Fluxo OAuth concluído com sucesso para {} ({})", user.getId(), user.getUsername());

            String html = "<html><body style='font-family: sans-serif; text-align: center; margin-top: 80px;'>"
                    + "<h1>DevStats conectado com sucesso!</h1>"
                    + "<p>Olá, " + escapeHtml(user.getUsername()) + "!</p>"
                    + "<p>Você já pode fechar esta janela.</p>"
                    + "</body></html>";

            sendResponse(exchange, 200, html);

        } catch (Exception e) {
            log.error("Erro no callback OAuth: {}", e.getMessage(), e);
            String html = "<html><body style='font-family: sans-serif; text-align: center; margin-top: 80px;'>"
                    + "<h1>Erro ao conectar DevStats</h1>"
                    + "<p>" + escapeHtml(e.getMessage()) + "</p>"
                    + "<p>Tente novamente usando /widget setup no Discord.</p>"
                    + "</body></html>";
            sendResponse(exchange, 500, html);
        }
    }

    private Map<String, String> parseQueryParams(java.net.URI uri) {
        String query = uri.getQuery();
        if (query == null || query.isBlank()) {
            return Map.of();
        }
        return Stream.of(query.split("&"))
                .map(param -> param.split("=", 2))
                .collect(Collectors.toMap(
                        parts -> URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        parts -> parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "",
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

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
