package com.roomviz.data;

import com.roomviz.model.User;

public class Session {
    private User currentUser;

    public User getCurrentUser() { return currentUser; }
    public boolean isLoggedIn() { return currentUser != null; }
    public void login(User u) { this.currentUser = u; }
    public void logout() { this.currentUser = null; }
}