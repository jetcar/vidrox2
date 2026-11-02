import SwiftUI

struct ContentView: View {
    static let televisionAspectRatio: CGFloat = 16.0 / 9.0
    static let webContentInsets = EdgeInsets(
        top: 24,
        leading: 8,
        bottom: 24,
        trailing: 8
    )

    @StateObject private var model = WebViewModel()

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            YouTubeWebView(model: model)
                .aspectRatio(Self.televisionAspectRatio, contentMode: .fit)
                .padding(Self.webContentInsets)

            VStack {
                HStack {
                    Button {
                        model.navigateBack()
                    } label: {
                        Image(systemName: "chevron.backward")
                            .frame(width: 48, height: 48)
                    }
                    .buttonStyle(.remoteControl)
                    .accessibilityLabel("Назад")

                    Spacer()
                }

                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .zIndex(2)

            if !model.isPageLoaded && !model.isOffline {
                LoadingOverlay(progress: model.loadingProgress)
            }

            if model.controlsVisible {
                VStack {
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
