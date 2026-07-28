package com.gepetto.toydb.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.sysTextColor
import com.gepetto.toydb.database.Toy

@Composable
fun ToyForm(
    initialToy: Toy,
    onSave: (Toy) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("General", "Makers & Parts", "Financials", "Restoration", "Images")

    // General state
    var description by remember { mutableStateOf(initialToy.description) }
    var scale by remember { mutableStateOf(initialToy.scale) }
    var condition by remember { mutableStateOf(initialToy.condition) }
    var color by remember { mutableStateOf(initialToy.color) }
    var boxed by remember { mutableStateOf(initialToy.boxed == "y") }
    var factoryCar by remember { mutableStateOf(initialToy.factoryCar == "y") }

    // Makers state
    var bodyMaker by remember { mutableStateOf(initialToy.bodyMaker) }
    var chassisMaker by remember { mutableStateOf(initialToy.chassisMaker) }
    var chassisType by remember { mutableStateOf(initialToy.chassisType) }
    var motorMaker by remember { mutableStateOf(initialToy.motorMaker) }
    var motorDetails by remember { mutableStateOf(initialToy.motorDetails) }
    var catalogNumber by remember { mutableStateOf(initialToy.catalogNumber) }

    // Financials state
    var acquired by remember { mutableStateOf(initialToy.acquired) }
    var amountPaid by remember { mutableStateOf(if (initialToy.amountPaid > 0) initialToy.amountPaid.toString() else "") }
    var value by remember { mutableStateOf(if (initialToy.value > 0) initialToy.value.toString() else "") }
    var amountSold by remember { mutableStateOf(initialToy.amountSold) }
    var traded by remember { mutableStateOf(initialToy.traded) }
    var buy by remember { mutableStateOf(initialToy.buy) }

    // Restoration state
    var majorWork by remember { mutableStateOf(initialToy.majorWork) }
    var minorWork by remember { mutableStateOf(initialToy.minorWork) }
    var repro by remember { mutableStateOf(initialToy.repro) }
    var maintenance by remember { mutableStateOf(initialToy.maintenance) }
    var toMake by remember { mutableStateOf(initialToy.toMake) }
    var detail by remember { mutableStateOf(initialToy.detail) }
    var comments by remember { mutableStateOf(initialToy.comments) }

    // Images state
    var picture by remember { mutableStateOf(initialToy.picture) }
    var pictureSize by remember { mutableStateOf(if (initialToy.pictureSize > 0) initialToy.pictureSize.toString() else "") }
    var pictureTimeStamp by remember { mutableStateOf(if (initialToy.pictureTimeStamp > 0) initialToy.pictureTimeStamp.toString() else "") }
    var hasPicture by remember { mutableStateOf(initialToy.hasPicture == "y") }
    var bitmaps by remember { mutableStateOf(initialToy.bitmaps) }
    var bitmapsSize by remember { mutableStateOf(initialToy.bitmapsSize) }
    var bitmapsTimeStamp by remember { mutableStateOf(initialToy.bitmapsTimeStamp) }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    text = { Text(title, fontSize = 12.sp) }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(GcSpacing.Standard)
        ) {
            when (tabIndex) {
                0 -> { // General
                    FormField(label = "Description*", value = description, onValueChange = { description = it })
                    FormField(label = "Scale", value = scale, onValueChange = { scale = it })
                    FormField(label = "Condition", value = condition, onValueChange = { condition = it })
                    FormField(label = "Color", value = color, onValueChange = { color = it })
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = GcSpacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = boxed, onCheckedChange = { boxed = it })
                        Text("Boxed", color = sysTextColor())
                        Spacer(modifier = Modifier.width(GcSpacing.Large))
                        Checkbox(checked = factoryCar, onCheckedChange = { factoryCar = it })
                        Text("Factory Original", color = sysTextColor())
                    }
                    FormField(label = "General Comments", value = comments, onValueChange = { comments = it })
                }
                1 -> { // Makers
                    FormField(label = "Body Maker", value = bodyMaker, onValueChange = { bodyMaker = it })
                    FormField(label = "Chassis Maker", value = chassisMaker, onValueChange = { chassisMaker = it })
                    FormField(label = "Chassis Type", value = chassisType, onValueChange = { chassisType = it })
                    FormField(label = "Motor Maker", value = motorMaker, onValueChange = { motorMaker = it })
                    FormField(label = "Motor Details", value = motorDetails, onValueChange = { motorDetails = it })
                    FormField(label = "Catalog/Model Number", value = catalogNumber, onValueChange = { catalogNumber = it })
                }
                2 -> { // Financials
                    FormField(label = "Acquisition Details", value = acquired, onValueChange = { acquired = it })
                    FormField(label = "Amount Paid ($)", value = amountPaid, onValueChange = { amountPaid = it })
                    FormField(label = "Estimated Value ($)", value = value, onValueChange = { value = it })
                    FormField(label = "Amount Sold ($ / Traded)", value = amountSold, onValueChange = { amountSold = it })
                    FormField(label = "Traded Info", value = traded, onValueChange = { traded = it })
                    FormField(label = "Buy Info", value = buy, onValueChange = { buy = it })
                }
                3 -> { // Restoration
                    FormField(label = "Major Work Done", value = majorWork, onValueChange = { majorWork = it })
                    FormField(label = "Minor Work Done", value = minorWork, onValueChange = { minorWork = it })
                    FormField(label = "Repro Details (y/r)", value = repro, onValueChange = { repro = it })
                    FormField(label = "Maintenance Log", value = maintenance, onValueChange = { maintenance = it })
                    FormField(label = "To Build Info", value = toMake, onValueChange = { toMake = it })
                    FormField(label = "Detail/Decal Work", value = detail, onValueChange = { detail = it })
                }
                4 -> { // Images
                    FormField(label = "Main Picture Filename", value = picture, onValueChange = { picture = it })
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = GcSpacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = hasPicture, onCheckedChange = { hasPicture = it })
                        Text("Has Picture", color = sysTextColor())
                    }
                    
                    FormField(label = "Secondary Bitmaps (space separated)", value = bitmaps, onValueChange = { bitmaps = it })
                }
            }
        }

        // Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GcSpacing.Standard),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(end = GcSpacing.Small)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (description.trim().isEmpty()) return@Button
                    val body = bodyMaker.trim()
                    val chassis = chassisMaker.trim()
                    val calculatedMakerCombo = if (body == chassis) {
                        body
                    } else if (body.isEmpty()) {
                        chassis
                    } else if (chassis.isEmpty()) {
                        body
                    } else {
                        "$chassis/$body"
                    }
                    val updated = Toy(
                        refNum = initialToy.refNum,
                        toyType = initialToy.toyType,
                        description = description,
                        makerCombo = calculatedMakerCombo,
                        scale = scale,
                        factoryCar = if (factoryCar) "y" else "n",
                        bodyMaker = bodyMaker,
                        acquired = acquired,
                        chassisType = chassisType,
                        chassisMaker = chassisMaker,
                        condition = condition,
                        color = color,
                        motorMaker = motorMaker,
                        motorDetails = motorDetails,
                        catalogNumber = catalogNumber,
                        comments = comments,
                        majorWork = majorWork,
                        minorWork = minorWork,
                        repro = repro,
                        value = value.toDoubleOrNull() ?: 0.0,
                        amountPaid = amountPaid.toDoubleOrNull() ?: 0.0,
                        amountSold = amountSold,
                        traded = traded,
                        buy = buy,
                        maintenance = maintenance,
                        toMake = toMake,
                        detail = detail,
                        boxed = if (boxed) "y" else "n",
                        picture = picture,
                        pictureSize = pictureSize.toIntOrNull() ?: 0,
                        pictureTimeStamp = pictureTimeStamp.toLongOrNull() ?: 0L,
                        hasPicture = if (hasPicture) "y" else "n",
                        bitmaps = bitmaps,
                        bitmapsSize = bitmapsSize,
                        bitmapsTimeStamp = bitmapsTimeStamp
                    )
                    onSave(updated)
                },
                enabled = description.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
fun FormField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = label != "General Comments" && !label.contains("Work")
    )
}
