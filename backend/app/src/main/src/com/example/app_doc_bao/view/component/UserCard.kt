package com.example.app_doc_bao.view.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.app_doc_bao.data.model.UserModel
import com.example.app_doc_bao.data.domain.UserStatus

@Composable
fun UserCard(
    userModel: UserModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = userModel.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Email: ${userModel.email}")
            Text(text = "Quyền: ${userModel.role.name}")
            Text(text = "Trạng thái: ${userModel.status.name}")

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onEdit) {
                    Text("Sửa")
                }

                Button(onClick = onDelete) {
                    Text("Xóa tài khoản")
                }
            }
        }
    }
}