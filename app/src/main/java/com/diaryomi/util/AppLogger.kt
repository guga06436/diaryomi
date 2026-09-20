package com.diaryomi.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Utilitário central de logs do Diaryomi.
 * Envia para o Logcat do Android e mantém os últimos 100 eventos
 * em memória para visualização direta na interface do usuário.
 */
object AppLogger {
    private const val TAG = "Diaryomi"
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    fun d(message: String) {
        Log.d(TAG, message)
        appendLog("🔹 $message")
    }

    fun i(message: String) {
        Log.i(TAG, message)
        appendLog("ℹ️ $message")
    }

    fun w(message: String) {
        Log.w(TAG, message)
        appendLog("⚠️ $message")
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
        val errorDetail = throwable?.message?.let { " - $it" } ?: ""
        appendLog("❌ $message$errorDetail")
    }

    private fun appendLog(entry: String) {
        val timestamp = LocalTime.now().format(timeFormatter)
        val line = "[$timestamp] $entry"
        _logs.value = (_logs.value + line).takeLast(100)
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
