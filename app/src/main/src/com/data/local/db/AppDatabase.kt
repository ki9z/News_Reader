package com.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.data.initializer.DatabaseInitializer
import com.data.local.dao.*
import com.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ArticleEntity::class, UserEntity::class, UserAuthProviderEntity::class,
        UserSettingsEntity::class, CategoryEntity::class, UserFollowedCategoryEntity::class,
        UserBookmarkEntity::class, ArticleBlockEntity::class, LocalNewsCacheEntity::class,
        ReadingHistoryEntity::class, UserDownloadEntity::class, UserSearchHistoryEntity::class,
        SyncOutboxEntity::class
    ],
    version = 6,
    exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun userDao(): UserDao
    abstract fun userAuthProviderDao(): UserAuthProviderDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun categoryDao(): CategoryDao
    abstract fun articleBlockDao(): ArticleBlockDao
    abstract fun localNewsCacheDao(): LocalNewsCacheDao
    abstract fun readingHistoryDao(): ReadingHistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "news_reader_db"
                )
                    .addMigrations(
                        DbMigrations.MIGRATION_1_2,
                        DbMigrations.MIGRATION_2_3,
                        DbMigrations.MIGRATION_3_4,
                        DbMigrations.MIGRATION_4_5,
                        DbMigrations.MIGRATION_5_6
                    )
                    // BỔ SUNG ĐOẠN CALLBACK NÀY ĐỂ KÍCH HOẠT INITIALIZER
                    // Trong AppDatabase.kt
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Dùng onOpen để kiểm tra mỗi khi mở App, nếu trống thì nạp bù
                            CoroutineScope(Dispatchers.IO).launch {
                                // Lấy INSTANCE đã có sẵn, không gọi getInstance nữa để tránh lặp
                                INSTANCE?.let { database ->
                                    DatabaseInitializer.init(context, database)
                                }
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}