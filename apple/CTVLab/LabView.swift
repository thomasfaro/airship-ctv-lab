import AirshipCore
import SwiftUI

struct LabView: View {
    @Bindable var store = LabStore.shared
    @State private var namedUserInput = ""
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("Airship Lab")
                    .font(.largeTitle.weight(.black))
                    .foregroundStyle(Brand.red)
                Text("Status: \(store.status)")
                    .foregroundStyle(.secondary)
                Text("Channel ID: \(store.channelId ?? "—")")
                    .font(.title3)
                Text("Named user: \(store.namedUser ?? "not identified")")
                    .foregroundStyle(.secondary)

                TextField("Named user (e.g. mediaset-qa-01)", text: $namedUserInput)
                    .textFieldStyle(.plain)

                HStack(spacing: 20) {
                    Button("Identify") {
                        AirshipBootstrap.identify(namedUserInput)
                    }
                    Button("Reset") {
                        namedUserInput = ""
                        AirshipBootstrap.identify("")
                    }
                    Button("Preference Center") {
                        guard Airship.isFlying else { return }
                        Airship.preferenceCenter.display("default")
                    }
                    Button("Back") {
                        dismiss()
                    }
                }

                Text("tvOS: visible push is badge only. Publish a Scene / IAA (no HTML) targeting the ctv_lab tag, then return to the catalog or start playback.")
                    .foregroundStyle(.secondary)
                    .padding(.top, 12)
            }
            .padding(60)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Brand.background.ignoresSafeArea())
        .onAppear {
            namedUserInput = store.namedUser ?? ""
            AirshipBootstrap.refreshIdentity()
        }
    }
}
