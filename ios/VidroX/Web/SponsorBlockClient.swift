import Foundation

struct SponsorBlockClient: Sendable {
    enum ClientError: Error, Equatable {
        case invalidURL
        case disallowedEndpoint
        case invalidResponse
        case unsuccessfulStatus(Int)
    }

    static let allowedHost = "api.sponsor.ajay.app"
    static let allowedPathPrefix = "/api/skipSegments/"

    func fetch(urlString: String, videoID: String) async throws -> String {
        let url = try Self.validatedURL(from: urlString)
        let (data, response) = try await URLSession.shared.data(from: url)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw ClientError.invalidResponse
        }
        guard 200..<300 ~= httpResponse.statusCode else {
            throw ClientError.unsuccessfulStatus(httpResponse.statusCode)
        }
        return try Self.filteredResponse(from: data, videoID: videoID)
    }

    static func validatedURL(from value: String) throws -> URL {
        guard
            let components = URLComponents(string: value),
            components.scheme == "https",
            components.host?.lowercased() == allowedHost,
            components.port == nil,
            components.user == nil,
            components.password == nil,
            components.fragment == nil,
            components.path.hasPrefix(allowedPathPrefix),
            let url = components.url
        else {
            throw ClientError.disallowedEndpoint
        }

        let queryNames = Set(components.queryItems?.map(\.name) ?? [])
        guard queryNames.isSubset(of: ["categories"]) else {
            throw ClientError.disallowedEndpoint
        }
        return url
    }

    static func filteredResponse(from data: Data, videoID: String) throws -> String {
        guard
            !videoID.isEmpty,
            videoID.count <= 128,
            let items = try JSONSerialization.jsonObject(with: data) as? [[String: Any]]
        else {
            throw ClientError.invalidResponse
        }

        guard let match = items.first(where: { $0["videoID"] as? String == videoID }) else {
            return "{}"
        }
        let result = try JSONSerialization.data(withJSONObject: match, options: [.sortedKeys])
        guard let string = String(data: result, encoding: .utf8) else {
            throw ClientError.invalidResponse
        }
        return string
    }
}

