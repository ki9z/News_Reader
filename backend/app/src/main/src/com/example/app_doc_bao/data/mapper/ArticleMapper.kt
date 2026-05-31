package com.example.app_doc_bao.data.mapper

import com.example.app_doc_bao.data.local.entity.Block
import com.example.app_doc_bao.data.remote.dto.ArticleJson
import com.example.app_doc_bao.data.remote.dto.BlockJson
import com.example.app_doc_bao.data.local.entity.Article
import com.util.toTimestamp
import com.example.app_doc_bao.data.domain.*
import com.example.app_doc_bao.data.local.AppDatabase
import com.example.app_doc_bao.data.local.relation.ArticleWithDetails
import com.example.app_doc_bao.data.model.ArticleModel
import com.example.app_doc_bao.data.model.BlockModel
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

object ArticleMapper {

    fun toArticleEntity(
        json: ArticleJson,
        categoryId: Long,
        sourceId: Long?
    ): Article {
        return Article(
            remoteId = json.id.trim(),
            title = json.title.trim(),
            thumbnail = json.thumbnail?.trim(),
            author = json.author?.trim()?.takeIf { it.isNotBlank() },
            sourceId = sourceId,
            categoryId = categoryId,
            publishAt = json.time.toTimestamp(),
            createdAt = System.currentTimeMillis(),
            view = 0L,
            status = ArticleStatus.PUBLISHED,
            url = json.url.trim()
        )
    }

    fun toBlockEntities(
        blocks: List<BlockJson>,
        articleId: Long
    ): List<Block> {
        return blocks.mapIndexed { index, b ->
            Block(
                articleId = articleId,
                type = b.type,
                content = b.data?: "",
                imageUrl = b.url,
                caption = b.caption,
                position = index.toLong()
            )
        }
    }
    fun toArticleModel(data: ArticleWithDetails): ArticleModel {

        val contentBlocks = data.blocks.toBlockModels()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val publishDateString = dateFormat.format(Date(data.article.publishAt))

        return ArticleModel(
            id = data.article.id,
            remoteId = data.article.remoteId,
            title = data.article.title,
            thumbnail = data.article.thumbnail,
            category = data.category?.name ?: "Chưa phân loại",
            author = data.article.author?.takeIf { it.isNotBlank() } ?: "Ẩn danh",
            source = data.source?.name ?: "Nguồn khác",
            publishDate = publishDateString,
            status = data.article.status,
            views = data.article.view ?: 0L,
            url = data.article.url,
            contentBlocks = contentBlocks
        )
    }
    /**
     * Chuyển đổi 1 Entity Block thành BlockModel
     */
    fun toBlockModel(entity: Block): BlockModel {
        val blockType = try {
            BlockType.valueOf(entity.type.uppercase())
        } catch (e: IllegalArgumentException) {
            BlockType.UNKNOWN
        }

        return BlockModel(
            id = entity.id,
            type = blockType,
            content = entity.content ?: "",
            imageUrl = entity.imageUrl ?: "",
            caption = entity.caption ?: ""
        )
    }

    //Chuyển đổi Danh sách Entity Block thành Danh sách BlockModel cho UI
    fun List<Block>.toBlockModels(): List<BlockModel> {
        return this
            .sortedBy { it.position ?: 0L } // Sắp xếp lại đúng thứ tự bài báo
            .map { toBlockModel(it) }       // Chuyển từng item sang Model
    }
}
