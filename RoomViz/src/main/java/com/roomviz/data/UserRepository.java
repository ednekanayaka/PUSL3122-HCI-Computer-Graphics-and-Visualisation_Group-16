package com.roomviz.data;

import com.roomviz.model.User;
import com.roomviz.security.PasswordUtil;

import java.io.File;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

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
              password_hash TEXT NOT NULL,
              job_title TEXT DEFAULT '',
              department TEXT DEFAULT ''
            );
        """;
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             Statement st = c.createStatement()) {

            st.execute(sql);

            // ✅ Migration for existing DBs (add missing columns)
            ensureColumnExists(c, "users", "job_title", "TEXT", "''");
            ensureColumnExists(c, "users", "department", "TEXT", "''");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ensureColumnExists(Connection c, String table, String col, String type, String defVal) {
        try {
            Set<String> cols = new HashSet<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
                while (rs.next()) cols.add(rs.getString("name"));
            }
            if (!cols.contains(col)) {
                try (Statement st = c.createStatement()) {
                    st.execute("ALTER TABLE " + table + " ADD COLUMN " + col + " " + type + " DEFAULT " + defVal);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public User findByEmail(String emailLower) {
        String sql = "SELECT id, full_name, email, password_hash, job_title, department FROM users WHERE lower(email)=?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, emailLower);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        safe(rs.getString("job_title")),
                        safe(rs.getString("department"))
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public User findById(int id) {
        String sql = "SELECT id, full_name, email, password_hash, job_title, department FROM users WHERE id=?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        safe(rs.getString("job_title")),
                        safe(rs.getString("department"))
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

        String sql = "INSERT INTO users(full_name, email, password_hash, job_title, department) VALUES(?,?,?,?,?)";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, safe(fullName));
            ps.setString(2, emailLower);
            ps.setString(3, hash);
            ps.setString(4, ""); // job_title initially blank
            ps.setString(5, ""); // department initially blank
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                int id = keys.next() ? keys.getInt(1) : -1;
                return new User(id, safe(fullName), emailLower, hash, "", "");
            }
        }
    }

    public boolean verifyLogin(String email, char[] password) {
        User u = findByEmail(email.trim().toLowerCase());
        if (u == null) return false;
        return PasswordUtil.verify(password, u.getPasswordHash());
    }

    public void updateProfile(int userId, String fullName, String jobTitle, String department) throws SQLException {
        String sql = "UPDATE users SET full_name=?, job_title=?, department=? WHERE id=?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, safe(fullName));
            ps.setString(2, safe(jobTitle));
            ps.setString(3, safe(department));
            ps.setInt(4, userId);
            ps.executeUpdate();
        }
    }

    public boolean verifyPasswordById(int userId, char[] password) {
        String sql = "SELECT password_hash FROM users WHERE id=?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String hash = rs.getString("password_hash");
                return PasswordUtil.verify(password, hash);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void updatePasswordById(int userId, char[] newPassword) throws SQLException {
        String newHash = PasswordUtil.hash(newPassword);
        String sql = "UPDATE users SET password_hash=? WHERE id=?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}