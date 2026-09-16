import SwiftUI

struct RootView: View {
    @Bindable private var links = DeepLinks.shared

    var body: some View {
        NavigationStack {
            HomeView(
                onPlay: { links.screen = .player($0) },
                onOpenLab: { links.screen = .lab },
                onOpenInbox: { links.screen = .inbox }
            )
            .toolbar(.hidden)
        }
        .preferredColorScheme(.dark)
        .onChange(of: links.screen, initial: true) { _, screen in
            AirshipBootstrap.setHomeVisible(screen == nil)
        }
        .fullScreenCover(item: $links.screen) { screen in
            ZStack {
                Brand.background.ignoresSafeArea()
                switch screen {
                case .lab:
                    LabView()
                case .inbox:
                    // Owns its own NavigationStack; nesting it in ours pops the destination.
                    MessageCenterInboxView()
                case .player(let show):
                    PlayerView(show: show)
                }
            }
        }
    }
}
