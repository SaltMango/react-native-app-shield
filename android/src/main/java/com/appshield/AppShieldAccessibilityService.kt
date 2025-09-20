package com.appshield

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlin.collections.isNotEmpty
import kotlin.collections.sortedWith
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.graphics.Color
import com.appshield.R
import android.content.pm.ServiceInfo
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.bridge.WritableNativeMap
import java.util.TreeSet

class AppShieldAccessibilityService : AccessibilityService() {
  @Volatile private var allowAll = true
  @Volatile private var defaultAllowedPackages: Set<String> = emptySet()
  @Volatile private var customAllowedApps: Set<String> = emptySet()
  @Volatile private var lastBlockTime: Long = 0L
  private val blockINTERVAL: Long = 1500
  @Volatile private var lastToastTime: Long = 0L
  private val toastIntervalMs: Long = 4000
  @Volatile private var lastRestrictTime: Long = 0L
  @Volatile private var lastToastMessage: String? = null
  private var restrictUninstall = true
  private var allowedApps: List<String> = emptyList()

    companion object Controller {
        @Volatile private var activeInstance: AppShieldAccessibilityService? = null
        const val ACTION_ENABLE_BLOCK_ALL = "ENABLE_BLOCK_ALL"
        @Volatile private var toastEnabled: Boolean = true
    const val ACTION_DISABLE_BLOCK_ALL = "DISABLE_BLOCK_ALL"

    fun enableBlockAll() {
      activeInstance?.enableBlockAll()
    }

    fun disableBlockAll() {
      activeInstance?.disableBlockAll()
    }

    fun setCustomAllowedApps(apps: Set<String>) {
      activeInstance?.setCustomAllowedApps(apps)
    }

    fun getDefaultAllowedApps(): List<String> {
      return activeInstance?.defaultAllowedPackages?.toList() ?: emptyList()
    }

    fun initializeDefaultApps() {
      activeInstance?.initializeDefaultAllowedApps()
    }

    fun setToastEnabled(enabled: Boolean) {
      toastEnabled = enabled
    }

    fun isToastEnabled(): Boolean {
      return toastEnabled
    }

  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    activeInstance = this
    
    // Start as a foreground service to prevent being killed
    startForegroundService()
    
    // Restore persisted state
    allowAll = !getPersistedBlockAll()
    
    // Initialize allowlist like BHere does
    initializeDefaultAllowedApps()
    
    // Load persisted custom allowed apps
    loadCustomAllowedApps()

    // Load toast preference
    val prefs = applicationContext.getSharedPreferences("appshield_prefs", MODE_PRIVATE)
    toastEnabled = prefs.getBoolean("toast_enabled", true) // Default to true

    // Tune service info for broader coverage
    val info = AccessibilityServiceInfo()
    info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
      AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
      AccessibilityEvent.TYPE_VIEW_CLICKED or
      AccessibilityEvent.TYPE_VIEW_FOCUSED or
      AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
      AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL
    info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
      AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
      AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
    serviceInfo = info

    val filter = IntentFilter("ACTION_DEACTIVATE_RESTRICT_UNINSTALL")
    LocalBroadcastManager.getInstance(this).registerReceiver(deactivateReceiver, filter)
  }

  private fun startForegroundService() {
    val channelId = "appshield_service_channel"
    val channelName = "AppShield Service"
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
      chan.lightColor = Color.BLUE
      chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
      val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      service.createNotificationChannel(chan)
    }

    val notification = NotificationCompat.Builder(this, channelId)
      .setContentTitle(getString(R.string.service_notification_title))
      .setContentText(getString(R.string.service_notification_text))
      .setSmallIcon(android.R.drawable.ic_dialog_info) // Use a standard icon
      .setPriority(NotificationCompat.PRIORITY_MIN)
      .setCategory(Notification.CATEGORY_SERVICE)
      .build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(1, notification)
    }
  }

  fun enableBlockAll() {
    allowAll = false
    persistBlockAll(true)
  }

  fun disableBlockAll() {
    allowAll = true
    persistBlockAll(false)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return

    val packageName = event.packageName?.toString() ?: return

    // Reload settings on each event to ensure we have the latest state
    allowAll = !getPersistedBlockAll()
    customAllowedApps = getPersistedCustomAllowedApps()

    // Combine default and custom allowlists
    val combinedAllowed = defaultAllowedPackages.toMutableSet()
    combinedAllowed.addAll(customAllowedApps)
    allowedApps = combinedAllowed.toList()

    if (allowAll) {
      return
    }

    val eventType = event.eventType
    val className = event.className?.toString() ?: ""
    val eventText = event.text?.joinToString() ?: ""

    if (!restrictUninstall) {
      disableSelf()
      return
    }

    // Enhanced OEM-specific blocking and protection logic
    if (className != null) {
      detectAppInfoScreen(event, packageName)
      detectSearchScreen(event, packageName)
    }

    // Handle OEM-specific UI dialogs
    if (className == "com.oplus.systemui.common.dialog.OplusThemeSystemUiDialog") {
      performGlobalAction(GLOBAL_ACTION_BACK)
    }

    restrictDeveloperOption(className, eventText)
    if (isResetSettingsScreen(eventText) && !eventText.contains("The battery is full")) {
      restrictAccess("Usage Restricted")
    }

    // Samsung-specific settings restrictions
    if (packageName == getSettingsPackage(packageManager)) {
      if (!eventText.contains("Security and privacy", ignoreCase = true) &&
          eventText.contains("Permission manager", ignoreCase = true)) {
        performGlobalAction(GLOBAL_ACTION_BACK)
      }
    }

    // Samsung device-specific protections
    if (isSamsungDevice()) {
      restrictForceStopFromRecentAppScreen(event, packageName)
    }

    // BHere's core app blocking logic with launcher/systemui checks
    if (!allowAll) {
      // Skip for own app, launcher, and system UI (BHere's exact logic)
      val defaultLauncher = getDefaultLauncherPackage()
      val systemUIPackages = getSystemUIPackages()
      
      if (packageName == applicationContext.packageName || 
          packageName == defaultLauncher || 
          systemUIPackages.contains(packageName)) {
        return
      }

      // BHere's exact event filtering
      if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
          event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
          event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
        
        blockApps(packageName)
      }
    }
  }

  override fun onInterrupt() {}


  private fun showBlockToast() {
    val now = System.currentTimeMillis()
    if (now - lastToastTime < toastIntervalMs) return
    lastToastTime = now

    Handler(Looper.getMainLooper()).post {
      Toast.makeText(
        applicationContext,
        "App blocked",
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  // Comprehensive system app allowlist for 2025
  private fun getComprehensiveSystemAllowlist(): List<String> {
    return listOf(
      // Core Android system packages
      "android",
      "com.android.systemui",
      "com.android.settings",
      "com.android.phone",
      "com.android.dialer",
      "com.android.contacts",
      "com.android.mms",
      "com.android.packageinstaller",
      "com.android.vending", // Play Store
      "com.google.android.gms",
      "com.google.android.gsf",
      "com.google.android.dialer",
      "com.google.android.contacts",
      "com.google.android.googlequicksearchbox",
      
      // Xiaomi MIUI specific (2025)
      "com.miui.securitycenter",
      "com.miui.securitycore",
      "com.miui.home",
      "com.mi.android.globallauncher",
      "com.miui.packageinstaller",
      "com.miui.systemui.statusbar",
      "com.miui.powerkeeper",
      "com.miui.optimizecenter",
      // Enhanced MIUI 14/15 system apps
      "com.miui.permcenter.autostart",
      "com.miui.permcenter",
      "com.xiaomi.finddevice",
      "com.miui.mishare.connectivity",
      "com.miui.daemon",
      "com.miui.core",
      "com.miui.systemAdSolution",
      "com.miui.analytics",
      "com.miui.hybrid",
      "com.miui.backup",
      "com.miui.cloudservice",
      "com.miui.cloudbackup",
      "com.xiaomi.account",
      "com.xiaomi.micloud.sdk",
      "com.mi.android.globalminusscreen",
      "com.miui.notification",
      "com.miui.touchassistant",
      "com.miui.securityadd",
      "com.miui.guardprovider",
      "com.miui.contentcatcher",
      "com.miui.system",
      "com.miui.weather2",
      "com.miui.calculator",
      "com.miui.clock",
      "com.miui.notes",
      "com.miui.compass",
      "com.miui.bugreport",
      "com.miui.fm",
      "com.miui.player",
      "com.miui.screenrecorder",
      "com.miui.screenshot",
      "com.miui.gallery",
      "com.miui.camera",
      
      // Samsung OneUI specific (2025)
      "com.samsung.android.packageinstaller",
      "com.samsung.android.bixby.agent",
      "com.samsung.android.app.galaxylabs",
      "com.samsung.android.oneconnect",
      "com.samsung.android.smartswitchassistant",
      "com.samsung.android.app.settings.bixby",
      "com.samsung.android.spay", // Samsung Pay
      "com.samsung.android.app.telephonyui",
      "com.samsung.android.messaging",
      
      // OnePlus OxygenOS specific (2025)
      "com.oneplus.launcher",
      "com.oneplus.security",
      "com.oplus.osense",
      "com.oneplus.aod",
      "com.oneplus.screenshot",
      "com.oneplus.gallery",
      "net.oneplus.widget",
      "com.oneplus.opbugs",
      
      // OPPO/Realme ColorOS specific (2025)
      "com.coloros.safecenter",
      "com.coloros.findmyphone",
      "com.coloros.smartsidebar",
      "com.oppo.quicksearchbox",
      "com.oplus.screenshot",
      "com.coloros.healthcheck",
      "com.coloros.securitypermission",
      "com.coloros.assistantscreen",
      
      // Huawei/Honor HarmonyOS specific (2025)
      "com.huawei.systemmanager",
      "com.hihonor.systemmanager",
      "com.huawei.android.launcher",
      "com.huawei.hwid",
      "com.huawei.appmarket",
      "com.huawei.health",
      "com.huawei.trustagent",
      
      // Vivo FuntouchOS/OriginOS specific (2025)
      "com.vivo.permissionmanager",
      "com.vivo.safe",
      "com.vivo.launcher",
      "com.vivo.smartshot",
      "com.vivo.easyshare",
      "com.vivo.assistant",
      "com.vivo.globalsearch",
      
      // Motorola specific (2025)
      "com.motorola.launcher3",
      "com.motorola.motocare",
      "com.motorola.democard",
      "com.motorola.timeweatherwidget",
      
      // Asus specific (2025)
      "com.asus.mobilemanager",
      "com.asus.launcher",
      "com.asus.zentalk",
      
      // LG specific (legacy support)
      "com.lge.packageinstaller",
      "com.lge.launcher3",
      
      // Sony specific
      "com.sonyericsson.packageinstaller",
      "com.sonymobile.home",
      
      // Essential system services and utilities
      "com.android.launcher3",
      "com.android.chrome",
      "com.google.android.apps.maps",
      "com.android.emergency",
      "com.android.cellbroadcastreceiver",
      "com.android.providers.downloads",
      "com.android.bluetooth",
      "com.android.nfc",
      "com.android.camera2",
      "com.android.gallery3d",
      "com.android.calculator2",
      "com.android.calendar",
      "com.android.deskclock",
      "com.android.soundrecorder",
      "com.android.music",
      "com.android.documentsui",
      "com.android.externalstorage",
      "com.android.keychain",
      "com.android.certinstaller",
      
      // Additional critical system packages
      "com.android.inputmethod.latin",
      "com.google.android.inputmethod.latin",
      "com.swiftkey.swiftkeyapp",
      "com.touchtype.swiftkey",
      
      // Network and connectivity
      "com.android.wifi.dialog",
      "com.android.wifi.resources",
      "com.android.networkstack.tethering",
      
      // Device admin and security
      "com.android.devicelock",
      "com.android.managedprovisioning",
      "com.android.work.identity"
    )
  }

  private fun blockApps(packageName: String) {
    if (!allowedApps.any { it.equals(packageName, ignoreCase = true) }
        && !packageName.contains("home", ignoreCase = true)
        && !packageName.contains("timeweatherwidget", ignoreCase = true)
        && !packageName.contains("ccc.ota", ignoreCase = true)
        && !packageName.contains("wallpaper", ignoreCase = true)
        && !packageName.contains("action", ignoreCase = true)
        && !packageName.contains("help", ignoreCase = true)
        && !packageName.contains("systemui", ignoreCase = true)
        && !packageName.contains("android.as", ignoreCase = true)
        && !packageName.contains("restore", ignoreCase = true)
        && !packageName.contains("android.voc", ignoreCase = true)
        && !packageName.contains("rsupport.aas2", ignoreCase = true)
        && !packageName.contains("android.lool", ignoreCase = true)
        && !packageName.contains(
            ".powersaving.g3",
            ignoreCase = true
        )
        && !packageName.contains("launcher", ignoreCase = true)) {
      Log.d("currentPackage name -->", packageName)
      if (packageName.contains("com.google.android.googlequicksearchbox")) {
        handleBlockingScenario(packageName, false)
      } else
        handleBlockingScenario(packageName, true)
    }
  }


  private fun detectAppInfoScreen(event: AccessibilityEvent, packageName: String) {
    val settingsPackages = listOf(
      "com.android.settings",
      "com.miui.securitycenter",
      "com.miui.securitycore",
      "com.motorola.launcher3",
      "com.motorola.motocare",
      "com.samsung.android.packageinstaller",
      "com.oneplus.security",
      "com.coloros.safecenter",
      "com.vivo.permissionmanager",
      "com.huawei.systemmanager"
    )
    
    if (settingsPackages.contains(packageName)) {
      val nodeInfo = event.source ?: return
      val appName = "AppShield"
      val appId = applicationContext.packageName

      val isAppInfoScreen = (containsTextInNode(nodeInfo, "App info") ||
          containsTextInNode(nodeInfo, "Force stop")) &&
          (containsTextInNode(nodeInfo, appId) || containsTextInNode(nodeInfo, appName))

      val isPermissionsScreen = containsTextInNode(nodeInfo, "Permissions") &&
          nodeInfo.findAccessibilityNodeInfosByText(appName).isNotEmpty()

      if (isAppInfoScreen || isPermissionsScreen) {
        restrictWithRetry()
      }
    }
  }

  private fun containsTextInNode(node: AccessibilityNodeInfo?, text: String): Boolean {
    if (node == null) return false
    if (node.text?.toString()?.contains(text, ignoreCase = true) == true) {
      return true
    }
    for (i in 0 until node.childCount) {
      if (containsTextInNode(node.getChild(i), text)) {
        return true
      }
    }
    return false
  }

  private fun restrictWithRetry() {
    performGlobalAction(GLOBAL_ACTION_BACK)
    performGlobalAction(GLOBAL_ACTION_HOME)
    Handler(Looper.getMainLooper()).postDelayed({
      restrictAccess("Usage Restricted")
    }, 100)
  }

  private fun restrictAccess(message: String) {
    val currentTime = System.currentTimeMillis()
    if ((currentTime - lastRestrictTime) < blockINTERVAL) return
    lastRestrictTime = currentTime
    
    performGlobalAction(GLOBAL_ACTION_BACK)
    Handler(Looper.getMainLooper()).postDelayed({
      performGlobalAction(GLOBAL_ACTION_HOME)
      showToast(message)
    }, 100)
  }

  private fun detectSearchScreen(event: AccessibilityEvent, packageName: String) {
    if (packageName.contains("com.android.settings")) {
      val nodeInfo = event.source ?: return

      if (listOf(
          AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
          AccessibilityEvent.TYPE_VIEW_FOCUSED,
          AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
          AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
          AccessibilityEvent.TYPE_VIEW_CLICKED
        ).contains(event.eventType)) {
        
        val inputText = nodeInfo.text?.toString() ?: return
        if (isRestrictedSearchQuery(inputText)) {
          restrictAccess("Access to permissions is restricted.")
        }
      }
    }
  }

  private fun isRestrictedSearchQuery(inputText: String): Boolean {
    return inputText.contains("permissions", ignoreCase = true) ||
        inputText.contains("per", ignoreCase = true) ||
        inputText.contains("perm", ignoreCase = true) ||
        inputText.contains("permiss", ignoreCase = true)
  }

  private fun restrictDeveloperOption(className: String, eventText: String) {
    if (className.contains("SettingsHomepageActivity") || 
        className.contains("SubSettings") || 
        className.contains("Develop")) {
      
      if (listOf(
          "Developer options", "Developer settings", "Developer mode",
          "Development settings", "Advanced settings", "System settings",
          "mock location"
        ).any { eventText.contains(it, true) }) {
        restrictAccess("Usage Restricted")
      }
    }
  }

  private fun isResetSettingsScreen(eventText: String): Boolean {
    val restrictedTerms = listOf(
      "Factory data reset", "Reset all settings", "Factory reset", "reset",
      "Digital Wellbeing", "Force stop", "stop", "Enabled apps", "storage",
      "battery", "Accessibility", "Power saving", "Battery Saver",
      "Second Space", "Reset accessibility"
    )
    return restrictedTerms.any { eventText.contains(it, ignoreCase = true) }
  }

  private fun getSettingsPackage(pm: PackageManager): String? {
    return try {
      val intent = Intent(Settings.ACTION_SETTINGS)
      val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
      resolveInfo?.activityInfo?.packageName
    } catch (_: Exception) { null }
  }

  private fun isSamsungDevice(): Boolean {
    return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
  }

  private fun restrictForceStopFromRecentAppScreen(event: AccessibilityEvent, packageName: String) {
    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      if (packageName.contains("com.android.systemui")) {
        val rootNode = rootInActiveWindow ?: return
        val selectedApp = restrictAppInRecentApps(rootNode)
        if (selectedApp) {
          Log.d("Samsung", "User selected app in Recent Apps!")
          restrictAccess("Restricted to stop")
        }
      }
    }
  }

  private fun restrictAppInRecentApps(rootNode: AccessibilityNodeInfo): Boolean {
    for (i in 0 until rootNode.childCount) {
      val child = rootNode.getChild(i) ?: continue
      val nodeText = child.text?.toString()
      
      if (nodeText?.contains("AppShield", ignoreCase = true) == true) {
        return true
      }
    }
    return false
  }

  private fun handleBlockingScenario(currentApp: String, showToast: Boolean) {
    performGlobalAction(GLOBAL_ACTION_HOME)
    Log.d("AppShieldService", "Blocking app: $currentApp")

    // Use a Handler to post-delay the force stop, giving time for the UI to respond.
    Handler(Looper.getMainLooper()).postDelayed({
      forceStopApp(currentApp)
    }, 200)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      requestAudioFocus()
    }

    Log.d("AppShieldService", "handleBlockingScenario completed, showToast: $showToast")
    if (showToast) {
      showToast("The App is restricted now")
    }

  }

  private fun forceStopApp(packageName: String) {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    activityManager?.killBackgroundProcesses(packageName)
  }

  private fun requestAudioFocus() {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
      .setOnAudioFocusChangeListener(focusChangeListener)
      .setAcceptsDelayedFocusGain(false)
      .setWillPauseWhenDucked(true)
      .build()

    val result = audioManager.requestAudioFocus(audioFocusRequest)
    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      Log.d("AppShieldService", "AUDIOFOCUS_REQUEST_GRANTED")
    } else {
      Log.d("AppShieldService", "Audio focus request failed")
    }
  }

  private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
    when (focusChange) {
      AudioManager.AUDIOFOCUS_LOSS -> {
        Log.d("AppShieldService", "AUDIOFOCUS_LOSS")
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
        Log.d("AppShieldService", "AUDIOFOCUS_LOSS_TRANSIENT")
      }
      AudioManager.AUDIOFOCUS_GAIN -> {
        Log.d("AppShieldService", "AUDIOFOCUS_GAIN")
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == "BLOCK_CURRENT_APP") {
      val currentApp = getCurrentRunningApp()
      if (getPersistedBlockAll()) {
        val customAllowed = getPersistedCustomAllowedApps()
        val defaultAllowed = defaultAllowedPackages.toMutableSet()
        defaultAllowed.addAll(customAllowed)
        val allowedApps = defaultAllowed.toList()
        if (currentApp != null) {
          blockApps(currentApp)
        }
      }
    }
    return START_STICKY
  }

  private fun getCurrentRunningApp(): String? {
    val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val time = System.currentTimeMillis()
    val appList = usageStatsManager.queryUsageStats(
      UsageStatsManager.INTERVAL_DAILY,
      time - 1000 * 10,
      time
    )

    if (appList != null && appList.isNotEmpty()) {
      val sortedAppList = appList.sortedWith(compareByDescending { it.lastTimeUsed })
      return sortedAppList[0].packageName
    }
    return null
  }

  fun setCustomAllowedApps(apps: Set<String>) {
    customAllowedApps = apps
  }

  fun setRestrictUninstall(active: Boolean) {
    restrictUninstall = active
  }

  private fun initializeDefaultAllowedApps() {
    val pm = packageManager
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    val installedApps = apps.filter { pm.getLaunchIntentForPackage(it.packageName) != null }
    val defaultAppsList = apps - installedApps.toSet()

    // Get system default apps explicitly (like BHere does)
    val callApp = getCallRelatedApps(pm)
    val smsApp = getDefaultSmsPackage(pm)
    val contactsApp = getContactsPackage(pm)
    val settingsApp = getSettingsPackage(pm)
    val systemUIPackages = getSystemUIPackages()
    val launcherPackage = getDefaultLauncherPackage()
    val gMapPackage = getGoogleMapsPackage(pm)
    val clockPackage = getClockPackage(pm)
    val calendarPackage = getCalendarPackage(pm)
    val miSystemList = getSystemApps(pm) // MI devices

    // Combine system default apps and dynamically found ones (BHere logic)
    val dynamicAllowedApps = listOfNotNull(
        applicationContext.packageName,
        launcherPackage,
        smsApp,
        contactsApp,
        gMapPackage,
        clockPackage,
        calendarPackage,
        settingsApp,
        "com.preff.kb.xm",
        "com.oppo.quicksearchbox",
        "com.google.android.contacts",
        "net.oneplus.widget",
        "com.coloros.smartsidebar",
        "com.oplus.screenshot"
    ) + systemUIPackages + callApp + defaultAppsList.map { it.packageName } + miSystemList

    // Add comprehensive system allowlist
    val allAllowedApps = (dynamicAllowedApps + getComprehensiveSystemAllowlist()).distinct()
    
    defaultAllowedPackages = allAllowedApps.map { it.lowercase() }.toSet()
    
    Log.d("AppShieldService", "Initialized ${defaultAllowedPackages.size} default allowed apps")
    
    // MIUI specific logging
    if (Build.MANUFACTURER.equals("xiaomi", ignoreCase = true)) {
      Log.d("AppShieldService", "MIUI device detected - enhanced allowlist active")
      val miuiApps = allAllowedApps.filter { it.contains("miui", ignoreCase = true) || it.contains("xiaomi", ignoreCase = true) }
      Log.d("AppShieldService", "MIUI apps in allowlist: ${miuiApps.size} - ${miuiApps.take(5)}")
    }
  }

  private fun loadCustomAllowedApps() {
    try {
      val prefs = applicationContext.getSharedPreferences("appshield_prefs", Context.MODE_PRIVATE)
      val customApps = prefs.getStringSet("custom_allowed_apps", emptySet()) ?: emptySet()
      customAllowedApps = customApps.map { it.lowercase() }.toSet()
      Log.d("AppShieldService", "Loaded ${customAllowedApps.size} custom allowed apps")
    } catch (e: Exception) {
      Log.e("AppShieldService", "Error loading custom allowed apps", e)
      customAllowedApps = emptySet()
    }
  }

  // Helper methods like BHere
  private fun getCallRelatedApps(pm: PackageManager): List<String> {
    val callApps = mutableListOf<String>()
    try {
      val intent = Intent(Intent.ACTION_CALL)
      intent.data = Uri.parse("tel:123456789")
      val activities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
      activities.forEach { 
        callApps.add(it.activityInfo.packageName)
      }
    } catch (_: Exception) {}
    return callApps
  }

  private fun getDefaultSmsPackage(pm: PackageManager): String? {
    return try {
      Telephony.Sms.getDefaultSmsPackage(applicationContext)
    } catch (_: Exception) { null }
  }

  private fun getContactsPackage(pm: PackageManager): String? {
    return try {
      val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
      val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
      resolveInfo?.activityInfo?.packageName
    } catch (_: Exception) { null }
  }

  private fun getSystemUIPackages(): List<String> {
    val systemUIApps = mutableListOf<String>()
    try {
      val pm = packageManager
      val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
      apps.forEach { appInfo ->
        val pkg = appInfo.packageName
        if (pkg.contains("systemui", ignoreCase = true)) {
          systemUIApps.add(pkg)
        }
      }
    } catch (_: Exception) {}
    return systemUIApps
  }

  private fun getDefaultLauncherPackage(): String? {
    return try {
      val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
      val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
      resolveInfo?.activityInfo?.packageName
    } catch (_: Exception) { null }
  }

  private fun getGoogleMapsPackage(pm: PackageManager): String? {
    return try {
      pm.getPackageInfo("com.google.android.apps.maps", 0)
      "com.google.android.apps.maps"
    } catch (_: Exception) { null }
  }

  private fun getClockPackage(pm: PackageManager): String? {
    return try {
      val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
      val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
      resolveInfo?.activityInfo?.packageName
    } catch (_: Exception) { null }
  }

  private fun getCalendarPackage(pm: PackageManager): String? {
    return try {
      val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
      val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
      resolveInfo?.activityInfo?.packageName
    } catch (_: Exception) { null }
  }

  private fun getSystemApps(pm: PackageManager): List<String> {
    val systemApps = mutableListOf<String>()
    try {
      val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
      apps.forEach { appInfo ->
        if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
          val pkg = appInfo.packageName
          // Add comprehensive OEM-specific system apps
          if (pkg.contains("miui", ignoreCase = true) || 
              pkg.contains("xiaomi", ignoreCase = true) ||
              pkg.startsWith("com.mi.") ||
              pkg.startsWith("com.miui.") ||
              pkg.contains("systemui", ignoreCase = true) ||
              pkg.contains("globalminusscreen", ignoreCase = true) ||
              pkg.contains("securitycenter", ignoreCase = true) ||
              pkg.contains("powerkeeper", ignoreCase = true) ||
              pkg.contains("optimizecenter", ignoreCase = true)) {
            systemApps.add(pkg)
          }
        }
      }
    } catch (_: Exception) {}
    return systemApps
  }

  override fun onDestroy() {
    super.onDestroy()
    if (activeInstance === this) activeInstance = null
    LocalBroadcastManager.getInstance(this).unregisterReceiver(deactivateReceiver)
  }

  private val deactivateReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      Log.d("AppShieldService", "Deactivate received")
      setRestrictUninstall(false)
    }
  }

  private fun showToast(message: String) {
    Log.d("AppShieldService", "showToast called with message: $message, toastEnabled: $toastEnabled")
    
    // Check if toasts are enabled
    if (!toastEnabled) {
      Log.d("AppShieldService", "Toast disabled by user setting")
      return
    }

    val currentTime = System.currentTimeMillis()
    val isSameMessage = message == lastToastMessage
    val isWithinTimeLimit = (currentTime - lastToastTime) < 5000

    if (isSameMessage && isWithinTimeLimit) {
      Log.d("AppShieldService", "Toast suppressed due to rate limiting")
      return
    }

    lastToastMessage = message
    lastToastTime = currentTime

    Log.d("AppShieldService", "Attempting to show toast: $message")
    
    Handler(Looper.getMainLooper()).post {
      try {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)
        toast.show()
        Log.d("AppShieldService", "Toast.show() called successfully")
      } catch (e: Exception) {
        Log.e("AppShieldService", "Failed to show toast: ${e.message}")
      }
    }
    
    // Also log system overlay permission status for troubleshooting
    val canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Settings.canDrawOverlays(applicationContext)
    } else {
      true // Permission not needed on older versions
    }
    
    Log.d("AppShieldService", "System might be suppressing toasts - this is common on MIUI devices")
    Log.d("AppShieldService", "Toast message would have been: '$message'")
    Log.d("AppShieldService", "Can draw overlays permission: $canDrawOverlays")
    Log.d("AppShieldService", "To enable toasts on MIUI: Settings > Apps > AppShield Example > Permissions > Display pop-up windows")
    Log.d("AppShieldService", "Or: Settings > Apps > AppShield Example > Notifications > Allow notifications")

    // Fallback: show a lightweight notification if notifications are enabled (helps on MIUI)
    try {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val notificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        nm.areNotificationsEnabled()
      } else {
        true
      }

      if (notificationsEnabled) {
        showFallbackNotification(message)
      } else {
        Log.d("AppShieldService", "Notifications disabled; fallback notification suppressed")
      }
    } catch (e: Exception) {
      Log.e("AppShieldService", "Failed to post fallback notification: ${e.message}")
    }
  }

  private fun showFallbackNotification(message: String) {
    val channelId = "appshield_toast_fallback_channel"
    val channelName = "AppShield Alerts"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      var chan = service.getNotificationChannel(channelId)
      if (chan == null) {
        chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        service.createNotificationChannel(chan)
      }
    }

    val notification = NotificationCompat.Builder(this, channelId)
      .setContentTitle(getString(R.string.usage_restricted))
      .setContentText(message)
      .setSmallIcon(android.R.drawable.ic_dialog_alert)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setAutoCancel(true)
      .build()

    try {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      nm.notify(1002, notification)
    } catch (e: Exception) {
      Log.e("AppShieldService", "Failed to show fallback notification: ${e.message}")
    }
  }

  private fun prefs() = applicationContext.getSharedPreferences("appshield_prefs", MODE_PRIVATE)
  private fun persistBlockAll(enabled: Boolean) {
    try { prefs().edit().putBoolean("block_all_enabled", enabled).apply() } catch (_: Exception) {}
  }
  private fun getPersistedBlockAll(): Boolean {
    return try { prefs().getBoolean("block_all_enabled", false) } catch (_: Exception) { false }
  }
  private fun getPersistedCustomAllowedApps(): Set<String> {
    return try {
      val customApps = prefs().getStringSet("custom_allowed_apps", emptySet())
      customApps?.map { it.lowercase() }?.toSet() ?: emptySet()
    } catch (_: Exception) {
      emptySet()
    }
  }
}


