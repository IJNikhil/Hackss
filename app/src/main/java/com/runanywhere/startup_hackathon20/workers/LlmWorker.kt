package com.runanywhere.startup_hackathon20.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.runanywhere.startup_hackathon20.R
import kotlinx.coroutines.delay

class LlmWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val input = inputData.getString("INPUT_DATA") ?: return Result.failure()

        // Create the notification channel if needed
        createNotificationChannel()

        // Set initial progress and show foreground notification
        setForeground(createForegroundInfo(0))

        // Simulate processing
        for (i in 1..10) {
            delay(1000) // Simulate 1 second of work
            val progress = i * 10
            setProgress(workDataOf("Progress" to progress))
            setForeground(createForegroundInfo(progress))
        }

        return Result.success()
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val channelId = "llm_channel_id"
        val title = "Processing Document..."
        val cancel = "Cancel"
        
        // Create a Notification
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText("$progress% Complete")
            .setSmallIcon(android.R.drawable.ic_menu_upload) // Using a system icon for now
            .setOngoing(true)
            .setProgress(100, progress, false)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel,
                androidx.work.WorkManager.getInstance(applicationContext)
                    .createCancelPendingIntent(id))
            .build()

        return ForegroundInfo(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "LLM Processing"
            val descriptionText = "Notifications for LLM background processing"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("llm_channel_id", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }
    }
}
