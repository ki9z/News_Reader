package com.example.app_doc_bao.view.screen.manageUsers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.app_doc_bao.data.domain.*
import com.example.app_doc_bao.viewmodel.UserVM

@Composable
fun AddAdminScreen(
    navController: NavHostController,
    viewModel: UserVM
) {
    var fullname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val role = UserRole.ADMIN
    var status by remember { mutableStateOf(UserStatus.ACTIVE) }

    val statuses = UserStatus.entries
    var statusExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.clearError()
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
        Text("Thêm Admin mới", style = MaterialTheme.typography.headlineSmall)

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
            label = { Text("Số điện thoại (Không bắt buộc)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu khởi tạo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box {
            OutlinedButton(onClick = { statusExpanded = true }) {
                Text("Trạng thái: ${status.name}")
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
                    viewModel.addUser(
                        fullname = fullname,
                        username = username,
                        email = email,
                        password = password,
                        phone = phone.ifBlank { null },
                        role = role,
                        status = status,
                        onSuccess = { navController.popBackStack() }
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Lưu tài khoản")
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}