import Foundation
import XCTest
@testable import VidroX

final class ContentRuleTests: XCTestCase {
    func testContentRuleJSONContainsEveryAdvertisingHost() throws {
        let data = try XCTUnwrap(WebContentConfiguration.contentRuleJSON().data(using: .utf8))
        let rules = try XCTUnwrap(
            JSONSerialization.jsonObject(with: data) as? [[String: Any]]
        )

        XCTAssertEqual(rules.count, WebContentConfiguration.advertisingHosts.count)
        let filters = try rules.map { rule in
            let trigger = try XCTUnwrap(rule["trigger"] as? [String: Any])
            return try XCTUnwrap(trigger["url-filter"] as? String)
        }

        for host in WebContentConfiguration.advertisingHosts {
            XCTAssertTrue(filters.contains { $0.contains(NSRegularExpression.escapedPattern(for: host)) })
        }
    }

    func testContentRulesUseBlockAction() throws {
        let data = try XCTUnwrap(WebContentConfiguration.contentRuleJSON().data(using: .utf8))
        let rules = try XCTUnwrap(
            JSONSerialization.jsonObject(with: data) as? [[String: Any]]
        )

        for rule in rules {
            let action = try XCTUnwrap(rule["action"] as? [String: String])
            XCTAssertEqual(action["type"], "block")
        }
    }
}

