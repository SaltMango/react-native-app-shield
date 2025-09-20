package com.appshield

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.Promise
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.ReadableArray
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.app.AppOpsManager
import android.content.Context
import android.net.Uri
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.Manifest
import android.app.usage.UsageStatsManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import android.util.Log

@ReactModule(name = AppShieldModule.NAME)  
class AppShieldModule(private val context: ReactApplicationContext) :
  ReactContextBaseJavaModule(context) {

  override fun getName(): String {
    return NAME
  }

  @ReactMethod
  fun blockAllApps() {
    val ctx = context
    // Ensure service is running
    val intent = Intent(ctx, AppShieldAccessibilityService::class.java)
    intent.action = AppShieldAccessibilityService.ACTION_ENABLE_BLOCK_ALL
    ContextCompat.startForegroundService(ctx, intent)
    // Command the active service (if connected) to enable block
    AppShieldAccessibilityService.enableBlockAll()
    // Persist for resilience
    try {
      ctx.getSharedPreferences("appshield_prefs", Context.MODE_PRIVATE)
        .edit().putBoolean("block_all_enabled", true).apply()
    } catch (_: Exception) {}
  }

  @ReactMethod
  fun unblockAllApps() {
    val ctx = context
    AppShieldAccessibilityService.disableBlockAll()
    val intent = Intent(ctx, AppShieldAccessibilityService::class.java)
    intent.action = AppShieldAccessibilityService.ACTION_DISABLE_BLOCK_ALL
    ctx.stopService(intent)
    try {
      ctx.getSharedPreferences("appshield_prefs", Context.MODE_PRIVATE)
        .edit().putBoolean("block_all_enabled", false).apply()
    } catch (_: Exception) {}
  }

  @ReactMethod
  fun getPermissionStatus(promise: Promise) {
    try {
      val ctx = context
      val result = Arguments.createMap()
      result.putBoolean("accessibility", isAccessibilityEnabled(ctx))
      result.putBoolean("usageAccess", isUsageAccessGranted(ctx))
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("permission_error", e.message, e)
    }
  }

  @ReactMethod
  fun requestPermissions(promise: Promise) {
    try {
      val ctx = context
      val accessibilityGranted = isAccessibilityEnabled(ctx)
      val usageAccessGranted = isUsageAccessGranted(ctx)
      
      // If permissions are missing, open settings
      if (!accessibilityGranted) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
      } else if (!usageAccessGranted) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
      }
      
      val result = Arguments.createMap()
      result.putBoolean("accessibility", accessibilityGranted)
      result.putBoolean("usageAccess", usageAccessGranted)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("permission_error", e.message, e)
    }
  }
  
  @ReactMethod
  fun isBlockingActive(promise: Promise) {
    try {
      val ctx = context
      val accessibilityGranted = isAccessibilityEnabled(ctx)
      val prefs = ctx.getSharedPreferences("appshield_prefs", Context.MODE_PRIVATE)
      val blockingEnabled = prefs.getBoolean("block_all_enabled", false)
      
      val isActive = accessibilityGranted && blockingEnabled
      promise.resolve(isActive)
    } catch (e: Exception) {
      promise.reject("blocking_status_error", e.message, e)
    }
  }
  
  // iOS-only API - provide Android stub to satisfy codegen
  @ReactMethod
  fun requestScreenTimeAuthorization(promise: Promise) {
    try {
      val result = Arguments.createMap()
      result.putBoolean("screenTime", false)
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

  // Enhanced methods for custom app management
  @ReactMethod
  fun setCustomAllowedApps(apps: ReadableArray?) {
    try {
      val allowedApps = mutableSetOf<String>()
      apps?.let {
        for (i in 0 until it.size()) {
          it.getString(i)?.let { pkg -> allowedApps.add(pkg.lowercase()) }
        }
      }
      
      // Set custom allowed apps in the service
      AppShieldAccessibilityService.setCustomAllowedApps(allowedApps)
      
      // Persist the list
      val ctx = context
      ctx.getSharedPreferences("appshield_prefs", Context.MODE_PRIVATE)
        .edit().putStringSet("custom_allowed_apps", allowedApps).apply()
    } catch (e: Exception) {
      Log.e("AppShieldModule", "Error setting custom allowed apps: ${e.message}")
    }
  }

  @ReactMethod
  fun getCustomAllowedApps(promise: Promise) {
    try {
      val ctx = context
      val allowedApps = ctx.getSharedPreferences("appshield_prefs", Context.MODE_PRIVATE)
        .getStringSet("custom_allowed_apps", emptySet()) ?: emptySet()
      
      val result = Arguments.createArray()
      allowedApps.forEach { app -> result.pushString(app) }
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("get_allowed_apps_error", e.message, e)
    }
  }

  @ReactMethod
  fun getInstalledApps(promise: Promise) {
    try {
      val ctx = context
      val pm = ctx.packageManager
      val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
      
      val result = Arguments.createArray()
      apps.forEach { appInfo ->
        val appData = Arguments.createMap()
        appData.putString("packageName", appInfo.packageName)
        appData.putString("appName", pm.getApplicationLabel(appInfo).toString())
        appData.putBoolean("isSystemApp", (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
        result.pushMap(appData)
      }
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("get_installed_apps_error", e.message, e)
    }
  }

  @ReactMethod
  fun checkDeviceCompatibility(promise: Promise) {
    try {
      val ctx = context
      val result = Arguments.createMap()
      
      // Check device manufacturer and version
      result.putString("manufacturer", Build.MANUFACTURER)
      result.putString("model", Build.MODEL)
      result.putString("version", Build.VERSION.RELEASE)
      result.putInt("sdkInt", Build.VERSION.SDK_INT)
      
      // Check if accessibility service can be enabled
      val accessibilityEnabled = isAccessibilityEnabled(ctx)
      result.putBoolean("accessibilitySupported", true)
      result.putBoolean("accessibilityEnabled", accessibilityEnabled)
      
      // Check usage access capability
      val usageAccessGranted = isUsageAccessGranted(ctx)
      result.putBoolean("usageAccessSupported", true)
      result.putBoolean("usageAccessGranted", usageAccessGranted)
      
      // OEM-specific compatibility checks
      val oemInfo = Arguments.createMap()
      when (Build.MANUFACTURER.lowercase()) {
        "xiaomi" -> {
          oemInfo.putString("oem", "xiaomi")
          oemInfo.putString("ui", "miui")
          oemInfo.putBoolean("requiresSpecialPermissions", true)
        }
        "samsung" -> {
          oemInfo.putString("oem", "samsung")
          oemInfo.putString("ui", "oneui")
          oemInfo.putBoolean("requiresSpecialPermissions", false)
        }
        "oneplus" -> {
          oemInfo.putString("oem", "oneplus")
          oemInfo.putString("ui", "oxygenos")
          oemInfo.putBoolean("requiresSpecialPermissions", true)
        }
        "oppo" -> {
          oemInfo.putString("oem", "oppo")
          oemInfo.putString("ui", "coloros")
          oemInfo.putBoolean("requiresSpecialPermissions", true)
        }
        "vivo" -> {
          oemInfo.putString("oem", "vivo")
          oemInfo.putString("ui", "funtouch")
          oemInfo.putBoolean("requiresSpecialPermissions", true)
        }
        "huawei" -> {
          oemInfo.putString("oem", "huawei")
          oemInfo.putString("ui", "emui")
          oemInfo.putBoolean("requiresSpecialPermissions", true)
        }
        else -> {
          oemInfo.putString("oem", Build.MANUFACTURER.lowercase())
          oemInfo.putString("ui", "stock")
          oemInfo.putBoolean("requiresSpecialPermissions", false)
        }
      }
      result.putMap("oemInfo", oemInfo)
      
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("compatibility_check_error", e.message, e)
    }
  }

  @ReactMethod
  fun getDefaultAllowedApps(promise: Promise) {
    try {
      // Get default allowed apps list from the accessibility service
      val allowedApps = AppShieldAccessibilityService.getDefaultAllowedApps()
      val result = Arguments.createArray()
      allowedApps.forEach { app -> result.pushString(app) }
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("get_default_allowed_apps_error", e.message, e)
    }
  }

  @ReactMethod 
  fun initializeAllowedApps() {
    try {
      // Trigger allowlist initialization
      AppShieldAccessibilityService.initializeDefaultApps()
    } catch (e: Exception) {
      Log.e("AppShieldModule", "Error initializing allowed apps: ${e.message}")
    }
  }

  @ReactMethod
  fun openAutoStartSettings(promise: Promise) {
    try {
      val intent = Intent()
      val manufacturer = Build.MANUFACTURER.lowercase()
      
      when (manufacturer) {
        "xiaomi" -> {
          intent.component = ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
          )
        }
        "oppo" -> {
          intent.component = ComponentName(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
          )
        }
        "vivo" -> {
          intent.component = ComponentName(
            "com.vivo.permissionmanager",
            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
          )
        }
        "huawei" -> {
          intent.component = ComponentName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.optimize.process.ProtectActivity"
          )
        }
        else -> {
          intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
          intent.data = Uri.parse("package:${context.packageName}")
        }
      }
      
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      context.startActivity(intent)
      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject("autostart_settings_error", "Could not open autostart settings: ${e.message}", e)
    }
  }

  @ReactMethod
  fun openBatteryOptimizationSettings(promise: Promise) {
    try {
      val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      context.startActivity(intent)
      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject("battery_optimization_error", "Could not open battery optimization settings: ${e.message}", e)
    }
  }

  @ReactMethod
  fun isMIUIDevice(promise: Promise) {
    try {
      val isMIUI = Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) ||
                   Build.MANUFACTURER.equals("redmi", ignoreCase = true) ||
                   Build.BRAND.equals("xiaomi", ignoreCase = true) ||
                   Build.BRAND.equals("redmi", ignoreCase = true)
      promise.resolve(isMIUI)
    } catch (e: Exception) {
      promise.reject("miui_check_error", e.message, e)
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