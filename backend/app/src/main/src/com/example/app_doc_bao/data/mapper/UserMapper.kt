package com.example.app_doc_bao.data.mapper

import com.example.app_doc_bao.data.domain.UserRole
import com.example.app_doc_bao.data.domain.UserStatus
import com.example.app_doc_bao.data.local.entity.User
import com.example.app_doc_bao.data.model.UserModel

object UserMapper {

    /**
     * Chuyển đổi từ Entity lên Model
     */
    fun toUserModel(entity: User): UserModel {
        val userRole = try {
            UserRole.valueOf(entity.role.uppercase())
        } catch (e: IllegalArgumentException) {
            UserRole.USER //
        }

        val userStatus = try {
            UserStatus.valueOf(entity.status.uppercase())
        } catch (e: IllegalArgumentException) {
            UserStatus.ACTIVE //
        }

        return UserModel(
            id = entity.id,
            email = entity.email,
            fullname = entity.fullname,
            username = entity.username,
            password = entity.password,
            phone = entity.phone,
            role = userRole,
            status = userStatus,
            createdAt = entity.createdAt
        )
    }

    /**
     * Chuyển đổi từ Model (UI) xuống Entity để lưu vào DB
     */
    fun toUserEntity(model: UserModel): User {
        return User(
            id = model.id,
            email = model.email.trim(),
            fullname = model.fullname.trim(),
            username = model.username.trim(),
            password = model.password,
            role = model.role.name,
            status = model.status.name,
            createdAt = model.createdAt,
            phone = model.phone?.trim()
        )
    }

    /**
     * Hàm mở rộng tiện lợi: Dịch cả một danh sách Entity sang Model
     */
    fun List<User>.toUserModels(): List<UserModel> {
        return this.map { toUserModel(it) }
    }
}