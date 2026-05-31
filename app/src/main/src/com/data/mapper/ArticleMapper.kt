package com.data.mapper

import com.data.local.entity.ArticleBlockEntity
import com.data.local.entity.ArticleEntity
import com.data.remote.dto.ArticleJson
import com.data.remote.dto.BlockJson
import com.data.domain.ArticleStatus
import com.data.domain.BlockType
import com.data.domain.model.ArticleModel
import com.data.domain.model.BlockModel

object ArticleMapper {

    fun toArticleEntity(
        json: ArticleJson,
        categoryId: String,
        sourceId: String?
    ): ArticleEntity {

        val descriptionText = json.blocks.firstOrNull { it.type == "sapo" }?.data

        return ArticleEntity(
            url = json.url.trim(),
            title = json.title.trim(),
            urlToImage = json.thumbnail?.trim(),
            author = json.author?.trim()?.takeIf { it.isNotBlank() } ?: "Ẩn danh",
            sourceId = sourceId,
            sourceName = json.source,
            categoryId = categoryId,
            publishedAt = json.time,
            createdAt = System.currentTimeMillis(),
            view = 0L,
            status = "PUBLISHED",
            city = null,
            content = null, // Vẫn để null vì nội dung chi tiết đã lưu ở bảng ArticleBlockEntity
            description = descriptionText // Gắn đoạn sapo vào đây
        )
    }

    /**
     * Chuyển đổi danh sách Block từ JSON sang ArticleBlockEntity
     */
    fun toBlockEntities(
        blocks: List<BlockJson>,
        articleUrl: String // Sử dụng URL làm sợi dây liên kết
    ): List<ArticleBlockEntity> {
        return blocks.mapIndexed { index, b ->
            ArticleBlockEntity(
                articleUrl = articleUrl,
                type = b.type,
                content = b.data ?: "",
                imageUrl = b.url,
                caption = b.caption,
                blockOrder = index, // Đổi từ position sang blockOrder
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * Chuyển đổi dữ liệu từ Database (kèm chi tiết) sang Model cho UI
     * Lưu ý: Bạn cần cập nhật class ArticleWithBlocks trong phần Relation của đồng nghiệp
     */
    fun toArticleModel(
        article: ArticleEntity,
        blocks: List<ArticleBlockEntity>,
        categoryName: String?
    ): ArticleModel {

        val contentBlocks = blocks.toBlockModels()

        return ArticleModel(
            id = article.url,
            remoteId = article.url,
            title = article.title ?: "",
            thumbnail = article.urlToImage,
            category = categoryName ?: "Chưa phân loại",
            author = article.author ?: "Ẩn danh",
            source = article.sourceName ?: "Nguồn khác",
            publishDate = article.publishedAt ?: "",
            status = try {
                ArticleStatus.valueOf(article.status.uppercase())
            } catch (e: Exception) {
                ArticleStatus.PUBLISHED
            },
            views = article.view ?: 0L,
            url = article.url,
            contentBlocks = contentBlocks
        )
    }

    /**
     * Chuyển đổi Entity Block sang BlockModel hiển thị trên UI
     */
    fun toBlockModel(entity: ArticleBlockEntity): BlockModel {
        val blockType = try {
            BlockType.valueOf(entity.type.uppercase())
        } catch (e: Exception) {
            BlockType.UNKNOWN
        }

        return BlockModel(
            id = entity.id,
            type = blockType, // Truyền Enum đã convert
            content = entity.content ?: "",
            imageUrl = entity.imageUrl ?: "",
            caption = entity.caption ?: ""
        )
    }

    /**
     * Extension function để chuyển danh sách Block Entity sang Model
     */
    fun List<ArticleBlockEntity>.toBlockModels(): List<BlockModel> {
        return this
            .sortedBy { it.blockOrder } // Sắp xếp theo blockOrder mới
            .map { toBlockModel(it) }
    }
}