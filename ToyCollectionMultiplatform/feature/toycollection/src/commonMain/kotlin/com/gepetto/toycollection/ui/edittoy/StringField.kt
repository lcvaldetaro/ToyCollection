package com.gepetto.toycollection.ui.edittoy

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.composeutils.isDark
import club.gepetto.GcLog

@Composable
fun StringField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    onValueChange: (String) -> Unit,
) {
    val fColor = if (isDark())
        OutlinedTextFieldDefaults.colors().copy(
            focusedLabelColor = Color.Yellow,
            unfocusedLabelColor = Color.LightGray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
        )
    else
        OutlinedTextFieldDefaults.colors()

    var text by remember { mutableStateOf(value) }

    OutlinedTextField (
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        maxLines = maxLines,
        label = { Text (text = label, fontSize = 12.sp) },
        textStyle = MaterialTheme.typography.bodySmall,
        value = text,
        onValueChange = {
            GcLog.v("new value = $it")
            text = it
            onValueChange(it)
        },
        colors = fColor
    )
}
