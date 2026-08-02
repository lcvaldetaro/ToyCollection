# Walkthrough - Preserving UI State during Navigation

I have updated the search and filter logic in several screens to ensure that user selections are preserved when navigating away and returning. This was achieved by replacing `remember` with `rememberSaveable` for the relevant state variables.

## Changes

### [ExplorerScreen.kt](file:///Users/luiz/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/ExplorerScreen.kt)
The search query and filters (scale and maker) now use `rememberSaveable` with the `category` as a key. This ensures that:
- State is preserved when navigating to a toy detail and back.
- State is reset when switching to a different category explorer.

```diff
-    var searchQuery by remember { mutableStateOf("") }
-    var selectedScale by remember { mutableStateOf("") }
+    var searchQuery by rememberSaveable(category) { mutableStateOf("") }
+    var selectedScale by rememberSaveable(category) { mutableStateOf("") }
     val selectedCondition = ""
-    var selectedMaker by remember { mutableStateOf("") }
+    var selectedMaker by rememberSaveable(category) { mutableStateOf("") }
```

### [MakerDetailScreen.kt](file:///Users/luiz/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/MakerDetailScreen.kt)
The selected category filter now uses `rememberSaveable` with `makerName` as a key.

```diff
-    var selectedCategory by remember { mutableStateOf("all") }
+    var selectedCategory by rememberSaveable(makerName) { mutableStateOf("all") }
```

### [MakerDirectoryScreen.kt](file:///Users/luiz/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/MakerDirectoryScreen.kt)
The search query for manufacturers is now preserved.

```diff
-    var searchQuery by remember { mutableStateOf("") }
+    var searchQuery by rememberSaveable { mutableStateOf("") }
```

## Verification Results

### Manual Verification
- Navigated to "Cars" Explorer, searched for "Porsche", selected Scale "1:64".
- Tapped on a toy to view details.
- Tapped back.
- **Result**: "Porsche" search and "1:64" filter were still active.
- Navigated to "Makers", searched for "Hot Wheels".
- Tapped on "Hot Wheels" to view details.
- Tapped back to Makers list.
- **Result**: "Hot Wheels" search was still active.
