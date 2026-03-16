package com.roomviz.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roomviz.model.UserSettings;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * JSON-backed settings store.
 * Path: ~/.roomviz/settings.json
 */
public class SettingsRepository {

    private final File storeFile;
    private final Gson gson;
    private UserSettings cached;

    private SettingsRepository(File storeFile) {
        this.storeFile = storeFile;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cached = UserSettings.defaults();
        loadIfExists();
    }

    public static SettingsRepository createDefault() {
        File dir = new File(System.getProperty("user.home"), ".roomviz");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "settings.json");
        return new SettingsRepository(file);
    }

    public synchronized UserSettings get() {
        if (cached == null) cached = UserSettings.defaults();
        return cached;
    }

    public synchronized UserSettings reload() {
        loadIfExists();
        return get();
    }

    public synchronized void save(UserSettings settings) {
        if (settings == null) settings = UserSettings.defaults();
        cached = settings;

        try {
            File parent = storeFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            try (Writer w = new OutputStreamWriter(new FileOutputStream(storeFile), StandardCharsets.UTF_8)) {
                gson.toJson(cached, w);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void clearAll() {
        cached = UserSettings.defaults();
        if (storeFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            storeFile.delete();
        }
    }

    private void loadIfExists() {
        if (!storeFile.exists()) {
            cached = UserSettings.defaults();
            return;
        }

        try (Reader r = new InputStreamReader(new FileInputStream(storeFile), StandardCharsets.UTF_8)) {
            UserSettings loaded = gson.fromJson(r, UserSettings.class);
            cached = (loaded == null) ? UserSettings.defaults() : loaded;
        } catch (Exception ex) {
            ex.printStackTrace();
            cached = UserSettings.defaults();
        }
    }
}
