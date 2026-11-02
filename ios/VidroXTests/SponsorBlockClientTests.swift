import Foundation
import XCTest
@testable import VidroX

final class SponsorBlockClientTests: XCTestCase {
    func testAllowsExpectedSponsorBlockEndpoint() throws {
        let url = try SponsorBlockClient.validatedURL(
            from: "https://api.sponsor.ajay.app/api/skipSegments/abcd?categories=%5B%22sponsor%22%5D"
        )

        XCTAssertEqual(url.host, SponsorBlockClient.allowedHost)
        XCTAssertTrue(url.path.hasPrefix(SponsorBlockClient.allowedPathPrefix))
    }

    func testRejectsUntrustedEndpoints() {
        let endpoints = [
            "http://api.sponsor.ajay.app/api/skipSegments/abcd",
            "https://api.sponsor.ajay.app.attacker.example/api/skipSegments/abcd",
            "https://api.sponsor.ajay.app/other/abcd",
            "https://user@api.sponsor.ajay.app/api/skipSegments/abcd",
            "https://api.sponsor.ajay.app:443/api/skipSegments/abcd",
            "https://api.sponsor.ajay.app/api/skipSegments/abcd?redirect=https://example.com",
        ]

        for endpoint in endpoints {
            XCTAssertThrowsError(try SponsorBlockClient.validatedURL(from: endpoint))
        }
    }

    func testFiltersResponseToRequestedVideo() throws {
        let data = Data(
            """
            [
              {"videoID":"other","segments":[]},
              {"videoID":"wanted","segments":[{"segment":[1,2]}]}
            ]
            """.utf8
        )

        let response = try SponsorBlockClient.filteredResponse(from: data, videoID: "wanted")
        let object = try XCTUnwrap(
            JSONSerialization.jsonObject(with: Data(response.utf8)) as? [String: Any]
        )

        XCTAssertEqual(object["videoID"] as? String, "wanted")
    }

    func testReturnsEmptyObjectWhenVideoIsMissing() throws {
        let data = Data("[{\"videoID\":\"other\",\"segments\":[]}]".utf8)
        XCTAssertEqual(
            try SponsorBlockClient.filteredResponse(from: data, videoID: "wanted"),
            "{}"
        )
    }

    func testRejectsMalformedResponse() {
        XCTAssertThrowsError(
            try SponsorBlockClient.filteredResponse(from: Data("{}".utf8), videoID: "wanted")
        )
    }
}

