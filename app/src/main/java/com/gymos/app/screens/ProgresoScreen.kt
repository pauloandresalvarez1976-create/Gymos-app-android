package com.gymos.app.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Medida(val fecha: String, val peso: String, val cintura: String, val pecho: String, val brazo: String)

@Composable
fun ProgresoScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gymos_progreso", Context.MODE_PRIVATE) }
    val color = LocalAppColor.current

    var peso by remember { mutableStateOf("") }
    var cintura by remember { mutableStateOf("") }
    var pecho by remember { mutableStateOf("") }
    var brazo by remember { mutableStateOf("") }
    var guardado by remember { mutableStateOf(false) }
    var graficoCampo by remember { mutableStateOf("peso") }

    val historial = remember {
        val json = prefs.getString("historial", "[]") ?: "[]"
        try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                Medida(
                    obj.getString("fecha"),
                    obj.optString("peso", ""),
                    obj.optString("cintura", ""),
                    obj.optString("pecho", ""),
                    obj.optString("brazo", "")
                )
            }.toMutableList()
        } catch (e: Exception) { mutableListOf() }
    }

    fun guardar() {
        val hoy = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
        val nueva = Medida(hoy, peso, cintura, pecho, brazo)
        historial.add(0, nueva)
        val arr = JSONArray()
        historial.forEach { m ->
            val obj = JSONObject()
            obj.put("fecha", m.fecha)
            obj.put("peso", m.peso)
            obj.put("cintura", m.cintura)
            obj.put("pecho", m.pecho)
            obj.put("brazo", m.brazo)
            arr.put(obj)
        }
        prefs.edit().putString("historial", arr.toString()).apply()
        peso = ""; cintura = ""; pecho = ""; brazo = ""
        guardado = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("📊 Progreso Corporal", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Registrá tus medidas y seguí tu evolución", fontSize = 13.sp, color = Color(0xFFAAAAAA), modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

        // FORMULARIO
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF111111)).padding(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Nueva medición", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                CampoMedida("Peso (kg)", peso, Icons.Default.MonitorWeight, color) { peso = it }
                CampoMedida("Cintura (cm)", cintura, Icons.Default.LinearScale, color) { cintura = it }
                CampoMedida("Pecho (cm)", pecho, Icons.Default.FitnessCenter, color) { pecho = it }
                CampoMedida("Brazo (cm)", brazo, Icons.Default.Straighten, color) { brazo = it }
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { if (peso.isNotEmpty()) guardar() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar medición", fontWeight = FontWeight.Bold)
                }
                if (guardado) {
                    Text("✅ Medición guardada", color = Color(0xFF00FF88), fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }

        // GRÁFICO
        if (historial.size >= 2) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF111111))
                    .padding(20.dp)
            ) {
                Column {
                    Text("📈 Evolución", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Selector de campo
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("peso" to "Peso", "cintura" to "Cintura", "pecho" to "Pecho", "brazo" to "Brazo").forEach { (campo, label) ->
                            val sel = graficoCampo == campo
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) color else Color(0xFF1A1A1A))
                                    .clickable { graficoCampo = campo }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(label, fontSize = 12.sp, color = if (sel) Color.White else Color(0xFF666666), fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val datos = historial.reversed().mapNotNull { m ->
                        val valor = when (graficoCampo) {
                            "peso" -> m.peso.toDoubleOrNull()
                            "cintura" -> m.cintura.toDoubleOrNull()
                            "pecho" -> m.pecho.toDoubleOrNull()
                            "brazo" -> m.brazo.toDoubleOrNull()
                            else -> null
                        }
                        if (valor != null) Pair(m.fecha, valor) else null
                    }

                    if (datos.size >= 2) {
                        val maxVal = datos.maxOf { it.second }
                        val minVal = datos.minOf { it.second }
                        val rango = (maxVal - minVal).coerceAtLeast(1.0)

                        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val paso = w / (datos.size - 1).coerceAtLeast(1)
                                val puntos = datos.mapIndexed { i, (_, v) ->
                                    Offset(i * paso, h * (1 - ((v - minVal) / rango).toFloat()).coerceIn(0f, 1f))
                                }

                                // Área rellena
                                val path = Path()
                                path.moveTo(puntos.first().x, h)
                                puntos.forEach { path.lineTo(it.x, it.y) }
                                path.lineTo(puntos.last().x, h)
                                path.close()
                                drawPath(path, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.4f), Color.Transparent)))

                                // Línea
                                for (i in 0 until puntos.size - 1) {
                                    drawLine(color = color, start = puntos[i], end = puntos[i + 1], strokeWidth = 3.dp.toPx())
                                }

                                // Puntos
                                puntos.forEach { p ->
                                    drawCircle(color = color, radius = 5.dp.toPx(), center = p)
                                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = p)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(datos.first().first, fontSize = 10.sp, color = Color(0xFF666666))
                            val unidad = if (graficoCampo == "peso") "kg" else "cm"
                            Text("%.1f → %.1f $unidad".format(datos.first().second, datos.last().second), fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
                            Text(datos.last().first, fontSize = 10.sp, color = color)
                        }
                    } else {
                        Text("Necesitás al menos 2 mediciones de ${graficoCampo} para ver el gráfico", fontSize = 13.sp, color = Color(0xFF666666))
                    }
                }
            }
        }

        // HISTORIAL
        if (historial.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Historial", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                historial.forEach { medida ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF111111))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(medida.fecha, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (medida.peso.isNotEmpty()) FilaMedida("Peso", "${medida.peso} kg")
                                if (medida.cintura.isNotEmpty()) FilaMedida("Cintura", "${medida.cintura} cm")
                                if (medida.pecho.isNotEmpty()) FilaMedida("Pecho", "${medida.pecho} cm")
                                if (medida.brazo.isNotEmpty()) FilaMedida("Brazo", "${medida.brazo} cm")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CampoMedida(
    label: String,
    valor: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(label, color = Color(0xFFAAAAAA)) },
        leadingIcon = { Icon(icono, contentDescription = null, tint = color) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = color, focusedLabelColor = color,
            unfocusedBorderColor = Color(0xFF333333), unfocusedLabelColor = Color(0xFFAAAAAA),
            focusedTextColor = Color.White, unfocusedTextColor = Color.White
        )
    )
}

@Composable
fun FilaMedida(label: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color(0xFFAAAAAA))
    }
}