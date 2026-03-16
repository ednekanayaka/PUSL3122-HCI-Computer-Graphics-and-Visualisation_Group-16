package com.roomviz.model;

public class User {
    private final int id;
    private final String fullName;
    private final String email;
    private final String passwordHash;

    // NEW
    private final String jobTitle;
    private final String department;

    // Backward compatible (old constructor)
    public User(int id, String fullName, String email, String passwordHash) {
        this(id, fullName, email, passwordHash, "", "");
    }

    public User(int id, String fullName, String email, String passwordHash, String jobTitle, String department) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.jobTitle = jobTitle;
        this.department = department;
    }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }

    public String getJobTitle() { return jobTitle; }
    public String getDepartment() { return department; }
}