package com.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.ui.admin.screen.*
import com.data.local.db.AppDatabase
import com.data.repository.ArticleRepository
import com.data.repository.EmailOtpRepository
import com.data.repository.UserRepository
import com.data.repository.AuthRepository
import com.data.security.TokenManager
import com.ui.auth.ChangePasswordScreen
import com.ui.auth.ForgotPasswordScreen
import com.ui.auth.LoginScreen
import com.ui.auth.OtpScreen
import com.ui.auth.RegisterScreen
import com.ui.auth.ResetPasswordScreen
import com.ui.admin.screen.manageArticles.*
import com.ui.admin.screen.manageUsers.*
import com.util.SecureStorage
import com.viewmodel.admin.AdminHomeVM
import com.viewmodel.admin.ArticleVM
import com.viewmodel.admin.StatisticVM
import com.viewmodel.admin.UserVM
import com.viewmodel.auth.ChangePasswordVM
import com.viewmodel.auth.ForgotPasswordVM
import com.viewmodel.auth.LoginVM
import com.viewmodel.auth.OtpVM
import com.viewmodel.auth.RegisterVM
import com.viewmodel.auth.ResetPasswordVM
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AdminNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // 1. TẠO FACTORY CHUNG ĐỂ QUẢN LÝ REPOSITORY
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(context)
            val articleRepo = ArticleRepository(db)
            val userRepo = UserRepository(db)
            val otpRepo = EmailOtpRepository()

            val tokenManager = TokenManager(context)
            val authRepo = AuthRepository(tokenManager, db.userDao())

            val app = context.applicationContext as com.NewsApp
            val userSettingsRepo = app.userSettingsRepository
            val secureStorage = SecureStorage(context)


            return when {
                modelClass.isAssignableFrom(ArticleVM::class.java) -> ArticleVM(articleRepo) as T
                modelClass.isAssignableFrom(StatisticVM::class.java) -> StatisticVM(
                    articleRepo,
                    userRepo
                ) as T
                modelClass.isAssignableFrom(AdminHomeVM::class.java) -> AdminHomeVM(
                    articleRepo,
                    userRepo,
                    authRepo
                ) as T
                modelClass.isAssignableFrom(UserVM::class.java) -> UserVM(
                    userRepo,
                    authRepo
                ) as T

                modelClass.isAssignableFrom(LoginVM::class.java) -> LoginVM(
                    authRepo,
                    userSettingsRepo,
                    secureStorage
                ) as T

                modelClass.isAssignableFrom(RegisterVM::class.java) -> RegisterVM(authRepo) as T

                modelClass.isAssignableFrom(ChangePasswordVM::class.java) -> ChangePasswordVM(
                    authRepo,
                    userRepo
                ) as T

                modelClass.isAssignableFrom(ForgotPasswordVM::class.java) -> ForgotPasswordVM(
                    userRepo
                ) as T
                modelClass.isAssignableFrom(OtpVM::class.java) -> OtpVM(otpRepo) as T
                modelClass.isAssignableFrom(ResetPasswordVM::class.java) -> ResetPasswordVM(userRepo) as T
                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    // 2. KHỞI TẠO CÁC BIẾN VIEWMODEL TỪ FACTORY
    val articleVM: ArticleVM = viewModel(factory = factory)
    val userVM: UserVM = viewModel(factory = factory)
    val statisticVM: StatisticVM = viewModel(factory = factory)
    val adminHomeVM: AdminHomeVM = viewModel(factory = factory)
    val registerVM: RegisterVM = viewModel(factory = factory)
    val forgotPasswordVM: ForgotPasswordVM = viewModel(factory = factory)
    val otpVM: OtpVM = viewModel(factory = factory)

    // 3. THIẾT LẬP LUỒNG ĐIỀU HƯỚNG
    NavHost(
        navController = navController,
        startDestination = "login"
    ){
        composable("login"){
            val loginVM: LoginVM = viewModel(factory = factory)
            LoginScreen(navController = navController, viewModel = loginVM)
        }

        composable("register"){
            RegisterScreen(
                navController = navController,
                viewModel = registerVM
            )
        }

        composable("forgot_password"){
            ForgotPasswordScreen(
                navController = navController,
                viewModel = forgotPasswordVM
            )
        }

        composable(route = "otp/{email}"){ backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            OtpScreen(
                navController = navController,
                email = email,
                viewModel = otpVM
            )
        }

        composable(route = "reset_password/{email}") { backStackEntry ->
            val resetPasswordVM: ResetPasswordVM = viewModel(factory = factory)
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ResetPasswordScreen(
                navController = navController,
                email = email,
                viewModel = resetPasswordVM
            )
        }

        composable(route = "admin_home") {
            AdminHomeScreen(
                navController = navController,
                viewModel = adminHomeVM
            )
        }

        composable("change_password") {
            val changePasswordVM: ChangePasswordVM = viewModel(factory = factory)
            ChangePasswordScreen(
                navController = navController,
                viewModel = changePasswordVM
            )
        }

        composable("manage_articles") {
            ManageArticlesScreen(navController, articleVM)
        }

        composable("add_article") {
            AddArticleScreen(navController, articleVM)
        }

        // SỬA Ở ĐÂY: Chuyển LongType thành StringType và thêm URLDecoder
        composable(
            route = "article_detail/{articleId}",
            arguments = listOf(navArgument("articleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString("articleId") ?: ""
            val articleId = URLDecoder.decode(rawId, StandardCharsets.UTF_8.toString())
            ArticleDetailScreen(
                navController,
                articleId,
                articleVM
            )
        }

        composable(
            route = "edit_article/{articleId}",
            arguments = listOf(navArgument("articleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString("articleId") ?: ""
            val articleId = URLDecoder.decode(rawId, StandardCharsets.UTF_8.toString())
            EditArticleScreen(
                navController,
                articleId,
                articleVM
            )
        }

        composable("manage_users") {
            ManageUsersScreen(navController, userVM)
        }

        composable("add_user") {
            AddAdminScreen(navController, userVM)
        }

        composable(
            route = "view_user/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ViewUserScreen(navController, userId, userVM)
        }

        composable("statistics") {
            StatisticScreen(navController, statisticVM)
        }
    }
}