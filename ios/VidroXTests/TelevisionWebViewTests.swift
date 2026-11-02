import CoreGraphics
import XCTest
@testable import VidroX

@MainActor
final class TelevisionWebViewTests: XCTestCase {
    func testLandscapeIPhoneViewportFitsEntireTelevisionCanvas() {
        let zoom = TelevisionWebView.pageZoom(
            for: CGSize(width: 852, height: 393)
        )

        XCTAssertEqual(
            zoom,
            CGFloat(393) / 2_160 * TelevisionWebView.zoomSafetyFactor,
            accuracy: 0.000_001
        )
    }

    func testPortraitViewportUsesWidthAsLimitingDimension() {
        let zoom = TelevisionWebView.pageZoom(
            for: CGSize(width: 393, height: 852)
        )

        XCTAssertEqual(
            zoom,
            CGFloat(393) / 3_840 * TelevisionWebView.zoomSafetyFactor,
            accuracy: 0.000_001
        )
    }

    func testZeroViewportKeepsDefaultZoom() {
        XCTAssertEqual(TelevisionWebView.pageZoom(for: .zero), 1)
    }

    func testPageZoomLeavesOverscanClearance() {
        let viewport = CGSize(width: 852, height: 393)
        let zoom = TelevisionWebView.pageZoom(for: viewport)
        let renderedHeight = TelevisionWebView.televisionCanvasSize.height * zoom

        XCTAssertLessThanOrEqual(renderedHeight, viewport.height * 0.98)
        XCTAssertGreaterThanOrEqual(viewport.height - renderedHeight, 7)
    }

    func testTelevisionCanvasMatchesInjectedFourKViewport() {
        XCTAssertEqual(TelevisionWebView.televisionCanvasSize.width, 3_840)
        XCTAssertEqual(TelevisionWebView.televisionCanvasSize.height, 2_160)
    }
}
