import Foundation
import Observation

@MainActor
@Observable
final class LabStore {
    static let shared = LabStore()

    var channelId: String?
    var namedUser: String?
    var status: String = "starting…"

    private init() {}
}
