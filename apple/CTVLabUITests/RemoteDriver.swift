import XCTest

/**
 * tvOS has no equivalent of `adb shell input`, so remote presses have to come from a UI test.
 * The sequence is read from the environment, which `xcodebuild` forwards to the runner when
 * prefixed with `TEST_RUNNER_`, and the report is printed for the build log to carry back:
 *
 *   TEST_RUNNER_CTVLAB_KEYS="down,right,tree" xcodebuild test …
 *
 * Keys are `up`, `down`, `left`, `right`, `select`, `menu`, `playPause`, plus `wait` to let the
 * screen settle and `tree` to dump the whole accessibility hierarchy rather than just the focus.
 */
/// Free-standing so that awaiting it from the main actor doesn't send the test case across actors.
private func settle(_ seconds: Double) async throws {
    try await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
}

final class RemoteDriver: XCTestCase {
    override func setUp() {
        continueAfterFailure = true
    }

    @MainActor
    func testDrive() async throws {
        line("driver start")
        let app = XCUIApplication()
        if ProcessInfo.processInfo.environment["CTVLAB_OPEN_INBOX"] == "1" {
            app.launchArguments += ["--open-inbox"]
            line("open-inbox argument set")
        }
        app.launch()

        let warmup = ProcessInfo.processInfo.environment["CTVLAB_WARMUP"].flatMap(Double.init) ?? 8
        try await settle(warmup)
        report(app, after: "launch")

        let keys = (ProcessInfo.processInfo.environment["CTVLAB_KEYS"] ?? "")
            .split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }

        for key in keys {
            switch key {
            case "wait":
                try await settle(2)
            case "tree":
                dumpTree(app)
                continue
            default:
                guard let button = remoteButton(key) else {
                    line("unknown key \(key)")
                    continue
                }
                XCUIRemote.shared.press(button)
            }
            try await settle(0.6)
            report(app, after: key)
        }
        line("driver done")
    }

    // MARK: - Reporting

    /// The focused element, which is all a D-pad test needs to follow.
    @MainActor
    private func report(_ app: XCUIApplication, after key: String) {
        let focused = app.descendants(matching: .any)
            .matching(NSPredicate(format: "hasFocus == YES"))
            .allElementsBoundByIndex

        guard !focused.isEmpty else {
            line("\(key) -> nothing focused")
            return
        }
        // Focus nests: a focused button reports its container as focused too. The last is the leaf.
        for element in focused.suffix(2) {
            line("\(key) -> \(describe(element))")
        }
    }

    @MainActor
    private func dumpTree(_ app: XCUIApplication) {
        line("tree ->")
        for element in app.descendants(matching: .any).allElementsBoundByIndex {
            let focus = element.hasFocus ? "  <<FOCUSED" : ""
            let enabled = element.isEnabled ? "" : " disabled"
            line("   \(describe(element))\(enabled)\(focus)")
        }
    }

    @MainActor
    private func describe(_ element: XCUIElement) -> String {
        let frame = element.frame
        let size = "\(Int(frame.width))x\(Int(frame.height))@(\(Int(frame.minX)),\(Int(frame.minY)))"
        let label = element.label.isEmpty ? "" : " label=\(element.label.prefix(46))"
        let id = element.identifier.isEmpty ? "" : " id=\(element.identifier.prefix(30))"
        return "\(typeName(element.elementType)) \(size)\(label)\(id)"
    }

    private func line(_ text: String) {
        print("CTVLAB| \(text)")
    }

    // MARK: - Mapping

    private func remoteButton(_ key: String) -> XCUIRemote.Button? {
        switch key {
        case "up": return .up
        case "down": return .down
        case "left": return .left
        case "right": return .right
        case "select": return .select
        case "menu": return .menu
        case "playPause": return .playPause
        default: return nil
        }
    }

    private func typeName(_ type: XCUIElement.ElementType) -> String {
        switch type {
        case .button: return "Button"
        case .staticText: return "Text"
        case .image: return "Image"
        case .cell: return "Cell"
        case .collectionView: return "Collection"
        case .scrollView: return "Scroll"
        case .table: return "Table"
        case .webView: return "WebView"
        case .other: return "Other"
        case .window: return "Window"
        case .link: return "Link"
        case .textView: return "TextView"
        case .any: return "Any"
        default: return "Type(\(type.rawValue))"
        }
    }
}
