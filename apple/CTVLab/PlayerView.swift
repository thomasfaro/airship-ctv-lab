import AVKit
import SwiftUI

struct PlayerView: View {
    let show: Show
    @State private var player: AVPlayer?

    var body: some View {
        ZStack(alignment: .topLeading) {
            if let player {
                VideoPlayer(player: player)
                    .ignoresSafeArea()
            }
            LinearGradient(
                colors: [.black.opacity(0.7), .clear],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 180)
            .ignoresSafeArea()
            VStack(alignment: .leading, spacing: 6) {
                Text(show.title)
                    .font(.title2.bold())
                Text(show.metaLine.isEmpty ? "Airship Scenes can overlay this player" : show.metaLine)
                    .foregroundStyle(Brand.mute)
            }
            .padding(48)
        }
        .background(Color.black)
        .onAppear {
            let item = AVPlayer(url: show.streamURL)
            player = item
            item.play()
            AirshipBootstrap.trackPlay(title: show.title)
        }
        .onDisappear {
            player?.pause()
            player = nil
        }
    }
}
