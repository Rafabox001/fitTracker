package com.rdc.fittracker.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import com.google.android.gms.fitness.data.DataUpdateNotification
import com.rdc.fittracker.MainActivity


class BloodPressureUpdateService : JobIntentService() {

    companion object {
        // Job-ID must be unique across your whole app.
        private const val UNIQUE_JOB_ID = 42
        private const val NOTIFICATION_ID = 101

        fun enqueueWork(context: Context) {
            enqueueWork(context, BloodPressureUpdateService::class.java, UNIQUE_JOB_ID, Intent())
        }
    }

    override fun onHandleWork(intent: Intent) {
        //Access data.dataType.fields if we need to display Notification with information
        val data = DataUpdateNotification.getDataUpdateNotification(intent)

        sendNotification()
    }

    private fun sendNotification() {

        val resultIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelID = "com.rdc.fittracker"

        val notification = NotificationCompat.Builder(this,
            channelID)
            .setContentTitle("New Information is available")
            .setContentText("A new blood pressure record has been added.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setChannelId(channelID)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}