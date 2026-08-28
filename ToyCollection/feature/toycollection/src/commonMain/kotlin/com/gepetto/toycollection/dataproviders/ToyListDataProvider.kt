package com.gepetto.toycollection.dataproviders

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gepetto.toycollection.models.Toy

data class ToyListProvider(val toyList: List<Toy>)

class ToyListDataProvider : PreviewParameterProvider<ToyListProvider> {
    override val values: Sequence<ToyListProvider>
        get() = sequenceOf(
            ToyListProvider(ToyDataProvider.allToys),
            ToyListProvider(ToyDataProvider.allToys),
        )
}
