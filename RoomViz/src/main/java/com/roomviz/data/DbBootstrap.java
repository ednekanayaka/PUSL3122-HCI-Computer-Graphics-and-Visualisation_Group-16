package com.roomviz.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class DbBootstrap {

    private DbBootstrap() {}

    /** DB file must live in the PROJECT */
    public static String jdbcUrl() {
        File db = new File("roomviz.db");
        return "jdbc:sqlite:" + db.getAbsolutePath();
    }

    /** Runs schema from PROJECT folder file: roomviz.sql + performs migrations for existing DBs */
    public static void ensureSchema(String jdbcUrl) {
        File sqlFile = new File("roomviz.sql");
        if (!sqlFile.exists()) {
            throw new IllegalStateException(
                    "Missing roomviz.sql in project folder. Expected at: " + sqlFile.getAbsolutePath()
            );
        }

        String sqlText;
        try {
            sqlText = Files.readString(sqlFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read roomviz.sql: " + e.getMessage(), e);
        }

        List<String> statements = splitSqlStatements(sqlText);

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            try (Statement st = conn.createStatement()) {
                // FK must be enabled per connection in SQLite
                st.execute("PRAGMA foreign_keys = ON;");
                for (String s : statements) {
                    String trimmed = s.trim();
                    if (trimmed.isEmpty()) continue;
                    st.execute(trimmed);
                }
            }

            // After schema creation, ensure migrations for older DBs
            migrateIfNeeded(conn);

        } catch (Exception e) {
            throw new RuntimeException("Failed to bootstrap DB schema: " + e.getMessage(), e);
        }
    }

    /**
     * Migrations for existing DBs (because CREATE TABLE IF NOT EXISTS won't add columns).
     */
    private static void migrateIfNeeded(Connection conn) throws SQLException {
        // users.role
        ensureColumn(conn, "users", "role", "TEXT NOT NULL DEFAULT 'ADMIN'");

        // designs ownership columns
        ensureColumn(conn, "designs", "owner_user_id", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn(conn, "designs", "created_by_user_id", "INTEGER NOT NULL DEFAULT 0");

        // Only run if the old user_id column exists (legacy DB migration).
        // On a fresh DB created from roomviz.sql, user_id doesn't exist.
        if (columnExists(conn, "designs", "user_id")) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("""
                    UPDATE designs
                    SET owner_user_id = user_id
                    WHERE owner_user_id = 0
                """);

                st.executeUpdate("""
                    UPDATE designs
                    SET created_by_user_id = user_id
                    WHERE created_by_user_id = 0
                """);
            }
        }
    }

    private static void ensureColumn(Connection conn, String table, String column, String definition) throws SQLException {
        if (columnExists(conn, table, column)) return;
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws SQLException {
        String sql = "PRAGMA table_info(" + table + ")";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (column.equalsIgnoreCase(name)) return true;
            }
            return false;
        }
    }

    /**
     * Very small SQL splitter:
     * - removes single-line comments starting with --
     * - splits on ; outside of quotes
     */
    private static List<String> splitSqlStatements(String sql) {
        String noComments = removeLineComments(sql);

        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < noComments.length(); i++) {
            char c = noComments.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                cur.append(c);
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                cur.append(c);
                continue;
            }

            if (c == ';' && !inSingleQuote && !inDoubleQuote) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }

        if (!cur.toString().trim().isEmpty()) {
            out.add(cur.toString());
        }

        return out;
    }

    private static String removeLineComments(String sql) {
        String[] lines = sql.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--")) continue; // full-line comment
            int idx = line.indexOf("--");
            if (idx >= 0) {
                sb.append(line, 0, idx);
            } else {
                sb.append(line);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}