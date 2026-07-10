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
                "github_username TEXT, " +
                "discord_access_token TEXT, " +
                "refresh_token TEXT, " +
                "last_sync INTEGER" +
                ");";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabela de usuários inicializada com sucesso!");
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar o banco de dados SQLite: " + e.getMessage());
        }
    }

    public void saveUser(UserData user) {
        String sql = "INSERT INTO users (discord_id, github_username, discord_access_token, refresh_token, last_sync) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(discord_id) DO UPDATE SET " +
                "github_username = excluded.github_username, " +
                "discord_access_token = excluded.discord_access_token, " +
                "refresh_token = excluded.refresh_token, " +
                "last_sync = excluded.last_sync;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getDiscordId());
            pstmt.setString(2, user.getGithubUsername());
            pstmt.setString(3, user.getDiscordAccessToken());
            pstmt.setString(4, user.getRefreshToken());
            if (user.getLastSync() != null) {
                pstmt.setLong(5, user.getLastSync());
            } else {
                pstmt.setNull(5, java.sql.Types.INTEGER);
            }
            pstmt.executeUpdate();
            System.out.println("Usuário salvo/atualizado: " + user.getDiscordId());
        } catch (SQLException e) {
            System.err.println("Erro ao salvar usuário: " + e.getMessage());
        }
    }

    public UserData findUser(String discordId) {
        String sql = "SELECT discord_id, github_username, discord_access_token, refresh_token, last_sync FROM users WHERE discord_id = ?;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, discordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UserData user = new UserData();
                    user.setDiscordId(rs.getString("discord_id"));
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
            System.err.println("Erro ao buscar usuário: " + e.getMessage());
        }
        return null;
    }

    public void saveOAuthTokens(String discordId, String accessToken, String refreshToken) {
        String sql = "INSERT INTO users (discord_id, discord_access_token, refresh_token, last_sync) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(discord_id) DO UPDATE SET " +
                "discord_access_token = excluded.discord_access_token, " +
                "refresh_token = excluded.refresh_token, " +
                "last_sync = excluded.last_sync;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, discordId);
            pstmt.setString(2, accessToken);
            pstmt.setString(3, refreshToken);
            pstmt.setLong(4, System.currentTimeMillis());
            pstmt.executeUpdate();
            System.out.println("Tokens OAuth salvos para o usuário: " + discordId);
        } catch (SQLException e) {
            System.err.println("Erro ao salvar tokens OAuth: " + e.getMessage());
        }
    }

    public void updateGithub(String discordId, String githubUsername) {
        String sql = "UPDATE users SET github_username = ? WHERE discord_id = ?;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, githubUsername);
            pstmt.setString(2, discordId);
            pstmt.executeUpdate();
            System.out.println("GitHub username atualizado para o usuário: " + discordId);
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar GitHub username: " + e.getMessage());
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
            System.out.println("Tokens atualizados para o usuário: " + discordId);
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar tokens: " + e.getMessage());
        }
    }

    public void updateLastSync(String discordId, long lastSync) {
        String sql = "UPDATE users SET last_sync = ? WHERE discord_id = ?;";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, lastSync);
            pstmt.setString(2, discordId);
            pstmt.executeUpdate();
            System.out.println("Última sincronização atualizada para o usuário: " + discordId);
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar última sincronização: " + e.getMessage());
        }
    }
}
