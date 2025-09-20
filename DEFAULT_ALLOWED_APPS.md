# AppShield Default Allowed Apps List

## 📱 **System Apps Automatically Detected & Allowed**

The AppShield library now properly initializes like BHere with dynamic system app discovery. Here's the comprehensive list:

### ✅ **Core System Components**

```
- Your app package (automatically detected)
- Default launcher (dynamically detected)
- System UI packages (all variants detected)
- Settings app (dynamically detected)
- Package installer
- Phone app
- SMS app (default detected)
- Contacts app (dynamically detected)
- Dialer app (default detected)
```

### ✅ **Essential Communication Apps**

```
- com.android.phone
- com.android.dialer
- com.android.contacts
- com.android.mms
- com.google.android.dialer
- com.google.android.contacts
- Default SMS app (dynamically detected)
- Default dialer (dynamically detected)
```

### ✅ **System Utilities**

```
- com.android.settings
- com.android.packageinstaller
- com.google.android.packageinstaller
- Clock app (dynamically detected)
- Calendar app (dynamically detected)
- com.android.documentsui
- com.android.externalstorage
```

### ✅ **OEM-Specific Essential Apps**

#### **Xiaomi MIUI/HyperOS**

```
- com.miui.securitycenter
- com.miui.securitycore
- com.miui.home
- com.mi.android.globallauncher
- com.miui.packageinstaller
- com.miui.systemui.statusbar
- com.miui.powerkeeper
- com.miui.optimizecenter
- All MIUI system apps (dynamically detected)
```

#### **Samsung OneUI**

```
- com.samsung.android.packageinstaller
- com.samsung.android.bixby.agent
- com.samsung.android.app.galaxylabs
- com.samsung.android.oneconnect
- com.samsung.android.smartswitchassistant
- com.samsung.android.spay
- com.samsung.android.messaging
```

#### **OnePlus OxygenOS**

```
- com.oneplus.launcher
- com.oneplus.security
- com.oplus.osense
- com.oneplus.aod
- com.oneplus.screenshot
- com.oneplus.gallery
- net.oneplus.widget
- com.oneplus.opbugs
```

#### **OPPO/Realme ColorOS**

```
- com.coloros.safecenter
- com.coloros.findmyphone
- com.coloros.smartsidebar
- com.oppo.quicksearchbox
- com.oplus.screenshot
- com.coloros.healthcheck
- com.coloros.securitypermission
```

#### **Vivo FuntouchOS/OriginOS**

```
- com.vivo.permissionmanager
- com.vivo.safe
- com.vivo.launcher
- com.vivo.smartshot
- com.vivo.easyshare
- com.vivo.assistant
- com.vivo.globalsearch
```

#### **Huawei/Honor EMUI/HarmonyOS**

```
- com.huawei.systemmanager
- com.hihonor.systemmanager
- com.huawei.android.launcher
- com.huawei.hwid
- com.huawei.appmarket
- com.huawei.health
- com.huawei.trustagent
```

#### **Motorola**

```
- com.motorola.launcher3
- com.motorola.motocare
- com.motorola.democard
- com.motorola.timeweatherwidget
```

### ✅ **Google Services & Apps**

```
- com.google.android.gms
- com.google.android.gsf
- com.google.android.googlequicksearchbox
- com.google.android.apps.maps (if detected)
- com.android.chrome
- com.android.vending (Play Store)
```

### ✅ **Input Methods**

```
- com.android.inputmethod.latin
- com.google.android.inputmethod.latin
- com.swiftkey.swiftkeyapp
- com.touchtype.swiftkey
- com.preff.kb.xm (Xiaomi keyboard)
```

### ✅ **Connectivity & Network**

```
- com.android.bluetooth
- com.android.nfc
- com.android.wifi.dialog
- com.android.wifi.resources
- com.android.networkstack.tethering
```

### ✅ **Security & Device Management**

```
- com.android.devicelock
- com.android.managedprovisioning
- com.android.work.identity
- com.android.keychain
- com.android.certinstaller
```

### ✅ **Media & Camera**

```
- com.android.camera2
- com.android.gallery3d
- com.android.music
- com.android.soundrecorder
```

### ✅ **Emergency & Safety**

```
- com.android.emergency
- com.android.cellbroadcastreceiver
```

## 🔧 **Dynamic Detection Features**

### **Smart System Discovery**

The AppShield library now includes BHere's smart detection system:

1. **Launcher Detection**: Automatically finds the default launcher
2. **Default App Detection**: Discovers default SMS, dialer, contacts apps
3. **System UI Discovery**: Finds all SystemUI variants across OEMs
4. **OEM System Apps**: Dynamically detects manufacturer-specific system apps
5. **Call-Related Apps**: Discovers all apps that can handle phone calls

### **Pattern-Based Protection**

Additional apps are allowed based on patterns:

```
- Packages containing "launcher"
- Packages containing "home"
- Packages containing "systemui"
- Packages containing "timeweatherwidget"
- Packages containing "wallpaper"
- Packages containing "android.as"
```

## 📊 **Total App Count**

**Typical device allowlist size**: 150-300+ apps

- **Base system apps**: ~50 apps
- **OEM-specific**: ~30-50 apps
- **Dynamic detection**: ~20-30 apps
- **Comprehensive static list**: ~150 apps

## ✅ **Initialization Process**

The allowlist is now properly initialized like BHere:

1. **App Launch**: `AppShieldManager.initializeAllowedApps()` called
2. **Dynamic Discovery**: System scans for installed system apps
3. **OEM Detection**: Manufacturer-specific apps added
4. **Pattern Matching**: Additional apps allowed by pattern
5. **Persistence**: List saved for future use
6. **Service Ready**: Accessibility service can now block effectively

## 🚨 **Critical Fix Applied**

**Issue Fixed**: App blocking wasn't working because the allowlist was empty
**Solution**: Implemented BHere's dynamic app discovery system
**Result**: Proper allowlist of 150-300+ essential apps now protects device stability

The AppShield library now functions exactly like BHere with proper system app protection!

## 🔍 **Viewing Your Device's Allowlist**

To see the exact apps allowed on your device:

```typescript
// Get the full list of default allowed apps
const defaultApps = await AppShieldManager.getDefaultAllowedApps();
console.log(`${defaultApps.length} default apps allowed:`, defaultApps);

// Get custom allowed apps
const customApps = await AppShieldManager.getCustomAllowedApps();
console.log(`${customApps.length} custom apps allowed:`, customApps);
```

The example app now displays both lists in the UI!
