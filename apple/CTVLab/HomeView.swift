import AirshipAutomation
import AirshipCore
import SwiftUI

struct HomeView: View {
    @Bindable var store = LabStore.shared
    var onPlay: (Show) -> Void
    var onOpenLab: () -> Void
    var onOpenInbox: () -> Void
    @FocusState private var playFocused: Bool

    var body: some View {
        ScrollView(.vertical) {
            VStack(alignment: .leading, spacing: 0) {
                featuredHero
                homeBanner
                    .padding(.horizontal, 60)
                    .padding(.top, 8)
                ForEach(Catalog.rows, id: \.0) { title, shows in
                    CatalogRow(title: title, shows: shows) { show in
                        if show.isLab {
                            onOpenLab()
                        } else {
                            onPlay(show)
                        }
                    }
                    .padding(.top, 22)
                }
            }
            .padding(.bottom, 60)
        }
        .background(Brand.background.ignoresSafeArea())
        .onAppear { playFocused = true }
    }

    private var featuredHero: some View {
        ZStack(alignment: .bottomLeading) {
            PosterImage(url: Catalog.featured.imageURL)
                .frame(maxWidth: .infinity)
                .frame(height: 640)
                .clipped()

            LinearGradient(
                colors: [
                    .black.opacity(0.82),
                    .black.opacity(0.35),
                    .clear,
                ],
                startPoint: .leading,
                endPoint: .trailing
            )
            LinearGradient(
                stops: [
                    .init(color: .black.opacity(0.55), location: 0),
                    .init(color: .clear, location: 0.28),
                    .init(color: .black.opacity(0.55), location: 0.68),
                    .init(color: Brand.background, location: 1),
                ],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack {
                HStack(alignment: .center) {
                    Text("CTVLAB")
                        .font(.title.weight(.black))
                        .foregroundStyle(Brand.red)
                        .tracking(1)
                    Text("Home")
                        .font(.headline)
                        .padding(.leading, 20)
                    Text(store.channelId.map { "Live  ·  \($0.prefix(8))" } ?? "Live")
                        .foregroundStyle(Brand.mute)
                        .padding(.leading, 16)
                    Spacer()
                    Button(action: onOpenInbox) {
                        Image(systemName: "bell")
                            .font(.title2)
                            .accessibilityLabel("Notifications")
                    }
                    .buttonStyle(.card)
                }
                .padding(.horizontal, 60)
                .padding(.top, 36)
                // Nothing shares the bell's row or column, so directional focus search never
                // reaches it. A focus section makes the whole bar a target Up can land in.
                .focusSection()
                Spacer()
            }

            VStack(alignment: .leading, spacing: 12) {
                Text("N  SERIES")
                    .font(.caption.bold())
                    .foregroundStyle(Brand.red)
                    .tracking(3)
                Text(Catalog.featured.title)
                    .font(.largeTitle.weight(.black))
                Text(Catalog.featured.metaLine)
                    .foregroundStyle(Brand.mute)
                Text(Catalog.featured.synopsis)
                    .foregroundStyle(.white.opacity(0.9))
                    .lineLimit(3)
                    .frame(maxWidth: 720, alignment: .leading)
                Button {
                    onPlay(Catalog.featured)
                } label: {
                    Label("Play", systemImage: "play.fill")
                        .font(.headline)
                        .padding(.horizontal, 8)
                }
                .focused($playFocused)
                .tint(.white)
            }
            .padding(.horizontal, 60)
            .padding(.bottom, 40)
            .frame(maxWidth: 820, alignment: .leading)
        }
        .frame(height: 640)
    }

    private var homeBanner: some View {
        GeometryReader { geo in
            AirshipEmbeddedView(
                embeddedID: AirshipIds.homeBanner,
                embeddedSize: AirshipEmbeddedSize(parentBounds: geo.size)
            ) {
                ZStack(alignment: .leading) {
                    PosterImage(url: Catalog.continueWatching.first?.imageURL)
                    Color.black.opacity(0.45)
                    VStack(alignment: .leading, spacing: 6) {
                        Text("AIRSHIP SCENE")
                            .font(.caption.bold())
                            .foregroundStyle(Brand.red)
                            .tracking(2)
                        Text("home_banner")
                            .font(.title2.bold())
                        Text("Publish an Embedded Content Scene with this ID")
                            .foregroundStyle(Brand.mute)
                    }
                    .padding(.horizontal, 28)
                }
            }
        }
        .frame(height: 168)
        .clipShape(RoundedRectangle(cornerRadius: 6))
    }
}

struct CatalogRow: View {
    let title: String
    let shows: [Show]
    var onSelect: (Show) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(title)
                .font(.title3.bold())
                .padding(.horizontal, 60)
            ScrollView(.horizontal) {
                HStack(spacing: 16) {
                    ForEach(shows) { show in
                        Button {
                            onSelect(show)
                        } label: {
                            ShowCard(show: show)
                        }
                        .buttonStyle(.card)
                    }
                }
                .padding(.horizontal, 60)
                .padding(.vertical, 12)
            }
        }
    }
}

struct ShowCard: View {
    let show: Show

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            if show.isLab {
                LinearGradient(
                    colors: [Brand.red, Color(red: 0.23, green: 0, blue: 0.03)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                Text("CTVLAB")
                    .font(.title.weight(.black))
                    .tracking(2)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                PosterImage(url: show.imageURL)
            }

            LinearGradient(
                colors: [.clear, .black.opacity(0.78)],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack(alignment: .leading, spacing: 4) {
                Text(show.title)
                    .font(.headline)
                Text(show.subtitle)
                    .font(.caption)
                    .foregroundStyle(Brand.mute)
            }
            .padding(14)

            if let progress = show.progress {
                GeometryReader { geo in
                    VStack {
                        Spacer()
                        ZStack(alignment: .leading) {
                            Color.white.opacity(0.28)
                            Brand.red.frame(width: geo.size.width * progress)
                        }
                        .frame(height: 4)
                    }
                }
            }
        }
        .frame(width: 340, height: 191)
        .clipped()
    }
}

struct PosterImage: View {
    let url: URL?

    var body: some View {
        AsyncImage(url: url) { phase in
            switch phase {
            case .success(let image):
                image.resizable().scaledToFill()
            default:
                Brand.card
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
