package com.example.app_doc_bao.view.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.example.app_doc_bao.view.component.*
import com.example.app_doc_bao.viewmodel.auth.OtpVM

@Composable
fun OtpScreen(
    navController: NavHostController,
    email: String,
    viewModel: OtpVM
) {

    val focusRequesters = List(6) { FocusRequester() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "XÁC THỰC OTP", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(text = "Mã xác thực gửi đến: $email", fontSize = 14.sp, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(25.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            viewModel.otpValues.forEachIndexed { index, value ->
                BasicTextField(
                    value = value,
                    onValueChange = { newValue ->
                        if (newValue.length > 1) {
                            val chars = newValue.filter { it.isDigit() }
                            chars.forEachIndexed { i, c ->
                                if (index + i < 6) {
                                    viewModel.otpValues[index + i] = c.toString()
                                }
                            }

                            val nextIndex = (index + chars.length).coerceAtMost(5)
                            if (nextIndex < 6) {
                                focusRequesters[nextIndex].requestFocus()
                            }
                        } else {
                            if (newValue.length <= 1 && (newValue.isEmpty() || newValue.all { it.isDigit() })) {
                                viewModel.otpValues[index] = newValue

                                if (newValue.isNotEmpty() && index < 5) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .width(45.dp)
                        .height(60.dp)
                        .focusRequester(focusRequesters[index])
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                                if (viewModel.otpValues[index].isEmpty() && index > 0) {
                                    viewModel.otpValues[index - 1] = ""
                                    focusRequesters[index - 1].requestFocus()
                                    true
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(vertical = 16.dp),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            innerTextField()
                        }
                    }
                )
            }
        }

        ErrorMessage(message = viewModel.message)

        Spacer(modifier = Modifier.height(10.dp))

        if (viewModel.isTimerRunning) {
            Text(text = "Mã hiệu lực trong: ${viewModel.timerValue}s", color = Color.Red, fontSize = 14.sp)
        } else {
            TextButton(onClick = { viewModel.sendOTP(email) }) {
                Text(text = "Gửi mã OTP", fontWeight = FontWeight.Bold, color = Color.Blue)
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        // Nút xác nhận
        AppButton(
            text = "Xác nhận",
            onClick = {
                viewModel.verifyOTP(email) { isValid ->
                    if (isValid) {
                        navController.navigate("reset_password/$email")
                    }
                }
            }
        )
    }
}