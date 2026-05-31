package com.ui.admin.screen.manageArticles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.data.domain.ArticleStatus
import com.data.remote.dto.BlockJson
import com.ui.admin.component.AppButton
import com.ui.admin.component.AutocompleteDropdown
import com.viewmodel.admin.ArticleVM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditArticleScreen(
    navController: NavHostController,
    articleId: String?,
    viewModel: ArticleVM
) {
    // 1. Gọi nạp chi tiết bài báo từ Database
    LaunchedEffect(articleId) {
        articleId?.let { viewModel.getArticleDetail(it) }
    }

    val article = viewModel.currentArticleDetail

    // 2. KHỞI TẠO STATE (Ban đầu để trống)
    var title by remember { mutableStateOf("") }
    var thumbnail by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ArticleStatus.PUBLISHED) }
    var url by remember { mutableStateOf("") }
    val blocks = remember { mutableStateListOf<BlockJson>() }

    // 3. LẮNG NGHE KHI DATA NẠP XONG THÌ ĐỔ VÀO STATE
    LaunchedEffect(article) {
        article?.let {
            title = it.title
            thumbnail = it.thumbnail ?: ""
            author = it.author?: ""
            source = it.source?: ""
            category = it.category
            status = it.status
            url = it.url

            // Làm sạch và nạp danh sách blocks mới từ DB
            blocks.clear()
            val initialBlocks = it.contentBlocks.map { b ->
                BlockJson(
                    type = b.type.name.lowercase(),
                    data = b.content,
                    url = b.imageUrl,
                    caption = b.caption
                )
            }
            blocks.addAll(initialBlocks)
        }
    }

    // Nếu chưa nạp xong bài viết đúng yêu cầu thì hiện Loading
    if (article == null || article.id != articleId) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
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
                        // Trường nhập URL cho Block Ảnh
                        OutlinedTextField(
                            value = block.url ?: "",
                            onValueChange = { blocks[index] = block.copy(url = it) },
                            label = { Text("Link URL Ảnh") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Trường nhập Chú thích cho Block Ảnh
                        OutlinedTextField(
                            value = block.caption ?: "",
                            onValueChange = { blocks[index] = block.copy(caption = it) },
                            label = { Text("Chú thích ảnh") },
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