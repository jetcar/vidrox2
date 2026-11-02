import SwiftUI

struct DirectionPadView: View {
    let onPress: (WebViewModel.RemoteKey) -> Void

    var body: some View {
        VStack(spacing: 6) {
            remoteButton(systemName: "chevron.up", key: .up)
            HStack(spacing: 6) {
                remoteButton(systemName: "chevron.left", key: .left)
                Button("OK") {
                    onPress(.select)
                }
                .font(.caption.bold())
                .frame(width: 48, height: 48)
                .buttonStyle(.remoteControl)
                remoteButton(systemName: "chevron.right", key: .right)
            }
            remoteButton(systemName: "chevron.down", key: .down)
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Remote control")
    }

    private func remoteButton(
        systemName: String,
        key: WebViewModel.RemoteKey
    ) -> some View {
        Button {
            onPress(key)
        } label: {
            Image(systemName: systemName)
                .frame(width: 48, height: 48)
        }
        .buttonStyle(.remoteControl)
    }
}

struct RemoteControlButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundStyle(.white)
            .background(.black.opacity(configuration.isPressed ? 0.6 : 0.28))
            .clipShape(Circle())
            .overlay {
                Circle()
                    .stroke(.white.opacity(configuration.isPressed ? 0.75 : 0.22))
            }
            .scaleEffect(configuration.isPressed ? 0.94 : 1)
    }
}

extension ButtonStyle where Self == RemoteControlButtonStyle {
    static var remoteControl: RemoteControlButtonStyle {
        RemoteControlButtonStyle()
    }
}

