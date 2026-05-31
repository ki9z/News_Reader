package com.example.app_doc_bao.view.screen.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.app_doc_bao.data.domain.UserRole
import com.example.app_doc_bao.view.component.*
import com.example.app_doc_bao.viewmodel.auth.LoginVM

@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: LoginVM
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Đăng nhập", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(30.dp))

        // Ô nhập tên tài khoản
        AppTextField(
            value = viewModel.username,
            onValueChange = { viewModel.username = it },
            label = "Tên đăng nhập"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Ô nhập mật khẩu
        AppTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = "Mật khẩu",
            isPassword = true
        )

        // Ghi nhớ & Quên mật khẩu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = viewModel.rememberMe,
                    onCheckedChange = { viewModel.rememberMe = it }
                )
                Text(text = "Nhớ mật khẩu", fontSize = 14.sp)
            }

            Text(
                text = "Quên mật khẩu?",
                fontSize = 14.sp,
                color = Color.Blue,
                modifier = Modifier.clickable {
                    navController.navigate("forgot_password")
                }
            )
        }

        // 3. Hiển thị thông báo lỗi
        ErrorMessage(message = viewModel.errorMessage)

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Nút Đăng nhập
        AppButton(
            text = "Đăng nhập",
            onClick = {
                viewModel.onLoginClick { user ->
                    if (user.role == UserRole.ADMIN) {
                        navController.navigate("admin_home") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        viewModel.errorMessage =
                            "Đăng nhập USER thành công. Màn hình Home đang phát triển!"
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ClickableAuthText(
            description = "Bạn chưa có tài khoản? ",
            actionText = "Đăng ký",
            onClick = { navController.navigate("register") }
        )
    }
}