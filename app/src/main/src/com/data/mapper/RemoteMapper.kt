package com.data.mapper

import com.data.model.Article

object RemoteMapper {
    fun Article.toClean(): Article = this.copy(
        title = title?.takeIf { it.isNotBlank() },
        description = description?.takeIf { it.isNotBlank() }
    )
}
