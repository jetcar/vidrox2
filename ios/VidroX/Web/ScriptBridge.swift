import Foundation
import WebKit

@MainActor
final class ScriptBridge: NSObject, WKScriptMessageHandler {
    // Source: https://developer.apple.com/documentation/webkit/wkscriptmessagehandler
    static let networkHandlerName = "networkBridge"
    static let exitHandlerName = "exitBridge"

    weak var webView: WKWebView?
    private let sponsorBlockClient: SponsorBlockClient
    private let onExit: () -> Void

    init(
        sponsorBlockClient: SponsorBlockClient = SponsorBlockClient(),
        onExit: @escaping () -> Void = {}
    ) {
        self.sponsorBlockClient = sponsorBlockClient
        self.onExit = onExit
    }

    func userContentController(
        _ userContentController: WKUserContentController,
        didReceive message: WKScriptMessage
    ) {
        switch message.name {
        case Self.networkHandlerName:
            handleNetworkMessage(message.body)
        case Self.exitHandlerName:
            handleExitMessage()
        default:
            break
        }
    }

    private func handleNetworkMessage(_ body: Any) {
        guard
            let payload = body as? [String: Any],
            let url = payload["url"] as? String,
            let videoID = payload["videoId"] as? String
        else {
            deliverNetworkResponse(nil)
            return
        }

        Task {
            do {
                let response = try await sponsorBlockClient.fetch(
                    urlString: url,
                    videoID: videoID
                )
                deliverNetworkResponse(response)
            } catch {
                deliverNetworkResponse(nil)
            }
        }
    }

    private func handleExitMessage() {
        onExit()
    }

    private func deliverNetworkResponse(_ response: String?) {
        let argument: String
        if
            let response,
            let data = try? JSONEncoder().encode(response),
            let encoded = String(data: data, encoding: .utf8)
        {
            argument = encoded
        } else {
            argument = "null"
        }
        webView?.evaluateJavaScript("window.onNetworkBridgeResponse(\(argument));")
    }
}
