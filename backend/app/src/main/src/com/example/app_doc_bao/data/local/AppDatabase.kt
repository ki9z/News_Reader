package com.example.app_doc_bao.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.TypeConverters
import androidx.room.RoomDatabase
import com.example.app_doc_bao.data.local.dao.*
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.app_doc_bao.data.local.entity.*
import com.example.app_doc_bao.data.local.initializer.DatabaseInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@TypeConverters(Converters::class)
@Database(
    entities = [
        Category::class,
        User::class,
        Article::class,
        Block::class,
        ArticleUser::class,
        CategoryUser::class,
        Source::class,
        SourceUser::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun userDao(): UserDao
    abstract fun sourceDao(): SourceDao
    abstract fun articleDao(): ArticleDao
    abstract fun blockDao(): BlockDao
    abstract fun articleUserDao(): ArticleUserDao
    abstract fun categoryUserDao(): CategoryUserDao
    abstract fun sourceUserDao(): SourceUserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "news_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            val scope = CoroutineScope(Dispatchers.IO)
                            scope.launch {
                                INSTANCE?.let { database ->
                                    DatabaseInitializer.init(context, database)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}