package com.roomviz.model;

public class User {
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    private final int id;
    private final String fullName;
    private final String email;
    private final String passwordHash;

    private final String jobTitle;
    private final String department;

    private final String role;

    // Backward compatible (old constructor)
    public User(int id, String fullName, String email, String passwordHash) {
        this(id, fullName, email, passwordHash, "", "", ROLE_ADMIN);
    }

    // Backward compatible (your current "new" constructor)
    public User(int id, String fullName, String email, String passwordHash, String jobTitle, String department) {
        this(id, fullName, email, passwordHash, jobTitle, department, ROLE_ADMIN);
    }

    public User(int id,
                String fullName,
                String email,
                String passwordHash,
                String jobTitle,
                String department,
                String role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.jobTitle = jobTitle;
        this.department = department;
        this.role = (role == null || role.trim().isEmpty()) ? ROLE_ADMIN : role.trim().toUpperCase();
    }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }

    public String getJobTitle() { return jobTitle; }
    public String getDepartment() { return department; }

    public String getRole() { return role; }
    public boolean isAdmin() { return ROLE_ADMIN.equalsIgnoreCase(role); }
    public boolean isCustomer() { return ROLE_CUSTOMER.equalsIgnoreCase(role); }
}