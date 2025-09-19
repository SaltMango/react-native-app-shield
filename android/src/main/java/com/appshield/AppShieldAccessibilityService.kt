package com.appshield

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent

class AppShieldAccessibilityService : AccessibilityService() {
  @Volatile private var allowAll = true
  @Volatile private var allowedPackages: Set<String> = emptySet()

  override fun onServiceConnected() {
    super.onServiceConnected()
    serviceInfo = AccessibilityServiceInfo().apply {
      eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
        AccessibilityEvent.TYPE_VIEW_CLICKED or
        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
        AccessibilityEvent.TYPE_VIEW_FOCUSED or
        AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
      feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
      flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
    }
  }

  fun enableBlockAll() {
    allowAll = false
    allowedPackages = emptySet()
  }

  fun disableBlockAll() {
    allowAll = true
    allowedPackages = emptySet()
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return
    if (allowAll) return
    val pkg = event.packageName?.toString() ?: return
    if (allowedPackages.isNotEmpty() && allowedPackages.contains(pkg)) return
    performGlobalAction(GLOBAL_ACTION_BACK)
    performGlobalAction(GLOBAL_ACTION_HOME)
  }

  override fun onInterrupt() {}
}


