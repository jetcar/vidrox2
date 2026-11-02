import SwiftUI

struct LoadingOverlay: View {
    let progress: Double

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 18) {
                Text("VidroX")
                    .font(.largeTitle.bold())
                    .foregroundStyle(.white)
                ProgressView(value: progress)
                    .tint(.red)
                    .frame(maxWidth: 240)
            }
            .padding(32)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Loading VidroX")
    }
}

struct OfflineOverlay: View {
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: "wifi.slash")
                .font(.system(size: 34))
            Text("No connection")
                .font(.headline)
            Text("Check the network and try again.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Button("Retry", action: onRetry)
                .buttonStyle(.borderedProminent)
                .tint(.red)
        }
        .multilineTextAlignment(.center)
        .padding(28)
        .foregroundStyle(.white)
        .background(.black.opacity(0.9), in: RoundedRectangle(cornerRadius: 20))
        .padding(24)
    }
}

