import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export type PermissionStatus = {
  // iOS
  screenTime?: boolean;
  // Android - only essential permissions for app blocking
  accessibility?: boolean;
  usageAccess?: boolean;
  // Enhanced status
  blockingActive?: boolean;
};

export type AppInfo = {
  packageName: string;
  appName: string;
  isSystemApp: boolean;
};

export type DeviceCompatibility = {
  platform: string;
  manufacturer?: string;
  model?: string;
  version: string;
  sdkInt?: number;
  accessibilitySupported?: boolean;
  accessibilityEnabled?: boolean;
  usageAccessSupported?: boolean;
  usageAccessGranted?: boolean;
  screenTimeSupported?: boolean;
  screenTimeEnabled?: boolean;
  requiresSpecialPermissions: boolean;
  oemInfo: {
    oem: string;
    ui: string;
    requiresSpecialPermissions: boolean;
  };
};

export interface Spec extends TurboModule {
  // Core blocking functionality
  blockAllApps(): void;
  unblockAllApps(): void;

  // Essential permission management
  getPermissionStatus(): Promise<PermissionStatus>;
  requestPermissions(): Promise<PermissionStatus>;

  // Check if service is active
  isBlockingActive(): Promise<boolean>;

  // iOS Screen Time authorization (stub for Android)
  requestScreenTimeAuthorization(): Promise<{ screenTime: boolean }>;

  // Enhanced app management
  setCustomAllowedApps(apps: string[]): void;
  getCustomAllowedApps(): Promise<string[]>;
  getInstalledApps(): Promise<AppInfo[]>;
  checkDeviceCompatibility(): Promise<DeviceCompatibility>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('AppShield');
