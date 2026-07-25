package devstats.services;

import devstats.models.UserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoSyncService {

    private static final Logger log = LoggerFactory.getLogger(AutoSyncService.class);

    private final WidgetSyncService widgetSyncService;
    private final DatabaseService databaseService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "auto-sync");
        t.setDaemon(true);
        return t;
    });

    public AutoSyncService(WidgetSyncService widgetSyncService, DatabaseService databaseService) {
        this.widgetSyncService = widgetSyncService;
        this.databaseService = databaseService;
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::syncAll, 1, devstats.models.Config.REFRESH_MINUTES, TimeUnit.MINUTES);
        log.info("Auto-sync iniciado (intervalo: {} min)", devstats.models.Config.REFRESH_MINUTES);
    }

    public void stop() {
        scheduler.shutdownNow();
        log.info("Auto-sync parado");
    }

    private void syncAll() {
        try {
            var users = databaseService.findAllUsers();
            int synced = 0;
            int skipped = 0;

            for (UserData user : users) {
                if (user.getGithubUsername() == null || user.getGithubUsername().isBlank()) {
                    skipped++;
                    continue;
                }

                try {
                    String accessToken = ensureValidToken(user);
                    widgetSyncService.sync(user.getDiscordId(), accessToken, user.getGithubUsername());
                    databaseService.updateLastSync(user.getDiscordId(), System.currentTimeMillis());
                    synced++;
                } catch (Exception e) {
                    log.error("Erro ao sincronizar {}: {}", user.getDiscordId(), e.getMessage());
                }
            }

            log.info("Auto-sync concluído: {} sincronizados, {} ignorados", synced, skipped);
        } catch (Exception e) {
            log.error("Erro no auto-sync: {}", e.getMessage(), e);
        }
    }

    private String ensureValidToken(UserData user) throws Exception {
        if (!user.isTokenExpired()) {
            return user.getDiscordAccessToken();
        }

        if (user.getRefreshToken() != null && !user.getRefreshToken().isBlank()) {
            log.info("Token expirado para {}, renovando...", user.getDiscordId());
            OAuthService oAuthService = new OAuthService();
            var tokenResponse = oAuthService.refreshAccessToken(user.getRefreshToken());
            databaseService.updateToken(user.getDiscordId(), tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), tokenResponse.getExpiresIn());
            return tokenResponse.getAccessToken();
        }

        log.warn("Token expirado e sem refresh token para {}, ignorando", user.getDiscordId());
        return user.getDiscordAccessToken();
    }
}
