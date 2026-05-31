package com.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DbMigrations {
    const val DEFAULT_USER_ID = "local_user"

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT NOT NULL,
                    email TEXT,
                    fullName TEXT,
                    username TEXT,
                    passwordHash TEXT,
                    role TEXT NOT NULL,
                    phone TEXT,
                    avatarUrl TEXT,
                    occupation TEXT,
                    location TEXT,
                    birthday TEXT,
                    bio TEXT,
                    interests TEXT,
                    isSignedIn INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_users_email
                ON users(email)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_users_username
                ON users(username)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS categories (
                    id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    parentId TEXT,
                    sortOrder INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_followed_categories (
                    userId TEXT NOT NULL,
                    categoryId TEXT NOT NULL,
                    followedAt INTEGER NOT NULL,
                    notificationsEnabled INTEGER NOT NULL,
                    PRIMARY KEY(userId, categoryId),
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_user_followed_categories_userId
                ON user_followed_categories(userId)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_user_followed_categories_categoryId
                ON user_followed_categories(categoryId)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_bookmarks (
                    userId TEXT NOT NULL,
                    articleUrl TEXT NOT NULL,
                    savedAt INTEGER NOT NULL,
                    note TEXT,
                    tags TEXT,
                    PRIMARY KEY(userId, articleUrl),
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(articleUrl) REFERENCES articles(url) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_user_bookmarks_userId
                ON user_bookmarks(userId)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_user_bookmarks_articleUrl
                ON user_bookmarks(articleUrl)
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT OR IGNORE INTO users(
                    id, email, fullName, username, passwordHash, role, phone,
                    avatarUrl, occupation, location, birthday, bio, interests,
                    isSignedIn, createdAt, updatedAt
                ) VALUES(
                    '$DEFAULT_USER_ID',
                    'guest@newsreader.app',
                    'Guest User',
                    NULL,
                    NULL,
                    'guest',
                    NULL,
                    NULL,
                    NULL,
                    NULL,
                    NULL,
                    NULL,
                    NULL,
                    0,
                    $now,
                    $now
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT OR IGNORE INTO user_bookmarks(userId, articleUrl, savedAt)
                SELECT '$DEFAULT_USER_ID', url, $now
                FROM articles
                """.trimIndent()
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_settings (
                    userId TEXT NOT NULL,
                    darkModeEnabled INTEGER NOT NULL,
                    notificationsEnabled INTEGER NOT NULL,
                    dataSaverEnabled INTEGER NOT NULL,
                    languageCode TEXT NOT NULL,
                    textSize TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(userId),
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT OR IGNORE INTO user_settings(
                    userId,
                    darkModeEnabled,
                    notificationsEnabled,
                    dataSaverEnabled,
                    languageCode,
                    textSize,
                    updatedAt
                )
                SELECT id, 0, 1, 0, 'en', 'M', $now
                FROM users
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_auth_providers (
                    userId TEXT NOT NULL,
                    providerCode TEXT NOT NULL,
                    providerUserId TEXT,
                    maskedPhone TEXT,
                    isPrimary INTEGER NOT NULL,
                    linkedAt INTEGER NOT NULL,
                    PRIMARY KEY(userId, providerCode),
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_user_auth_providers_userId
                ON user_auth_providers(userId)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS reading_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId TEXT NOT NULL,
                    articleUrl TEXT NOT NULL,
                    openedAt INTEGER NOT NULL,
                    closedAt INTEGER,
                    readSeconds INTEGER NOT NULL,
                    completionPercent INTEGER NOT NULL,
                    fromScreen TEXT,
                    deviceInfo TEXT,
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(articleUrl) REFERENCES articles(url) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_reading_history_userId_openedAt
                ON reading_history(userId, openedAt)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_reading_history_articleUrl
                ON reading_history(articleUrl)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_downloads (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId TEXT NOT NULL,
                    articleUrl TEXT NOT NULL,
                    downloadedAt INTEGER NOT NULL,
                    localPath TEXT,
                    fileSizeBytes INTEGER,
                    status TEXT NOT NULL,
                    expiresAt INTEGER,
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(articleUrl) REFERENCES articles(url) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_user_downloads_userId_articleUrl
                ON user_downloads(userId, articleUrl)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_user_downloads_userId_downloadedAt
                ON user_downloads(userId, downloadedAt)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_user_downloads_status
                ON user_downloads(status)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_user_downloads_articleUrl
                ON user_downloads(articleUrl)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS article_blocks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    articleUrl TEXT NOT NULL,
                    type TEXT NOT NULL,
                    content TEXT,
                    imageUrl TEXT,
                    caption TEXT,
                    blockOrder INTEGER NOT NULL,
                    metadataJson TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(articleUrl) REFERENCES articles(url) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_article_blocks_articleUrl_blockOrder
                ON article_blocks(articleUrl, blockOrder)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_search_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId TEXT NOT NULL,
                    query TEXT NOT NULL,
                    searchedAt INTEGER NOT NULL,
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_user_search_history_userId_searchedAt
                ON user_search_history(userId, searchedAt)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS sync_outbox (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId TEXT,
                    actionType TEXT NOT NULL,
                    entityType TEXT NOT NULL,
                    entityId TEXT,
                    payloadJson TEXT NOT NULL,
                    status TEXT NOT NULL,
                    retryCount INTEGER NOT NULL,
                    lastError TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE SET NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_sync_outbox_status_createdAt
                ON sync_outbox(status, createdAt)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_sync_outbox_userId
                ON sync_outbox(userId)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS local_news_cache (
                    cacheKey TEXT NOT NULL,
                    cityTitle TEXT NOT NULL,
                    countryCode TEXT NOT NULL,
                    locationQuery TEXT,
                    articlesJson TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(cacheKey)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            if (!hasColumn(db, "articles", "city")) {
                db.execSQL(
                    """
                    ALTER TABLE articles
                    ADD COLUMN city TEXT
                    """.trimIndent()
                )
            }
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            if (!hasColumn(db, "users", "status")) {
                db.execSQL(
                    """
                    ALTER TABLE users
                    ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'
                    """.trimIndent()
                )
            }
        }
    }

    private fun hasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
        val cursor = db.query("PRAGMA table_info($tableName)")
        cursor.use {
            val nameIndex = it.getColumnIndex("name")
            while (it.moveToNext()) {
                if (nameIndex >= 0 && it.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }
}
