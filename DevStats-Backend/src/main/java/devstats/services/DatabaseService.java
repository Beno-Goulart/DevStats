package devstats.services;

import devstats.models.UserData;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseService {

    private final String url;

    public DatabaseService() {
        this("jdbc:sqlite:devstats.db");
    }

    public DatabaseService(String url) {
        this.url = url;
        initializeDatabase();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "discord_id TEXT PRIMARY KEY, " +
                "discord_username TEXT, " +
                "github_username TEXT, " +
                "discord_access_token TEXT, " +
                "refresh_token TEXT, " +
                "last_sync INTEGER" +
                ");";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("[DB] Tabela de usuários inicializada com sucesso!");
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao inicializar o banco de dados: " + e.getMessage());
        }

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE users ADD COLUMN discord_username TEXT");
            System.out.println("[DB] Coluna discord_username adicionada.");
        } catch (SQLException e) {
            // Coluna já existe, ignorar
        }
    }

    public void saveUser(UserData user) {
        String sql = "INSERT INTO users (discord_id, discord_username, github_username, discord_access_token, refresh_token, last_sync) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(discord_id) DO UPDATE SET " +
                "discord_username = excluded.discord_username, " +
                "github_username = excluded.github_username, " +
                "discord_access_token = excluded.discord_access_token, " +
                "refresh_token = excluded.refresh_token, " +
                "last_sync = excluded.last_sync;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getDiscordId());
            pstmt.setString(2, user.getDiscordUsername());
            pstmt.setString(3, user.getGithubUsername());
            pstmt.setString(4, user.getDiscordAccessToken());
            pstmt.setString(5, user.getRefreshToken());
            if (user.getLastSync() != null) {
                pstmt.setLong(6, user.getLastSync());
            } else {
                pstmt.setNull(6, java.sql.Types.INTEGER);
            }
            pstmt.executeUpdate();
            System.out.println("[DB] Usuário salvo/atualizado: " + user.getDiscordId());
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao salvar usuário: " + e.getMessage());
        }
    }

    public UserData findUser(String discordId) {
        String sql = "SELECT discord_id, discord_username, github_username, discord_access_token, refresh_token, last_sync FROM users WHERE discord_id = ?;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, discordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UserData user = new UserData();
                    user.setDiscordId(rs.getString("discord_id"));
                    user.setDiscordUsername(rs.getString("discord_username"));
                    user.setGithubUsername(rs.getString("github_username"));
                    user.setDiscordAccessToken(rs.getString("discord_access_token"));
                    user.setRefreshToken(rs.getString("refresh_token"));
                    long lastSync = rs.getLong("last_sync");
                    if (!rs.wasNull()) {
                        user.setLastSync(lastSync);
                    }
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao buscar usuário: " + e.getMessage());
        }
        return null;
    }

    public void saveOAuthTokens(String discordId, String discordUsername, String accessToken, String refreshToken) {
        String sql = "INSERT INTO users (discord_id, discord_username, discord_access_token, refresh_token, last_sync) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(discord_id) DO UPDATE SET " +
                "discord_username = excluded.discord_username, " +
                "discord_access_token = excluded.discord_access_token, " +
                "refresh_token = excluded.refresh_token, " +
                "last_sync = excluded.last_sync;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, discordId);
            pstmt.setString(2, discordUsername);
            pstmt.setString(3, accessToken);
            pstmt.setString(4, refreshToken);
            pstmt.setLong(5, System.currentTimeMillis());
            pstmt.executeUpdate();
            System.out.println("[DB] Tokens OAuth salvos para: " + discordId + " (" + discordUsername + ")");
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao salvar tokens OAuth: " + e.getMessage());
        }
    }

    public void updateGithub(String discordId, String githubUsername) {
        String sql = "UPDATE users SET github_username = ? WHERE discord_id = ?;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, githubUsername);
            pstmt.setString(2, discordId);
            pstmt.executeUpdate();
            System.out.println("[DB] GitHub username atualizado para: " + discordId);
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao atualizar GitHub username: " + e.getMessage());
        }
    }

    public void updateToken(String discordId, String accessToken, String refreshToken) {
        String sql = "UPDATE users SET discord_access_token = ?, refresh_token = ? WHERE discord_id = ?;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accessToken);
            pstmt.setString(2, refreshToken);
            pstmt.setString(3, discordId);
            pstmt.executeUpdate();
            System.out.println("[DB] Tokens atualizados para: " + discordId);
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao atualizar tokens: " + e.getMessage());
        }
    }

    public void updateLastSync(String discordId, long lastSync) {
        String sql = "UPDATE users SET last_sync = ? WHERE discord_id = ?;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, lastSync);
            pstmt.setString(2, discordId);
            pstmt.executeUpdate();
            System.out.println("[DB] Última sincronização atualizada para: " + discordId);
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao atualizar última sincronização: " + e.getMessage());
        }
    }
}
