package com.example.app_doc_bao.data.local.relation

import com.example.app_doc_bao.data.local.entity.*
import androidx.room.*
import com.example.app_doc_bao.data.local.entity.User

data class UserWithCategories(
    @Embedded val user: User,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = CategoryUser::class,
            parentColumn = "userId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<Category>
)
