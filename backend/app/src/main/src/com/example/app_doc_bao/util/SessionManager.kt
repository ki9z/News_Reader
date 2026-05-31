package com.example.app_doc_bao.util

import com.example.app_doc_bao.data.model.UserModel
import com.example.app_doc_bao.data.domain.UserRole

object SessionManager {
    var currentUserModel: UserModel? = null

    fun isAdmin(): Boolean {
        return currentUserModel?.role == UserRole.ADMIN
    }

    fun logout() {
        currentUserModel = null
    }
}