package com.example.app_doc_bao.data.remote

import android.content.Context
import java.io.IOException

object JsonReader {

    fun readFromAssets(context: Context, fileName: String): String {
        return try {
            context.assets.open(fileName)
                .bufferedReader()
                .use { it.readText() }
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }
}
