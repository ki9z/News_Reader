package com.util

import android.app.Application
import android.content.Context
import android.util.Log
import java.io.File

object CrashLogger {
    private const val FILE_NAME = "last_crash.txt"

    fun install(app: Application) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val file = File(app.filesDir, FILE_NAME)
                val stack = Log.getStackTraceString(throwable)
                file.writeText("Thread: ${thread.name}\nTime: ${System.currentTimeMillis()}\n\n$stack")
            } catch (_: Exception) {
                // Ignore logging failures and let the default handler proceed.
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun readAndClear(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        return runCatching {
            val text = file.readText()
            file.delete()
            text
        }.getOrNull()
    }
}

