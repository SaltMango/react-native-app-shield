import { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  Alert,
  Platform,
  ScrollView,
} from 'react-native';
import AppShieldManager, {
  type PermissionStatus,
  type DeviceCompatibility,
} from 'react-native-app-shield';

export default function App() {
  const [permissions, setPermissions] = useState<PermissionStatus>({});
  const [isBlocking, setIsBlocking] = useState(false);
  const [loading, setLoading] = useState(false);
  const [compatibility, setCompatibility] =
    useState<DeviceCompatibility | null>(null);
  const [customAllowedApps, setCustomAllowedApps] = useState<string[]>([]);
  const [defaultAllowedApps, setDefaultAllowedApps] = useState<string[]>([]);
  const [isMIUI, setIsMIUI] = useState(false);

  useEffect(() => {
    initializeApp();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const initializeApp = async () => {
    // Initialize the allowed apps list first (critical for blocking to work)
    AppShieldManager.initializeAllowedApps();

    await checkPermissions();
    await checkBlockingStatus();
    await checkDeviceCompatibility();
    await loadCustomAllowedApps();
    await loadDefaultAllowedApps();
    await checkMIUIDevice();
  };

  const checkPermissions = async () => {
    try {
      const permissionStatus = await AppShieldManager.getPermissionStatus();
      setPermissions(permissionStatus);
    } catch (error) {
      console.error('Error checking permissions:', error);
    }
  };

  const checkBlockingStatus = async () => {
    try {
      const blockingStatus = await AppShieldManager.isBlockingActive();
      setIsBlocking(blockingStatus);
    } catch (error) {
      console.error('Error checking blocking status:', error);
    }
  };

  const checkDeviceCompatibility = async () => {
    try {
      const deviceCompatibility =
        await AppShieldManager.checkDeviceCompatibility();
      setCompatibility(deviceCompatibility);
    } catch (error) {
      console.error('Error checking device compatibility:', error);
    }
  };

  const loadCustomAllowedApps = async () => {
    try {
      const apps = await AppShieldManager.getCustomAllowedApps();
      setCustomAllowedApps(apps);
    } catch (error) {
      console.error('Error loading custom allowed apps:', error);
    }
  };

  const loadDefaultAllowedApps = async () => {
    try {
      const apps = await AppShieldManager.getDefaultAllowedApps();
      setDefaultAllowedApps(apps);
      console.log(`Loaded ${apps.length} default allowed apps`);
    } catch (error) {
      console.error('Error loading default allowed apps:', error);
    }
  };

  const checkMIUIDevice = async () => {
    try {
      const miuiDevice = await AppShieldManager.isMIUIDevice();
      setIsMIUI(miuiDevice);
      if (miuiDevice) {
        console.log(
          'MIUI device detected - special permissions may be required'
        );
      }
    } catch (error) {
      console.error('Error checking MIUI device:', error);
    }
  };

  const openAutoStartSettings = async () => {
    try {
      await AppShieldManager.openAutoStartSettings();
    } catch (error) {
      console.error('Error opening autostart settings:', error);
    }
  };

  const openBatteryOptimizationSettings = async () => {
    try {
      await AppShieldManager.openBatteryOptimizationSettings();
    } catch (error) {
      console.error('Error opening battery optimization settings:', error);
    }
  };

  const requestPermissions = async () => {
    try {
      setLoading(true);
      const permissionStatus = await AppShieldManager.requestPermissions();
      setPermissions(permissionStatus);

      const allGranted = await AppShieldManager.arePermissionsGranted();

      Alert.alert(
        allGranted ? 'Success' : 'Incomplete',
        allGranted
          ? 'All permissions granted successfully!'
          : 'Some permissions are still missing. Please enable them in settings.'
      );
    } catch (error) {
      Alert.alert('Error', `Failed to request permissions: ${error}`);
    } finally {
      setLoading(false);
    }
  };

  const toggleBlocking = async () => {
    try {
      setLoading(true);

      const allGranted = await AppShieldManager.arePermissionsGranted();
      if (!allGranted) {
        Alert.alert(
          'Permissions Required',
          'Please grant all required permissions first.',
          [{ text: 'Grant Permissions', onPress: requestPermissions }]
        );
        return;
      }

      if (isBlocking) {
        AppShieldManager.unblockAllApps();
        setIsBlocking(false);
        Alert.alert('Success', 'App blocking disabled');
      } else {
        AppShieldManager.blockAllApps();
        setIsBlocking(true);
        Alert.alert(
          'Success',
          'App blocking enabled. Only essential system apps will be allowed.'
        );
      }
    } catch (error) {
      Alert.alert('Error', `Failed to toggle blocking: ${error}`);
    } finally {
      setLoading(false);
    }
  };

  const renderPermissionStatus = () => {
    if (Platform.OS !== 'android') {
      return (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>iOS Support</Text>
          <Text style={styles.infoText}>
            iOS app blocking requires Screen Time API integration.
          </Text>
        </View>
      );
    }

    return (
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Required Permissions</Text>

        <View style={styles.permissionItem}>
          <Text style={styles.permissionText}>Accessibility Service</Text>
          <View
            style={[
              styles.statusBadge,
              permissions.accessibility ? styles.granted : styles.denied,
            ]}
          >
            <Text style={styles.statusText}>
              {permissions.accessibility ? 'Granted' : 'Required'}
            </Text>
          </View>
        </View>

        <View style={styles.permissionItem}>
          <Text style={styles.permissionText}>Usage Access</Text>
          <View
            style={[
              styles.statusBadge,
              permissions.usageAccess ? styles.granted : styles.denied,
            ]}
          >
            <Text style={styles.statusText}>
              {permissions.usageAccess ? 'Granted' : 'Required'}
            </Text>
          </View>
        </View>

        <TouchableOpacity
          style={styles.button}
          onPress={requestPermissions}
          disabled={loading}
        >
          <Text style={styles.buttonText}>
            {loading ? 'Requesting...' : 'Request Permissions'}
          </Text>
        </TouchableOpacity>
      </View>
    );
  };

  return (
    <ScrollView style={{}}>
      <View style={styles.container}>
        <Text style={styles.title}>AppShield Demo</Text>
        <Text style={styles.subtitle}>
          Comprehensive App Blocking with OEM Support
        </Text>

        {compatibility && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Device Information</Text>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Manufacturer:</Text>
              <Text style={styles.infoValue}>
                {compatibility.manufacturer || 'Unknown'}
              </Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Model:</Text>
              <Text style={styles.infoValue}>
                {compatibility.model || 'Unknown'}
              </Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>OS Version:</Text>
              <Text style={styles.infoValue}>{compatibility.version}</Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>OEM UI:</Text>
              <Text style={styles.infoValue}>
                {compatibility.oemInfo.oem} ({compatibility.oemInfo.ui})
              </Text>
            </View>
            {compatibility.oemInfo.requiresSpecialPermissions && (
              <Text style={styles.warningText}>
                ⚠️ This device may require additional OEM-specific permissions
              </Text>
            )}
          </View>
        )}

        {renderPermissionStatus()}

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>App Blocking</Text>

          <View style={styles.statusContainer}>
            <Text style={styles.statusLabel}>Status:</Text>
            <Text
              style={[
                styles.statusValue,
                isBlocking ? styles.statusBlocking : styles.statusAllowed,
              ]}
            >
              {isBlocking ? 'BLOCKING ACTIVE' : 'APPS ALLOWED'}
            </Text>
          </View>

          <TouchableOpacity
            style={[
              styles.button,
              isBlocking ? styles.dangerButton : styles.successButton,
            ]}
            onPress={toggleBlocking}
            disabled={loading}
          >
            <Text style={styles.buttonText}>
              {loading
                ? 'Processing...'
                : isBlocking
                  ? 'Disable Blocking'
                  : 'Enable Blocking'}
            </Text>
          </TouchableOpacity>

          <Text style={styles.infoText}>
            When blocking is enabled, only essential system apps and custom
            allowed apps are permitted.
          </Text>

          {defaultAllowedApps.length > 0 && (
            <View style={styles.allowedAppsContainer}>
              <Text style={styles.allowedAppsTitle}>
                Default System Apps: {defaultAllowedApps.length}
              </Text>
              <Text style={styles.allowedAppsText}>
                {defaultAllowedApps.slice(0, 5).join(', ')}
                {defaultAllowedApps.length > 5 && '...'}
              </Text>
            </View>
          )}

          {customAllowedApps.length > 0 && (
            <View style={styles.allowedAppsContainer}>
              <Text style={styles.allowedAppsTitle}>
                Custom Allowed Apps: {customAllowedApps.length}
              </Text>
              <Text style={styles.allowedAppsText}>
                {customAllowedApps.slice(0, 3).join(', ')}
                {customAllowedApps.length > 3 && '...'}
              </Text>
            </View>
          )}

          {isMIUI && (
            <View style={styles.miuiSection}>
              <Text style={styles.miuiTitle}>⚠️ MIUI Device Detected</Text>
              <Text style={styles.miuiDescription}>
                For app blocking to work properly on MIUI, you need to:
              </Text>
              <View style={styles.miuiSteps}>
                <Text style={styles.miuiStep}>
                  1. Enable AutoStart permission
                </Text>
                <Text style={styles.miuiStep}>
                  2. Disable battery optimization
                </Text>
                <Text style={styles.miuiStep}>
                  3. Allow "Display pop-up windows while running in background"
                </Text>
              </View>

              <View style={styles.buttonRow}>
                <TouchableOpacity
                  style={[styles.button, styles.miuiButton]}
                  onPress={openAutoStartSettings}
                >
                  <Text style={styles.buttonText}>AutoStart Settings</Text>
                </TouchableOpacity>

                <TouchableOpacity
                  style={[styles.button, styles.miuiButton]}
                  onPress={openBatteryOptimizationSettings}
                >
                  <Text style={styles.buttonText}>Battery Settings</Text>
                </TouchableOpacity>
              </View>
            </View>
          )}
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    padding: 20,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    textAlign: 'center',
    marginTop: 40,
    marginBottom: 8,
    color: '#2c3e50',
  },
  subtitle: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 30,
    color: '#7f8c8d',
  },
  section: {
    backgroundColor: 'white',
    padding: 20,
    marginBottom: 20,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 16,
    color: '#2c3e50',
  },
  permissionItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  permissionText: {
    fontSize: 16,
    color: '#34495e',
    flex: 1,
  },
  statusBadge: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
    minWidth: 80,
    alignItems: 'center',
  },
  granted: {
    backgroundColor: '#d5f4e6',
  },
  denied: {
    backgroundColor: '#ffeaa7',
  },
  statusText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#2c3e50',
  },
  button: {
    backgroundColor: '#3498db',
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 15,
  },
  successButton: {
    backgroundColor: '#27ae60',
  },
  dangerButton: {
    backgroundColor: '#e74c3c',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  statusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 15,
  },
  statusLabel: {
    fontSize: 16,
    fontWeight: '600',
    marginRight: 10,
    color: '#2c3e50',
  },
  statusValue: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  infoText: {
    fontSize: 14,
    color: '#7f8c8d',
    lineHeight: 20,
    marginTop: 10,
    textAlign: 'center',
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  infoLabel: {
    fontSize: 14,
    color: '#7f8c8d',
    fontWeight: '500',
  },
  infoValue: {
    fontSize: 14,
    color: '#2c3e50',
    fontWeight: '600',
  },
  warningText: {
    fontSize: 12,
    color: '#e67e22',
    marginTop: 10,
    textAlign: 'center',
    fontStyle: 'italic',
  },
  allowedAppsContainer: {
    marginTop: 15,
    padding: 10,
    backgroundColor: '#ecf0f1',
    borderRadius: 8,
  },
  allowedAppsTitle: {
    fontSize: 12,
    color: '#2c3e50',
    fontWeight: '600',
    marginBottom: 4,
  },
  allowedAppsText: {
    fontSize: 11,
    color: '#7f8c8d',
  },
  statusBlocking: {
    color: '#e74c3c',
  },
  statusAllowed: {
    color: '#27ae60',
  },
  miuiSection: {
    backgroundColor: '#fff3cd',
    borderColor: '#ffeaa7',
    borderWidth: 1,
    borderRadius: 8,
    padding: 16,
    marginTop: 16,
  },
  miuiTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#856404',
    marginBottom: 8,
  },
  miuiDescription: {
    fontSize: 14,
    color: '#856404',
    marginBottom: 12,
  },
  miuiSteps: {
    marginBottom: 16,
  },
  miuiStep: {
    fontSize: 14,
    color: '#856404',
    marginBottom: 4,
    paddingLeft: 8,
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 8,
  },
  miuiButton: {
    flex: 1,
    backgroundColor: '#fd79a8',
  },
});
