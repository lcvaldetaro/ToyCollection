package com.gepetto.toycollection.ui.edittoy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import com.gepetto.toycollection.dataproviders.ToyDataProvider
import com.gepetto.toycollection.models.Toy
import com.gepetto.toycollection.models.isYes

@Composable
fun EditToyBody(
    toy: Toy,
    modifier: Modifier = Modifier,
    secretData: Boolean = true,
    evaluate: (Toy) -> Unit = {}
) {
    Column(modifier = modifier.background(sysBackgroundColor()).verticalScroll(rememberScrollState())) {

        StringField("description", toy.description, maxLines = 2) { toy.description = it; evaluate(toy) }

        Row {
            StringField("comments", toy.comments, maxLines = 4) { toy.comments = it; evaluate(toy) }
        }

        Row {
            StringField("chassis brand", toy.chassisMaker, modifier = Modifier.weight(1f)) { toy.chassisMaker = it; evaluate(toy) }
            StringField("chassis type", toy.chassisType, modifier = Modifier.weight(1f)) { toy.chassisType = it; evaluate(toy) }
            StringField("body", toy.bodyMaker, modifier = Modifier.weight(1f)) { toy.bodyMaker = it; evaluate(toy) }
        }

        Row {
            StringField("motor brand", toy.motorMaker, modifier = Modifier.weight(1f)) { toy.motorMaker = it; evaluate(toy) }
            StringField("motor type", toy.motorDetails, modifier = Modifier.weight(1f)) { toy.motorDetails = it; evaluate(toy) }
        }

        Row {
            StringField("scale", toy.scale, modifier = Modifier.weight(1f)) { toy.scale = it; evaluate(toy) }
            StringField("work", toy.majorWork, modifier = Modifier.weight(1f)) { toy.majorWork = it; evaluate(toy) }
            StringField("picture", toy.picture, modifier = Modifier.weight(1f)) { toy.picture = it; evaluate(toy) }
        }

        StringField("extra pictures", toy.bitmaps, maxLines = 4) { toy.bitmaps = it; evaluate(toy) }

        if (secretData) {
            StringField("acquired", toy.acquired, maxLines = 2) { toy.acquired = it; evaluate(toy) }

            Row {
                StringField("amount paid", toy.amountPaid, modifier = Modifier.weight(1f)) { toy.amountPaid = it; evaluate(toy) }
                StringField("value", toy.value, modifier = Modifier.weight(1f)) { toy.value = it; evaluate(toy) }
                StringField("amount sold", toy.amountSold, modifier = Modifier.weight(1f)) { toy.amountSold = it; evaluate(toy) }
            }
        }

        Row {
            StringField("condition", toy.condition, modifier = Modifier.weight(2f)) { toy.condition = it; evaluate(toy) }
            CheckBoxField("has picture", toy.hasPicture.isYes(), modifier = Modifier.weight(1f)) {
                toy.hasPicture = if (it) "y" else ""
                toy.normalizeData()
                evaluate(toy)
            }
            CheckBoxField("repro", toy.repro.isYes(), modifier = Modifier.weight(1f)) {
                toy.repro = if (it) "y" else ""
                toy.normalizeData()
                evaluate(toy)
            }
            CheckBoxField("boxed", toy.boxed.isYes(), modifier = Modifier.weight(1f)) {
                toy.boxed = if (it) "y" else ""
                toy.normalizeData()
                evaluate(toy)
            }
        }

        toy.makerCombo =
            if (toy.chassisMaker.isNotEmpty() && toy.chassisMaker != toy.bodyMaker)
                "${toy.chassisMaker}/${toy.bodyMaker}"
            else
                toy.bodyMaker

        Row {
            StringField("traded", toy.traded, modifier = Modifier.weight(2f)) { toy.traded = it; evaluate(toy) }
            StringField("color", toy.color, modifier = Modifier.weight(1f)) { toy.color = it; evaluate(toy) }
            StringField("catalog", toy.catalogNumber, modifier = Modifier.weight(1f)) { toy.catalogNumber = it; evaluate(toy) }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    GcTheme {
        Surface {
            EditToyBody(toy = ToyDataProvider.toy1)
        }
    }
}
