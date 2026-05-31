package com.example.app_doc_bao.view.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.app_doc_bao.data.model.ArticleModel

@Composable
fun ArticleCard(
    articleModel: ArticleModel,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onView() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Tiêu đề
            Text(
                text = articleModel.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Trái (Thông số) - Phải (Thumbnail)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // CỘT TRÁI: Các thông số chi tiết
                Column(modifier = Modifier.weight(1f)) {
                    InfoText(label = "Danh mục", value = articleModel.category)
                    InfoText(label = "Tác giả", value = articleModel.author ?: "Ẩn danh")
                    InfoText(label = "Nguồn", value = articleModel.source ?: "N/A")
                    InfoText(label = "Ngày đăng", value = articleModel.publishDate)
                    InfoText(label = "Trạng thái", value = articleModel.status.name)
                    InfoText(label = "Lượt xem", value = articleModel.views.toString())
                }

                Spacer(modifier = Modifier.width(12.dp))

                // CỘT PHẢI: Hiển thị Thumbnail
                AsyncImage(
                    model = articleModel.thumbnail,
                    contentDescription = "Thumbnail",
                    modifier = Modifier
                        .size(width = 110.dp, height = 80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Hàng nút bấm
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("Sửa", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onDelete,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("Xóa", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun InfoText(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}