import { NativeModules, Platform } from 'react-native';

type PermissionStatus = {
  // iOS
  screenTime?: boolean;
  // Android
  accessibility?: boolean;
  usageAccess?: boolean;
  notifications?: boolean;
};

export type AppShieldNative = {
  blockAllApps(): void;
  unblockAllApps(): void;
  requestRequiredPermissions(): Promise<PermissionStatus>;
};

const LINKING_ERROR =
  `The package 'AppShield' doesn't seem to be linked. Make sure:
\n\n` +
  Platform.select({
    ios: "- You have run 'pod install' in the ios folder\n",
    default: '',
  }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go (need Dev Build or Bare)\n';

const AppShieldModule = NativeModules.AppShield as AppShieldNative | undefined;

if (!AppShieldModule) {
  throw new Error(LINKING_ERROR);
}

export default AppShieldModule;
