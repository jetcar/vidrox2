import Combine
import Foundation
import WebKit

@MainActor
final class WebViewModel: ObservableObject {
    enum RemoteKey {
        case up
        case down
        case left
        case right
        case select
        case escape

        fileprivate var event: (key: String, code: String, keyCode: Int) {
            switch self {
            case .up: return ("ArrowUp", "ArrowUp", 38)
            case .down: return ("ArrowDown", "ArrowDown", 40)
            case .left: return ("ArrowLeft", "ArrowLeft", 37)
            case .right: return ("ArrowRight", "ArrowRight", 39)
            case .select: return ("Enter", "Enter", 13)
            case .escape: return ("Escape", "Escape", 27)
            }
        }
    }

    @Published private(set) var isPageLoaded = false
    @Published private(set) var isOffline = false
    @Published private(set) var loadingProgress = 0.0
    @Published private(set) var controlsVisible = true

    private weak var webView: WKWebView?
    private var hideControlsTask: Task<Void, Never>?

    var versionLabel: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String
        return "v\(version ?? "0.1.0")"
    }

    func attach(_ webView: WKWebView) {
        self.webView = webView
    }

    func detach(_ webView: WKWebView) {
        if self.webView === webView {
            self.webView = nil
        }
    }

    func didStartLoading() {
        isPageLoaded = false
        isOffline = false
        loadingProgress = 0.1
    }

    func didCommitNavigation() {
        loadingProgress = 0.6
    }

    func didFinishLoading() {
        isPageLoaded = true
        isOffline = false
        loadingProgress = 1.0
        showControls()
    }

    func didFailLoading(_ error: Error) {
        let urlError = error as? URLError
        if urlError?.code == .cancelled {
            return
        }
        isPageLoaded = false
        isOffline = true
        loadingProgress = 0.0
        showControls()
    }

    func reload() {
        isOffline = false
        didStartLoading()
        webView?.reload()
    }

    func showControls() {
        controlsVisible = true
        hideControlsTask?.cancel()
        hideControlsTask = Task { @MainActor [weak self] in
            try? await Task.sleep(for: .seconds(4))
            guard !Task.isCancelled else { return }
            self?.controlsVisible = false
        }
    }

    func sendRemoteKey(_ remoteKey: RemoteKey) {
        showControls()
        webView?.evaluateJavaScript(Self.javaScript(for: remoteKey))
    }

    static func javaScript(for remoteKey: RemoteKey) -> String {
        let event = remoteKey.event
        return """
        (function () {
          const init = {
            key: "\(event.key)", code: "\(event.code)",
            keyCode: \(event.keyCode), which: \(event.keyCode),
            bubbles: true, cancelable: true
          };
          document.dispatchEvent(new KeyboardEvent("keydown", init));
          document.dispatchEvent(new KeyboardEvent("keyup", init));
        })();
        """
    }

    func navigateBack() {
        sendRemoteKey(.escape)
    }

    func handleExitRequest() {
        navigateBack()
    }

    deinit {
        hideControlsTask?.cancel()
    }
}
