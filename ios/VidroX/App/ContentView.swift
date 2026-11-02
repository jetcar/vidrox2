import SwiftUI

struct ContentView: View {
    static let webContentInsets = EdgeInsets(
        top: 32,
        leading: 8,
        bottom: 16,
        trailing: 8
    )

    @StateObject private var model = WebViewModel()

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            YouTubeWebView(model: model)
                .padding(Self.webContentInsets)

            if !model.isPageLoaded && !model.isOffline {
                LoadingOverlay(progress: model.loadingProgress)
            }

            if model.controlsVisible {
                VStack {
                    HStack {
                        Button {
                            model.sendRemoteKey(.escape)
                        } label: {
                            Image(systemName: "arrow.uturn.backward")
                                .frame(width: 44, height: 44)
                        }
                        .buttonStyle(.remoteControl)

                        Spacer()
                    }

                    Spacer()

                    HStack(alignment: .bottom) {
                        Text(model.versionLabel)
                            .font(.caption2.monospacedDigit())
                            .foregroundStyle(.white.opacity(0.55))

                        Spacer()

                        DirectionPadView { key in
                            model.sendRemoteKey(key)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .transition(.opacity)
            }

            if model.isOffline {
                OfflineOverlay {
                    model.reload()
                }
            }
        }
        .background(Color.black)
        .preferredColorScheme(.dark)
        .animation(.easeInOut(duration: 0.2), value: model.controlsVisible)
        .onAppear {
            model.showControls()
        }
    }
}
