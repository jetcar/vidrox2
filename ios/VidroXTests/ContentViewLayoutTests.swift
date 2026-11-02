import SwiftUI
import XCTest
@testable import VidroX

@MainActor
final class ContentViewLayoutTests: XCTestCase {
    func testWebContentHasExtraTopClearance() {
        XCTAssertGreaterThanOrEqual(ContentView.webContentInsets.top, 32)
        XCTAssertGreaterThan(ContentView.webContentInsets.top, ContentView.webContentInsets.bottom)
    }
}
