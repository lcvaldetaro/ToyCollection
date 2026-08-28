package com.gepetto.toycollection.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.isDark
import club.gepetto.composeutils.sysTextColor
import com.gepetto.common.Common
import com.gepetto.toycollection.models.CollectionList
import com.gepetto.toycollection.ui.common.images.MediumImage
import club.gepetto.composeutils.Res
import club.gepetto.composeutils.*
import com.gepetto.toycollection.ui.collection.main.getResourceFromFilename
import org.jetbrains.compose.resources.stringResource
import toycollectionmultiplatform.shared.common.generated.resources.Res as SharedRes
import toycollectionmultiplatform.shared.common.generated.resources.privacy_policy_link

@Composable
fun AboutSheet (
    modifier : Modifier = Modifier,
    showVersion: Boolean = false,
    showPpLink: Boolean = false,
    collectionList: CollectionList? = null,
    about: String = "",
    aboutString: String = "",
    collectionImage: String = "",
    onPpClick: () -> Unit = {},
    onCloseClick: () -> Unit
) {
    val textColor = sysTextColor()

    BackHandler(true) { onCloseClick() }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        if (collectionImage.isEmpty()) {
            val imageResource = if (isDark()) Res.drawable.invertedgepetto else Res.drawable.gepetto
            MediumImage(
                imageResource = imageResource,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            val imageResource = getResourceFromFilename(collectionImage)
            MediumImage(
                imageResource = imageResource,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        if (showVersion) {
            val text = "${Common.appName} version ${Common.versionString} (${Common.versionBuild})"
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = "Copyright © 2026 Gepetto Club",
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }

        if (about.isNotEmpty()) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = about,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
        }

        if (showPpLink) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
                    .clickable { onPpClick() },
                text = stringResource(SharedRes.string.privacy_policy_link),
                style = MaterialTheme.typography.bodySmall,
                color = sysLinkColor(),
            )
        }

        HorizontalDivider(thickness = 2.dp)
        if (aboutString.isNotEmpty()) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = aboutString,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
        }
        HorizontalDivider(thickness = 2.dp, color = textColor)

        if (collectionList != null && collectionList.totals != null) {
            Text(
                text = collectionList.totals.toString(),
                color = textColor,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun AboutPreview() {
    Common.testInit()

    GcTheme {
        Surface {
            AboutSheet(showVersion = true) {}
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun AboutCollectionPreview() {
    Common.testInit()

    GcTheme {
        Surface {
            AboutSheet(about = "Collection About") {}
        }
    }
}