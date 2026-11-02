import SwiftUI
import XCTest
@testable import VidroX

@MainActor
final class ContentViewLayoutTests: XCTestCase {
    func testWebContentHasExtraTopClearance() {
        XCTAssertGreaterThanOrEqual(ContentView.webContentInsets.top, 24)
        XCTAssertEqual(ContentView.webContentInsets.top, ContentView.webContentInsets.bottom)
    }

    func testTelevisionContentUsesSixteenByNineLayout() {
        XCTAssertEqual(ContentView.televisionAspectRatio, 16.0 / 9.0, accuracy: 0.000_001)
    }
}
