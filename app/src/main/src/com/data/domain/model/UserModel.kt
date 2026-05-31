package com.data.domain.model

import com.data.domain.UserRole
import com.data.domain.UserStatus

data class UserModel(
    val id: String,
    var email: String,
    var fullname: String,
    var username: String,
    var password: String,
    var phone: String?,
    var role: UserRole = UserRole.USER,
    var status: UserStatus = UserStatus.ACTIVE,
    var createdAt: Long,
    var updatedAt: Long = 0L,
    var isSignedIn: Boolean = false,
    var birthday: String?="",
    var location: String? = "",
    var interests: String? = ""
)