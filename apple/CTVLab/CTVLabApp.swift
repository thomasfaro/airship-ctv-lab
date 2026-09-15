import SwiftUI
import UIKit

@main
struct CTVLabApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        AirshipBootstrap.start()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .onOpenURL { DeepLinks.shared.open($0) }
        }
    }
}

/// Cold-start `ctvlab://` URLs arrive here on tvOS; `onOpenURL` is not always called before first layout.
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        if let url = launchOptions?[.url] as? URL {
            DeepLinks.shared.open(url)
        }
        return true
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        DeepLinks.shared.open(url)
        return true
    }
}
