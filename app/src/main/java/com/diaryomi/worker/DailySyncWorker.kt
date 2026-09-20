package com.diaryomi.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.diaryomi.MainActivity
import com.diaryomi.R
import com.diaryomi.domain.usecase.SyncUseCase
import com.diaryomi.util.AppLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailySyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncUseCase: SyncUseCase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "daily_sync_worker"
        private const val TAG = "DailySyncWorker"
        const val CHANNEL_ID = "diaryomi_sync_results"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        AppLogger.i("⏰ [Worker] Iniciando sincronização periódica de 24h em background...")
        return try {
            val newResultsCount = syncUseCase.syncAll()
            AppLogger.i("⏰ [Worker] Sincronização concluída com sucesso. $newResultsCount novos resultados.")

            if (newResultsCount > 0) {
                showNotification(newResultsCount)
                AppLogger.i("🔔 [Worker] Notificação disparada para o usuário ($newResultsCount publicações).")
            }

            Result.success()
        } catch (e: Exception) {
            AppLogger.e("⏰ [Worker] Erro na sincronização periódica: ${e.message}", e)
            Result.retry()
        }
    }

    private fun showNotification(count: Int) {
        createNotificationChannel()

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                appContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                Log.w(TAG, "Permissão de notificação não concedida.")
                return
            }
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Novas publicações encontradas"
        val text = if (count == 1) {
            "1 nova publicação encontrada nos Diários Oficiais"
        } else {
            "$count novas publicações encontradas nos Diários Oficiais"
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val name = "Resultados de busca"
        val descriptionText = "Notificações quando novas publicações são encontradas nos Diários Oficiais"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
