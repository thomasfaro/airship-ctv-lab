package com.airship.ctvlab.data

data class Show(
    val id: String,
    val title: String,
    val subtitle: String,
    val synopsis: String = "",
    val year: String = "",
    val rating: String = "",
    val duration: String = "",
    val imageUrl: String? = null,
    val streamUrl: String = SAMPLE_STREAM,
    val progress: Float? = null,
    val isLab: Boolean = false,
) {
    val metaLine: String
        get() = listOf(year, rating, duration).filter { it.isNotBlank() }.joinToString("  ·  ")
}

/**
 * One short trailer for every tile. This is a POC: the catalogue exists to give Airship
 * something to overlay, not to stream real content.
 */
const val SAMPLE_STREAM = "https://media.w3.org/2010/05/sintel/trailer.mp4"

/**
 * Artwork is a frame Wikimedia Commons renders from the film it hosts, so the still always
 * matches the title on the tile. Only the default frame is used: an explicit `thumbtime`
 * forces on-demand extraction and gets rate-limited.
 */
private fun frame(dir: String, file: String) =
    "https://upload.wikimedia.org/wikipedia/commons/thumb/$dir/$file/960px--$file.jpg"

private fun film(
    id: String,
    title: String,
    subtitle: String,
    synopsis: String,
    year: String,
    rating: String,
    duration: String,
    imageUrl: String,
    progress: Float? = null,
) = Show(
    id = id,
    title = title,
    subtitle = subtitle,
    synopsis = synopsis,
    year = year,
    rating = rating,
    duration = duration,
    imageUrl = imageUrl,
    progress = progress,
)

object Catalog {
    private val sintel = film(
        id = "sintel",
        title = "Sintel",
        subtitle = "N  ·  Fantasy",
        synopsis = "A lone wanderer crosses a hostile world to find the baby dragon that was taken from her, and pays for it.",
        year = "2010",
        rating = "13+",
        duration = "15m",
        imageUrl = frame("f/f1", "Sintel_movie_4K.webm"),
    )

    private val bigBuckBunny = film(
        id = "big-buck-bunny",
        title = "Big Buck Bunny",
        subtitle = "Animation",
        synopsis = "A giant, good-natured rabbit has had enough of three bully rodents and turns the tables with a few forest tricks.",
        year = "2008",
        rating = "All",
        duration = "10m",
        imageUrl = frame("c/c0", "Big_Buck_Bunny_4K.webm"),
    )

    private val tearsOfSteel = film(
        id = "tears-of-steel",
        title = "Tears of Steel",
        subtitle = "Sci-fi",
        synopsis = "Amsterdam, the future. A group of warriors and scientists must decide whether a robot can be trusted to undo the past.",
        year = "2012",
        rating = "16+",
        duration = "12m",
        imageUrl = frame("c/cb", "Tears_of_Steel_1080p.webm"),
    )

    private val elephantsDream = film(
        id = "elephants-dream",
        title = "Elephants Dream",
        subtitle = "Surreal",
        synopsis = "Two men wander a vast mechanical world, arguing about what is real while the machine around them comes alive.",
        year = "2006",
        rating = "13+",
        duration = "11m",
        imageUrl = frame("a/a2", "Elephants_Dream_%282006%29.webm"),
    )

    private val cosmosLaundromat = film(
        id = "cosmos-laundromat",
        title = "Cosmos Laundromat",
        subtitle = "Fantasy",
        synopsis = "A suicidal sheep meets a salesman offering the gift of nine lives, on an island at the end of everything.",
        year = "2015",
        rating = "16+",
        duration = "12m",
        imageUrl = frame("3/36", "Cosmos_Laundromat_-_First_Cycle_-_Official_Blender_Foundation_release.webm"),
    )

    private val spring = film(
        id = "spring",
        title = "Spring",
        subtitle = "Drama",
        synopsis = "A shepherd girl and her dog face an ancient spirit that guards the cycle of the seasons.",
        year = "2019",
        rating = "All",
        duration = "8m",
        imageUrl = frame("a/a5", "Spring_-_Blender_Open_Movie.webm"),
    )

    private val granDillama = film(
        id = "caminandes-gran-dillama",
        title = "Caminandes: Gran Dillama",
        subtitle = "Comedy",
        synopsis = "Koro the llama wants the grass on the other side of the fence. The fence disagrees.",
        year = "2013",
        rating = "All",
        duration = "2m",
        imageUrl = frame("8/8b", "Caminandes%2C_Gran_Dillama_-_Blender_Foundation.webm"),
    )

    private val llamigos = film(
        id = "caminandes-llamigos",
        title = "Caminandes: Llamigos",
        subtitle = "Comedy",
        synopsis = "Winter in Patagonia. Koro and a stubborn penguin fight over the last berry on the plain.",
        year = "2016",
        rating = "All",
        duration = "3m",
        imageUrl = frame("a/ab", "Caminandes_3_-_Llamigos_-_Blender_Animated_Short.webm"),
    )

    val featured = sintel

    val continueWatching = listOf(
        tearsOfSteel.copy(subtitle = "41% watched", progress = 0.41f),
        cosmosLaundromat.copy(subtitle = "64% watched", progress = 0.64f),
        elephantsDream.copy(subtitle = "18% watched", progress = 0.18f),
    )

    val trending = listOf(spring, bigBuckBunny, granDillama, llamigos, sintel)

    val animation = listOf(bigBuckBunny, granDillama, llamigos, elephantsDream)

    val blenderStudio = listOf(sintel, spring, cosmosLaundromat, tearsOfSteel)

    val airshipLab = listOf(
        Show(
            id = "lab",
            title = "Airship Lab",
            subtitle = "SDK tools",
            synopsis = "Channel ID, named user, Preference Center, and Scene testing.",
            year = "Lab",
            rating = "Dev",
            duration = "",
            isLab = true,
        ),
    )

    val rows = listOf(
        "Continue Watching" to continueWatching,
        "Trending Now" to trending,
        "Animation" to animation,
        "Blender Studio" to blenderStudio,
        "Tools" to airshipLab,
    )

    private val films = listOf(
        sintel,
        bigBuckBunny,
        tearsOfSteel,
        elephantsDream,
        cosmosLaundromat,
        spring,
        granDillama,
        llamigos,
    )

    /** Resolves the id a deep link carries. Rows hold copies, so this returns the original. */
    fun findFilm(id: String?): Show? = films.firstOrNull { it.id == id }
}
