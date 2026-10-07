package com.gepetto.toydb.ui

import toydb.composeapp.generated.resources.Res
import toydb.composeapp.generated.resources.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.SportsEsports
import club.gepetto.composeutils.Res as GcRes
import club.gepetto.composeutils.slotcaricon
import club.gepetto.composeutils.train
import club.gepetto.composeutils.staticmodel
import club.gepetto.composeutils.plastickits
import club.gepetto.composeutils.others

class CategoryLocalizationTest {

    @Test
    fun testCategoryShortLabelResourceMapping() {
        assertEquals(Res.string.nav_cat_slots, getCategoryShortLabelResource("slot"))
        assertEquals(Res.string.nav_cat_slots, getCategoryShortLabelResource("slots"))
        assertEquals(Res.string.nav_cat_trains, getCategoryShortLabelResource("train"))
        assertEquals(Res.string.nav_cat_trains, getCategoryShortLabelResource("trains"))
        assertEquals(Res.string.nav_cat_static, getCategoryShortLabelResource("static"))
        assertEquals(Res.string.nav_cat_kits, getCategoryShortLabelResource("kit"))
        assertEquals(Res.string.nav_cat_kits, getCategoryShortLabelResource("kits"))
        assertEquals(Res.string.nav_cat_misc, getCategoryShortLabelResource("misc"))
        assertEquals(Res.string.nav_cat_misc, getCategoryShortLabelResource("others"))

        // Unknown categories return null (fallback will be used)
        assertNull(getCategoryShortLabelResource("custom_category"))
        assertNull(getCategoryShortLabelResource("lego"))
    }

    @Test
    fun testCategoryLabelResourceMapping() {
        assertEquals(Res.string.cat_label_slots, getCategoryLabelResource("slot"))
        assertEquals(Res.string.cat_label_slots, getCategoryLabelResource("slots"))
        assertEquals(Res.string.cat_label_trains, getCategoryLabelResource("train"))
        assertEquals(Res.string.cat_label_trains, getCategoryLabelResource("trains"))
        assertEquals(Res.string.cat_label_static, getCategoryLabelResource("static"))
        assertEquals(Res.string.cat_label_kits, getCategoryLabelResource("kit"))
        assertEquals(Res.string.cat_label_kits, getCategoryLabelResource("kits"))
        assertEquals(Res.string.cat_label_misc, getCategoryLabelResource("misc"))
        assertEquals(Res.string.cat_label_misc, getCategoryLabelResource("others"))

        // Unknown categories return null
        assertNull(getCategoryLabelResource("custom_category"))
        assertNull(getCategoryLabelResource("diecast"))
    }

    @Test
    fun testGetIconByNameMultilingualSupport() {
        // English
        assertEquals(Icons.Default.DirectionsCar, getIconByName("car"))
        assertEquals(Icons.Default.Train, getIconByName("train"))
        assertEquals(Icons.Default.Build, getIconByName("build"))
        assertEquals(Icons.Default.Category, getIconByName("category"))
        assertEquals(Icons.Default.Toys, getIconByName("toys"))

        // Portuguese
        assertEquals(Icons.Default.DirectionsCar, getIconByName("carro"))
        assertEquals(Icons.Default.Train, getIconByName("trem"))
        assertEquals(Icons.Default.Build, getIconByName("ferramenta"))
        assertEquals(Icons.Default.Category, getIconByName("diversos"))
        assertEquals(Icons.Default.Category, getIconByName("outros"))
        assertEquals(Icons.Default.Toys, getIconByName("brinquedo"))
        assertEquals(Icons.Default.SportsEsports, getIconByName("jogo"))

        // Spanish
        assertEquals(Icons.Default.DirectionsCar, getIconByName("coche"))
        assertEquals(Icons.Default.Train, getIconByName("tren"))
        assertEquals(Icons.Default.Build, getIconByName("maqueta"))
        assertEquals(Icons.Default.Category, getIconByName("varios"))

        // French
        assertEquals(Icons.Default.DirectionsCar, getIconByName("voiture"))
        assertEquals(Icons.Default.Train, getIconByName("train"))
        assertEquals(Icons.Default.Build, getIconByName("outillage"))
        assertEquals(Icons.Default.Category, getIconByName("divers"))

        // Italian
        assertEquals(Icons.Default.Train, getIconByName("treno"))
        assertEquals(Icons.Default.Build, getIconByName("attrezzo"))
        assertEquals(Icons.Default.Category, getIconByName("vari"))

        // German
        assertEquals(Icons.Default.Train, getIconByName("zug"))
        assertEquals(Icons.Default.Build, getIconByName("werkzeug"))
        assertEquals(Icons.Default.Category, getIconByName("sonstiges"))
    }

    @Test
    fun testGetCollectionDrawableResourceMultilingualSupport() {
        // Portuguese terms
        assertEquals(GcRes.drawable.slotcaricon, getCollectionDrawableResource("autorama"))
        assertEquals(GcRes.drawable.train, getCollectionDrawableResource("trem"))
        assertEquals(GcRes.drawable.staticmodel, getCollectionDrawableResource("estatico"))
        assertEquals(GcRes.drawable.plastickits, getCollectionDrawableResource("kit"))
        assertEquals(GcRes.drawable.others, getCollectionDrawableResource("diversos"))

        // German terms
        assertEquals(GcRes.drawable.train, getCollectionDrawableResource("zug"))
        assertEquals(GcRes.drawable.staticmodel, getCollectionDrawableResource("standmodell"))
        assertEquals(GcRes.drawable.others, getCollectionDrawableResource("sonstiges"))

        // Spanish terms
        assertEquals(GcRes.drawable.train, getCollectionDrawableResource("tren"))
        assertEquals(GcRes.drawable.others, getCollectionDrawableResource("varios"))
    }
}
