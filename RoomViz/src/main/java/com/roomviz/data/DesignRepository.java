package com.roomviz.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.roomviz.model.Design;
import com.roomviz.model.DesignStatus;
import com.roomviz.model.RoomSpec;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

/**
 * SQLite-backed design repository with role-based ownership filtering.
 */
public class DesignRepository {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Type LIST_TYPE = new TypeToken<List<Design>>() {}.getType();

    private final String jdbcUrl;

    // logged-in user (viewer)
    private final int viewerUserId;

    // if not admin: restrict to owner_user_id = ownerFilterUserId
    private final boolean adminView;
    private final int ownerFilterUserId;

    // cache
    private final Map<String, Design> byId = new LinkedHashMap<>();

    // keep ownership info for each design (important for admin edits)
    private final Map<String, Integer> ownerByDesignId = new HashMap<>();
    private final Map<String, Integer> createdByByDesignId = new HashMap<>();

    private DesignRepository(String jdbcUrl, int viewerUserId, boolean adminView, int ownerFilterUserId) {
        this.jdbcUrl = jdbcUrl;
        this.viewerUserId = viewerUserId;
        this.adminView = adminView;
        this.ownerFilterUserId = ownerFilterUserId;
        init();
        loadAll();
    }

    /** Legacy fallback (owner=0, created_by=0). */
    public static DesignRepository createDefault() {
        String url = DbBootstrap.jdbcUrl();
        return new DesignRepository(url, 0, false, 0);
    }

    /** Customer view: only designs owned by this user. */
    public static DesignRepository createForOwner(int ownerUserId) {
        String url = DbBootstrap.jdbcUrl();
        return new DesignRepository(url, ownerUserId, false, ownerUserId);
    }

    /** Admin view: see all designs, create for others. */
    public static DesignRepository createAdminView(int adminUserId) {
        String url = DbBootstrap.jdbcUrl();
        return new DesignRepository(url, adminUserId, true, -1);
    }

    // --- Public API ---

    public boolean isAdminView() { return adminView; }

    public synchronized List<Design> getAllSortedByLastUpdatedDesc() {
        List<Design> list = new ArrayList<>(byId.values());
        list.sort((a, b) -> Long.compare(b.getLastUpdatedEpochMs(), a.getLastUpdatedEpochMs()));
        return list;
    }

    public synchronized List<Design> listAll() {
        return new ArrayList<>(byId.values());
    }

    public synchronized Design getById(String id) {
        if (id == null) return null;
        return byId.get(id);
    }

    public int countByOwner(int ownerUserId) {
        String sql = "SELECT COUNT(*) AS c FROM designs WHERE owner_user_id=?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, ownerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 0;
                return rs.getInt("c");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Default upsert.
     * - customer mode: owner = ownerFilterUserId, created_by = viewerUserId
     * - admin mode: owner = previously loaded owner (or viewer if unknown), created_by = viewerUserId
     */
    public synchronized boolean upsert(Design d) {
        if (d == null || d.getId() == null) return false;

        int ownerId;
        if (adminView) {
            ownerId = ownerByDesignId.getOrDefault(d.getId(), viewerUserId);
        } else {
            ownerId = ownerFilterUserId;
        }

        return upsertInternal(d, ownerId, viewerUserId);
    }

    /**
     * Admin-only helper: save a design for a specific customer (owner).
     * created_by will always be the current admin (viewerUserId).
     */
    public synchronized boolean upsertForOwner(Design d, int ownerUserId) {
        if (!adminView) {
            // customers should never assign owners
            return false;
        }
        return upsertInternal(d, ownerUserId, viewerUserId);
    }

    private synchronized boolean upsertInternal(Design d, int ownerUserId, int createdByUserId) {
        long now = System.currentTimeMillis();
        d.setLastUpdatedEpochMs(now);

        if (d.getStatus() == null) d.setStatus(DesignStatus.DRAFT);
        if (d.getCreatedAtEpochMs() <= 0) d.setCreatedAtEpochMs(now);

        Design previous = byId.put(d.getId(), d);
        Integer prevOwner = ownerByDesignId.put(d.getId(), ownerUserId);
        Integer prevCreatedBy = createdByByDesignId.put(d.getId(), createdByUserId);

        String json = GSON.toJson(d);

        String sql = """
            INSERT INTO designs(id, owner_user_id, created_by_user_id, design_name, status, created_at, updated_at, design_json)
            VALUES(?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET
              owner_user_id=excluded.owner_user_id,
              created_by_user_id=excluded.created_by_user_id,
              design_name=excluded.design_name,
              status=excluded.status,
              created_at=excluded.created_at,
              updated_at=excluded.updated_at,
              design_json=excluded.design_json
        """;

        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, d.getId());
            ps.setInt(2, ownerUserId);
            ps.setInt(3, createdByUserId);
            ps.setString(4, safe(d.getDesignName()));
            ps.setString(5, String.valueOf(d.getStatus()));
            ps.setLong(6, d.getCreatedAtEpochMs());
            ps.setLong(7, d.getLastUpdatedEpochMs());
            ps.setString(8, json);

            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();

            // rollback cache
            if (previous == null) byId.remove(d.getId());
            else byId.put(d.getId(), previous);

            if (prevOwner == null) ownerByDesignId.remove(d.getId());
            else ownerByDesignId.put(d.getId(), prevOwner);

            if (prevCreatedBy == null) createdByByDesignId.remove(d.getId());
            else createdByByDesignId.put(d.getId(), prevCreatedBy);

            return false;
        }
    }

    public synchronized boolean delete(String id) {
        if (id == null) return false;

        Design removed = byId.remove(id);
        Integer removedOwner = ownerByDesignId.remove(id);
        Integer removedCreatedBy = createdByByDesignId.remove(id);

        if (removed == null) return false;

        String sql;
        if (adminView) {
            sql = "DELETE FROM designs WHERE id=?";
        } else {
            sql = "DELETE FROM designs WHERE id=? AND owner_user_id=?";
        }

        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, id);
            if (!adminView) ps.setInt(2, ownerFilterUserId);

            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();

            // restore cache
            byId.put(id, removed);
            if (removedOwner != null) ownerByDesignId.put(id, removedOwner);
            if (removedCreatedBy != null) createdByByDesignId.put(id, removedCreatedBy);

            return false;
        }
    }

    public synchronized Design duplicate(String id) {
        Design orig = byId.get(id);
        if (orig == null) return null;

        Design copy = GSON.fromJson(GSON.toJson(orig), Design.class);
        copy.setId(UUID.randomUUID().toString());
        copy.setDesignName(safe(orig.getDesignName()) + " (Copy)");

        long now = System.currentTimeMillis();
        copy.setCreatedAtEpochMs(now);
        copy.setLastUpdatedEpochMs(now);

        if (copy.getStatus() == null) copy.setStatus(DesignStatus.DRAFT);

        if (adminView) {
            int owner = ownerByDesignId.getOrDefault(id, viewerUserId);
            upsertForOwner(copy, owner);
        } else {
            upsert(copy);
        }
        return copy;
    }

    public synchronized void exportTo(File targetFile) {
        if (targetFile == null) return;

        List<Design> list = fetchAllFromDb();

        try (Writer w = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8)) {
            GSON.toJson(list, LIST_TYPE, w);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public synchronized int importFrom(File sourceFile) {
        if (sourceFile == null || !sourceFile.isFile()) return 0;

        List<Design> incoming = parseImportedDesigns(sourceFile);
        if (incoming.isEmpty()) return 0;

        int importedCount = 0;
        for (Design raw : incoming) {
            Design normalized = normalizeImportedDesign(raw);
            if (normalized == null) continue;

            boolean ok;
            if (adminView) {
                // imported designs default to admin as owner unless you assign later
                ok = upsertForOwner(normalized, viewerUserId);
            } else {
                ok = upsert(normalized);
            }

            if (ok) importedCount++;
        }

        return importedCount;
    }

    /** Clear designs for current customer owner. (Admin can also clear own if needed.) */
    public synchronized void clearAll() {
        byId.clear();
        ownerByDesignId.clear();
        createdByByDesignId.clear();

        String sql;
        if (adminView) {
            // safest: only clear designs owned by this admin (not all designs)
            sql = "DELETE FROM designs WHERE owner_user_id=?";
        } else {
            sql = "DELETE FROM designs WHERE owner_user_id=?";
        }

        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, adminView ? viewerUserId : ownerFilterUserId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- DB init + load ---

    private void init() {
        DbBootstrap.ensureSchema(jdbcUrl);
    }

    private synchronized void loadAll() {
        byId.clear();
        ownerByDesignId.clear();
        createdByByDesignId.clear();

        String sql;
        if (adminView) {
            sql = "SELECT id, owner_user_id, created_by_user_id, design_json FROM designs ORDER BY updated_at DESC";
        } else {
            sql = "SELECT id, owner_user_id, created_by_user_id, design_json FROM designs WHERE owner_user_id=? ORDER BY updated_at DESC";
        }

        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (!adminView) ps.setInt(1, ownerFilterUserId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    int ownerId = rs.getInt("owner_user_id");
                    int createdById = rs.getInt("created_by_user_id");

                    String json = rs.getString("design_json");
                    Design d = safeParseDesign(json);
                    if (d != null && d.getId() != null) {
                        if (d.getStatus() == null) d.setStatus(DesignStatus.DRAFT);

                        byId.put(d.getId(), d);
                        ownerByDesignId.put(id, ownerId);
                        createdByByDesignId.put(id, createdById);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Design> fetchAllFromDb() {
        List<Design> out = new ArrayList<>();

        String sql;
        if (adminView) {
            sql = "SELECT design_json FROM designs ORDER BY updated_at DESC";
        } else {
            sql = "SELECT design_json FROM designs WHERE owner_user_id=? ORDER BY updated_at DESC";
        }

        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (!adminView) ps.setInt(1, ownerFilterUserId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Design d = safeParseDesign(rs.getString("design_json"));
                    if (d != null) {
                        if (d.getStatus() == null) d.setStatus(DesignStatus.DRAFT);
                        out.add(d);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out;
    }

    private List<Design> parseImportedDesigns(File sourceFile) {
        try {
            String json = Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) return Collections.emptyList();

            try {
                List<Design> list = GSON.fromJson(json, LIST_TYPE);
                if (list != null && !list.isEmpty()) return list;
            } catch (Exception ignored) { }

            try {
                Design single = GSON.fromJson(json, Design.class);
                if (single != null) return List.of(single);
            } catch (Exception ignored) { }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return Collections.emptyList();
    }

    private Design normalizeImportedDesign(Design raw) {
        if (raw == null) return null;

        Design d;
        try {
            d = GSON.fromJson(GSON.toJson(raw), Design.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        if (d == null) return null;

        if (d.getId() == null || d.getId().isBlank() || byId.containsKey(d.getId())) {
            d.setId(UUID.randomUUID().toString());
        }

        if (safe(d.getDesignName()).isBlank()) d.setDesignName("Imported Design");
        d.setCustomerName(safe(d.getCustomerName()));
        d.setNotes(safe(d.getNotes()));

        if (d.getRoomSpec() == null) {
            d.setRoomSpec(new RoomSpec(12, 10, "ft", "Rectangular", "Neutral Tones"));
        }

        if (d.getStatus() == null) d.setStatus(DesignStatus.DRAFT);

        long now = System.currentTimeMillis();
        if (d.getCreatedAtEpochMs() <= 0) d.setCreatedAtEpochMs(now);
        if (d.getLastUpdatedEpochMs() <= 0) d.setLastUpdatedEpochMs(now);

        d.setItems(d.getItems());
        return d;
    }

    private Design safeParseDesign(String json) {
        try {
            if (json == null || json.isBlank()) return null;
            return GSON.fromJson(json, Design.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}