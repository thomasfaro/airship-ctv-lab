import Foundation
import SwiftUI

struct Show: Identifiable, Hashable {
    let id: String
    let title: String
    var subtitle: String
    var synopsis: String = ""
    var year: String = ""
    var rating: String = ""
    var duration: String = ""
    var imageURL: URL? = nil
    var streamURL: URL = Show.sampleStream
    var progress: Double? = nil
    var isLab: Bool = false
    var color: Color = Color(white: 0.16)

    var metaLine: String {
        [year, rating, duration].filter { !$0.isEmpty }.joined(separator: "  ·  ")
    }

    /// One short trailer for every tile. This is a POC: the catalogue exists to give Airship
    /// something to overlay, not to stream real content.
    static let sampleStream = URL(string: "https://media.w3.org/2010/05/sintel/trailer.mp4")!
}

/// Artwork is a frame Wikimedia Commons renders from the film it hosts, so the still always
/// matches the title on the tile. Only the default frame is used: an explicit `thumbtime`
/// forces on-demand extraction and gets rate-limited.
private func frame(_ dir: String, _ file: String) -> URL? {
    URL(string: "https://upload.wikimedia.org/wikipedia/commons/thumb/\(dir)/\(file)/960px--\(file).jpg")
}

private func film(
    id: String,
    title: String,
    subtitle: String,
    synopsis: String,
    year: String,
    rating: String,
    duration: String,
    imageURL: URL?,
    progress: Double? = nil
) -> Show {
    Show(
        id: id,
        title: title,
        subtitle: subtitle,
        synopsis: synopsis,
        year: year,
        rating: rating,
        duration: duration,
        imageURL: imageURL,
        progress: progress
    )
}

enum Catalog {
    private static let sintel = film(
        id: "sintel",
        title: "Sintel",
        subtitle: "N  ·  Fantasy",
        synopsis: "A lone wanderer crosses a hostile world to find the baby dragon that was taken from her, and pays for it.",
        year: "2010",
        rating: "13+",
        duration: "15m",
        imageURL: frame("f/f1", "Sintel_movie_4K.webm")
    )

    private static let bigBuckBunny = film(
        id: "big-buck-bunny",
        title: "Big Buck Bunny",
        subtitle: "Animation",
        synopsis: "A giant, good-natured rabbit has had enough of three bully rodents and turns the tables with a few forest tricks.",
        year: "2008",
        rating: "All",
        duration: "10m",
        imageURL: frame("c/c0", "Big_Buck_Bunny_4K.webm")
    )

    private static let tearsOfSteel = film(
        id: "tears-of-steel",
        title: "Tears of Steel",
        subtitle: "Sci-fi",
        synopsis: "Amsterdam, the future. A group of warriors and scientists must decide whether a robot can be trusted to undo the past.",
        year: "2012",
        rating: "16+",
        duration: "12m",
        imageURL: frame("c/cb", "Tears_of_Steel_1080p.webm")
    )

    private static let elephantsDream = film(
        id: "elephants-dream",
        title: "Elephants Dream",
        subtitle: "Surreal",
        synopsis: "Two men wander a vast mechanical world, arguing about what is real while the machine around them comes alive.",
        year: "2006",
        rating: "13+",
        duration: "11m",
        imageURL: frame("a/a2", "Elephants_Dream_%282006%29.webm")
    )

    private static let cosmosLaundromat = film(
        id: "cosmos-laundromat",
        title: "Cosmos Laundromat",
        subtitle: "Fantasy",
        synopsis: "A suicidal sheep meets a salesman offering the gift of nine lives, on an island at the end of everything.",
        year: "2015",
        rating: "16+",
        duration: "12m",
        imageURL: frame("3/36", "Cosmos_Laundromat_-_First_Cycle_-_Official_Blender_Foundation_release.webm")
    )

    private static let spring = film(
        id: "spring",
        title: "Spring",
        subtitle: "Drama",
        synopsis: "A shepherd girl and her dog face an ancient spirit that guards the cycle of the seasons.",
        year: "2019",
        rating: "All",
        duration: "8m",
        imageURL: frame("a/a5", "Spring_-_Blender_Open_Movie.webm")
    )

    private static let granDillama = film(
        id: "caminandes-gran-dillama",
        title: "Caminandes: Gran Dillama",
        subtitle: "Comedy",
        synopsis: "Koro the llama wants the grass on the other side of the fence. The fence disagrees.",
        year: "2013",
        rating: "All",
        duration: "2m",
        imageURL: frame("8/8b", "Caminandes%2C_Gran_Dillama_-_Blender_Foundation.webm")
    )

    private static let llamigos = film(
        id: "caminandes-llamigos",
        title: "Caminandes: Llamigos",
        subtitle: "Comedy",
        synopsis: "Winter in Patagonia. Koro and a stubborn penguin fight over the last berry on the plain.",
        year: "2016",
        rating: "All",
        duration: "3m",
        imageURL: frame("a/ab", "Caminandes_3_-_Llamigos_-_Blender_Animated_Short.webm")
    )

    static let featured = sintel

    static let continueWatching: [Show] = [
        watched(tearsOfSteel, 0.41),
        watched(cosmosLaundromat, 0.64),
        watched(elephantsDream, 0.18),
    ]

    private static func watched(_ show: Show, _ progress: Double) -> Show {
        var copy = show
        copy.subtitle = "\(Int(progress * 100))% watched"
        copy.progress = progress
        return copy
    }

    static let trending = [spring, bigBuckBunny, granDillama, llamigos, sintel]

    static let animation = [bigBuckBunny, granDillama, llamigos, elephantsDream]

    static let blenderStudio = [sintel, spring, cosmosLaundromat, tearsOfSteel]

    static let lab = [
        Show(id: "lab", title: "Airship Lab", subtitle: "SDK tools", synopsis: "Channel ID, named user, Preference Center, and Scene testing.", year: "Lab", rating: "Dev", isLab: true, color: Color(red: 0.90, green: 0.04, blue: 0.08)),
    ]

    static let rows: [(String, [Show])] = [
        ("Continue Watching", continueWatching),
        ("Trending Now", trending),
        ("Animation", animation),
        ("Blender Studio", blenderStudio),
        ("Tools", lab),
    ]

    private static let films = [
        sintel, bigBuckBunny, tearsOfSteel, elephantsDream,
        cosmosLaundromat, spring, granDillama, llamigos,
    ]

    /// Resolves the id a deep link carries. Rows hold copies, so this returns the original.
    static func findFilm(_ id: String?) -> Show? {
        guard let id else { return nil }
        return films.first { $0.id == id }
    }
}

enum Brand {
    static let red = Color(red: 0.898, green: 0.035, blue: 0.078)
    static let background = Color(red: 0.078, green: 0.078, blue: 0.078)
    static let card = Color(red: 0.165, green: 0.165, blue: 0.165)
    static let mute = Color(white: 0.70)
}
