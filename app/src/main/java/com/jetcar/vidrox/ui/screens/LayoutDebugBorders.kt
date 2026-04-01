package com.jetcar.vidrox.ui.screens

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity

internal val DEBUG_LAYOUT_BORDERS = false

@Composable
internal fun Modifier.debugLayoutBorder(
    color: Color,
    shape: Shape = RectangleShape,
): Modifier {
    if (!DEBUG_LAYOUT_BORDERS) return this

    val borderWidth = with(LocalDensity.current) { 2.toDp() }
    return border(width = borderWidth, color = color, shape = shape)
}


