package com.data.repository

import com.data.domain.UserRole
import com.data.domain.UserStatus
import com.data.local.db.AppDatabase
import com.data.mapper.UserMapper
import com.data.domain.model.UserModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class UserRepository(
    db: AppDatabase
) {

    private val userDao = db.userDao()

    fun getAllUsersFlow(): Flow<List<UserModel>> {
        return userDao.observeAllUsersForAdmin().map { entities ->
            entities.map { UserMapper.toUserModel(it) }
        }
    }

    suspend fun getAllUsers(): List<UserModel> {
        return userDao.observeAllUsersForAdmin().first().map { UserMapper.toUserModel(it) }
    }

    suspend fun getUserById(userId: String): UserModel? {
        val entity = userDao.getById(userId)
        return entity?.let { UserMapper.toUserModel(it) }
    }

    suspend fun getUserByEmail(email: String): UserModel? {
        val entity = userDao.getByEmail(email)
        return entity?.let { UserMapper.toUserModel(it) }
    }

    suspend fun isUserExists(checkEmail: String): Boolean {
        // Đồng nghiệp chỉ dùng Email làm định danh chính, nên ta check Email
        val existingUser = userDao.getByEmail(checkEmail.trim())
        return existingUser != null
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

        if (isUserExists( email)) {
            return Result.failure(Exception("Email đã tồn tại trên hệ thống"))
        }

        // Tạo UserModel với ID chuỗi (UUID)
        val newUserModel = UserModel(
            id = UUID.randomUUID().toString(),
            email = email.trim(),
            fullname = fullname.trim(),
            username = username.trim(),
            password = password.hashCode().toString(),
            phone = phone?.trim(),
            role = role,
            status = status,
            createdAt = System.currentTimeMillis()
        )

        val newEntity = UserMapper.toUserEntity(newUserModel)

        userDao.upsert(newEntity)
        return Result.success(Unit)
    }

    suspend fun updateUser(updatedUserModel: UserModel): Result<Unit> {
        val existing = userDao.getById(updatedUserModel.id)
            ?: return Result.failure(Exception("Không tìm thấy người dùng"))

        val duplicateUser = userDao.getByEmail(updatedUserModel.email.trim())
        if (duplicateUser != null && duplicateUser.id != updatedUserModel.id) {
            return Result.failure(Exception("Email đã tồn tại ở tài khoản khác"))
        }

        val updatedEntity = UserMapper.toUserEntity(updatedUserModel)

        val finalEntityToSave = updatedEntity.copy(
            createdAt = existing.createdAt,
            isSignedIn = existing.isSignedIn,
            role = existing.role
        )

        userDao.upsert(finalEntityToSave)
        return Result.success(Unit)
    }

    suspend fun deleteUser(id: String) {
        userDao.deleteUserById(id)
    }
}