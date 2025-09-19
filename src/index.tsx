import Native from './NativeAppShield';

export const AppShield = {
  blockAllApps: () => Native.blockAllApps(),
  unblockAllApps: () => Native.unblockAllApps(),
  requestRequiredPermissions: () => Native.requestRequiredPermissions(),
};

export default AppShield;
