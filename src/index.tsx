import { NativeModules, Platform } from 'react-native';
import {
  type PermissionStatus,
  type AppInfo,
  type DeviceCompatibility,
} from './NativeAppShield';

const LINKING_ERROR =
  `The package 'react-native-app-shield' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({
    ios: "- You have run 'cd ios && pod install'\n",
    default: '',
  }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// Use the reliable old architecture for better compatibility
const AppShield = NativeModules.AppShield;

if (!AppShield) {
  throw new Error(LINKING_ERROR);
}

/**
 * AppShield - A comprehensive React Native library for blocking apps with OEM-specific support
 *
 * Enhanced functionality:
 * - Block/unblock all apps with comprehensive OEM support
 * - Extensive system app allowlist to prevent device breaking
 * - Custom app allowlist management
 * - Advanced permission handling with OEM-specific logic
 * - Device compatibility checking
 * - Works reliably across all major Android OEMs (Samsung, Xiaomi, OnePlus, Oppo, Vivo, Huawei, etc.)
 * - iOS Screen Time integration
 * - Protection against app uninstall and permission bypass attempts
 */
class AppShieldManager {
  /**
   * Enable blocking for all apps except essential system apps
   */
  static blockAllApps(): void {
    return AppShield.blockAllApps();
  }

  /**
   * Disable app blocking - allow all apps
   */
  static unblockAllApps(): void {
    return AppShield.unblockAllApps();
  }

  /**
   * Get current status of required permissions
   */
  static async getPermissionStatus(): Promise<PermissionStatus> {
    return AppShield.getPermissionStatus();
  }

  /**
   * Request all required permissions for app blocking
   * This will guide users through enabling accessibility and usage access
   */
  static async requestPermissions(): Promise<PermissionStatus> {
    return AppShield.requestPermissions();
  }

  /**
   * Check if app blocking is currently active
   */
  static async isBlockingActive(): Promise<boolean> {
    return AppShield.isBlockingActive();
  }

  /**
   * Check if all required permissions are granted
   */
  static async arePermissionsGranted(): Promise<boolean> {
    const permissions = await this.getPermissionStatus();

    if (Platform.OS === 'android') {
      return Boolean(permissions.accessibility && permissions.usageAccess);
    } else {
      return Boolean(permissions.screenTime);
    }
  }

  /**
   * Set custom allowed apps that won't be blocked
   * @param apps Array of package names (Android) or bundle IDs (iOS)
   */
  static setCustomAllowedApps(apps: string[]): void {
    return AppShield.setCustomAllowedApps(apps);
  }

  /**
   * Get list of custom allowed apps
   */
  static async getCustomAllowedApps(): Promise<string[]> {
    return AppShield.getCustomAllowedApps();
  }

  /**
   * Get list of default allowed apps (system apps)
   * Android only - returns empty array on iOS
   */
  static async getDefaultAllowedApps(): Promise<string[]> {
    if (Platform.OS === 'android') {
      return AppShield.getDefaultAllowedApps();
    }
    return [];
  }

  /**
   * Initialize the default allowed apps list
   * Android only - triggers dynamic discovery of system apps
   */
  static initializeAllowedApps(): void {
    if (Platform.OS === 'android') {
      AppShield.initializeAllowedApps();
    }
  }

  /**
   * Open autostart settings for MIUI and other OEMs
   * Essential for MIUI devices to ensure app blocking works
   */
  static async openAutoStartSettings(): Promise<boolean> {
    if (Platform.OS === 'android') {
      return AppShield.openAutoStartSettings();
    }
    return false;
  }

  /**
   * Open battery optimization settings
   * Important for MIUI and other OEMs to prevent service killing
   */
  static async openBatteryOptimizationSettings(): Promise<boolean> {
    if (Platform.OS === 'android') {
      return AppShield.openBatteryOptimizationSettings();
    }
    return false;
  }

  /**
   * Check if device is MIUI (Xiaomi/Redmi)
   * Returns true for Xiaomi/Redmi devices that need special permissions
   */
  static async isMIUIDevice(): Promise<boolean> {
    if (Platform.OS === 'android') {
      return AppShield.isMIUIDevice();
    }
    return false;
  }

  /**
   * Get list of installed apps (Android only, returns empty array on iOS)
   */
  static async getInstalledApps(): Promise<AppInfo[]> {
    return AppShield.getInstalledApps();
  }

  /**
   * Check device compatibility and OEM-specific requirements
   */
  static async checkDeviceCompatibility(): Promise<DeviceCompatibility> {
    return AppShield.checkDeviceCompatibility();
  }

  /**
   * Get comprehensive device and permission status
   */
  static async getFullStatus(): Promise<{
    permissions: PermissionStatus;
    compatibility: DeviceCompatibility;
    isBlocking: boolean;
    customAllowedApps: string[];
  }> {
    const [permissions, compatibility, isBlocking, customAllowedApps] =
      await Promise.all([
        this.getPermissionStatus(),
        this.checkDeviceCompatibility(),
        this.isBlockingActive(),
        this.getCustomAllowedApps(),
      ]);

    return {
      permissions,
      compatibility,
      isBlocking,
      customAllowedApps,
    };
  }

  /**
   * Quick setup method that handles permissions and initial configuration
   */
  static async quickSetup(): Promise<{
    success: boolean;
    permissions: PermissionStatus;
    compatibility: DeviceCompatibility;
    message: string;
  }> {
    try {
      const compatibility = await this.checkDeviceCompatibility();
      const permissions = await this.requestPermissions();
      const allGranted = await this.arePermissionsGranted();

      let message = 'Setup completed successfully';
      if (!allGranted) {
        if (Platform.OS === 'android') {
          message =
            'Please enable Accessibility Service and Usage Access permissions';
        } else {
          message = 'Please enable Screen Time permissions';
        }
      }

      if (compatibility.oemInfo.requiresSpecialPermissions) {
        message += `. Note: ${compatibility.oemInfo.oem} devices may require additional permissions.`;
      }

      return {
        success: allGranted,
        permissions,
        compatibility,
        message,
      };
    } catch (error) {
      return {
        success: false,
        permissions: {},
        compatibility: {} as DeviceCompatibility,
        message: `Setup failed: ${error instanceof Error ? error.message : 'Unknown error'}`,
      };
    }
  }

  /**
   * Enable or disable toast messages when apps are blocked
   * @param enabled - true to show toast messages, false to disable them
   */
  static setToastEnabled(enabled: boolean): void {
    return AppShield.setToastEnabled(enabled);
  }

  /**
   * Check if toast messages are currently enabled
   * @returns Promise<boolean> - true if toast messages are enabled
   */
  static async isToastEnabled(): Promise<boolean> {
    return AppShield.isToastEnabled();
  }

  /**
   * Request notification permission (required for toasts on Android 13+)
   * Opens the notification settings page for the app
   * @returns Promise<boolean> - true if settings were opened successfully
   */
  static async requestNotificationPermission(): Promise<boolean> {
    if (Platform.OS === 'android') {
      return AppShield.requestNotificationPermission();
    }
    return true; // Not needed on iOS
  }
}

export type { PermissionStatus, AppInfo, DeviceCompatibility };
export { AppShieldManager };
export default AppShieldManager;
