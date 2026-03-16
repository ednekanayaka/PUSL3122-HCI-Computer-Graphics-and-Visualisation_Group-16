package com.roomviz.model;

public class UserSettings {

    private String fullName = "Sarah Johnson";
    private String email = "sarah.johnson@designstudio.com";
    private String jobTitle = "Senior UI/UX Designer";
    private String department = "Design Team";

    private boolean autosaveEnabled = true;
    private String defaultUnit = "cm"; // "cm","m","in","ft"

    private String fontSize = "Small"; // Small|Medium|Large
    private boolean highContrast = false;

    // coursework demo only
    private String passwordPlain = "";

    public UserSettings() {}

    public static UserSettings defaults() {
        return new UserSettings();
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = safe(fullName); }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = safe(email); }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = safe(jobTitle); }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = safe(department); }

    public boolean isAutosaveEnabled() { return autosaveEnabled; }
    public void setAutosaveEnabled(boolean autosaveEnabled) { this.autosaveEnabled = autosaveEnabled; }

    public String getDefaultUnit() { return defaultUnit; }
    public void setDefaultUnit(String defaultUnit) { this.defaultUnit = safe(defaultUnit); }

    public String getFontSize() { return fontSize; }
    public void setFontSize(String fontSize) { this.fontSize = safe(fontSize); }

    public boolean isHighContrast() { return highContrast; }
    public void setHighContrast(boolean highContrast) { this.highContrast = highContrast; }

    public String getPasswordPlain() { return passwordPlain; }
    public void setPasswordPlain(String passwordPlain) { this.passwordPlain = safe(passwordPlain); }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
