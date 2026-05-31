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
import com.example.app_doc_bao.viewmodel.auth.ChangePasswordVM

@Composable
fun ChangePasswordScreen(
    navController: NavHostController,
    viewModel: ChangePasswordVM
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Đổi mật khẩu",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(30.dp))

        AppTextField(
            value = viewModel.oldPassword,
            onValueChange = { viewModel.oldPassword = it },
            label = "Mật khẩu cũ",
            isPassword = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        AppTextField(
            value = viewModel.newPassword,
            onValueChange = { viewModel.newPassword = it },
            label = "Mật khẩu mới",
            isPassword = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        AppTextField(
            value = viewModel.confirmPassword,
            onValueChange = { viewModel.confirmPassword = it },
            label = "Nhập lại mật khẩu mới",
            isPassword = true
        )

        ErrorMessage(message = viewModel.message)

        Spacer(modifier = Modifier.height(10.dp))

        // Nút xác nhận
        AppButton(
            text = "Xác nhận đổi mật khẩu",
            onClick = {
                viewModel.onChangePasswordClick {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nút quay lại
        TextButton(onClick = { navController.popBackStack() }) {
            Text("Quay lại")
        }
    }
}