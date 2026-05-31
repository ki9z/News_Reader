package com.ui.admin.screen.manageUsers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ui.admin.component.UserCard
import com.viewmodel.admin.UserVM

@Composable
fun ManageUsersScreen(
    navController: NavHostController,
    viewModel: UserVM
) {
    val users = viewModel.getFilteredUsers()

    val roles = listOf("Tất cả", "ADMIN", "USER")
    val statuses = listOf("Tất cả", "ACTIVE", "INACTIVE")

    var roleExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var userIdToDelete by remember { mutableStateOf<String?>(null) }

    val currentUserEmail = viewModel.currentUserEmail
    val currentUserId = viewModel.currentUserId

    LaunchedEffect(Unit) {
        viewModel.clearError()
        viewModel.loadCurrentUserId()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Quản lý người dùng",
                style = MaterialTheme.typography.headlineSmall,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Tìm theo tên / email") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (viewModel.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, "Xóa văn bản")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { roleExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (viewModel.selectedRole == "Tất cả") "Quyền" else viewModel.selectedRole, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        roles.forEach { role ->
                            DropdownMenuItem(text = { Text(role) }, onClick = { viewModel.updateRole(role); roleExpanded = false })
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { statusExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (viewModel.selectedStatus == "Tất cả") "Trạng thái" else viewModel.selectedStatus, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        statuses.forEach { status ->
                            DropdownMenuItem(text = { Text(status) }, onClick = { viewModel.updateStatus(status); statusExpanded = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("add_user") }, modifier = Modifier.fillMaxWidth()) {
                Text("Thêm Admin mới")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.errorMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearError() }) { Text("Đóng", color = MaterialTheme.colorScheme.onErrorContainer) }
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(users, key = { it.id }) { user ->

                    val isMe = (currentUserId != null && user.id == currentUserId) ||
                            (currentUserEmail != null && user.email.trim().equals(currentUserEmail.trim(), ignoreCase = true))

                    UserCard(
                        userModel = user,
                        onEdit = { navController.navigate("view_user/${user.id}") },
                        onDelete = { if (!isMe) userIdToDelete = user.id },
                        isDeleteEnabled = !isMe
                    )
                }
            }
        }

        if (userIdToDelete != null) {
            AlertDialog(
                onDismissRequest = { userIdToDelete = null },
                title = { Text("Xác nhận xóa") },
                text = { Text("Thao tác này không thể hoàn tác.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteUser(userIdToDelete!!); userIdToDelete = null }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { userIdToDelete = null }) { Text("Hủy") } }
            )
        }
    }
}