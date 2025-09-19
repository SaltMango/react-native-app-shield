import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export type PermissionStatus = {
  // iOS
  screenTime?: boolean;
  // Android
  accessibility?: boolean;
  usageAccess?: boolean;
  notifications?: boolean;
};

export interface Spec extends TurboModule {
  blockAllApps(): void;
  unblockAllApps(): void;
  requestRequiredPermissions(): Promise<PermissionStatus>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('AppShield');
