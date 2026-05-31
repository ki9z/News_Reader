package com.ui.admin.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.data.domain.model.UserModel

@Composable
fun UserCard(
    userModel: UserModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isDeleteEnabled: Boolean = true
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
                    Text("Xem")
                }

                Button(
                    onClick = onDelete,
                    enabled = isDeleteEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDeleteEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isDeleteEnabled) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Xóa tài khoản")
                }
            }
        }
    }
}