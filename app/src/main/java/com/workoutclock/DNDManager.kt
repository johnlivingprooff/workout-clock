package com.workoutclock

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi

class DNDManager(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    /**
     * Check if the app has permission to modify DND settings
     */
    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true // Pre-M devices don't need this permission
        }
    }
    
    /**
     * Request DND permission from the user
     */
    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasPermission()) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            Toast.makeText(
                context,
                "Please grant 'Do Not Disturb access' permission for Focus mode",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Enable Do Not Disturb mode
     */
    fun enableDND(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (hasPermission()) {
                    // Set DND to Priority Only mode (allows alarms, media, and system sounds)
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    showToast("🔕 Do Not Disturb enabled for Focus session")
                    true
                } else {
                    requestPermission()
                    false
                }
            } else {
                // For older devices, we can't control DND programmatically
                showToast("📱 Please enable Do Not Disturb manually for Focus session")
                false
            }
        } catch (e: Exception) {
            showToast("⚠️ Unable to enable Do Not Disturb")
            false
        }
    }
    
    /**
     * Disable Do Not Disturb mode
     */
    fun disableDND(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (hasPermission()) {
                    // Restore normal notification behavior
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    showToast("🔔 Do Not Disturb disabled for break time")
                    true
                } else {
                    false
                }
            } else {
                showToast("📱 Please disable Do Not Disturb manually for break time")
                false
            }
        } catch (e: Exception) {
            showToast("⚠️ Unable to disable Do Not Disturb")
            false
        }
    }
    
    /**
     * Check if DND is currently enabled
     */
    fun isDNDEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            false
        }
    }
    
    /**
     * Reset DND to normal mode (used when timer is stopped/reset)
     */
    fun resetDND() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hasPermission()) {
            try {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            } catch (e: Exception) {
                // Silently handle errors during reset
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
