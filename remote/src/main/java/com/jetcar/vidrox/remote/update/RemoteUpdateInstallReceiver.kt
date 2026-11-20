package com.jetcar.vidrox.remote.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import android.widget.Toast
import androidx.core.content.IntentCompat

private const val UPDATE_LOG_TAG = "VidroXRemoteUpdate"

class RemoteUpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = IntentCompat.getParcelableExtra(
                    intent,
                    Intent.EXTRA_INTENT,
                    Intent::class.java,
                ) ?: return
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(confirmation) }
                    .onFailure {
                        Log.w(UPDATE_LOG_TAG, "could not show install confirmation", it)
                        Toast.makeText(
                            context,
                            "Не удалось открыть подтверждение установки",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            PackageInstaller.STATUS_FAILURE_ABORTED ->
                Toast.makeText(context, "Обновление отменено", Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(
                context,
                "Ошибка обновления: ${message ?: "код $status"}",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
