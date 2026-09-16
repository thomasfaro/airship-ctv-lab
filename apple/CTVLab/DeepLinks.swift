import Foundation
import Observation

/// `ctvlab://play/<film-id>`, `ctvlab://inbox`, `ctvlab://lab`, `ctvlab://home`.
enum DeepLinkScreen: Hashable, Identifiable {
    case lab
    case inbox
    case player(Show)

    var id: String {
        switch self {
        case .lab: return "lab"
        case .inbox: return "inbox"
        case .player(let show): return "play-\(show.id)"
        }
    }
}

@MainActor
@Observable
final class DeepLinks {
    static let shared = DeepLinks()

    /// `nil` is home. A single optional so SwiftUI's `navigationDestination(item:)` honours a
    /// value that is already set when the stack first appears — `isPresented` does not.
    var screen: DeepLinkScreen?

    private init() {
        if CommandLine.arguments.contains("--open-inbox") {
            screen = .inbox
        }
    }

    func open(_ url: URL) {
        guard url.scheme == LabInfo.deepLinkScheme else { return }
        switch url.host {
        case LabInfo.playHost:
            let id = url.path.split(separator: "/").first.map(String.init)
            screen = Catalog.findFilm(id).map(DeepLinkScreen.player)
        case LabInfo.inboxHost:
            screen = .inbox
        case LabInfo.labHost:
            screen = .lab
        case LabInfo.homeHost:
            screen = nil
        default:
            return
        }
    }
}
