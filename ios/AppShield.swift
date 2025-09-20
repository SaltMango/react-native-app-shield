import Foundation
import ManagedSettings
import FamilyControls
import React

@objc(AppShieldSwift)
class AppShieldSwift: NSObject {
    private let store = ManagedSettingsStore()

    @objc static func requiresMainQueueSetup() -> Bool { true }

    @objc func constantsToExport() -> [AnyHashable: Any]! {
        return ["available": true]
    }

    // MARK: - Shielding Controls
    @objc func blockAllApps() {
        store.shield.applicationCategories = .all()
        store.shield.webDomainCategories = .all()
    }

    @objc func unblockAllApps() {
        store.shield.applicationCategories = nil
        store.shield.webDomainCategories = nil
    }

    // MARK: - Permissions
    // Removed combined permissions API

    @objc func getPermissionStatus(_ resolve: RCTPromiseResolveBlock,
                                   rejecter reject: RCTPromiseRejectBlock) {
        // Reuse same logic but without launching any system UI
        let granted = AuthorizationCenter.shared.authorizationStatus == .approved
        resolve([
            "screenTime": granted
        ])
    }

    // MARK: - Enhanced Permission Management
    @objc func requestScreenTimeAuthorization(_ resolve: RCTPromiseResolveBlock,
                                              rejecter reject: RCTPromiseRejectBlock) {
        Task { @MainActor in
            do {
                if AuthorizationCenter.shared.authorizationStatus != .approved {
                    try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
                }
                resolve([
                    "screenTime": AuthorizationCenter.shared.authorizationStatus == .approved
                ])
            } catch {
                reject("permission_error", error.localizedDescription, error)
            }
        }
    }
    
    @objc func requestPermissions(_ resolve: RCTPromiseResolveBlock,
                                  rejecter reject: RCTPromiseRejectBlock) {
        // For iOS, this is the same as requesting Screen Time authorization
        requestScreenTimeAuthorization(resolve, rejecter: reject)
    }
    
    @objc func isBlockingActive(_ resolve: RCTPromiseResolveBlock,
                               rejecter reject: RCTPromiseRejectBlock) {
        let granted = AuthorizationCenter.shared.authorizationStatus == .approved
        let blockingEnabled = UserDefaults.standard.bool(forKey: "appshield_blocking_enabled")
        let isActive = granted && blockingEnabled
        resolve(isActive)
    }
    
    // MARK: - Custom App Management (iOS Stubs)
    @objc func setCustomAllowedApps(_ apps: [String]) {
        // Store custom allowed apps for iOS
        UserDefaults.standard.set(apps, forKey: "appshield_custom_allowed_apps")
    }
    
    @objc func getCustomAllowedApps(_ resolve: RCTPromiseResolveBlock,
                                   rejecter reject: RCTPromiseRejectBlock) {
        let allowedApps = UserDefaults.standard.stringArray(forKey: "appshield_custom_allowed_apps") ?? []
        resolve(allowedApps)
    }
    
    @objc func getInstalledApps(_ resolve: RCTPromiseResolveBlock,
                               rejecter reject: RCTPromiseRejectBlock) {
        // iOS doesn't allow listing installed apps for privacy reasons
        // Return empty array with explanation
        resolve([])
    }
    
    @objc func checkDeviceCompatibility(_ resolve: RCTPromiseResolveBlock,
                                       rejecter reject: RCTPromiseRejectBlock) {
        let deviceInfo: [String: Any] = [
            "platform": "ios",
            "version": UIDevice.current.systemVersion,
            "model": UIDevice.current.model,
            "screenTimeSupported": true,
            "screenTimeEnabled": AuthorizationCenter.shared.authorizationStatus == .approved,
            "requiresSpecialPermissions": true,
            "oemInfo": [
                "oem": "apple",
                "ui": "ios",
                "requiresSpecialPermissions": true
            ]
        ]
        resolve(deviceInfo)
    }

    // MARK: - Toast Control (iOS dummy implementations)
    @objc func setToastEnabled(_ enabled: Bool) {
        // iOS doesn't show toast messages like Android, so this is a no-op
        // Could potentially be used to control other notification types in the future
        UserDefaults.standard.set(enabled, forKey: "appshield_toast_enabled")
    }

    @objc func isToastEnabled(_ resolve: RCTPromiseResolveBlock,
                              rejecter reject: RCTPromiseRejectBlock) {
        // Return the stored preference, defaulting to true
        let enabled = UserDefaults.standard.object(forKey: "appshield_toast_enabled") as? Bool ?? true
        resolve(enabled)
    }
}

// MARK: - React Native Bridge
@objc(AppShieldSwift)
extension AppShieldSwift: RCTBridgeModule {}


