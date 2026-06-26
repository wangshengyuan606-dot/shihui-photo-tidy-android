package com.example.phototidy.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import kotlin.math.abs

fun Modifier.edgeHorizontalBackGesture(
    enabled: Boolean,
    onBack: () -> Unit,
): Modifier {
    if (!enabled) return this

    return pointerInput(enabled, onBack) {
        val edgeWidth = 32.dp.toPx()
        val triggerDistance = 78.dp.toPx()

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val isEdgeDrag = down.position.x <= edgeWidth ||
                down.position.x >= size.width - edgeWidth

            if (!isEdgeDrag) {
                return@awaitEachGesture
            }

            var totalDrag = 0f
            var triggered = false
            horizontalDrag(down.id) { change ->
                totalDrag += change.positionChange().x
                if (!triggered) {
                    change.consume()
                }
                if (!triggered && abs(totalDrag) >= triggerDistance) {
                    triggered = true
                    onBack()
                }
            }
        }
    }
}
