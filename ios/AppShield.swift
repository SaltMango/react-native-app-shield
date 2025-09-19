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
    @objc func requestRequiredPermissions(_ resolve: RCTPromiseResolveBlock,
                                          rejecter reject: RCTPromiseRejectBlock) {
        Task { @MainActor in
            do {
                // Screen Time
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
}

// MARK: - React Native Bridge
@objc(AppShieldSwift)
extension AppShieldSwift: RCTBridgeModule {}


