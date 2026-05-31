package com.example.app_doc_bao.view.screen.manageArticles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.app_doc_bao.data.domain.ArticleStatus
import com.example.app_doc_bao.data.remote.dto.BlockJson
import com.example.app_doc_bao.view.component.AppButton
import com.example.app_doc_bao.view.component.AutocompleteDropdown
import com.example.app_doc_bao.viewmodel.ArticleVM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditArticleScreen(
    navController: NavHostController,
    articleId: Long,
    viewModel: ArticleVM
) {
    // 1. TÌM BÀI BÁO
    val article = viewModel.articleModelList.find { it.id == articleId }

    if (article == null) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    // 2. KHỞI TẠO STATE
    var title by remember { mutableStateOf(article.title) }
    var thumbnail by remember { mutableStateOf(article.thumbnail ?: "") }
    var author by remember { mutableStateOf(article.author ?: "") }
    var source by remember { mutableStateOf(article.source ?: "") }
    var category by remember { mutableStateOf(article.category) }
    var status by remember { mutableStateOf(article.status) }
    var url by remember { mutableStateOf(article.url) }

    // 3. CHUYỂN ĐỔI BLOCKS
    val blocks = remember {
        val initialBlocks = article.contentBlocks.map { blockModel ->
            BlockJson(
                type = blockModel.type.name.lowercase(),
                data = blockModel.content,
                url = blockModel.imageUrl,
                caption = blockModel.caption
            )
        }
        mutableStateListOf(*initialBlocks.toTypedArray())
    }

    val categories = viewModel.categories.filter { it != "Tất cả" }
    val sources = viewModel.articleModelList.mapNotNull { it.source }.distinct()
    val statuses = ArticleStatus.entries
    var statusExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.clearError() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 30.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Sửa bài báo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Hiển thị lỗi từ ViewModel
        if (viewModel.errorMessage.isNotEmpty()) {
            Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        // --- PHẦN 1: THÔNG TIN CHUNG ---
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Tiêu đề bài báo") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = thumbnail,
            onValueChange = { thumbnail = it },
            label = { Text("Link ảnh đại diện (Thumbnail)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Tác giả") },
                modifier = Modifier.weight(1f)
            )

            AutocompleteDropdown(
                label = "Nguồn",
                value = source,
                onValueChange = { source = it },
                suggestions = sources,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AutocompleteDropdown(
                label = "Danh mục",
                value = category,
                onValueChange = { category = it },
                suggestions = categories,
                modifier = Modifier.weight(1f)
            )

            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { statusExpanded = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("TT: ${status.name}")
                }
                DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    statuses.forEach {
                        DropdownMenuItem(text = { Text(it.name) }, onClick = { status = it; statusExpanded = false })
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // --- PHẦN 2: BLOCKS ---
        Text("Nội dung bài báo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        blocks.forEachIndexed { index, block ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Khối ${index + 1}: ${block.type.uppercase()}", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { blocks.removeAt(index) }) {
                            Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    if (block.type != "image") {
                        OutlinedTextField(
                            value = block.data ?: "",
                            onValueChange = { blocks[index] = block.copy(data = it) },
                            label = { Text(if (block.type == "sapo") "Sapo" else if(block.type == "heading") "Heading" else "Đoạn văn") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = block.data ?: "",
                            onValueChange = { blocks[index] = block.copy(data = it) },
                            label = { Text("Link URL Ảnh") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = { blocks.add(BlockJson(type = "sapo")) }, modifier = Modifier.weight(1f)) { Text("+Sapo", fontSize = 10.sp) }
            Button(onClick = { blocks.add(BlockJson(type = "heading")) }, modifier = Modifier.weight(1f)) { Text("+Head", fontSize = 10.sp) }
            Button(onClick = { blocks.add(BlockJson(type = "text")) }, modifier = Modifier.weight(1f)) { Text("+Text", fontSize = 10.sp) }
            Button(onClick = { blocks.add(BlockJson(type = "image")) }, modifier = Modifier.weight(1f)) { Text("+Ảnh", fontSize = 10.sp) }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- PHẦN 3: NÚT CẬP NHẬT ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f).height(50.dp)) {
                Text("Hủy")
            }
            AppButton(
                text = "Cập nhật",
                onClick = {
                    viewModel.updateArticle(
                        id = article.id,
                        remoteId = article.remoteId,
                        title = title,
                        thumbnail = thumbnail,
                        author = author,
                        source = source,
                        category = category,
                        timeString = article.publishDate,
                        url = url,
                        blocks = blocks.toList(),
                        status = status,
                        onSuccess = { navController.popBackStack() }
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(50.dp))
    }
}