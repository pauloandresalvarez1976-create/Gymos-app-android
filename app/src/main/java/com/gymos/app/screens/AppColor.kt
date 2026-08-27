package com.gymos.app.screens

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAppColor = compositionLocalOf { Color(0xFFFF4500) }

fun getColorGuardado(context: Context): Color {
    val prefs = context.getSharedPreferences("gymos_config", Context.MODE_PRIVATE)
    val index = prefs.getInt("color_index", 0)
    return coloresDisponibles.getOrNull(index)?.second ?: Color(0xFFFF4500)
}