package com.gepetto.toycollection.ui.toy

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gepetto.toycollection.models.Toy
import com.gepetto.toycollection.models.CollectionData
import com.gepetto.toycollection.ui.common.images.ImageListItem

@Composable
fun ToyDetailsCarousel(
    toy: Toy,
    collection: CollectionData,
    modifier: Modifier = Modifier,
) {
    val thisTimeStamp = remember { mutableStateOf(0L) }

    if (toy.bitmapFiles != null && toy.bitmapFiles!!.isNotEmpty()) {
        val files = Array<String>(toy.bitmapFiles!!.size + 1) { "" }
        var i = toy.bitmapFiles!!.size - 1
        var j = 0
        while (i >= 0) {
            files.set(j++, toy.bitmapFiles!![i--])
        }

        val maker = toy.getMaker(collection)

        if (maker!!.picture == "") {
            files.set(j, "word ${maker.name}")
        }

        val newFiles = files.toMutableList()
        if (files.get(j).equals(""))
            newFiles.removeAt(files.size - 1)

        LazyRow(
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(
                items = newFiles,
                itemContent = {
                    val image = it
                    ImageListItem(
                        imageFile = image,
                        files = newFiles.toTypedArray(),
                        onUpdate = { thisTimeStamp.value = it }
                    )
                }
            )
        }
    }
}
