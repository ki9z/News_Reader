package com.example.app_doc_bao.view.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.app_doc_bao.view.component.*
import com.example.app_doc_bao.viewmodel.auth.ResetPasswordVM

@Composable
fun ResetPasswordScreen(
    navController: NavHostController,
    email: String?,
    viewModel: ResetPasswordVM
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Đặt lại mật khẩu",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Ô nhập mật khẩu mới
        AppTextField(
            value = viewModel.newPassword,
            onValueChange = { viewModel.newPassword = it },
            label = "Mật khẩu mới",
            isPassword = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Ô nhập lại mật khẩu
        AppTextField(
            value = viewModel.confirmPassword,
            onValueChange = { viewModel.confirmPassword = it },
            label = "Nhập lại mật khẩu",
            isPassword = true
        )

        ErrorMessage(message = viewModel.message)

        Spacer(modifier = Modifier.height(10.dp))

        AppButton(
            text = "Đổi mật khẩu",
            onClick = {
                viewModel.onResetPasswordClick(email) {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { navController.popBackStack() }) {
            Text("Quay lại")
        }
    }
}