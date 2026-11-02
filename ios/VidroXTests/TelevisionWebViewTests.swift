import CoreGraphics
import XCTest
@testable import VidroX

@MainActor
final class TelevisionWebViewTests: XCTestCase {
    func testLandscapeIPhoneViewportFitsEntireTelevisionCanvas() {
        let zoom = TelevisionWebView.pageZoom(
            for: CGSize(width: 852, height: 393)
        )

        XCTAssertEqual(zoom, CGFloat(393) / 4_000, accuracy: 0.000_001)
    }

    func testPortraitViewportUsesWidthAsLimitingDimension() {
        let zoom = TelevisionWebView.pageZoom(
            for: CGSize(width: 393, height: 852)
        )

        XCTAssertEqual(zoom, CGFloat(393) / 7_000, accuracy: 0.000_001)
    }

    func testZeroViewportKeepsDefaultZoom() {
        XCTAssertEqual(TelevisionWebView.pageZoom(for: .zero), 1)
    }
}
