package com.example.app_doc_bao.view.screen.manageArticles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.app_doc_bao.data.domain.BlockType
import com.example.app_doc_bao.view.component.AppButton
import com.example.app_doc_bao.viewmodel.ArticleVM

@Composable
fun ArticleDetailScreen(
    navController: NavHostController,
    articleId: Long,
    viewModel: ArticleVM
) {

    val article = viewModel.articleModelList.find { it.id == articleId } ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 30.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. TIÊU ĐỀ CHÍNH
        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. THÔNG TIN METADATA (Nguồn, Ngày tháng, Tác giả)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    text = "Nguồn: ${article.source ?: "N/A"}", // Sử dụng trường source mới
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = article.publishDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "TG: ${article.author ?: "Ẩn danh"}",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            thickness = 0.5.dp,
            color = DividerDefaults.color
        )

        // 4. NỘI DUNG CHI TIẾT TỪ BLOCKS
        article.contentBlocks.forEach { block ->
            when (block.type) {
                BlockType.SAPO -> {
                    Text(
                        text = block.content,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                BlockType.HEADING -> {
                    Text(
                        text = block.content,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                }

                BlockType.TEXT -> {
                    Text(
                        text = block.content,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }

                BlockType.IMAGE -> {
                    Column(modifier = Modifier.padding(bottom = 20.dp).fillMaxWidth()) {
                        AsyncImage(
                            model = block.imageUrl,
                            contentDescription = block.caption,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.FillWidth
                        )
                        if (block.caption.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = block.caption,
                                style = MaterialTheme.typography.labelMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
                else -> {}
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. NÚT QUAY LẠI
        AppButton(
            text = "Quay lại",
            onClick = { navController.popBackStack() }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}