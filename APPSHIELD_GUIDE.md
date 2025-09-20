# AppShield - Comprehensive App Blocking Library

## Overview

AppShield is a React Native library that provides comprehensive app blocking functionality with extensive OEM-specific support. It implements all the advanced app blocking logic from the BHere app, including protection against bypass attempts and comprehensive system app allowlists.

## Features

### ✅ **Complete Implementation from BHere**

- **Advanced Accessibility Service**: Comprehensive monitoring and blocking with OEM-specific logic
- **System Protection**: Prevents uninstall attempts, settings bypass, and developer options access
- **Custom Allowlists**: Support for custom app allowlists with persistent storage
- **Audio Focus Management**: Prevents background audio when blocking apps

### ✅ **Enhanced OEM Support (2025 Updated)**

- **Samsung OneUI**: Complete support with Samsung-specific protections
- **Xiaomi MIUI/HyperOS**: Full compatibility with MIUI security features
- **OnePlus OxygenOS**: Comprehensive OnePlus-specific handling
- **OPPO/Realme ColorOS**: Complete ColorOS integration
- **Vivo FuntouchOS/OriginOS**: Full Vivo-specific support
- **Huawei/Honor EMUI/HarmonyOS**: Complete Huawei ecosystem support
- **Motorola**: Full Motorola-specific handling
- **Stock Android**: Complete support for AOSP and Pixel devices

### ✅ **Comprehensive System Allowlist**

- **150+ Essential System Apps**: Prevents device breaking
- **Core Android Services**: All critical system services protected
- **OEM-Specific Apps**: Manufacturer-specific essential apps
- **Input Methods**: Keyboard and IME protection
- **Network Services**: Connectivity and system communication
- **Security Services**: Device admin and security components

### ✅ **Advanced Protection Features**

- **Uninstall Protection**: Prevents app removal attempts
- **Settings Bypass Protection**: Blocks access to app info and permissions
- **Developer Options Blocking**: Prevents developer mode activation
- **Search Query Filtering**: Blocks permission-related searches
- **Force Stop Protection**: Prevents app termination from recents
- **Factory Reset Prevention**: Blocks reset attempts

## Installation

```bash
npm install react-native-app-shield
# or
yarn add react-native-app-shield
```

For React Native 0.60+, autolinking will handle the native modules automatically.

For older versions:

```bash
react-native link react-native-app-shield
```

## iOS Setup

Add the following to your iOS app's Info.plist:

```xml
<key>NSScreenTimeUsageDescription</key>
<string>This app uses Screen Time to block access to specified applications.</string>
```

## Android Setup

The library requires two critical permissions:

1. **Accessibility Service Permission**
2. **Usage Access Permission**

### Accessibility Service Setup

Add to your app's `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
```

## Usage

### Basic Setup

```typescript
import AppShield from 'react-native-app-shield';

// Quick setup with device compatibility check
const setupResult = await AppShield.quickSetup();
if (setupResult.success) {
  console.log('AppShield ready!');
} else {
  console.log('Setup required:', setupResult.message);
}
```

### Permission Management

```typescript
// Check current permissions
const permissions = await AppShield.getPermissionStatus();
console.log('Accessibility:', permissions.accessibility);
console.log('Usage Access:', permissions.usageAccess);

// Request permissions (opens system settings)
const newPermissions = await AppShield.requestPermissions();

// Check if all permissions are granted
const allGranted = await AppShield.arePermissionsGranted();
```

### Core Blocking Functionality

```typescript
// Enable app blocking (blocks all apps except essential system apps)
AppShield.blockAllApps();

// Disable app blocking (allows all apps)
AppShield.unblockAllApps();

// Check if blocking is currently active
const isActive = await AppShield.isBlockingActive();
```

### Custom App Management

```typescript
// Set custom allowed apps that won't be blocked
const allowedApps = [
  'com.example.educational.app',
  'com.example.productivity.app',
];
AppShield.setCustomAllowedApps(allowedApps);

// Get current custom allowed apps
const currentAllowed = await AppShield.getCustomAllowedApps();

// Get list of installed apps (Android only)
const installedApps = await AppShield.getInstalledApps();
```

### Device Compatibility

```typescript
// Check device compatibility and OEM requirements
const compatibility = await AppShield.checkDeviceCompatibility();
console.log('OEM:', compatibility.oemInfo.oem);
console.log('UI:', compatibility.oemInfo.ui);
console.log(
  'Requires special permissions:',
  compatibility.oemInfo.requiresSpecialPermissions
);

if (compatibility.oemInfo.requiresSpecialPermissions) {
  // Show additional setup instructions for the specific OEM
}
```

### Complete Status Check

```typescript
// Get comprehensive status
const fullStatus = await AppShield.getFullStatus();
console.log('Permissions:', fullStatus.permissions);
console.log('Device compatibility:', fullStatus.compatibility);
console.log('Currently blocking:', fullStatus.isBlocking);
console.log('Custom allowed apps:', fullStatus.customAllowedApps);
```

## OEM-Specific Implementation

### Samsung Devices

- **Recent Apps Protection**: Prevents force stopping from recent apps screen
- **Bixby Integration**: Handles Samsung-specific UI elements
- **Galaxy Labs Compatibility**: Works with Samsung's app ecosystem

### Xiaomi MIUI/HyperOS

- **Security Center Integration**: Works with MIUI Security app
- **Autostart Management**: Handles MIUI's autostart restrictions
- **Battery Optimization**: Manages MIUI's aggressive battery optimization

### OnePlus OxygenOS

- **OxygenOS UI Handling**: Specific support for OnePlus UI elements
- **Gaming Mode Compatibility**: Works with OnePlus gaming features
- **Zen Mode Integration**: Compatible with OnePlus focus features

### OPPO/Realme ColorOS

- **ColorOS Security**: Integration with OPPO's security framework
- **App Freezing Prevention**: Prevents ColorOS from freezing the service
- **Smart Sidebar Compatibility**: Works with OPPO's smart features

## System Apps Allowlist

AppShield includes a comprehensive allowlist of 150+ essential system apps:

### Core Android Components

- System UI and framework components
- Phone, contacts, and messaging apps
- Settings and package installer
- Google Play Services and Store

### Input and Communication

- All major keyboard apps (Google, SwiftKey, Samsung, etc.)
- Network and connectivity services
- Bluetooth and NFC services

### OEM-Specific Essential Apps

- Manufacturer launchers and home apps
- OEM security and system management apps
- Essential OEM services and frameworks

### Developer and System Tools

- Android debugging and development tools
- System restore and recovery services
- Device administration components

## Advanced Features

### Uninstall Protection

Prevents users from uninstalling the app through:

- Settings app info screens
- Package manager interfaces
- OEM-specific uninstall methods

### Settings Bypass Prevention

Blocks access to:

- App info and permissions screens
- Developer options
- Factory reset options
- Accessibility service settings

### Search Query Filtering

Prevents users from searching for:

- "Permissions" in settings
- "Developer options"
- App-specific settings

## Error Handling

```typescript
try {
  await AppShield.quickSetup();
} catch (error) {
  console.error('AppShield setup failed:', error);

  // Handle specific error types
  if (error.code === 'PERMISSIONS_DENIED') {
    // Guide user to enable required permissions
  } else if (error.code === 'OEM_COMPATIBILITY') {
    // Show OEM-specific setup instructions
  }
}
```

## Best Practices

### 1. Always Check Compatibility First

```typescript
const compatibility = await AppShield.checkDeviceCompatibility();
if (compatibility.oemInfo.requiresSpecialPermissions) {
  // Show OEM-specific guidance
}
```

### 2. Handle Permission Requests Gracefully

```typescript
const permissions = await AppShield.requestPermissions();
if (!permissions.accessibility) {
  // Guide user to accessibility settings
}
if (!permissions.usageAccess) {
  // Guide user to usage access settings
}
```

### 3. Provide Clear User Guidance

Different OEMs have different permission flows. Always check the device compatibility and provide manufacturer-specific instructions.

### 4. Test on Multiple Devices

The blocking behavior can vary significantly between OEMs. Test on devices from major manufacturers.

## Troubleshooting

### Accessibility Service Not Working

1. Check if the service is enabled in Settings > Accessibility
2. Ensure usage access permission is granted
3. Verify the app isn't being killed by battery optimization
4. Check for OEM-specific permission requirements

### Apps Not Being Blocked

1. Verify all permissions are granted
2. Check if the app is in the system allowlist
3. Ensure the accessibility service is active
4. Test with a known non-system app

### Service Stops Working

1. Disable battery optimization for your app
2. Enable autostart (on MIUI/ColorOS/FuntouchOS)
3. Check if the service was disabled by the system
4. Restart the accessibility service

## Technical Implementation Details

### Android Accessibility Service

- **Event Types**: Window state changes, content changes, view clicks
- **Flags**: Report view IDs, retrieve interactive windows
- **Performance**: Optimized event filtering to prevent excessive calls
- **Memory**: Efficient pattern matching and caching

### iOS Screen Time Integration

- **Family Controls Framework**: Native iOS app blocking
- **Managed Settings**: Centralized restriction management
- **Authorization Center**: Permission management
- **Shield Configuration**: App and category blocking

### Security Considerations

- **Process Isolation**: Service runs in separate process for stability
- **Permission Validation**: Continuous permission state monitoring
- **Bypass Prevention**: Multiple layers of protection against circumvention
- **Data Encryption**: Sensitive configuration data is encrypted

## API Reference

### Core Methods

- `blockAllApps()`: Enable comprehensive app blocking
- `unblockAllApps()`: Disable all app blocking
- `isBlockingActive()`: Check current blocking status
- `getPermissionStatus()`: Get current permission state
- `requestPermissions()`: Request required permissions

### App Management

- `setCustomAllowedApps(apps: string[])`: Set custom allowlist
- `getCustomAllowedApps()`: Get current custom allowlist
- `getInstalledApps()`: Get installed apps (Android only)

### Device Information

- `checkDeviceCompatibility()`: Get device and OEM information
- `getFullStatus()`: Get comprehensive status
- `quickSetup()`: Automated setup with guidance

## License

MIT License - see LICENSE file for details.

## Support

For issues related to specific OEMs or devices, please provide:

1. Device manufacturer and model
2. Android/iOS version
3. App blocking behavior observed
4. Steps to reproduce the issue

## Contributing

Please read CONTRIBUTING.md for guidelines on contributing to this project.
