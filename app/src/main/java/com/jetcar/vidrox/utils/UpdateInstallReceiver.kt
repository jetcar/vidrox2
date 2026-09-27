package com.jetcar.vidrox.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import android.widget.Toast
import androidx.core.content.IntentCompat

private const val UPDATE_LOG_TAG = "VidroXUpdate"

/** Receives the status of the update install session committed by UpdateViewModel. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        Log.i(UPDATE_LOG_TAG, "install status=$status message=$message")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                    ?: return
                confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(confirmIntent)
                } catch (error: Exception) {
                    Log.w(UPDATE_LOG_TAG, "could not show install confirmation", error)
                    Toast.makeText(context, "Could not show the update confirmation.", Toast.LENGTH_LONG).show()
                }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            PackageInstaller.STATUS_FAILURE_ABORTED ->
                Toast.makeText(context, "Update cancelled.", Toast.LENGTH_SHORT).show()
            else ->
                Toast.makeText(context, "Update failed: ${message ?: "status $status"}", Toast.LENGTH_LONG).show()
        }
    }
}
