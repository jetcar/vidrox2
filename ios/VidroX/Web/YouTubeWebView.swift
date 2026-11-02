import SwiftUI
import UIKit
import WebKit

struct YouTubeWebView: UIViewRepresentable {
    @ObservedObject var model: WebViewModel

    private static let youtubeTVURLString = "https://www.youtube.com/tv"
    private static let televisionUserAgent = "Mozilla/5.0 Cobalt/25 (Sony, PS4, Wired)"

    func makeCoordinator() -> Coordinator {
        Coordinator(model: model)
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WebContentConfiguration.makeConfiguration(
            scriptBridge: context.coordinator.scriptBridge
        )

        let webView = WKWebView(frame: .zero, configuration: configuration)
        context.coordinator.connect(to: webView)
        webView.customUserAgent = Self.televisionUserAgent
        webView.scrollView.bounces = false
        guard let youtubeTVURL = URL(string: Self.youtubeTVURLString) else {
            assertionFailure("The built-in YouTube TV URL is invalid")
            return webView
        }
        Task { @MainActor in
            do {
                try await WebContentConfiguration.installContentRules(
                    on: configuration.userContentController
                )
            } catch {
                // Userscript-based filtering still works if content-rule compilation fails.
                assertionFailure("Unable to compile WebKit content rules: \(error)")
            }
            webView.load(URLRequest(url: youtubeTVURL))
        }
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {}

    static func dismantleUIView(_ webView: WKWebView, coordinator: Coordinator) {
        webView.stopLoading()
        let controller = webView.configuration.userContentController
        controller.removeScriptMessageHandler(forName: ScriptBridge.networkHandlerName)
        controller.removeScriptMessageHandler(forName: ScriptBridge.exitHandlerName)
        coordinator.disconnect(from: webView)
    }

    @MainActor
    final class Coordinator: NSObject, WKNavigationDelegate {
        let model: WebViewModel
        lazy var scriptBridge = ScriptBridge { [weak self] in
            self?.model.handleExitRequest()
        }

        private lazy var tapRecognizer = UITapGestureRecognizer(
            target: self,
            action: #selector(userInteracted)
        )

        init(model: WebViewModel) {
            self.model = model
            super.init()
            tapRecognizer.cancelsTouchesInView = false
        }

        func connect(to webView: WKWebView) {
            webView.navigationDelegate = self
            webView.addGestureRecognizer(tapRecognizer)
            webView.scrollView.panGestureRecognizer.addTarget(
                self,
                action: #selector(userInteracted)
            )
            scriptBridge.webView = webView
            model.attach(webView)
        }

        func disconnect(from webView: WKWebView) {
            webView.navigationDelegate = nil
            webView.removeGestureRecognizer(tapRecognizer)
            webView.scrollView.panGestureRecognizer.removeTarget(
                self,
                action: #selector(userInteracted)
            )
            scriptBridge.webView = nil
            model.detach(webView)
        }

        @objc private func userInteracted() {
            model.showControls()
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation?) {
            model.didStartLoading()
        }

        func webView(_ webView: WKWebView, didCommit navigation: WKNavigation?) {
            model.didCommitNavigation()
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation?) {
            model.didFinishLoading()
        }

        func webView(
            _ webView: WKWebView,
            didFailProvisionalNavigation navigation: WKNavigation?,
            withError error: Error
        ) {
            model.didFailLoading(error)
        }

        func webView(
            _ webView: WKWebView,
            didFail navigation: WKNavigation?,
            withError error: Error
        ) {
            model.didFailLoading(error)
        }

        func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
            model.didFailLoading(URLError(.networkConnectionLost))
        }
    }
}
