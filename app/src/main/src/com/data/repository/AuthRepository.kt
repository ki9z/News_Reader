package com.data.repository

import com.data.local.dao.UserDao
import com.data.local.entity.UserEntity
import com.data.security.TokenManager
import com.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.HttpURLConnection
import java.io.OutputStreamWriter
import org.json.JSONObject
import com.util.Constants
import java.util.UUID

class AuthRepository(
    private val tokenManager: TokenManager,
    private val userDao: UserDao
) {
    suspend fun register(email: String, password: String, name: String? = null): NetworkResult<Unit> {
        return try {
            if (email.isBlank() || password.isBlank()) {
                return NetworkResult.Error(
                    message = "Email và mật khẩu không được để trống",
                    type = NetworkResult.ErrorType.CLIENT
                )
            }

            // Check if user already exists
            val existingUser = withContext(Dispatchers.IO) {
                userDao.getByEmail(email.trim())
            }
            if (existingUser != null) {
                return NetworkResult.Error(
                    message = "Email đã được đăng ký",
                    type = NetworkResult.ErrorType.CLIENT
                )
            }

            val userId = UUID.randomUUID().toString()
            val currentTime = System.currentTimeMillis()
            
            val newUser = UserEntity(
                id = userId,
                email = email.trim(),
                fullName = name.orEmpty(),
                passwordHash = hashPassword(password),
                role = "user",
                isSignedIn = true,
                createdAt = currentTime,
                updatedAt = currentTime
            )

            withContext(Dispatchers.IO) {
                userDao.signOutAll(currentTime)
                userDao.upsert(newUser)
            }

            saveLocalSession(userId = userId, role = "user")
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(
                message = "Registration error: ${e.message}",
                type = NetworkResult.ErrorType.CLIENT
            )
        }
    }

    suspend fun login(email: String, password: String): NetworkResult<Unit> {
        return try {
            if (email.isBlank() || password.isBlank()) {
                return NetworkResult.Error(
                    message = "Email và mật khẩu không được để trống",
                    type = NetworkResult.ErrorType.CLIENT
                )
            }

            val user = withContext(Dispatchers.IO) {
                userDao.getByEmail(email.trim())
            } ?: return NetworkResult.Error(
                message = "Email không chính xác",
                type = NetworkResult.ErrorType.CLIENT
            )

            if (!verifyPassword(password, user.passwordHash.orEmpty())) {
                return NetworkResult.Error(
                    message = "Mật khẩu không chính xác",
                    type = NetworkResult.ErrorType.CLIENT
                )
            }

            val currentTime = System.currentTimeMillis()
            withContext(Dispatchers.IO) {
                userDao.signOutAll(currentTime)
                userDao.markSingleSignedIn(user.id, currentTime)
            }

            saveLocalSession(userId = user.id, role = user.role)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(
                message = "Login error: ${e.message}",
                type = NetworkResult.ErrorType.CLIENT
            )
        }
    }

    suspend fun loginWithOAuth(
        provider: String,
        idToken: String?,
        accessToken: String?,
        email: String,
        name: String? = null
    ): NetworkResult<Unit> {
        return try {
            if (provider.equals("google", ignoreCase = true) && !idToken.isNullOrBlank() && Constants.USES_BACKEND_PROXY) {
                runCatching {
                    withContext(Dispatchers.IO) { postFirebaseAuth(idToken) }
                }
            }

            val userEmail = email.trim().ifBlank { "oauth_${provider}_${UUID.randomUUID()}" }
            
            // Check if user exists
            var user = withContext(Dispatchers.IO) {
                userDao.getByEmail(userEmail)
            }

            // If not exists, create new user
            if (user == null) {
                val userId = UUID.randomUUID().toString()
                val currentTime = System.currentTimeMillis()
                user = UserEntity(
                    id = userId,
                    email = userEmail,
                    fullName = name.orEmpty(),
                    passwordHash = "oauth_${provider}_${UUID.randomUUID()}", // OAuth doesn't use password
                    role = "user",
                    isSignedIn = true,
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
                withContext(Dispatchers.IO) {
                    userDao.signOutAll(currentTime)
                    userDao.upsert(user)
                }
            } else {
                // Sign in existing user
                val currentTime = System.currentTimeMillis()
                withContext(Dispatchers.IO) {
                    userDao.signOutAll(currentTime)
                    userDao.markSingleSignedIn(user.id, currentTime)
                }
            }

            saveLocalSession(userId = user.id, role = user.role)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(
                message = "OAuth login error: ${e.message}",
                type = NetworkResult.ErrorType.CLIENT
            )
        }
    }

    suspend fun requestPhoneOtp(phone: String): NetworkResult<Unit> {
        return try {
            if (phone.isBlank()) {
                return NetworkResult.Error(
                    message = "Số điện thoại không được để trống",
                    type = NetworkResult.ErrorType.CLIENT
                )
            }

            if (!Constants.USES_BACKEND_PROXY) {
                return NetworkResult.Error(
                    message = "Chức năng OTP SMS cần chạy qua backend proxy",
                    type = NetworkResult.ErrorType.SERVER
                )
            }

            val success = withContext(Dispatchers.IO) {
                postOtpRequest(identifier = phone.trim(), channel = "sms")
            }

            if (success) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(
                    message = "Không gửi được OTP SMS. Vui lòng kiểm tra Twilio/backend.",
                    type = NetworkResult.ErrorType.SERVER
                )
            }
        } catch (e: Exception) {
            NetworkResult.Error(
                message = "OTP request error: ${e.message}",
                type = NetworkResult.ErrorType.SERVER
            )
        }
    }

    suspend fun verifyPhoneOtp(phone: String, otp: String, name: String? = null): NetworkResult<Unit> {
        return try {
            if (phone.isBlank()) {
                return NetworkResult.Error(
                    message = "Số điện thoại không được để trống",
                    type = NetworkResult.ErrorType.CLIENT
                )
            }

            if (otp.length != 6 || !otp.all { it.isDigit() }) {
                return NetworkResult.Error(
                    message = "OTP không hợp lệ",
                    type = NetworkResult.ErrorType.CLIENT
                )
            }

            if (!Constants.USES_BACKEND_PROXY) {
                return NetworkResult.Error(
                    message = "Chức năng OTP SMS cần chạy qua backend proxy",
                    type = NetworkResult.ErrorType.SERVER
                )
            }

            val verified = withContext(Dispatchers.IO) {
                postOtpVerify(identifier = phone.trim(), channel = "sms", code = otp)
            }

            if (!verified) {
                return NetworkResult.Error(
                    message = "Mã OTP không chính xác hoặc đã hết hạn",
                    type = NetworkResult.ErrorType.CLIENT
                )
            }

            val phoneKey = "phone_$phone"
            var user = withContext(Dispatchers.IO) {
                userDao.getByEmail(phoneKey)
            }

            if (user == null) {
                val userId = UUID.randomUUID().toString()
                val currentTime = System.currentTimeMillis()
                user = UserEntity(
                    id = userId,
                    email = phoneKey,
                    phone = phone.trim(),
                    fullName = name.orEmpty(),
                    passwordHash = "phone_otp_verified",
                    role = "user",
                    isSignedIn = true,
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
                withContext(Dispatchers.IO) {
                    userDao.signOutAll(currentTime)
                    userDao.upsert(user)
                }
            } else {
                val currentTime = System.currentTimeMillis()
                withContext(Dispatchers.IO) {
                    userDao.signOutAll(currentTime)
                    userDao.markSingleSignedIn(user.id, currentTime)
                }
            }

            saveLocalSession(userId = user.id, role = user.role)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(
                message = "OTP verification error: ${e.message}",
                type = NetworkResult.ErrorType.SERVER
            )
        }
    }

    private fun postFirebaseAuth(idToken: String): Boolean {
        val body = JSONObject().put("idToken", idToken)
        return postJsonToBackend("api/auth/firebase", body)
    }

    private fun postOtpRequest(identifier: String, channel: String): Boolean {
        val body = JSONObject()
            .put("identifier", identifier)
            .put("channel", channel)
        return postJsonToBackend("api/auth/otp/request", body)
    }

    private fun postOtpVerify(identifier: String, channel: String, code: String): Boolean {
        val body = JSONObject()
            .put("identifier", identifier)
            .put("channel", channel)
            .put("code", code)
        return postJsonToBackend("api/auth/otp/verify", body)
    }

    private fun postJsonToBackend(path: String, body: JSONObject): Boolean {
        val url = URL(Constants.BASE_URL.trimEnd('/') + "/" + path.trimStart('/'))
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (Constants.BACKEND_APP_TOKEN.isNotBlank()) {
                setRequestProperty("X-App-Token", Constants.BACKEND_APP_TOKEN)
            }
        }

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body.toString())
        }

        val responseCode = connection.responseCode
        connection.disconnect()
        return responseCode in 200..299
    }

    suspend fun getCurrentUser(): UserEntity? {
        return withContext(Dispatchers.IO) {
            userDao.getSignedInUser()
        }
    }

    suspend fun getCurrentUserRole(): String? {
        return withContext(Dispatchers.IO) {
            getCurrentUser()?.role
        }
    }

    suspend fun isAdmin(): Boolean {
        return withContext(Dispatchers.IO) {
            getCurrentUser()?.role == "admin"
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            userDao.signOutAll(System.currentTimeMillis())
        }
        tokenManager.clearTokens()
    }

    fun hasValidToken(): Boolean {
        return tokenManager.hasToken() && !tokenManager.isAccessTokenExpired()
    }

    private fun saveLocalSession(userId: String, role: String) {
        tokenManager.saveAccessToken("local_access_token_$userId")
        tokenManager.saveRefreshToken("local_refresh_token_$userId")

        // Lưu hạn đăng nhập 30 ngày.
        val thirtyDaysInSeconds = 30L * 24L * 60L * 60L
        tokenManager.saveTokenExpiry(thirtyDaysInSeconds)

        tokenManager.saveUserId(userId)
    }

    // Simple password hashing (in production, use bcrypt)
    private fun hashPassword(password: String): String {
        return password.hashCode().toString()
    }

    private fun verifyPassword(password: String, hash: String): Boolean {
        return password.hashCode().toString() == hash
    }
}