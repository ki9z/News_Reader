package com.example.app_doc_bao.view.component

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Composable
fun ClickableAuthText(
    description: String,
    actionText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {

            withStyle(style = SpanStyle(color = Color.Gray)) {
                append(description)
            }
            withStyle(
                style = SpanStyle(
                    color = Color(0xFF1E88E5),
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(actionText)
            }
        },
        fontSize = 14.sp,
        modifier = modifier.clickable { onClick() }
    )
}