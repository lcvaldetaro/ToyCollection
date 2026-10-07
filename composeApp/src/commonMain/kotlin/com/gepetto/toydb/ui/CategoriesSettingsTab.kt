package com.gepetto.toydb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import com.gepetto.toydb.database.CategorySetting
import com.gepetto.toydb.database.ToyDatabase
import com.gepetto.toydb.database.ToyRepository
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

@Composable
fun CategoriesSettingsTab(
    db: ToyDatabase,
    onCategoriesChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val repository = remember(db) { ToyRepository(db) }
    var categoriesList by remember { mutableStateOf(repository.getCategorySettings()) }

    LaunchedEffect(Unit) {
        categoriesList = repository.getCategorySettings()
    }

    // Category Dialog State
    var showCategoryDialog by remember { mutableStateOf(false) }
    var dialogIsEditMode by remember { mutableStateOf(false) }
    var dialogCategoryKey by remember { mutableStateOf("") }
    var dialogCategoryLabel by remember { mutableStateOf("") }
    var dialogCategoryPrefix by remember { mutableStateOf("") }
    var dialogCategoryTitle by remember { mutableStateOf("") }
    var dialogCategoryIcon by remember { mutableStateOf("") }
    var dialogErrorText by remember { mutableStateOf("") }

    // Delete Category confirmation state
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategorySetting?>(null) }

    val allFieldsRequiredText = stringResource(Res.string.all_fields_required)
    val categoryExistsText = stringResource(Res.string.category_exists)

    val isDark = isSystemInDarkTheme()
    val dialogModifier = if (isDark) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), AlertDialogDefaults.shape)
    } else {
        Modifier
    }

    // Category Create/Edit Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            modifier = dialogModifier,
            containerColor = sysBackgroundColor(),
            title = {
                Text(
                    text = if (dialogIsEditMode) stringResource(Res.string.edit_category_title) else stringResource(Res.string.add_new_category_title),
                    color = sysTextColor()
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (dialogErrorText.isNotEmpty()) {
                        Text(dialogErrorText, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    OutlinedTextField(
                        value = dialogCategoryKey,
                        onValueChange = { dialogCategoryKey = it.trim().lowercase() },
                        label = { Text(stringResource(Res.string.category_id_label)) },
                        placeholder = { Text(stringResource(Res.string.category_id_placeholder)) },
                        enabled = !dialogIsEditMode, // Key cannot be edited
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = sysTextColor(),
                            unfocusedTextColor = sysTextColor(),
                            disabledTextColor = sysTextColor().copy(alpha = 0.5f)
                        )
                    )
                    OutlinedTextField(
                        value = dialogCategoryLabel,
                        onValueChange = { dialogCategoryLabel = it },
                        label = { Text(stringResource(Res.string.category_name_label)) },
                        placeholder = { Text(stringResource(Res.string.category_name_placeholder)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = sysTextColor(),
                            unfocusedTextColor = sysTextColor()
                        )
                    )
                    OutlinedTextField(
                        value = dialogCategoryPrefix,
                        onValueChange = { dialogCategoryPrefix = it.trim() },
                        label = { Text(stringResource(Res.string.filename_prefix_label)) },
                        placeholder = { Text(stringResource(Res.string.filename_prefix_placeholder)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = sysTextColor(),
                            unfocusedTextColor = sysTextColor()
                        )
                    )
                    OutlinedTextField(
                        value = dialogCategoryTitle,
                        onValueChange = { dialogCategoryTitle = it },
                        label = { Text(stringResource(Res.string.html_title_label)) },
                        placeholder = { Text(stringResource(Res.string.html_title_placeholder)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = sysTextColor(),
                            unfocusedTextColor = sysTextColor()
                        )
                    )
                    OutlinedTextField(
                        value = dialogCategoryIcon,
                        onValueChange = { dialogCategoryIcon = it.trim().lowercase() },
                        label = { Text(stringResource(Res.string.category_icon_label)) },
                        placeholder = { Text(stringResource(Res.string.category_icon_placeholder)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = sysTextColor(),
                            unfocusedTextColor = sysTextColor()
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dialogCategoryKey.trim().isEmpty() || dialogCategoryLabel.trim().isEmpty() || dialogCategoryPrefix.trim().isEmpty() || dialogCategoryTitle.trim().isEmpty() || dialogCategoryIcon.trim().isEmpty()) {
                            dialogErrorText = allFieldsRequiredText
                            return@Button
                        }
                        
                        val newSetting = CategorySetting(
                            category = dialogCategoryKey.trim(),
                            label = dialogCategoryLabel.trim(),
                            imagePrefix = dialogCategoryPrefix.trim(),
                            title = dialogCategoryTitle.trim(),
                            icon = dialogCategoryIcon.trim()
                        )
                        
                        if (dialogIsEditMode) {
                            repository.updateCategorySetting(newSetting)
                        } else {
                            if (categoriesList.any { it.category == newSetting.category }) {
                                dialogErrorText = categoryExistsText.replace("%1\$s", newSetting.category)
                                return@Button
                            }
                            repository.addCategorySetting(newSetting)
                        }
                        
                        // Reload state & trigger callback
                        categoriesList = repository.getCategorySettings()
                        onCategoriesChanged()
                        showCategoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(Res.string.save))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCategoryDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    // Delete Category Confirmation Dialog
    if (showDeleteConfirmDialog && categoryToDelete != null) {
        val cat = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            modifier = dialogModifier,
            containerColor = sysBackgroundColor(),
            title = { Text(stringResource(Res.string.delete_category_title), color = sysTextColor()) },
            text = {
                Text(
                    stringResource(Res.string.delete_category_confirm, cat.label),
                    color = MaterialTheme.colorScheme.error
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteCategorySetting(cat.category)
                        categoriesList = repository.getCategorySettings()
                        onCategoriesChanged()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(Res.string.delete_category_title))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    CategoriesManager(
        categoriesList = categoriesList,
        onAddCategory = {
            dialogIsEditMode = false
            dialogCategoryKey = ""
            dialogCategoryLabel = ""
            dialogCategoryPrefix = ""
            dialogCategoryTitle = ""
            dialogCategoryIcon = "category"
            dialogErrorText = ""
            showCategoryDialog = true
        },
        onEditCategory = { cat ->
            dialogIsEditMode = true
            dialogCategoryKey = cat.category
            dialogCategoryLabel = cat.label
            dialogCategoryPrefix = cat.imagePrefix
            dialogCategoryTitle = cat.title
            dialogCategoryIcon = cat.icon
            dialogErrorText = ""
            showCategoryDialog = true
        },
        onDeleteCategory = { cat ->
            categoryToDelete = cat
            showDeleteConfirmDialog = true
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesManager(
    categoriesList: List<CategorySetting>,
    onAddCategory: () -> Unit,
    onEditCategory: (CategorySetting) -> Unit,
    onDeleteCategory: (CategorySetting) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(Res.string.categories_manager_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
            IconButton(onClick = onAddCategory) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_category_desc), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GcSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(GcSpacing.Small)
        ) {
            categoriesList.forEach { cat ->
                Card(
                    modifier = Modifier.width(320.dp),
                    colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(GcSpacing.Standard),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(stringResource(Res.string.category_key_prefix_icon_info, cat.category, cat.imagePrefix, cat.icon), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (cat.title.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(stringResource(Res.string.category_html_title_info, cat.title), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            }
                        }
                        Row {
                            IconButton(onClick = { onEditCategory(cat) }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit_category_desc), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDeleteCategory(cat) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.delete_category_title), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "Landscape", widthDp = 800, heightDp = 480)
@Composable
fun CategoriesManagerPreview() {
    GcTheme {
        CategoriesManager(
            categoriesList = listOf(
                CategorySetting("diecast", "Die-cast Models", "diecast", "Die-cast Collection", "car"),
                CategorySetting("action_figures", "Action Figures", "figure", "Action Figures", "person")
            ),
            onAddCategory = {},
            onEditCategory = {},
            onDeleteCategory = {}
        )
    }
}
