PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     full_name TEXT NOT NULL,
                                     email TEXT NOT NULL UNIQUE,
                                     password_hash TEXT NOT NULL,
                                     job_title TEXT DEFAULT '',
                                     department TEXT DEFAULT '',
                                     role TEXT NOT NULL DEFAULT 'ADMIN' -- ADMIN or CUSTOMER
);

CREATE TABLE IF NOT EXISTS user_settings (
                                             user_id INTEGER PRIMARY KEY,
                                             autosave_enabled INTEGER DEFAULT 1,
                                             default_unit TEXT DEFAULT 'ft',
                                             font_size INTEGER DEFAULT 14,
                                             high_contrast INTEGER DEFAULT 0,
                                             theme_mode TEXT DEFAULT 'light',
                                             FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS designs (
                                       id TEXT PRIMARY KEY,

    -- who owns the design (customer who can view it)
                                       owner_user_id INTEGER NOT NULL,

    -- who created the design (admin or customer)
                                       created_by_user_id INTEGER NOT NULL,

                                       design_name TEXT DEFAULT '',
                                       status TEXT DEFAULT 'DRAFT',
                                       created_at INTEGER DEFAULT 0,
                                       updated_at INTEGER DEFAULT 0,
                                       design_json TEXT NOT NULL,

                                       FOREIGN KEY(owner_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(created_by_user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_designs_owner_updated
    ON designs(owner_user_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_designs_createdby_updated
    ON designs(created_by_user_id, updated_at DESC);
