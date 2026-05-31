package com.example.app_doc_bao.data.local.relation

import com.example.app_doc_bao.data.local.entity.*
import androidx.room.*
data class UserWithSource(
    @Embedded val user: User,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SourceUser::class,
            parentColumn = "userId",
            entityColumn = "sourceId"
        )
    )
    val sources: List<Source>
)
