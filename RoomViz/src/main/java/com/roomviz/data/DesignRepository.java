package com.roomviz.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.roomviz.model.Design;
import com.roomviz.model.DesignStatus;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Minimal JSON-backed repository.
 *
 * Why JSON first?
 * - Fast to implement
 * - Works offline
 * - Satisfies "save designs for future use" requirement
 *
 * Later you can swap to SQLite (dependency already exists in build.gradle).
 */
public class DesignRepository {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Type LIST_TYPE = new TypeToken<List<Design>>() {}.getType();

    private final File storeFile;
    private final Map<String, Design> byId = new LinkedHashMap<>();

    public DesignRepository(File storeFile) {
        this.storeFile = storeFile;
        load();
    }

    /**
     * Default path: ~/.roomviz/designs.json
     */
    public static DesignRepository createDefault() {
        File dir = new File(System.getProperty("user.home"), ".roomviz");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return new DesignRepository(new File(dir, "designs.json"));
    }

    public synchronized List<Design> getAllSortedByLastUpdatedDesc() {
        List<Design> list = new ArrayList<>(byId.values());
        list.sort((a, b) -> Long.compare(b.getLastUpdatedEpochMs(), a.getLastUpdatedEpochMs()));
        return list;
    }

    public synchronized Design getById(String id) {
        if (id == null) return null;
        return byId.get(id);
    }

    public synchronized void upsert(Design d) {
        if (d == null || d.getId() == null) return;
        d.setLastUpdatedEpochMs(System.currentTimeMillis());

        // ensure status isn't null (older designs / gson)
        if (d.getStatus() == null) {
            d.setStatus(DesignStatus.DRAFT);
        }

        byId.put(d.getId(), d);
        save();
    }

    public synchronized boolean delete(String id) {
        if (id == null) return false;
        Design removed = byId.remove(id);
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    public synchronized Design duplicate(String id) {
        Design orig = byId.get(id);
        if (orig == null) return null;

        Design copy = GSON.fromJson(GSON.toJson(orig), Design.class); // deep copy
        copy.setId(UUID.randomUUID().toString());
        copy.setDesignName(safe(orig.getDesignName()) + " (Copy)");
        long now = System.currentTimeMillis();
        copy.setCreatedAtEpochMs(now);
        copy.setLastUpdatedEpochMs(now);

        byId.put(copy.getId(), copy);
        save();
        return copy;
    }

    /* ========================= persistence ========================= */

    private synchronized void load() {
        byId.clear();
        if (!storeFile.exists()) return;

        try (Reader r = new InputStreamReader(new FileInputStream(storeFile), StandardCharsets.UTF_8)) {
            List<Design> list = GSON.fromJson(r, LIST_TYPE);
            if (list == null) return;
            for (Design d : list) {
                if (d != null && d.getId() != null) byId.put(d.getId(), d);
            }
        } catch (Exception e) {
            // If file is corrupted, don't crash the whole app.
            e.printStackTrace();
        }
    }

    private synchronized void save() {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(storeFile), StandardCharsets.UTF_8)) {
            List<Design> list = new ArrayList<>(byId.values());
            GSON.toJson(list, LIST_TYPE, w);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
