package com.gepetto.toydb.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

/**
 * Maps a category key or alias to its corresponding short navigation StringResource.
 * Returns null if the category is not a built-in category.
 */
fun getCategoryShortLabelResource(category: String): StringResource? {
    return when (category.trim().lowercase()) {
        "slot", "slots", "slotcar", "slotcars" -> Res.string.nav_cat_slots
        "train", "trains" -> Res.string.nav_cat_trains
        "static", "staticmodel", "staticmodels" -> Res.string.nav_cat_static
        "kit", "kits", "modelkit", "modelkits" -> Res.string.nav_cat_kits
        "misc", "miscellaneous", "others", "other" -> Res.string.nav_cat_misc
        else -> null
    }
}

/**
 * Maps a category key or alias to its corresponding full display label StringResource.
 * Returns null if the category is not a built-in category.
 */
fun getCategoryLabelResource(category: String): StringResource? {
    return when (category.trim().lowercase()) {
        "slot", "slots", "slotcar", "slotcars" -> Res.string.cat_label_slots
        "train", "trains" -> Res.string.cat_label_trains
        "static", "staticmodel", "staticmodels" -> Res.string.cat_label_static
        "kit", "kits", "modelkit", "modelkits" -> Res.string.cat_label_kits
        "misc", "miscellaneous", "others", "other" -> Res.string.cat_label_misc
        else -> null
    }
}

/**
 * Returns a resolver lambda suitable for calling from non-composable contexts (like inside `remember`).
 */
@Composable
fun rememberCategoryShortLabelResolver(): (category: String, fallback: String) -> String {
    val slots = stringResource(Res.string.nav_cat_slots)
    val trains = stringResource(Res.string.nav_cat_trains)
    val static = stringResource(Res.string.nav_cat_static)
    val kits = stringResource(Res.string.nav_cat_kits)
    val misc = stringResource(Res.string.nav_cat_misc)

    return remember(slots, trains, static, kits, misc) {
        { category: String, fallback: String ->
            when (category.trim().lowercase()) {
                "slot", "slots", "slotcar", "slotcars" -> slots
                "train", "trains" -> trains
                "static", "staticmodel", "staticmodels" -> static
                "kit", "kits", "modelkit", "modelkits" -> kits
                "misc", "miscellaneous", "others", "other" -> misc
                else -> fallback.ifEmpty { category.take(6) }
            }
        }
    }
}

/**
 * Returns a full label resolver lambda suitable for calling from non-composable contexts.
 */
@Composable
fun rememberCategoryLabelResolver(): (category: String, fallback: String) -> String {
    val slots = stringResource(Res.string.cat_label_slots)
    val trains = stringResource(Res.string.cat_label_trains)
    val static = stringResource(Res.string.cat_label_static)
    val kits = stringResource(Res.string.cat_label_kits)
    val misc = stringResource(Res.string.cat_label_misc)

    return remember(slots, trains, static, kits, misc) {
        { category: String, fallback: String ->
            when (category.trim().lowercase()) {
                "slot", "slots", "slotcar", "slotcars" -> slots
                "train", "trains" -> trains
                "static", "staticmodel", "staticmodels" -> static
                "kit", "kits", "modelkit", "modelkits" -> kits
                "misc", "miscellaneous", "others", "other" -> misc
                else -> fallback.ifEmpty { category.replaceFirstChar { it.uppercase() } }
            }
        }
    }
}

/**
 * Returns the localized short label for navigation buttons.
 * Falls back to the provided fallback text or a truncated category key if unknown.
 */
@Composable
fun getLocalizedCategoryShortLabel(category: String, fallback: String = ""): String {
    val res = getCategoryShortLabelResource(category)
    return if (res != null) {
        stringResource(res)
    } else {
        fallback.ifEmpty { category.take(6) }
    }
}

/**
 * Returns the localized full display label for category headers, titles, and cards.
 * Falls back to the provided fallback text or capitalized category key if unknown.
 */
@Composable
fun getLocalizedCategoryLabel(category: String, fallback: String = ""): String {
    val res = getCategoryLabelResource(category)
    return if (res != null) {
        stringResource(res)
    } else {
        fallback.ifEmpty { category.replaceFirstChar { it.uppercase() } }
    }
}
