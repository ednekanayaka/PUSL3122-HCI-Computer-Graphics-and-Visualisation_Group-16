package com.roomviz.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserSettingsTest {

    @Test
    void defaults_returnsSensibleValues() {
        UserSettings s = UserSettings.defaults();

        assertNotNull(s);
        assertEquals("cm", s.getDefaultUnit());
        assertEquals("Small", s.getFontSize());
        assertFalse(s.isHighContrast());
        assertTrue(s.isAutosaveEnabled());
        assertEquals("light", s.getThemeMode());
    }

    @Test
    void setThemeMode_dark_normalizesToDarkBlue() {
        UserSettings s = new UserSettings();
        s.setThemeMode("dark");
        assertEquals("dark_blue", s.getThemeMode());
    }

    @Test
    void setThemeMode_darkBlue_preserved() {
        UserSettings s = new UserSettings();
        s.setThemeMode("dark_blue");
        assertEquals("dark_blue", s.getThemeMode());
    }

    @Test
    void setThemeMode_darkHyphen_normalizesToDarkBlue() {
        UserSettings s = new UserSettings();
        s.setThemeMode("dark-blue");
        assertEquals("dark_blue", s.getThemeMode());
    }

    @Test
    void setThemeMode_darkSpace_normalizesToDarkBlue() {
        UserSettings s = new UserSettings();
        s.setThemeMode("dark blue");
        assertEquals("dark_blue", s.getThemeMode());
    }

    @Test
    void setThemeMode_light_staysLight() {
        UserSettings s = new UserSettings();
        s.setThemeMode("light");
        assertEquals("light", s.getThemeMode());
    }

    @Test
    void setThemeMode_unknown_defaultsToLight() {
        UserSettings s = new UserSettings();
        s.setThemeMode("neon");
        assertEquals("light", s.getThemeMode());
    }

    @Test
    void setThemeMode_null_defaultsToLight() {
        UserSettings s = new UserSettings();
        s.setThemeMode(null);
        assertEquals("light", s.getThemeMode());
    }

    @Test
    void setFullName_null_becomesEmpty() {
        UserSettings s = new UserSettings();
        s.setFullName(null);
        assertEquals("", s.getFullName());
    }

    @Test
    void setFullName_trims() {
        UserSettings s = new UserSettings();
        s.setFullName("  Alice  ");
        assertEquals("Alice", s.getFullName());
    }

    @Test
    void setEmail_null_becomesEmpty() {
        UserSettings s = new UserSettings();
        s.setEmail(null);
        assertEquals("", s.getEmail());
    }
}
