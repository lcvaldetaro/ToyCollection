package com.gepetto.toydb.utils

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Extension modifier that allows scrolling a LazyRow horizontally using the vertical mouse scroll wheel.
 */
fun Modifier.scrollHorizontallyWithMouseWheel(
    lazyListState: LazyListState,
    coroutineScope: CoroutineScope
): Modifier = this.pointerInput(lazyListState) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Scroll) {
                val scrollDelta = event.changes.firstOrNull()?.scrollDelta ?: continue
                if (scrollDelta.y != 0f) {
                    val amount = scrollDelta.y * 60f // Adjust scroll speed factor as needed
                    coroutineScope.launch {
                        lazyListState.scrollBy(amount)
                    }
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
}
