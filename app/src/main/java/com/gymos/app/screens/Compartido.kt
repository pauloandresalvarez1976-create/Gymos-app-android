package com.gymos.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val ANTHROPIC_API_KEY: String = "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"

@Composable
fun SelectorSeccion(
    titulo: String,
    opciones: List<String>,
    seleccionado: String,
    onSeleccionar: (String) -> Unit,
    color: Color = LocalAppColor.current
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF111111))
            .padding(20.dp)
    ) {
        Column {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                opciones.forEach { opcion ->
                    val estaSeleccionado = opcion == seleccionado
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (estaSeleccionado) color.copy(alpha = 0.15f) else Color(0xFF1A1A1A))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = estaSeleccionado,
                            onClick = { onSeleccionar(opcion) },
                            colors = RadioButtonDefaults.colors(selectedColor = color)
                        )
                        Text(
                            opcion,
                            fontSize = 14.sp,
                            color = if (estaSeleccionado) Color.White else Color(0xFFAAAAAA),
                            fontWeight = if (estaSeleccionado) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}