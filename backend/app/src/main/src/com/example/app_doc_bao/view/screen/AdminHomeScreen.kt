package com.example.app_doc_bao.view.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.app_doc_bao.viewmodel.AdminHomeVM
import com.example.app_doc_bao.view.component.OverviewCard

@Composable
fun AdminHomeScreen(
    navController: NavHostController,
    viewModel: AdminHomeVM = viewModel()
) {
    val uiState by viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.loadAdminInfo()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 30.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Hệ thống quản lý",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Xin chào\n${uiState.fullname}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { navController.navigate("change_password") }
            ) {
                Text("Sửa mật khẩu")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Thông tin hệ thống",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // HÀNG 1: 3 Thẻ thống kê tổng quan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewCard(
                modifier = Modifier.weight(1f),
                title = "Bài báo",
                value = uiState.totalArticles,
                icon = Icons.AutoMirrored.Filled.Article,
                color = MaterialTheme.colorScheme.secondary
            )
            OverviewCard(
                modifier = Modifier.weight(1f),
                title = "Người dùng",
                value = uiState.totalUsers,
                icon = Icons.Default.People,
                color = MaterialTheme.colorScheme.primary
            )
            OverviewCard(
                modifier = Modifier.weight(1f),
                title = "Lượt xem",
                value = uiState.totalViews,
                icon = Icons.Default.Visibility,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // HÀNG 2: 2 Thẻ thống kê trong ngày
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewCard(
                modifier = Modifier.weight(1f),
                title = "Bài hôm nay",
                value = uiState.todayArticles,
                icon = Icons.Default.Today,
                color = MaterialTheme.colorScheme.secondary
            )
            OverviewCard(
                modifier = Modifier.weight(1f),
                title = "User mới",
                value = uiState.newUsers,
                icon = Icons.Default.PersonAdd,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = { navController.navigate("manage_articles") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Quản lý bài báo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("manage_users") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Quản lý người dùng")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("statistics") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Thống kê chi tiết")
        }

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedButton(
            onClick = {
                viewModel.logout(
                    onLogoutSuccess = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Đăng xuất")
        }
    }
}