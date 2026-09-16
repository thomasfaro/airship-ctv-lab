package com.airship.ctvlab

import com.airship.ctvlab.airship.AirshipIds

object LabInfo {
    const val LIVE_SITE_URL = "https://airship-ctv-web-lab.netlify.app"
    const val DEEP_LINK_SCHEME = "ctvlab"
    const val PLAY_HOST = "play"
    const val INBOX_HOST = "inbox"
    const val LAB_HOST = "lab"
    const val HOME_HOST = "home"

    val embeddedContentIds = listOf(AirshipIds.HOME_BANNER)
    val customComponents = listOf(
        "HomeBanner + AirshipEmbeddedView",
        "InboxScreen + MessageCenterMessage",
        "ThomasFocus D-pad corrections",
    )
    val availableUrls = listOf(
        "$LIVE_SITE_URL/",
        "$LIVE_SITE_URL/play/<film-id>",
        "$LIVE_SITE_URL/?play=<film-id>",
    )
    val deepLinks = listOf(
        "$DEEP_LINK_SCHEME://$HOME_HOST",
        "$DEEP_LINK_SCHEME://$LAB_HOST",
        "$DEEP_LINK_SCHEME://$INBOX_HOST",
        "$DEEP_LINK_SCHEME://$PLAY_HOST/<film-id>",
    )
}
