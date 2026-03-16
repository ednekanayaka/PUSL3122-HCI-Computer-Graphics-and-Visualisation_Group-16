package com.roomviz.data;

import com.roomviz.model.User;
import com.roomviz.security.PasswordUtil;

import java.io.File;
import java.sql.*;

public class UserRepository {

    private final String jdbcUrl;

    private UserRepository(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        init();
    }

    public static UserRepository createDefault() {
        File dir = new File(System.getProperty("user.home"), ".roomviz");
        if (!dir.exists()) dir.mkdirs();
        File db = new File(dir, "roomviz.db");
        return new UserRepository("jdbc:sqlite:" + db.getAbsolutePath());
    }

    private void init() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              full_name TEXT NOT NULL,
              email TEXT NOT NULL UNIQUE,
              password_hash TEXT NOT NULL
            );
        """;
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             Statement st = c.createStatement()) {
            st.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public User findByEmail(String emailLower) {
        String sql = "SELECT id, full_name, email, password_hash FROM users WHERE lower(email)=?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, emailLower);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password_hash")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public User createUser(String fullName, String email, char[] password) throws SQLException {
        String emailLower = email.trim().toLowerCase();
        String hash = PasswordUtil.hash(password);

        String sql = "INSERT INTO users(full_name, email, password_hash) VALUES(?,?,?)";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, fullName.trim());
            ps.setString(2, emailLower);
            ps.setString(3, hash);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                int id = keys.next() ? keys.getInt(1) : -1;
                return new User(id, fullName.trim(), emailLower, hash);
            }
        }
    }

    public boolean verifyLogin(String email, char[] password) {
        User u = findByEmail(email.trim().toLowerCase());
        if (u == null) return false;
        return PasswordUtil.verify(password, u.getPasswordHash());
    }
}