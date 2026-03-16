package com.roomviz.data;

import com.roomviz.model.User;
import com.roomviz.security.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private final String jdbcUrl;

    private UserRepository(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        init();
    }

    /**
     * DB must live in the PROJECT folder.
     * Uses DbBootstrap to resolve the path.
     */
    public static UserRepository createDefault() {
        String url = DbBootstrap.jdbcUrl();
        return new UserRepository(url);
    }

    /**
     * Schema must live ONLY in roomviz.sql (project folder).
     * No CREATE TABLE statements in Java.
     */
    private void init() {
        DbBootstrap.ensureSchema(jdbcUrl);
    }

    public User findByEmail(String emailLower) {
        String sql = """
            SELECT id, full_name, email, password_hash, job_title, department, role
            FROM users
            WHERE lower(email)=?
        """;
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, emailLower);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapUser(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public User findById(int id) {
        String sql = """
            SELECT id, full_name, email, password_hash, job_title, department, role
            FROM users
            WHERE id=?
        """;
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapUser(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public User findByEmailExact(String emailLower) {
        return findByEmail(emailLower);
    }

    // list users (for admin management page later)
    public List<User> listAllUsers() {
        String sql = """
            SELECT id, full_name, email, password_hash, job_title, department, role
            FROM users
            ORDER BY role ASC, full_name ASC
        """;
        List<User> out = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) out.add(mapUser(rs));
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            return out;
        }
    }

    public List<User> listUsersByRole(String role) {
        String sql = """
            SELECT id, full_name, email, password_hash, job_title, department, role
            FROM users
            WHERE upper(role)=upper(?)
            ORDER BY full_name ASC
        """;
        List<User> out = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, safe(role));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapUser(rs));
            }
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            return out;
        }
    }

    /**
     * Keep old behavior = creates ADMIN (so your current app keeps working until Step 2 UI is added).
     */
    public User createUser(String fullName, String email, char[] password) throws SQLException {
        return createUser(fullName, email, password, User.ROLE_ADMIN);
    }

    /**
     * Create user with a role (ADMIN or CUSTOMER).
     */
    public User createUser(String fullName, String email, char[] password, String role) throws SQLException {
        String emailLower = email.trim().toLowerCase();
        String hash = PasswordUtil.hash(password);

        String sql = "INSERT INTO users(full_name, email, password_hash, job_title, department, role) VALUES(?,?,?,?,?,?)";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, safe(fullName));
            ps.setString(2, emailLower);
            ps.setString(3, hash);
            ps.setString(4, ""); // job_title initially blank
            ps.setString(5, ""); // department initially blank

            String normalizedRole = (role == null || role.trim().isEmpty())
                    ? User.ROLE_ADMIN
                    : role.trim().toUpperCase();

            if (!User.ROLE_ADMIN.equals(normalizedRole) && !User.ROLE_CUSTOMER.equals(normalizedRole)) {
                normalizedRole = User.ROLE_ADMIN;
            }

            ps.setString(6, normalizedRole);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                int id = keys.next() ? keys.getInt(1) : -1;
                return new User(id, safe(fullName), emailLower, hash, "", "", normalizedRole);
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

    /**
     * Permanently delete the user record from SQLite.
     */
    public void deleteUserById(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id=?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                safe(rs.getString("job_title")),
                safe(rs.getString("department")),
                safe(rs.getString("role"))
        );
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}