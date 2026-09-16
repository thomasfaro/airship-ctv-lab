enum LabInfo {
    static let liveSiteURL = "https://airship-ctv-web-lab.netlify.app"
    static let deepLinkScheme = "ctvlab"
    static let playHost = "play"
    static let inboxHost = "inbox"
    static let labHost = "lab"
    static let homeHost = "home"

    static let embeddedContentIDs = [AirshipIds.homeBanner]
    static let customComponents = [
        "AirshipEmbeddedView",
        "MessageCenterInboxView + MessageCenterView",
    ]
    static let availableURLs = [
        "\(liveSiteURL)/",
        "\(liveSiteURL)/play/<film-id>",
        "\(liveSiteURL)/?play=<film-id>",
    ]
    static let deepLinks = [
        "\(deepLinkScheme)://\(homeHost)",
        "\(deepLinkScheme)://\(labHost)",
        "\(deepLinkScheme)://\(inboxHost)",
        "\(deepLinkScheme)://\(playHost)/<film-id>",
    ]
}
