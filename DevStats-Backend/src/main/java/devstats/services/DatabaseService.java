package devstats.services;

import devstats.models.Config;
import devstats.models.UserData;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);

    private final String url;

    public DatabaseService() {
        this.url = resolveDatabaseUrl();
        log.info("Database scheme: {}", url.contains("://") ? url.substring(0, url.indexOf("://") + 3) : "sqlite");
        runMigrations();
    }

    public DatabaseService(String url) {
        this.url = url;
        runMigrations();
    }

    private static String resolveDatabaseUrl() {
        String dbUrl = Config.DATABASE_URL;
        if (dbUrl != null && !dbUrl.isBlank()) {
            if (dbUrl.startsWith("postgresql://")) {
                String withoutScheme = dbUrl.substring("postgresql://".length());
                String user = null;
                String password = null;
                String hostAndRest = withoutScheme;

                if (withoutScheme.contains("@")) {
                    String userInfo = withoutScheme.substring(0, withoutScheme.indexOf("@"));
                    hostAndRest = withoutScheme.substring(withoutScheme.indexOf("@") + 1);
                    if (userInfo.contains(":")) {
                        user = userInfo.substring(0, userInfo.indexOf(":"));
                        password = userInfo.substring(userInfo.indexOf(":") + 1);
                    } else {
                        user = userInfo;
                    }
                }

                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(hostAndRest);

                if (user != null) {
                    jdbcUrl.append("?user=").append(user);
                    if (password != null) {
                        jdbcUrl.append("&password=").append(password);
                    }
                }

                return jdbcUrl.toString();
            }
            return dbUrl;
        }
        return "jdbc:sqlite:devstats.db";
    }

    private Connection connect() throws SQLException {
        if (url.startsWith("jdbc:postgresql:")) {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("PostgreSQL driver not found", e);
            }
            Properties props = new Properties();
            props.setProperty("sslmode", "require");
            return DriverManager.getConnection(url, props);
        }
        return DriverManager.getConnection(url);
    }

    private void runMigrations() {
        try {
            Connection conn = connect();
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml",
                    new ClassLoaderResourceAccessor(),
                    database
            );
            liquibase.update(new Contexts());
            conn.close();
            log.info("Migrations Liquibase executadas com sucesso");
        } catch (Exception e) {
            log.error("Erro ao executar migrations: {}", e.getMessage(), e);
        }
    }

    public void saveUser(UserData user) {
        String sql = "INSERT INTO users (discord_id, discord_username, github_username, discord_access_token, refresh_token, token_expires_at, last_sync) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(discord_id) DO UPDATE SET " +
                "discord_username = excluded.discord_username, " +
                "github_username = excluded.github_username, " +
                "discord_access_token = excluded.discord_access_token, " +
                "refresh_token = excluded.refresh_token, " +
                "token_expires_at = excluded.token_expires_at, " +
                "last_sync = excluded.last_sync;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getDiscordId());
            pstmt.setString(2, user.getDiscordUsername());
            pstmt.setString(3, user.getGithubUsername());
            pstmt.setString(4, user.getDiscordAccessToken());
            pstmt.setString(5, user.getRefreshToken());
            setNullableLong(pstmt, 6, user.getTokenExpiresAt());
            setNullableLong(pstmt, 7, user.getLastSync());
            pstmt.executeUpdate();
            log.debug("Usuário salvo/atualizado: {}", user.getDiscordId());
        } catch (SQLException e) {
            log.error("Erro ao salvar usuário {}: {}", user.getDiscordId(), e.getMessage());
        }
    }

    public UserData findUser(String discordId) {
        String sql = "SELECT discord_id, discord_username, github_username, discord_access_token, refresh_token, token_expires_at, last_sync FROM users WHERE discord_id = ?;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, discordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar usuário {}: {}", discordId, e.getMessage());
        }
        return null;
    }

    public UserData findUserByGithubUsername(String githubUsername) {
        String sql = "SELECT discord_id, discord_username, github_username, discord_access_token, refresh_token, token_expires_at, last_sync FROM users WHERE github_username = ?;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, githubUsername);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar usuário por GitHub {}: {}", githubUsername, e.getMessage());
        }
        return null;
    }

    public List<UserData> findAllUsers() {
        String sql = "SELECT discord_id, discord_username, github_username, discord_access_token, refresh_token, token_expires_at, last_sync FROM users;";
        List<UserData> users = new ArrayList<>();
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao listar usuários: {}", e.getMessage());
        }
        return users;
    }

    public void saveOAuthTokens(String discordId, String discordUsername, String accessToken, String refreshToken, long expiresInSeconds) {
        long expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000);
        String sql = "INSERT INTO users (discord_id, discord_username, discord_access_token, refresh_token, token_expires_at, last_sync) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(discord_id) DO UPDATE SET " +
                "discord_username = excluded.discord_username, " +
                "discord_access_token = excluded.discord_access_token, " +
                "refresh_token = excluded.refresh_token, " +
                "token_expires_at = excluded.token_expires_at, " +
                "last_sync = excluded.last_sync;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, discordId);
            pstmt.setString(2, discordUsername);
            pstmt.setString(3, accessToken);
            pstmt.setString(4, refreshToken);
            pstmt.setLong(5, expiresAt);
            pstmt.setLong(6, System.currentTimeMillis());
            pstmt.executeUpdate();
            log.info("Tokens OAuth salvos para: {} ({})", discordId, discordUsername);
        } catch (SQLException e) {
            log.error("Erro ao salvar tokens OAuth para {}: {}", discordId, e.getMessage());
        }
    }

    public void updateGithub(String discordId, String githubUsername) {
        String sql = "UPDATE users SET github_username = ? WHERE discord_id = ?;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, githubUsername);
            pstmt.setString(2, discordId);
            pstmt.executeUpdate();
            log.debug("GitHub username atualizado para {}: {}", discordId, githubUsername);
        } catch (SQLException e) {
            log.error("Erro ao atualizar GitHub username para {}: {}", discordId, e.getMessage());
        }
    }

    public void updateToken(String discordId, String accessToken, String refreshToken, long expiresInSeconds) {
        long expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000);
        String sql = "UPDATE users SET discord_access_token = ?, refresh_token = ?, token_expires_at = ? WHERE discord_id = ?;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accessToken);
            pstmt.setString(2, refreshToken);
            pstmt.setLong(3, expiresAt);
            pstmt.setString(4, discordId);
            pstmt.executeUpdate();
            log.debug("Tokens atualizados para: {}", discordId);
        } catch (SQLException e) {
            log.error("Erro ao atualizar tokens para {}: {}", discordId, e.getMessage());
        }
    }

    public void updateLastSync(String discordId, long lastSync) {
        String sql = "UPDATE users SET last_sync = ? WHERE discord_id = ?;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, lastSync);
            pstmt.setString(2, discordId);
            pstmt.executeUpdate();
            log.debug("Última sincronização atualizada para: {}", discordId);
        } catch (SQLException e) {
            log.error("Erro ao atualizar última sincronização para {}: {}", discordId, e.getMessage());
        }
    }

    public void close() {
        try {
            var conn = connect();
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            log.error("Erro ao fechar conexão: {}", e.getMessage());
        }
    }

    private UserData mapUser(ResultSet rs) throws SQLException {
        UserData user = new UserData();
        user.setDiscordId(rs.getString("discord_id"));
        user.setDiscordUsername(rs.getString("discord_username"));
        user.setGithubUsername(rs.getString("github_username"));
        user.setDiscordAccessToken(rs.getString("discord_access_token"));
        user.setRefreshToken(rs.getString("refresh_token"));
        long tokenExpiresAt = rs.getLong("token_expires_at");
        if (!rs.wasNull()) user.setTokenExpiresAt(tokenExpiresAt);
        long lastSync = rs.getLong("last_sync");
        if (!rs.wasNull()) user.setLastSync(lastSync);
        return user;
    }

    private void setNullableLong(PreparedStatement pstmt, int index, Long value) throws SQLException {
        if (value != null) {
            pstmt.setLong(index, value);
        } else {
            pstmt.setNull(index, java.sql.Types.INTEGER);
        }
    }
}
