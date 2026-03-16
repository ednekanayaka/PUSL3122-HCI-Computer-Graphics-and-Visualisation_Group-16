package com.roomviz.data;

import com.roomviz.model.UserSettings;

import java.sql.*;

/**
 * SQLite-backed settings store (per-user via user_id).
 *
 * DB file: ./roomviz.db (project folder)
 * Schema: ./roomviz.sql (project folder)
 *
 * No JSON migration / no user.home / no .roomviz
 * No CREATE TABLE SQL in Java (all schema is in roomviz.sql)
 */
public class SettingsRepository {

    private final String jdbcUrl;
    private final int userId;

    private UserSettings cached;

    private SettingsRepository(String jdbcUrl, int userId) {
        this.jdbcUrl = jdbcUrl;
        this.userId = userId;
        init();
        this.cached = loadFromDb();
        if (this.cached == null) this.cached = UserSettings.defaults();
    }

    public static SettingsRepository createDefault() {
        return createForUser(0);
    }

    public static SettingsRepository createForUser(int userId) {
        String url = DbBootstrap.jdbcUrl();
        return new SettingsRepository(url, userId);
    }

    public synchronized UserSettings get() {
        if (cached == null) cached = UserSettings.defaults();
        return cached;
    }

    public synchronized UserSettings reload() {
        cached = loadFromDb();
        if (cached == null) cached = UserSettings.defaults();
        return cached;
    }

    public synchronized void save(UserSettings settings) {
        if (settings == null) settings = UserSettings.defaults();
        cached = settings;

        String sql = """
            INSERT INTO user_settings(user_id, autosave_enabled, default_unit, font_size, high_contrast, theme_mode)
            VALUES(?,?,?,?,?,?)
            ON CONFLICT(user_id) DO UPDATE SET
              autosave_enabled=excluded.autosave_enabled,
              default_unit=excluded.default_unit,
              font_size=excluded.font_size,
              high_contrast=excluded.high_contrast,
              theme_mode=excluded.theme_mode
        """;

        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, settings.isAutosaveEnabled() ? 1 : 0);
            ps.setString(3, safe(settings.getDefaultUnit(), "cm"));
            ps.setString(4, safe(settings.getFontSize(), "Small"));
            ps.setInt(5, settings.isHighContrast() ? 1 : 0);
            ps.setString(6, safeThemeMode(settings.getThemeMode()));

            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void clearAll() {
        cached = UserSettings.defaults();
        String sql = "DELETE FROM user_settings WHERE user_id=?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* ========================= DB init/load ========================= */

    /**
     * Schema must come ONLY from roomviz.sql (project folder).
     * No CREATE TABLE statements in Java.
     */
    private void init() {
        DbBootstrap.ensureSchema(jdbcUrl);
        ensureThemeModeColumn();
    }

    private UserSettings loadFromDb() {
        String sql = "SELECT autosave_enabled, default_unit, font_size, high_contrast, theme_mode FROM user_settings WHERE user_id=?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                UserSettings s = new UserSettings();
                s.setAutosaveEnabled(rs.getInt("autosave_enabled") == 1);
                s.setDefaultUnit(safe(rs.getString("default_unit"), "cm"));
                s.setFontSize(safe(rs.getString("font_size"), "Small"));
                s.setHighContrast(rs.getInt("high_contrast") == 1);
                s.setThemeMode(safeThemeMode(rs.getString("theme_mode")));
                return s;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void ensureThemeModeColumn() {
        try (Connection c = DriverManager.getConnection(jdbcUrl)) {
            if (columnExists(c, "user_settings", "theme_mode")) return;
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE user_settings ADD COLUMN theme_mode TEXT DEFAULT 'light'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean columnExists(Connection c, String tableName, String columnName) throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ")";
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String col = rs.getString("name");
                if (columnName.equalsIgnoreCase(col)) return true;
            }
        }
        return false;
    }

    private static String safe(String s, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }

    private static String safeThemeMode(String mode) {
        if (mode == null) return "light";
        String t = mode.trim().toLowerCase();
        if ("dark_blue".equals(t) || "dark blue".equals(t) || "dark-blue".equals(t) || "dark".equals(t)) {
            return "dark_blue";
        }
        return "light";
    }
}
