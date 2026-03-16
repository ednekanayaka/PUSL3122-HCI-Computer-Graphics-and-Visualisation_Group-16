package com.roomviz.model;

public class User {
    private final int id;
    private final String fullName;
    private final String email;
    private final String passwordHash;

    public User(int id, String fullName, String email, String passwordHash) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
}