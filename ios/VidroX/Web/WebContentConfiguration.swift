import Foundation
import WebKit

enum WebContentConfiguration {
    static let advertisingHosts = [
        "doubleclick.net",
        "googlesyndication.com",
        "googletagservices.com",
        "googleadservices.com",
        "2mdn.net",
        "adservice.google.com",
    ]

    @MainActor
    static func makeConfiguration(
        scriptBridge: ScriptBridge,
        bundle: Bundle = .main
    ) -> WKWebViewConfiguration {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = []
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true

        // WebKit user scripts run before page content when registered at document start.
        // Source: https://developer.apple.com/documentation/webkit/wkuserscriptinjectiontime
        addUserScript(named: "ios_bridge", to: configuration.userContentController, bundle: bundle)
        addUserScript(named: "userscripts", to: configuration.userContentController, bundle: bundle)
        configuration.userContentController.add(scriptBridge, name: ScriptBridge.networkHandlerName)
        configuration.userContentController.add(scriptBridge, name: ScriptBridge.exitHandlerName)
        return configuration
    }

    @MainActor
    static func installContentRules(on controller: WKUserContentController) async throws {
        // WebKit compiles JSON rule lists before applying them to a content controller.
        // Source: https://developer.apple.com/documentation/webkit/wkcontentruleliststore
        let encodedRules = try contentRuleJSON()
        let ruleList = try await WKContentRuleListStore.default().compileContentRuleList(
            forIdentifier: "VidroXAdvertisingHosts",
            encodedContentRuleList: encodedRules
        )
        if let ruleList {
            controller.add(ruleList)
        }
    }

    static func contentRuleJSON() throws -> String {
        let rules: [[String: Any]] = advertisingHosts.map { host in
            let escapedHost = NSRegularExpression.escapedPattern(for: host)
            return [
                "trigger": [
                    "url-filter": "^https?://([^/]+\\.)?\(escapedHost)/",
                    "resource-type": [
                        "document", "image", "style-sheet", "script", "font",
                        "raw", "svg-document", "media", "popup", "ping", "fetch",
                    ],
                ],
                "action": ["type": "block"],
            ]
        }

        let data = try JSONSerialization.data(withJSONObject: rules, options: [.sortedKeys])
        guard let json = String(data: data, encoding: .utf8) else {
            throw CocoaError(.fileWriteInapplicableStringEncoding)
        }
        return json
    }

    @MainActor
    private static func addUserScript(
        named name: String,
        to controller: WKUserContentController,
        bundle: Bundle
    ) {
        guard
            let url = bundle.url(forResource: name, withExtension: "js"),
            let source = try? String(contentsOf: url, encoding: .utf8)
        else {
            assertionFailure("Missing bundled script: \(name).js")
            return
        }

        controller.addUserScript(
            WKUserScript(
                source: source,
                injectionTime: .atDocumentStart,
                forMainFrameOnly: true
            )
        )
    }
}
