package com.roomviz.data;

import com.roomviz.model.Design;

/**
 * Simple in-memory app session state shared across screens.
 *
 * Step 1 uses this to track the "currently selected" design.
 */
public class AppState {

    private final DesignRepository repo;
    private String currentDesignId;

    public AppState(DesignRepository repo) {
        this.repo = repo;
    }

    public DesignRepository repo() {
        return repo;
    }

    public String getCurrentDesignId() {
        return currentDesignId;
    }

    public void setCurrentDesignId(String currentDesignId) {
        this.currentDesignId = currentDesignId;
    }

    public Design getCurrentDesign() {
        if (currentDesignId == null) return null;
        return repo.getById(currentDesignId);
    }
}
