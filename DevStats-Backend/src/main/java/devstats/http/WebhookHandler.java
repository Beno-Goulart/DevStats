package devstats.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import devstats.models.UserData;
import devstats.services.DatabaseService;
import devstats.services.OAuthService;
import devstats.services.WidgetSyncService;
import devstats.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WebhookHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(WebhookHandler.class);
    private static final String GITHUB_EVENT_HEADER = "X-GitHub-Event";
    private static final String GITHUB_SIGNATURE_HEADER = "X-Hub-Signature-256";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final WidgetSyncService widgetSyncService;
    private final DatabaseService databaseService;

    public WebhookHandler(WidgetSyncService widgetSyncService, DatabaseService databaseService) {
        this.widgetSyncService = widgetSyncService;
        this.databaseService = databaseService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String event = exchange.getRequestHeaders().getFirst(GITHUB_EVENT_HEADER);
            if (event == null) {
                sendResponse(exchange, 400, "Missing X-GitHub-Event header");
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            String secret = devstats.models.Config.GITHUB_WEBHOOK_SECRET;
            if (secret != null && !secret.isBlank()) {
                String signature = exchange.getRequestHeaders().getFirst(GITHUB_SIGNATURE_HEADER);
                if (!verifySignature(body, signature, secret)) {
                    log.warn("Webhook signature inválida");
                    sendResponse(exchange, 401, "Invalid signature");
                    return;
                }
            }

            log.debug("Webhook GitHub recebido: event={}", event);

            if ("push".equals(event)) {
                handlePush(body);
                sendResponse(exchange, 200, "OK");
            } else if ("ping".equals(event)) {
                log.info("GitHub webhook ping recebido");
                sendResponse(exchange, 200, "pong");
            } else {
                log.debug("Evento ignorado: {}", event);
                sendResponse(exchange, 200, "Ignored");
            }

        } catch (Exception e) {
            log.error("Erro ao processar webhook: {}", e.getMessage(), e);
            sendResponse(exchange, 500, "Internal Server Error");
        }
    }

    private void handlePush(String body) {
        try {
            JsonNode payload = JsonUtils.parseTree(body);
            JsonNode repository = payload.path("repository");
            String repoName = repository.path("name").asText("");
            JsonNode sender = payload.path("sender");
            String senderLogin = sender.path("login").asText("");

            log.info("Push recebido no repo={} por user={}", repoName, senderLogin);

            UserData user = databaseService.findUserByGithubUsername(senderLogin);
            if (user == null) {
                log.debug("Nenhum usuário DevStats encontrado para GitHub user: {}", senderLogin);
                return;
            }

            if (user.getGithubUsername() == null || user.getGithubUsername().isBlank()) {
                return;
            }

            String accessToken = user.getDiscordAccessToken();
            if (user.isTokenExpired() && user.getRefreshToken() != null) {
                OAuthService oAuthService = new OAuthService();
                var tokenResponse = oAuthService.refreshAccessToken(user.getRefreshToken());
                databaseService.updateToken(user.getDiscordId(), tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), tokenResponse.getExpiresIn());
                accessToken = tokenResponse.getAccessToken();
            }

            widgetSyncService.sync(user.getGithubUsername(), user.getDiscordId(), accessToken);
            databaseService.updateLastSync(user.getDiscordId(), System.currentTimeMillis());

            log.info("Widget sincronizado via webhook para {}", user.getDiscordId());

        } catch (Exception e) {
            log.error("Erro ao processar push webhook: {}", e.getMessage(), e);
        }
    }

    private boolean verifySignature(String body, String signature, String secret) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = "sha256=" + bytesToHex(hash);
            return MessageDigest.isEqual(expectedSignature.getBytes(), signature.getBytes());
        } catch (Exception e) {
            log.error("Erro ao verificar assinatura: {}", e.getMessage());
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
