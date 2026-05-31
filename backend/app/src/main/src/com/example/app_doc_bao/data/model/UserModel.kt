package com.example.app_doc_bao.data.model

import com.example.app_doc_bao.data.domain.*

data class UserModel(
    val id: Long,
    var email: String,
    var fullname: String,
    var username: String,
    var password: String,
    var phone: String?,
    var role: UserRole = UserRole.USER,
    var status: UserStatus = UserStatus.ACTIVE,
    var createdAt: Long
)