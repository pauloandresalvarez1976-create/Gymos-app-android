package com.gymos.app.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HidratacionScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gymos_hidratacion", Context.MODE_PRIVATE) }
    val prefsConfig = remember { context.getSharedPreferences("gymos_config", Context.MODE_PRIVATE) }
    val color = LocalAppColor.current

    val hoy = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
    }

    val meta = remember { prefsConfig.getInt("meta_vasos", 8) }

    var vasos by remember {
        val fechaGuardada = prefs.getString("fecha", "")
        val vasosGuardados = if (fechaGuardada == hoy) prefs.getInt("vasos", 0) else 0
        mutableIntStateOf(vasosGuardados)
    }

    fun guardar(cantidad: Int) {
        prefs.edit().putInt("vasos", cantidad).putString("fecha", hoy).apply()
    }

    val porcentaje = (vasos.toFloat() / meta).coerceIn(0f, 1f)
    val completo = vasos >= meta
    val colorAgua = if (completo) Color(0xFF00FF88) else color

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "💧 Hidratación",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Meta: $meta vasos por día",
            fontSize = 13.sp,
            color = Color(0xFFAAAAAA),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "%.1f L".format(vasos * 0.25),
            fontSize = 72.sp,
            fontWeight = FontWeight.Black,
            color = colorAgua
        )
        Text(
            text = if (completo) "¡Meta cumplida! 🎉" else "$vasos de $meta vasos",
            fontSize = 15.sp,
            color = if (completo) Color(0xFF00FF88) else Color(0xFFAAAAAA),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            repeat(meta) { index ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (index < vasos) colorAgua else Color(0xFF222222)
                        )
                        .clickable {
                            val nuevo = if (index < vasos) index else index + 1
                            vasos = nuevo
                            guardar(nuevo)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💧",
                        fontSize = if (index < vasos) 18.sp else 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF222222))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(porcentaje)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colorAgua)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    if (vasos < meta) {
                        vasos++
                        guardar(vasos)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Agregar vaso", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = {
                    if (vasos > 0) {
                        vasos--
                        guardar(vasos)
                    }
                },
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, color)
            ) {
                Icon(Icons.Default.Remove, contentDescription = null, tint = color)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Quitar", color = color)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = {
            vasos = 0
            guardar(0)
        }) {
            Text("Reiniciar", color = Color(0xFF666666), fontSize = 13.sp)
        }
    }
}