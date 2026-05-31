package com.example.app_doc_bao.view.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.app_doc_bao.view.component.*
import com.example.app_doc_bao.viewmodel.auth.RegisterVM

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterVM
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Đăng ký",
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        // 1. Ô nhập Tên tài khoản
        AppTextField(
            value = viewModel.username,
            onValueChange = { viewModel.username = it },
            label = "Tên tài khoản"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Ô nhập Email
        AppTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = "Email"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Ô nhập Mật khẩu
        AppTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = "Mật khẩu",
            isPassword = true
        )

        // 4. Hiển thị lỗi
        ErrorMessage(message = viewModel.errorMessage)

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Nút Đăng ký
        AppButton(
            text = "Đăng ký",
            onClick = {
                viewModel.onRegisterClick {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Chuyển sang Đăng nhập
        ClickableAuthText(
            description = "Bạn đã có tài khoản? ",
            actionText = "Đăng nhập",
            onClick = {
                navController.navigate("login")
            }
        )
    }
}