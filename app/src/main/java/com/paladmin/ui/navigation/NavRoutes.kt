package com.paladmin.ui.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val PROFILE_LIST = "profiles"
    const val APP_SETTINGS = "settings"
    const val ADD_EDIT_PROFILE = "profiles/edit?profileId={profileId}"
    const val DASHBOARD = "dashboard/{profileId}"
    const val ITEM_PICKER = "dashboard/{profileId}/items"
    const val PAL_PICKER = "dashboard/{profileId}/pals"
    const val PLAYERS = "dashboard/{profileId}/players"
    const val GUILDS = "dashboard/{profileId}/guilds"
    const val BANS = "dashboard/{profileId}/bans"
    const val BROADCAST = "dashboard/{profileId}/broadcast"
    const val LIVE_MAP = "dashboard/{profileId}/livemap?focusPoints={focusPoints}"
    const val HUMAN_PICKER = "dashboard/{profileId}/humans"

    fun addEditProfile(profileId: Long? = null) = "profiles/edit?profileId=${profileId ?: -1}"
    fun dashboard(profileId: Long) = "dashboard/$profileId"
    fun itemPicker(profileId: Long) = "dashboard/$profileId/items"
    fun palPicker(profileId: Long) = "dashboard/$profileId/pals"
    fun players(profileId: Long) = "dashboard/$profileId/players"
    fun guilds(profileId: Long) = "dashboard/$profileId/guilds"
    fun bans(profileId: Long) = "dashboard/$profileId/bans"
    fun broadcast(profileId: Long) = "dashboard/$profileId/broadcast"
    /** Points de base à épingler sur la carte (une guilde peut avoir plusieurs camps) — encodés
     * "x1,y1;x2,y2;..." dans le paramètre de route, ré-analysés côté LiveMapViewModel. */
    fun liveMap(profileId: Long, focusPoints: List<Pair<Double, Double>> = emptyList()) =
        "dashboard/$profileId/livemap?focusPoints=${focusPoints.joinToString(";") { (x, y) -> "$x,$y" }}"
    fun humanPicker(profileId: Long) = "dashboard/$profileId/humans"
}
