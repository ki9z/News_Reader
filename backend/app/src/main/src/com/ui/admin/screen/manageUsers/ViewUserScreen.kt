package com.ui.admin.screen.manageUsers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.data.domain.UserStatus
import com.data.domain.model.UserModel
import com.viewmodel.admin.UserVM
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ViewUserScreen(
    navController: NavHostController,
    userId: String,
    viewModel: UserVM
) {
    var user by remember { mutableStateOf<UserModel?>(null) }

    val isEditingMe = userId == viewModel.currentUserId

    fun loadData() {
        viewModel.getUserById(userId) { fetchedUser ->
            user = fetchedUser
        }
    }

    LaunchedEffect(userId) {
        viewModel.clearError()
        loadData()
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
            .padding(top = 30.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Thông tin tài khoản", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.errorMessage.isNotEmpty()) {
            Text(
                text = viewModel.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // BẢNG HIỂN THỊ THÔNG TIN
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                val dateTimeFormat = remember {
                    SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        Locale.getDefault()
                    )
                }

                InfoRow("Họ và tên:", user!!.fullname)
                InfoRow("Email:", user!!.email)

                InfoRow("Ngày sinh:", user!!.birthday?.ifBlank { "Chưa cập nhật" } ?: "Chưa cập nhật")
                InfoRow("Địa chỉ:", user!!.location?.ifBlank { "Chưa cập nhật" } ?: "Chưa cập nhật")
                InfoRow("Sở thích:", user!!.interests?.ifBlank { "Không có" } ?: "Không có")

                InfoRow("Số điện thoại:", user!!.phone ?: "Chưa cập nhật")

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                InfoRow("Ngày tạo tài khoản:", dateTimeFormat.format(Date(user!!.createdAt)))
                InfoRow("Cập nhật lần cuối:", dateTimeFormat.format(Date(user!!.updatedAt)))

                // Hiển thị trạng thái nổi bật
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Trạng thái hiện tại:", fontWeight = FontWeight.Bold)
                    Text(
                        text = user!!.status.name,
                        fontWeight = FontWeight.Bold,
                        color = if (user!!.status == UserStatus.ACTIVE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // NÚT CHUYỂN ĐỔI TRẠNG THÁI
        if (!isEditingMe) { // Ẩn nút nếu đang tự xem mình
            val isActive = user!!.status == UserStatus.ACTIVE

            Button(
                onClick = {
                    viewModel.toggleUserStatus(user!!.id) {
                        loadData()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isActive) "Khóa tài khoản (INACTIVE)" else "Mở khóa tài khoản (ACTIVE)")
            }
        } else {
            Text(
                text = "Bạn không thể tự thay đổi trạng thái của chính mình.",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Quay lại")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, modifier = Modifier.weight(0.4f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, modifier = Modifier.weight(0.6f), fontWeight = FontWeight.Medium)
    }
}