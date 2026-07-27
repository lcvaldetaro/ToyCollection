package com.gepetto.common

const val FOLDER = "database"
const val SETTINGS_FILENAME = "${FOLDER}-settings"
const val SAVED_FILENAME = "${FOLDER}-file"
const val DESKTOP_BASE_URL = "https://gepetto.club/"
const val MOBILE_BASE_URL = "https://gepetto.club/"
val BASE_URL = getPlatformBaseUrl()
val WEBSITE_BASE_URL = "${BASE_URL}${FOLDER}/"
val PRIVACY_POLICY_URL = "${BASE_URL}privacypolicy.html"
const val TOYS_QUERY_STRING = "carlist.json"
const val TRAINS_QUERY_STRING = "tralist.json"
const val STATIC_QUERY_STRING = "stalist.json"
const val PLASTIC_QUERY_STRING = "plalist.json"
const val MISC_QUERY_STRING = "mislist.json"
const val MAKERS_QUERY_STRING = "carmaker.json"
const val CELL_SIZE = 180
val BANNER_CONTENT = """
        Gepetto loves toys and collects them. He has several collections: Slot Cars, Toy Trains, Static models, Model kits plus others.
        
        The icons here will direct you to each of his prized collections.
        
        Enjoy!
    """.trimIndent()
