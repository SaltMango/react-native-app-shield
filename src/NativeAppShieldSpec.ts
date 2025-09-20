import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  // Core blocking functionality
  blockAllApps(): void;
  unblockAllApps(): void;

  // Essential permission management
  getPermissionStatus(): Promise<Object>;
  requestPermissions(): Promise<Object>;

  // Check if service is active
  isBlockingActive(): Promise<boolean>;

  // iOS Screen Time authorization (stub for Android)
  requestScreenTimeAuthorization(): Promise<Object>;

  // Enhanced app management
  setCustomAllowedApps(apps: string[]): void;
  getCustomAllowedApps(): Promise<string[]>;
  getInstalledApps(): Promise<Object[]>;
  checkDeviceCompatibility(): Promise<Object>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('AppShield');
