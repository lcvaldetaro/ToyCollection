package com.gepetto.toycollection.ui.toy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysLinkColor
import com.gepetto.toycollection.intentprocessors.ListDetailsIntent
import com.gepetto.toycollection.intentprocessors.ListDetailsTapAction
import com.gepetto.toycollection.models.Toy
import com.gepetto.toycollection.models.CollectionData

@Composable
fun ToyNavBar(
    toy: Toy,
    collection: CollectionData,
    modifier: Modifier = Modifier,
    onClickAction: (ListDetailsIntent.Tapped) -> Unit,
) {
    Column (modifier.fillMaxWidth().background(sysBackgroundColor())) {
        Row(Modifier.padding(horizontal = 8.dp).align(Alignment.CenterHorizontally)) {
            Text(
                text = "Web Search",
                color = sysLinkColor(),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable { onClickAction(ListDetailsIntent.Tapped(ListDetailsTapAction.TapWebSearch(toy))) }
            )

            Text(
                text = "More ...",
                color = sysLinkColor(),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable { onClickAction(ListDetailsIntent.Tapped(ListDetailsTapAction.TapEditToy(toy.getMaker(collection)!!, toy))) }
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ToyTitlePreview(
) {
    com.gepetto.common.Common.testInit()
    club.gepetto.composeutils.GcTheme{
        androidx.compose.material3.Surface {
            Column {
                ToyNavBar(
                    toy = com.gepetto.toycollection.dataproviders.ToyDataProvider.toy1,
                    collection = com.gepetto.toycollection.dataproviders.MakerDataProvider.collectionDataDb,
                ) {}
            }
        }
    }
}
