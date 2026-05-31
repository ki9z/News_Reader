package com.example.app_doc_bao.view.screen.manageArticles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.app_doc_bao.data.domain.ArticleStatus
import com.example.app_doc_bao.view.component.ArticleCard
import com.example.app_doc_bao.view.component.AppButton
import com.example.app_doc_bao.viewmodel.ArticleVM

@Composable
fun ManageArticlesScreen(
    navController: NavHostController,
    viewModel: ArticleVM
) {

    val articles by remember {
        derivedStateOf { viewModel.getFilteredArticles() }
    }

    // 2. Lấy danh sách Nguồn
    val sources = remember(viewModel.articleModelList) {
        listOf("Tất cả") + viewModel.articleModelList
            .mapNotNull { it.source }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val categories = viewModel.categories
    val statuses = listOf("Tất cả") + ArticleStatus.entries.map { it.name }
    var articleIdToDelete by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 30.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Quản lý bài báo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        // Thanh tìm kiếm
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("Tìm tiêu đề, tác giả...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                if (viewModel.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Xóa")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Hàng bộ lọc
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterDropdown(Modifier.weight(1f), "Lĩnh vực", viewModel.selectedCategory, categories) { viewModel.updateCategory(it) }
            FilterDropdown(Modifier.weight(1f), "Nguồn", viewModel.selectedSource, sources) { viewModel.updateSource(it) }
            FilterDropdown(Modifier.weight(1f), "Trạng thái", viewModel.selectedStatus, statuses) { viewModel.updateStatus(it) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AppButton(
            text = "Thêm bài báo mới",
            onClick = { navController.navigate("add_article") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Danh sách bài báo
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(articles, key = { it.id }) { article ->
                ArticleCard(
                    articleModel = article,
                    onView = { navController.navigate("article_detail/${article.id}") },
                    onEdit = { navController.navigate("edit_article/${article.id}") },
                    onDelete = { articleIdToDelete = article.id }
                )
            }
        }
    }

    // Dialog xóa
    if (articleIdToDelete != null) {
        AlertDialog(
            onDismissRequest = { articleIdToDelete = null },
            title = { Text("Xác nhận xóa") },
            text = { Text("Thao tác này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteArticle(articleIdToDelete!!)
                    articleIdToDelete = null
                }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { articleIdToDelete = null }) { Text("Hủy") } }
        )
    }
}

@Composable
fun FilterDropdown(modifier: Modifier, label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Text(text = if (selected == "Tất cả") label else selected, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}