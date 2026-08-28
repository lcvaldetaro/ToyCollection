package com.gepetto.toycollection.ui.edittoy

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
fun CheckBoxField(
    label: String,
    value: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (Boolean) -> Unit,
) {
    var check by remember { (mutableStateOf(value)) }

    Column (modifier = modifier) {
        Text(text = label,  fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Checkbox(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            checked = check,
            onCheckedChange = {
                check = it
                onValueChange(it)
            }
        )
    }
}
