package com.rdc.fittracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rdc.fittracker.services.BloodPressureUpdateService

/** Receives broadcast for BloodPressureUpdate. */
class BloodPressureUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null) {
            // Schedule the job to be run in the background.
            BloodPressureUpdateService.enqueueWork(context)
        }
    }
}