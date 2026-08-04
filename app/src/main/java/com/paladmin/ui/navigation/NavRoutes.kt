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
    const val LIVE_MAP = "dashboard/{profileId}/livemap?focusX={focusX}&focusY={focusY}"
    const val HUMAN_PICKER = "dashboard/{profileId}/humans"

    fun addEditProfile(profileId: Long? = null) = "profiles/edit?profileId=${profileId ?: -1}"
    fun dashboard(profileId: Long) = "dashboard/$profileId"
    fun itemPicker(profileId: Long) = "dashboard/$profileId/items"
    fun palPicker(profileId: Long) = "dashboard/$profileId/pals"
    fun players(profileId: Long) = "dashboard/$profileId/players"
    fun guilds(profileId: Long) = "dashboard/$profileId/guilds"
    fun bans(profileId: Long) = "dashboard/$profileId/bans"
    fun broadcast(profileId: Long) = "dashboard/$profileId/broadcast"
    fun liveMap(profileId: Long, focusX: Double? = null, focusY: Double? = null) =
        "dashboard/$profileId/livemap?focusX=${focusX ?: ""}&focusY=${focusY ?: ""}"
    fun humanPicker(profileId: Long) = "dashboard/$profileId/humans"
}
