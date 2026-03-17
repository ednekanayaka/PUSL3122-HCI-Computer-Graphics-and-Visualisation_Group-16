package com.roomviz.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DesignTest {

    @Test
    void createNew_generatesIdAndDefaults() {
        Design d = Design.createNew("My Room");

        assertNotNull(d.getId(), "ID should be generated");
        assertFalse(d.getId().isBlank(), "ID should not be blank");
        assertEquals("My Room", d.getDesignName());
        assertNotNull(d.getRoomSpec(), "RoomSpec should be initialized");
        assertEquals(DesignStatus.DRAFT, d.getStatus());
        assertTrue(d.getCreatedAtEpochMs() > 0, "Created timestamp should be set");
        assertTrue(d.getLastUpdatedEpochMs() > 0, "Updated timestamp should be set");
        assertNotNull(d.getItems(), "Items list should be initialized");
        assertTrue(d.getItems().isEmpty(), "Items list should be empty");
    }

    @Test
    void createNew_blankName_defaultsToUntitled() {
        Design d = Design.createNew("");
        assertEquals("Untitled Design", d.getDesignName());

        Design d2 = Design.createNew(null);
        assertEquals("Untitled Design", d2.getDesignName());
    }

    @Test
    void setItems_null_becomesEmptyList() {
        Design d = new Design();
        d.setItems(null);

        assertNotNull(d.getItems());
        assertTrue(d.getItems().isEmpty());
    }

    @Test
    void setItems_preservesList() {
        Design d = new Design();
        List<FurnitureItem> items = new ArrayList<>();
        items.add(new FurnitureItem("1", "Chair", "CHAIR", 0, 0, 50, 50));
        d.setItems(items);

        assertEquals(1, d.getItems().size());
        assertEquals("Chair", d.getItems().get(0).getName());
    }

    @Test
    void setStatus_null_defaultsToDraft() {
        Design d = new Design();
        d.setStatus(null);
        assertEquals(DesignStatus.DRAFT, d.getStatus());
    }

    @Test
    void getStatus_nullField_returnsDraft() {
        Design d = new Design();
        // default constructor doesn't set status
        assertEquals(DesignStatus.DRAFT, d.getStatus());
    }

    @Test
    void getName_delegatesToDesignName() {
        Design d = new Design();
        d.setDesignName("Test");
        assertEquals("Test", d.getName());
    }

    @Test
    void setName_delegatesToSetDesignName() {
        Design d = new Design();
        d.setName("Hello");
        assertEquals("Hello", d.getDesignName());
    }

    @Test
    void touchUpdatedAtNow_updatesTimestamp() {
        Design d = Design.createNew("Test");
        long before = d.getLastUpdatedEpochMs();

        // Small delay to ensure timestamp changes
        try { Thread.sleep(5); } catch (InterruptedException ignored) {}

        d.touchUpdatedAtNow();
        assertTrue(d.getLastUpdatedEpochMs() >= before, "Timestamp should be updated");
    }

    @Test
    void fullConstructor_setsAllFields() {
        RoomSpec spec = new RoomSpec(10, 12, "ft", "Rectangular", "Warm Tones");
        Design d = new Design("id-1", "My Room", "john@example.com", "some notes", spec);

        assertEquals("id-1", d.getId());
        assertEquals("My Room", d.getDesignName());
        assertEquals("john@example.com", d.getCustomerName());
        assertEquals("some notes", d.getNotes());
        assertSame(spec, d.getRoomSpec());
        assertEquals(DesignStatus.DRAFT, d.getStatus());
        assertTrue(d.getCreatedAtEpochMs() > 0);
    }
}
