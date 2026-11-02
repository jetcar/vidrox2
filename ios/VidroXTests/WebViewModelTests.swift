import Foundation
import XCTest
@testable import VidroX

@MainActor
final class WebViewModelTests: XCTestCase {
    func testSuccessfulLoadingStateTransitions() {
        let model = WebViewModel()

        model.didStartLoading()
        XCTAssertFalse(model.isPageLoaded)
        XCTAssertFalse(model.isOffline)
        XCTAssertEqual(model.loadingProgress, 0.1)

        model.didCommitNavigation()
        XCTAssertEqual(model.loadingProgress, 0.6)

        model.didFinishLoading()
        XCTAssertTrue(model.isPageLoaded)
        XCTAssertFalse(model.isOffline)
        XCTAssertEqual(model.loadingProgress, 1.0)
    }

    func testLoadFailureShowsOfflineState() {
        let model = WebViewModel()

        model.didFailLoading(URLError(.notConnectedToInternet))

        XCTAssertFalse(model.isPageLoaded)
        XCTAssertTrue(model.isOffline)
        XCTAssertEqual(model.loadingProgress, 0.0)
    }

    func testCancelledNavigationDoesNotShowOfflineState() {
        let model = WebViewModel()
        model.didStartLoading()

        model.didFailLoading(URLError(.cancelled))

        XCTAssertFalse(model.isOffline)
    }

    func testBackNavigationDispatchesEscapeKey() {
        let script = WebViewModel.javaScript(for: .escape)

        XCTAssertTrue(script.contains("key: \"Escape\""))
        XCTAssertTrue(script.contains("keyCode: 27"))
    }
}
