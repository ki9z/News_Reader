package com.example.app_doc_bao.view.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.app_doc_bao.view.component.*
import com.example.app_doc_bao.viewmodel.auth.ForgotPasswordVM

@Composable
fun ForgotPasswordScreen(
    navController: NavHostController,
    viewModel: ForgotPasswordVM
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Quên mật khẩu",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(30.dp))

        // 1. Ô nhập email
        AppTextField(
            value = viewModel.email,
            onValueChange = {
                viewModel.email = it
                viewModel.errorMessage = ""
            },
            label = "Nhập email của bạn"
        )

        // 2. Hiển thị lỗi
        ErrorMessage(message = viewModel.errorMessage)

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Nút gửi yêu cầu
        AppButton(
            text = "Gửi yêu cầu",
            onClick = {
                viewModel.onSendRequestClick { email ->
                    navController.navigate("otp/$email")
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Chuyển về đăng nhập
        ClickableAuthText(
            description = "Nhớ ra mật khẩu? ",
            actionText = "Đăng nhập",
            onClick = {
                navController.navigate("login") {
                    popUpTo("login") { inclusive = true }
                }
            }
        )
    }
}