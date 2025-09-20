# AppShield - Simple React Native App Blocking

A focused React Native library for blocking all apps except essential system apps.

## Features

- 🚫 **Simple App Blocking**: Block all apps with one simple call
- 📱 **Essential Apps Only**: Only allows minimal system apps (launcher, phone, settings)
- 🔐 **Easy Permissions**: Handles required permissions automatically
- 🏭 **OEM Compatible**: Works reliably across different Android manufacturers
- 🔄 **Auto-Restart**: Automatically restarts after device reboot

## Installation

```bash
npm install react-native-app-shield
```

### Android Setup

Add required permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<!-- Essential permissions for app blocking -->
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="QueryAllPackagesPermission" />
```

Add the accessibility service to your app's `<application>` section:

```xml
<application>
  <!-- Accessibility Service for app blocking -->
  <service android:name="com.appshield.AppShieldAccessibilityService"
      android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
      android:exported="true">
      <intent-filter>
          <action android:name="android.accessibilityservice.AccessibilityService" />
      </intent-filter>
      <meta-data android:name="android.accessibilityservice"
          android:resource="@xml/accessibility_service_config" />
  </service>

  <!-- Boot receiver to restart after reboot -->
  <receiver android:name="com.appshield.BootReceiver"
      android:enabled="true"
      android:exported="true">
      <intent-filter android:priority="1000">
          <action android:name="android.intent.action.BOOT_COMPLETED" />
          <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
          <action android:name="android.intent.action.PACKAGE_REPLACED" />
          <data android:scheme="package" />
      </intent-filter>
  </receiver>
</application>
```

### iOS Setup

Currently Android only. iOS support would require Screen Time API integration.

## Usage

```javascript
import AppShieldManager from 'react-native-app-shield';

// Check permission status
const permissions = await AppShieldManager.getPermissionStatus();

// Request required permissions (opens settings)
await AppShieldManager.requestPermissions();

// Enable app blocking
AppShieldManager.blockAllApps();

// Disable app blocking
AppShieldManager.unblockAllApps();

// Check if blocking is active
const isActive = await AppShieldManager.isBlockingActive();

// Check if all permissions are granted
const allGranted = await AppShieldManager.arePermissionsGranted();
```

## API Reference

### Core Methods

#### `blockAllApps(): void`

Enable blocking for all apps except essential system apps.

#### `unblockAllApps(): void`

Disable app blocking - allow all apps.

#### `getPermissionStatus(): Promise<PermissionStatus>`

Get current status of required permissions.

```typescript
type PermissionStatus = {
  accessibility?: boolean; // Android accessibility service
  usageAccess?: boolean; // Android usage access permission
  screenTime?: boolean; // iOS Screen Time (future)
};
```

#### `requestPermissions(): Promise<PermissionStatus>`

Request all required permissions. Opens system settings for user to enable.

#### `isBlockingActive(): Promise<boolean>`

Check if app blocking is currently active.

#### `arePermissionsGranted(): Promise<boolean>`

Check if all required permissions are granted.

## Essential System Apps

When blocking is enabled, only these types of apps are allowed:

- **Launcher/Home**: Device launcher and home screen
- **System UI**: Status bar, navigation, system dialogs
- **Phone**: Dialer and phone app
- **Settings**: Device settings app
- **Package Installer**: For system updates
- **Your App**: The app using AppShield

All other apps will be blocked and users will be returned to the home screen.

## Permissions Required

### Android

1. **Accessibility Service**: Required to detect when apps are opened and block them
2. **Usage Access**: Required to identify which app is currently running

Both permissions require manual user approval in system settings.

## Example App

See the `example/` directory for a complete implementation showing:

- Permission checking and requesting
- Enabling/disabling app blocking
- Status monitoring
- Error handling

## Device Compatibility

### Tested Manufacturers

- Samsung (One UI)
- Xiaomi (MIUI)
- OnePlus (OxygenOS)
- OPPO/Realme (ColorOS)
- Vivo (FuntouchOS)
- Google Pixel
- Standard AOSP

### Requirements

- Android 5.0+ (API level 21+)
- Accessibility Service support
- Usage Stats permission support

## Troubleshooting

### Permissions Not Working

1. Ensure accessibility service is enabled in Settings > Accessibility
2. Ensure usage access is granted in Settings > Apps > Special Access > Usage Access
3. Restart the app after granting permissions

### Blocking Not Working

1. Check if `isBlockingActive()` returns true
2. Verify accessibility service is still enabled
3. Some OEMs may disable accessibility services - check device settings

### Service Stops After Reboot

1. The BootReceiver should automatically restart the service
2. Some devices may require manual restart of the accessibility service
3. Check if your app has battery optimization disabled

## Development

```bash
# Install dependencies
npm install

# Run example app
npm run example

# Android
npm run android

# iOS (not implemented)
npm run ios
```

## Contributing

1. Fork the repository
2. Create your feature branch
3. Add tests for new functionality
4. Submit a pull request

## License

MIT License - see LICENSE file for details

## Support

For issues and questions:

- 🐛 [Report a bug](https://github.com/your-repo/react-native-app-shield/issues)
- 💡 [Request a feature](https://github.com/your-repo/react-native-app-shield/issues)
