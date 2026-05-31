package com.example.app_doc_bao.view.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.app_doc_bao.view.component.*
import com.example.app_doc_bao.viewmodel.StatisticVM

@Composable
fun StatisticScreen(
    navController: NavHostController,
    viewModel: StatisticVM
) {
    val uiState by viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Thống kê & Báo cáo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )

        // 1. PHẦN TỔNG QUAN (3 Thẻ ngang)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewCard(modifier = Modifier.weight(1f), title = "Người dùng", value = "${uiState.totalUsers}", icon = Icons.Default.People, color = MaterialTheme.colorScheme.primary)
            OverviewCard(modifier = Modifier.weight(1f), title = "Bài viết", value = "${uiState.totalArticles}", icon = Icons.AutoMirrored.Filled.Article, color = MaterialTheme.colorScheme.secondary)
            OverviewCard(modifier = Modifier.weight(1f), title = "Lượt xem", value = "${uiState.totalViews}", icon = Icons.Default.Visibility, color = MaterialTheme.colorScheme.tertiary)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2. TOP BÀI BÁO NHIỀU VIEW NHẤT
        Text("🔥 Top Bài báo nổi bật", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        uiState.topArticleModels.forEachIndexed { index, article ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "#${index + 1}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(article.title, fontWeight = FontWeight.Bold)
                        Text("Tác giả: ${article.author}  •  👁 ${article.views} views", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. THỐNG KÊ NGƯỜI DÙNG
        Text("👥 Phân tích Người dùng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Theo Quyền hạn:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                uiState.usersByRole.forEach { (role, count) ->
                    ProgressRow(label = role, count = count, total = uiState.totalUsers)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Theo Trạng thái:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                uiState.usersByStatus.forEach { (status, count) ->
                    ProgressRow(label = status, count = count, total = uiState.totalUsers)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. THỐNG KÊ BÀI BÁO THEO DANH MỤC
        Text("📂 Tỉ trọng Danh mục", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                uiState.articlesByCategory.forEach { (category, count) ->
                    ProgressRow(label = category, count = count, total = uiState.totalArticles)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}


