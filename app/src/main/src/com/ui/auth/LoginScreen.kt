package com.ui.auth

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.ui.admin.component.*
import com.ui.main.MainActivity
import com.viewmodel.auth.LoginVM

@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: LoginVM
) {
    val context = LocalContext.current

    fun resolveGoogleWebClientId(): String {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id != 0) context.getString(id) else ""
    }

    fun openMainActivity() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
        (context as? Activity)?.finish()
    }

    val googleSignInClient = remember {
        val webClientId = resolveGoogleWebClientId()
        val optionsBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        if (webClientId.isNotBlank()) {
            optionsBuilder.requestIdToken(webClientId)
        }
        GoogleSignIn.getClient(context, optionsBuilder.build())
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val googleIdToken = account.idToken
            if (googleIdToken.isNullOrBlank()) {
                viewModel.errorMessage = "Thiếu Google ID token. Kiểm tra google-services.json và cấu hình Firebase."
                return@rememberLauncherForActivityResult
            }

            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnSuccessListener { authResult ->
                    authResult.user?.getIdToken(true)?.addOnSuccessListener { tokenResult ->
                        viewModel.onGoogleLoginSuccess(
                            idToken = tokenResult.token,
                            email = authResult.user?.email.orEmpty(),
                            name = authResult.user?.displayName,
                            onSuccess = { openMainActivity() }
                        )
                    }?.addOnFailureListener { error ->
                        viewModel.errorMessage = "Không lấy được Firebase token: ${error.message}"
                    }
                }
                .addOnFailureListener { error ->
                    viewModel.errorMessage = "Đăng nhập Google thất bại: ${error.message}"
                }
        } catch (e: ApiException) {
            viewModel.errorMessage = when (e.statusCode) {
                10 -> "Đăng nhập Google bị lỗi cấu hình OAuth (status 10). Kiểm tra SHA-1 của app trong Firebase và package name."
                else -> "Đăng nhập Google bị hủy hoặc lỗi: ${e.statusCode}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Đăng nhập", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(30.dp))

        AppTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = "Email"
        )

        Spacer(modifier = Modifier.height(16.dp))

        AppTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = "Mật khẩu",
            isPassword = true
        )

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

        ErrorMessage(message = viewModel.errorMessage)

        Spacer(modifier = Modifier.height(10.dp))

        AppButton(
            text = "Đăng nhập",
            onClick = {
                viewModel.onLoginClick { isAdmin ->
                    if (isAdmin) {
                        navController.navigate("admin_home") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        openMainActivity()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Hoặc", color = Color.Gray, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = resolveGoogleWebClientId().isNotBlank(),
            onClick = {
                val webClientId = resolveGoogleWebClientId()
                if (webClientId.isBlank()) {
                    viewModel.errorMessage = "Chưa có default_web_client_id từ google-services.json"
                } else {
                    googleLauncher.launch(googleSignInClient.signInIntent)
                }
            }
        ) {
            Text(text = "Tiếp tục với Google")
        }

        Spacer(modifier = Modifier.height(16.dp))

        ClickableAuthText(
            description = "Bạn chưa có tài khoản? ",
            actionText = "Đăng ký",
            onClick = { navController.navigate("register") }
        )
    }
}
