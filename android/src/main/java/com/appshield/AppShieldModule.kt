package com.appshield

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.Promise
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.bridge.ReactContextBaseJavaModule
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.app.AppOpsManager
import android.content.Context
import android.Manifest
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

@ReactModule(name = AppShieldModule.NAME)
class AppShieldModule(private val context: ReactApplicationContext) :
  NativeAppShieldSpec(context) {

  override fun getName(): String {
    return NAME
  }

  override fun blockAllApps() {
    val ctx = context
    val intent = Intent(ctx, AppShieldAccessibilityService::class.java)
    ContextCompat.startForegroundService(ctx, intent)
    // Send a command by binding or static reference if needed; here we rely on service instance
  }

  override fun unblockAllApps() {
    val ctx = context
    val intent = Intent(ctx, AppShieldAccessibilityService::class.java)
    ctx.stopService(intent)
  }

  override fun requestRequiredPermissions(promise: Promise) {
    try {
      val ctx = context
      val result = com.facebook.react.bridge.Arguments.createMap()

      // Accessibility
      val accessibilityEnabled = isAccessibilityEnabled(ctx)
      result.putBoolean("accessibility", accessibilityEnabled)
      if (!accessibilityEnabled) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
      }

      // Usage Access
      val usageAccess = isUsageAccessGranted(ctx)
      result.putBoolean("usageAccess", usageAccess)
      if (!usageAccess) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
      }

      // Notifications (Android 13+)
      val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
          PermissionChecker.PERMISSION_GRANTED
      } else true
      result.putBoolean("notifications", notificationsGranted)

      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("permission_error", e.message, e)
    }
  }

  private fun isAccessibilityEnabled(context: Context): Boolean {
    return try {
      val settingValue = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
      ) ?: return false
      settingValue.split(":").any { it.contains(context.packageName) }
    } catch (e: Exception) {
      false
    }
  }

  private fun isUsageAccessGranted(context: Context): Boolean {
    return try {
      val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
      val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
          "android:get_usage_stats",
          android.os.Process.myUid(),
          context.packageName
        )
      } else {
        appOps.checkOpNoThrow(
          "android:get_usage_stats",
          android.os.Process.myUid(),
          context.packageName
        )
      }
      mode == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
      false
    }
  }

  companion object {
    const val NAME = "AppShield"
  }
}
