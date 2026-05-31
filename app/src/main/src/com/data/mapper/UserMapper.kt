package com.data.mapper

import com.data.local.entity.UserEntity
import com.data.domain.model.UserModel
import com.data.domain.*

object UserMapper {
    fun toUserModel(entity: UserEntity): UserModel {
        return UserModel(
            id = entity.id,
            email = entity.email ?: "",
            fullname = entity.fullName ?: "",
            username = entity.username ?: "",
            password = entity.passwordHash ?: "",
            phone = entity.phone,
            role = try { UserRole.valueOf(entity.role.uppercase()) } catch (e: Exception) { UserRole.USER },
            status = try { UserStatus.valueOf(entity.status.uppercase()) } catch (e: Exception) { UserStatus.ACTIVE },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isSignedIn = entity.isSignedIn,
            birthday = entity.birthday ?: "",
            location = entity.location ?: "",
            interests = entity.interests ?: ""
        )
    }

    fun toUserEntity(model: UserModel): UserEntity {
        val currentTime = System.currentTimeMillis()
        return UserEntity(
            id = model.id,
            email = model.email.trim(),
            fullName = model.fullname.trim(),
            username = model.username.trim(),
            passwordHash = model.password,
            phone = model.phone?.trim(),
            role = model.role.name.lowercase(),
            status = model.status.name,
            createdAt = model.createdAt.takeIf { it > 0 } ?: currentTime,
            updatedAt = model.updatedAt.takeIf { it > 0 } ?: currentTime,
            isSignedIn = model.isSignedIn,
            birthday = model.birthday,
            location = model.location,
            interests = model.interests
        )
    }
}