package com.appshield

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d("BootReceiver", "Device booted or app updated, checking AppShield state")
                
                // Check if blocking was enabled before reboot
                val prefs = context.getSharedPreferences("appshield_prefs", Context.MODE_PRIVATE)
                val wasBlockingEnabled = prefs.getBoolean("block_all_enabled", false)
                
                if (wasBlockingEnabled) {
                    Log.d("BootReceiver", "Restarting AppShield service after boot")
                    
                    // Restart the accessibility service
                    val serviceIntent = Intent(context, AppShieldAccessibilityService::class.java)
                    serviceIntent.action = AppShieldAccessibilityService.ACTION_ENABLE_BLOCK_ALL
                    
                    try {
                        ContextCompat.startForegroundService(context, serviceIntent)
                        
                        // Re-enable blocking state
                        AppShieldAccessibilityService.enableBlockAll()
                        
                        Log.d("BootReceiver", "AppShield service restarted successfully")
                    } catch (e: Exception) {
                        Log.e("BootReceiver", "Failed to restart AppShield service: ${e.message}")
                    }
                }
            }
        }
    }
}
