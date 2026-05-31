package com.example.app_doc_bao.viewmodel.auth

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_doc_bao.data.repository.OtpRepository
import kotlinx.coroutines.*

class OtpVM(
    private val repository: OtpRepository
) : ViewModel() {

    var otpValues = mutableStateListOf("", "", "", "", "", "")
    var message by mutableStateOf("")

    var timerValue by mutableIntStateOf(60)
    var isTimerRunning by mutableStateOf(false)
    private var timerJob: Job? = null

    fun sendOTP(email: String) {
        clearOTP()
        message = "Đang gửi yêu cầu..."

        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                repository.sendOTP(email)
            }

            if (success) {
                message = "Đã gửi OTP mới"
                startTimer()
            } else {
                message = "Lỗi kết nối server"
            }
        }
    }

    fun verifyOTP(email: String, onResult: (Boolean) -> Unit) {
        val otpCode = otpValues.joinToString("")

        if (otpCode.length < 6) {
            message = "Vui lòng nhập đủ 6 số"
            return
        }

        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                repository.verifyOTP(email, otpCode)
            }

            if (success) {
                message = "Xác thực thành công"
                onResult(true)
            } else {
                message = "Mã OTP không chính xác hoặc đã hết hạn"
                onResult(false)
            }
        }
    }

    private fun clearOTP() {
        for (i in otpValues.indices) {
            otpValues[i] = ""
        }
    }

    private fun startTimer() {
        timerValue = 60
        isTimerRunning = true
        timerJob?.cancel()

        timerJob = viewModelScope.launch {
            while (timerValue > 0) {
                delay(1000)
                timerValue--
            }
            isTimerRunning = false

            if (message == "Đã gửi OTP mới") {
                message = "Mã OTP đã hết hiệu lực"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}