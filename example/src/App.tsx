import React, { useCallback, useState } from 'react';
import {
  Text,
  View,
  StyleSheet,
  TouchableOpacity,
  Platform,
} from 'react-native';
import AppShield from 'react-native-app-shield';

export default function App() {
  const [screenTimeGranted, setScreenTimeGranted] = useState<boolean | null>(
    null
  );
  const [shieldEnabled, setShieldEnabled] = useState<boolean>(false);
  const [message, setMessage] = useState<string | null>(null);

  const requestPermissions = useCallback(async () => {
    setMessage(null);
    try {
      const res = await AppShield.requestRequiredPermissions();
      setScreenTimeGranted(res?.screenTime ?? null);
      if (Platform.OS === 'ios') {
        setMessage(`Screen Time: ${res?.screenTime ? 'granted' : 'denied'}`);
      } else {
        setMessage(
          `Accessibility: ${res?.accessibility ? 'granted' : 'denied'} | Usage: ${
            res?.usageAccess ? 'granted' : 'denied'
          } | Notifications: ${res?.notifications ? 'granted' : 'denied'}`
        );
      }
    } catch (e: any) {
      setMessage(`Permission error: ${e?.message ?? 'unknown error'}`);
    }
  }, []);

  const toggleShield = useCallback(async () => {
    setMessage(null);
    try {
      if (!shieldEnabled) {
        AppShield.blockAllApps();
        setShieldEnabled(true);
        setMessage('All apps blocked');
      } else {
        AppShield.unblockAllApps();
        setShieldEnabled(false);
        setMessage('All apps unblocked');
      }
    } catch (e: any) {
      setMessage(`Shield error: ${e?.message ?? 'unknown error'}`);
    }
  }, [shieldEnabled]);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>AppShield Example</Text>
      {Platform.OS !== 'ios' ? (
        <Text style={styles.note}>iOS only features</Text>
      ) : null}

      <TouchableOpacity style={styles.button} onPress={requestPermissions}>
        <Text style={styles.buttonText}>Request Permissions</Text>
      </TouchableOpacity>

      <TouchableOpacity style={styles.button} onPress={toggleShield}>
        <Text style={styles.buttonText}>
          {shieldEnabled ? 'Unblock All Apps' : 'Block All Apps'}
        </Text>
      </TouchableOpacity>

      <View style={{ height: 16 }} />
      <Text style={styles.status}>
        Screen Time: {formatStatus(screenTimeGranted)}
      </Text>
      <Text style={styles.status}>Shield: {shieldEnabled ? 'ON' : 'OFF'}</Text>
      {message ? <Text style={styles.message}>{message}</Text> : null}
    </View>
  );
}

function formatStatus(v: boolean | null): string {
  if (v === null) return 'unknown';
  return v ? 'granted' : 'denied';
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 16,
  },
  note: {
    marginBottom: 16,
    color: '#666',
  },
  button: {
    backgroundColor: '#6c43f3',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 8,
    marginTop: 8,
    minWidth: 220,
    alignItems: 'center',
  },
  buttonText: {
    color: 'white',
    fontWeight: '600',
  },
  status: {
    marginTop: 4,
  },
  message: {
    marginTop: 8,
    color: '#333',
  },
});
