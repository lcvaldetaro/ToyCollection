package com.gepetto.toycollection.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysForegroundColor

@Composable
fun SearchInput (
    searchString: String,
    modifier: Modifier = Modifier,
    searchStringUpdated: (String) -> Unit = {},
) {
    Card(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
        border = BorderStroke(1.dp, sysForegroundColor().copy(alpha = 0.3f))
    ) {
        Row {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = searchString,
                onValueChange = { searchStringUpdated(it) },
                label = { Text("Search") },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = sysForegroundColor(),
                    unfocusedTextColor = sysForegroundColor(),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedLabelColor = sysForegroundColor(),
                    unfocusedLabelColor = sysForegroundColor().copy(alpha = 0.7f),
                    cursorColor = sysForegroundColor()
                )
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SearchInputPreview() {
    com.gepetto.common.Common.testInit()
    GcTheme {
        androidx.compose.material3.Surface {
            SearchInput("value")
        }
    }
}
