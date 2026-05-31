package com.example.app_doc_bao.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.app_doc_bao.view.screen.*
import com.example.app_doc_bao.view.screen.auth.*
import com.example.app_doc_bao.view.screen.manageArticles.*
import com.example.app_doc_bao.view.screen.manageUsers.*
import com.example.app_doc_bao.view.screen.StatisticScreen
import com.example.app_doc_bao.viewmodel.*
import com.example.app_doc_bao.viewmodel.auth.*
import com.example.app_doc_bao.data.local.AppDatabase
import com.example.app_doc_bao.data.repository.ArticleRepository
import com.example.app_doc_bao.data.repository.OtpRepository
import com.example.app_doc_bao.data.repository.UserRepository

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // 1. TẠO FACTORY CHUNG ĐỂ QUẢN LÝ REPOSITORY
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getDatabase(context)
            val articleRepo = ArticleRepository(db)
            val userRepo = UserRepository(db)
            val otpRepo = OtpRepository()

            return when {
                modelClass.isAssignableFrom(ArticleVM::class.java) -> ArticleVM(articleRepo) as T
                modelClass.isAssignableFrom(StatisticVM::class.java) -> StatisticVM(articleRepo, userRepo) as T
                modelClass.isAssignableFrom(AdminHomeVM::class.java) -> AdminHomeVM(articleRepo, userRepo) as T
                modelClass.isAssignableFrom(UserVM::class.java) -> UserVM(userRepo) as T
                modelClass.isAssignableFrom(ChangePasswordVM::class.java) -> ChangePasswordVM(userRepo) as T
                modelClass.isAssignableFrom(LoginVM::class.java) -> LoginVM(userRepo) as T
                modelClass.isAssignableFrom(RegisterVM::class.java) -> RegisterVM(userRepo) as T
                modelClass.isAssignableFrom(ForgotPasswordVM::class.java) -> ForgotPasswordVM(userRepo) as T
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
    val changePasswordVM: ChangePasswordVM = viewModel(factory = factory)
    val loginVM: LoginVM = viewModel(factory = factory)
    val registerVM: RegisterVM = viewModel(factory = factory)
    val forgotPasswordVM: ForgotPasswordVM = viewModel(factory = factory)
    val otpVM: OtpVM = viewModel(factory = factory)
    val resetPasswordVM: ResetPasswordVM = viewModel(factory = factory)

    // 3. THIẾT LẬP LUỒNG ĐIỀU HƯỚNG
    NavHost(
        navController = navController,
        startDestination = "login"
    ){
        composable("login"){
            LoginScreen(navController = navController, viewModel = loginVM)
        }

        composable("register"){
            RegisterScreen(navController = navController, viewModel = registerVM)
        }

        composable("forgot_password"){
            ForgotPasswordScreen(navController = navController, viewModel = forgotPasswordVM)
        }

        composable(route = "otp/{email}"){ backStackEntry ->
            val email = backStackEntry.arguments?.getString("email")?: ""
            OtpScreen(navController = navController, email = email, viewModel = otpVM)
        }

        composable(route = "reset_password/{email}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email")
            ResetPasswordScreen(navController = navController, email = email, viewModel = resetPasswordVM)
        }

        composable(route = "admin_home") {
            AdminHomeScreen(navController = navController, viewModel = adminHomeVM)
        }

        composable("change_password") {
            ChangePasswordScreen(navController = navController, viewModel = changePasswordVM)
        }

        composable("manage_articles") {
            ManageArticlesScreen(navController, articleVM)
        }

        composable("add_article") {
            AddArticleScreen(navController, articleVM)
        }

        composable(
            route = "article_detail/{articleId}",
            arguments = listOf(navArgument("articleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getLong("articleId") ?: 0L
            ArticleDetailScreen(navController, articleId, articleVM)
        }

        composable(
            route = "edit_article/{articleId}",
            arguments = listOf(navArgument("articleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getLong("articleId") ?: 0L
            EditArticleScreen(navController, articleId, articleVM)
        }

        composable("manage_users") {
            ManageUsersScreen(navController, userVM)
        }

        composable("add_user") {
            AddAdminScreen(navController, userVM)
        }

        composable(
            route = "edit_user/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.LongType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: 0L
            EditAdminScreen(navController, userId, userVM)
        }

        composable("statistics") {
            StatisticScreen(navController, statisticVM)
        }
    }
}