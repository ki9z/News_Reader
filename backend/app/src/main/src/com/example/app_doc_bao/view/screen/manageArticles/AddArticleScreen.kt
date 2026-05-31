package com.example.app_doc_bao.view.screen.manageArticles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.app_doc_bao.data.domain.ArticleStatus
import com.example.app_doc_bao.data.remote.dto.BlockJson
import com.example.app_doc_bao.view.component.AppButton
import com.example.app_doc_bao.viewmodel.ArticleVM
import com.example.app_doc_bao.view.component.AutocompleteDropdown
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddArticleScreen(
    navController: NavHostController,
    viewModel: ArticleVM
) {
    var title by remember { mutableStateOf("") }
    var thumbnail by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var articleUrl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ArticleStatus.DRAFT) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val timeString by remember { mutableStateOf(dateFormat.format(Date())) }

    val blocks = remember { mutableStateListOf<BlockJson>() }

    val categories = viewModel.categories.filter { it != "Tất cả" }

    val sources = viewModel.articleModelList.mapNotNull { it.source }.distinct()
    val statuses = ArticleStatus.entries
    var statusExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.clearError()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 30.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Thêm bài báo mới",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.errorMessage.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    text = viewModel.errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
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
            label = { Text("Link ảnh đại diện") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://example.com/image.jpg") }
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = articleUrl,
            onValueChange = { articleUrl = it },
            label = { Text("Link bài báo gốc (URL)") },
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
                OutlinedButton(
                    onClick = { statusExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text("TT: ${status.name}")
                }
                DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    statuses.forEach {
                        DropdownMenuItem(
                            text = { Text(it.name) },
                            onClick = { status = it; statusExpanded = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- PHẦN 2: QUẢN LÝ CÁC KHỐI NỘI DUNG (BLOCKS) ---
        Text("Nội dung bài báo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        blocks.forEachIndexed { index, block ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Khối ${index + 1}: ${block.type.uppercase()}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { blocks.removeAt(index) }) {
                            Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    if (block.type == "sapo" || block.type == "text" || block.type == "heading") {
                        OutlinedTextField(
                            value = block.data ?: "",
                            onValueChange = { blocks[index] = block.copy(data = it) },
                            label = {
                                Text(when(block.type){
                                    "sapo" -> "Đoạn mở đầu"
                                    "heading" -> "Tiêu đề phụ"
                                    else -> "Nội dung đoạn văn"
                                })
                            },
                            textStyle = if (block.type == "heading")
                                LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)
                            else LocalTextStyle.current,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (block.type == "image") {
                        OutlinedTextField(
                            value = block.data ?: "",
                            onValueChange = { blocks[index] = block.copy(data = it) },
                            label = { Text("Link URL Ảnh") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = block.caption ?: "",
                            onValueChange = { blocks[index] = block.copy(caption = it) },
                            label = { Text("Chú thích ảnh (Caption)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hàng nút thêm loại khối
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = { blocks.add(BlockJson(type = "sapo")) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("+Sapo", fontSize = 12.sp) }
            Button(onClick = { blocks.add(BlockJson(type = "heading")) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("+Head", fontSize = 12.sp) }
            Button(onClick = { blocks.add(BlockJson(type = "text")) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("+Text", fontSize = 12.sp) }
            Button(onClick = { blocks.add(BlockJson(type = "image")) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("+Ảnh", fontSize = 12.sp) }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- PHẦN 3: LƯU & HỦY ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Nút Hủy
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Hủy", fontSize = 16.sp)
            }

            // NÚT LƯU
            AppButton(
                text = "Lưu bài báo",
                onClick = {
                    viewModel.addArticle(
                        title = title,
                        thumbnail = thumbnail,
                        author = author,
                        source = source,
                        category = category,
                        timeString = timeString,
                        url = articleUrl,
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