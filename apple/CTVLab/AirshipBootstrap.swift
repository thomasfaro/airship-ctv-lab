import AirshipAutomation
import AirshipCore
import AirshipFeatureFlags
import AirshipPreferenceCenter
import Foundation
import OSLog

@MainActor
enum AirshipBootstrap {
    private static let log = Logger(subsystem: "com.airship.ctvlab", category: "Airship")
    private static let defaultNamedUser = "thomasfctv"

    static func start() {
        guard AirshipSecrets.isConfigured else {
            log.warning("Airship skipped: fill config/airship.local.properties")
            LabStore.shared.status = "fill config/airship.local.properties"
            return
        }

        var config = AirshipConfig()
        config.defaultAppKey = AirshipSecrets.appKey
        config.defaultAppSecret = AirshipSecrets.appSecret
        config.site = AirshipSecrets.site.lowercased() == "eu" ? .eu : .us
        config.inProduction = false
        config.developmentLogLevel = .verbose
        config.productionLogLevel = .verbose

        do {
            try Airship.takeOff(config)
            Airship.privacyManager.enableFeatures(.all)
            Airship.channel.editTags { editor in
                editor.add(["ctv_lab", "tvos"])
            }
            Airship.onReady {
                Airship.deepLinkDelegate = DeepLinkForwarder.shared
                identify(defaultNamedUser)
                refreshIdentity()
                Task { @MainActor in
                    for await _ in Airship.channel.identifierUpdates {
                        refreshIdentity()
                    }
                }
            }
            log.info("Airship takeOff ok")
        } catch {
            LabStore.shared.status = "takeOff failed: \(error.localizedDescription)"
            log.error("Airship takeOff failed: \(error.localizedDescription, privacy: .public)")
        }
    }

    @MainActor
    static func refreshIdentity() {
        LabStore.shared.channelId = Airship.channel.identifier
        LabStore.shared.status = Airship.channel.identifier == nil ? "SDK ready, creating channel…" : "ready"
        Task { @MainActor in
            LabStore.shared.namedUser = await Airship.contact.namedUserID
        }
    }

    static func identify(_ namedUserId: String) {
        guard Airship.isFlying else { return }
        let trimmed = namedUserId.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            Airship.contact.reset()
            LabStore.shared.namedUser = nil
        } else {
            Airship.contact.identify(trimmed)
            LabStore.shared.namedUser = trimmed
        }
    }

    static func trackPlay(title: String) {
        guard Airship.isFlying else { return }
        var event = CustomEvent(name: "content_play")
        event.setProperty(string: title, forKey: "title")
        event.setProperty(string: "tvos", forKey: "platform")
        event.track()
    }
}

/// Airship holds `deepLinkDelegate` weakly. Scene CTAs fire `deep_link_action` here rather than
/// through a system URL, so this has to stay alive for the process.
private final class DeepLinkForwarder: DeepLinkDelegate, @unchecked Sendable {
    static let shared = DeepLinkForwarder()

    @MainActor
    func receivedDeepLink(_ deepLink: URL) async {
        DeepLinks.shared.open(deepLink)
    }
}
