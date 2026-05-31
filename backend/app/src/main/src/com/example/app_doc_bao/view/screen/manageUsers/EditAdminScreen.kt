package com.example.app_doc_bao.view.screen.manageUsers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.app_doc_bao.data.domain.*
import com.example.app_doc_bao.data.model.UserModel
import com.example.app_doc_bao.viewmodel.UserVM

@Composable
fun EditAdminScreen(
    navController: NavHostController,
    userId: Long,
    viewModel: UserVM
) {
    var user by remember { mutableStateOf<UserModel?>(null) }

    var fullname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.USER) }
    var status by remember { mutableStateOf(UserStatus.ACTIVE) }

    val roles = UserRole.entries
    val statuses = UserStatus.entries

    var roleExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.clearError()
        viewModel.getUserById(userId) { fetchedUser ->
            user = fetchedUser
            fetchedUser?.let {
                fullname = it.fullname
                username = it.username
                email = it.email
                phone = it.phone ?: ""
                role = it.role
                status = it.status
            }
        }
    }

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = 30.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sửa tài khoản", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.errorMessage.isNotEmpty()) {
            Text(
                text = viewModel.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = fullname,
            onValueChange = { fullname = it },
            label = { Text("Họ và tên") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Tên đăng nhập (Username)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email đăng nhập") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Số điện thoại") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Menu chọn quyền
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { roleExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Quyền: ${role.name}")
                }

                DropdownMenu(
                    expanded = roleExpanded,
                    onDismissRequest = { roleExpanded = false }
                ) {
                    roles.forEach {
                        DropdownMenuItem(
                            text = { Text(it.name) },
                            onClick = {
                                role = it
                                roleExpanded = false
                            }
                        )
                    }
                }
            }

            // Menu chọn trạng thái
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { statusExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Status: ${status.name}")
                }

                DropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    statuses.forEach {
                        DropdownMenuItem(
                            text = { Text(it.name) },
                            onClick = {
                                status = it
                                statusExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Hủy")
            }

            Button(
                onClick = {
                    viewModel.updateUser(
                        id = user!!.id,
                        fullname = fullname,
                        username = username,
                        email = email,
                        phone = phone.ifBlank { null },
                        role = role,
                        status = status,
                        onSuccess = { navController.popBackStack() }
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Cập nhật")
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}