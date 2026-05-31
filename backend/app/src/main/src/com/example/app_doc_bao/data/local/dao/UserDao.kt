package com.example.app_doc_bao.data.local.dao

import androidx.room.*
import com.example.app_doc_bao.data.local.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getTotalUsers(): Int

    @Delete
    suspend fun delete(user: User)

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users")
    suspend fun getAll(): List<User>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE username = :username OR email = :email LIMIT 1")
    suspend fun findByUsernameOrEmail(username: String, email: String): User?

    @Query("SELECT * FROM users WHERE id != :idToExclude AND (username = :username OR email = :email) LIMIT 1")
    suspend fun findDuplicateUser(idToExclude: Long, username: String, email: String): User?

    @Query("SELECT * FROM users WHERE username = :username AND password = :password AND status = :status LIMIT 1")
    suspend fun login(username: String, password: String, status: String): User?

    // Sử dụng REPLACE để tránh crash khi nạp chồng dữ liệu bài báo/user
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Long)
}