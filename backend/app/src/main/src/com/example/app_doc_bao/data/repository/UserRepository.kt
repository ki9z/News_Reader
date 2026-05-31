package com.example.app_doc_bao.data.repository

import com.example.app_doc_bao.data.domain.UserRole
import com.example.app_doc_bao.data.domain.UserStatus
import com.example.app_doc_bao.data.local.AppDatabase
import com.example.app_doc_bao.data.local.entity.User
import com.example.app_doc_bao.data.mapper.UserMapper
import com.example.app_doc_bao.data.model.UserModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepository(
    db: AppDatabase
) {

    private val userDao = db.userDao()

    fun getAllUsersFlow(): Flow<List<UserModel>> {
        return userDao.getAllUsersFlow().map { entities ->
            entities.map { UserMapper.toUserModel(it) }
        }
    }

    suspend fun getAllUsers(): List<UserModel> {
        return userDao.getAll().map { UserMapper.toUserModel(it) }
    }

    suspend fun getUserById(userId: Long): UserModel? {
        val entity = userDao.getById(userId)
        return entity?.let { UserMapper.toUserModel(it) }
    }

    suspend fun getUserByEmail(email: String): UserModel? {
        val entity = userDao.getByEmail(email)
        return entity?.let { UserMapper.toUserModel(it) }
    }

    suspend fun isUserExists(checkUsername: String, checkEmail: String): Boolean {
        val existingUser = userDao.findByUsernameOrEmail(checkUsername.trim(), checkEmail.trim())
        return existingUser != null
    }

    suspend fun login(username: String, password: String): UserModel? {
        val entity = userDao.login(username, password, UserStatus.ACTIVE.name)
        return entity?.let { UserMapper.toUserModel(it) }
    }

    suspend fun addUser(
        email: String,
        fullname: String,
        username: String,
        password: String,
        phone: String?,
        role: UserRole,
        status: UserStatus
    ): Result<Unit> {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Vui lòng nhập đầy đủ thông tin bắt buộc"))
        }

        if (isUserExists(username, email)) {
            return Result.failure(Exception("Username hoặc Email đã tồn tại"))
        }

        val newUser = User(
            email = email.trim(),
            fullname = fullname.trim(),
            username = username.trim(),
            password = password,
            role = role.name,
            status = status.name,
            createdAt = System.currentTimeMillis(),
            phone = phone?.trim()
        )

        userDao.insertUser(newUser)
        return Result.success(Unit)
    }

    suspend fun updateUser(updatedUserModel: UserModel): Result<Unit> {
        val existing = userDao.getById(updatedUserModel.id)
            ?: return Result.failure(Exception("Không tìm thấy người dùng"))

        // Kiểm tra xem Username hoặc Email mới có bị trùng không
        val duplicate = userDao.findDuplicateUser(
            idToExclude = updatedUserModel.id,
            username = updatedUserModel.username.trim(),
            email = updatedUserModel.email.trim()
        )

        if (duplicate != null) {
            return Result.failure(Exception("Username hoặc Email đã tồn tại ở tài khoản khác"))
        }

        val updatedEntity = UserMapper.toUserEntity(updatedUserModel)
        userDao.updateUser(updatedEntity)
        return Result.success(Unit)
    }

    suspend fun deleteUser(id: Long) {
        userDao.deleteById(id)
    }
}