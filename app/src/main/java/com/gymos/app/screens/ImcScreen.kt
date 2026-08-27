package com.gymos.app.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ImcScreen() {
    val context = LocalContext.current
    val prefsImc = remember { context.getSharedPreferences("gymos_imc", Context.MODE_PRIVATE) }
    val color = LocalAppColor.current

    var peso by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var esMasculino by remember { mutableStateOf(true) }
    var resultado by remember { mutableStateOf<Double?>(null) }
    var alturaGuardada by remember { mutableStateOf(0.0) }
    var pesoGuardado by remember { mutableStateOf(0.0) }

    val historial = remember {
        val json = prefsImc.getString("historial", "[]") ?: "[]"
        try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                Triple(obj.getString("fecha"), obj.getDouble("imc"), obj.getDouble("peso"))
            }.toMutableList()
        } catch (e: Exception) { mutableListOf() }
    }

    fun guardarImc(imc: Double, pesoVal: Double) {
        val hoy = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
        historial.add(0, Triple(hoy, imc, pesoVal))
        if (historial.size > 10) historial.removeAt(historial.size - 1)
        val arr = JSONArray()
        historial.forEach { (f, i, p) ->
            val obj = org.json.JSONObject()
            obj.put("fecha", f); obj.put("imc", i); obj.put("peso", p)
            arr.put(obj)
        }
        prefsImc.edit().putString("historial", arr.toString()).apply()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("⚖️ Índice de Masa Corporal", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Calculá tu IMC y seguí tu evolución", fontSize = 13.sp, color = Color(0xFFAAAAAA), modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF111111)).padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Text("Ingresá tus datos", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)

                OutlinedTextField(
                    value = peso,
                    onValueChange = { peso = it },
                    placeholder = { Text("Peso en kg  (ej: 75)", color = Color(0xFF555555)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = color, unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = altura,
                    onValueChange = { altura = it },
                    placeholder = { Text("Altura en cm  (ej: 175)", color = Color(0xFF555555)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = color, unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = edad,
                    onValueChange = { edad = it },
                    placeholder = { Text("Edad en años  (opcional, para % grasa)", color = Color(0xFF555555)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = color, unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Masculino" to true, "Femenino" to false).forEach { (label, esMasc) ->
                        val sel = esMasculino == esMasc
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) color.copy(alpha = 0.2f) else Color(0xFF1A1A1A))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(onClick = { esMasculino = esMasc }) {
                                Text(label, color = if (sel) color else Color(0xFFAAAAAA), fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val p = peso.toDoubleOrNull()
                        val a = altura.toDoubleOrNull()
                        if (p != null && a != null && a > 0) {
                            val alturaM = a / 100
                            val imc = p / (alturaM * alturaM)
                            resultado = imc
                            alturaGuardada = a
                            pesoGuardado = p
                            guardarImc(imc, p)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) {
                    Text("Calcular IMC", fontWeight = FontWeight.Bold)
                }
            }
        }

        resultado?.let { imc ->
            Spacer(modifier = Modifier.height(20.dp))

            val (categoria, colorRes, emoji) = when {
                imc < 18.5 -> Triple("Bajo peso", Color(0xFF00B8D4), "😟")
                imc < 25.0 -> Triple("Normal", Color(0xFF00C853), "😊")
                imc < 30.0 -> Triple("Sobrepeso", Color(0xFFFFD600), "😐")
                imc < 35.0 -> Triple("Obesidad tipo 1", Color(0xFFFF6D00), "😟")
                imc < 40.0 -> Triple("Obesidad tipo 2", Color(0xFFDD2C00), "😰")
                else -> Triple("Obesidad tipo 3", Color(0xFFD50000), "🚨")
            }

            val alturaM = alturaGuardada / 100
            val pesoIdealMin = 18.5 * alturaM * alturaM
            val pesoIdealMax = 24.9 * alturaM * alturaM
            val diferencia = pesoGuardado - pesoIdealMax

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(colorRes.copy(alpha = 0.3f), Color(0xFF111111))))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emoji, fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("%.1f".format(imc), fontSize = 72.sp, fontWeight = FontWeight.Black, color = colorRes)
                    Text(categoria, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colorRes)
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF00B8D4), Color(0xFF00C853), Color(0xFFFFD600), Color(0xFFFF6D00), Color(0xFFD50000))))
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bajo", fontSize = 10.sp, color = Color(0xFF00B8D4))
                        Text("Normal", fontSize = 10.sp, color = Color(0xFF00C853))
                        Text("Sobre", fontSize = 10.sp, color = Color(0xFFFFD600))
                        Text("Obesidad", fontSize = 10.sp, color = Color(0xFFD50000))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).padding(12.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Peso ideal", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                            Text("%.1f — %.1f kg".format(pesoIdealMin, pesoIdealMax), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                            Spacer(modifier = Modifier.height(4.dp))
                            when {
                                diferencia > 0.5 -> Text("Te sobran %.1f kg".format(diferencia), fontSize = 13.sp, color = Color(0xFFFFD600))
                                diferencia < -0.5 -> Text("Te faltan %.1f kg".format(-diferencia), fontSize = 13.sp, color = Color(0xFF00B8D4))
                                else -> Text("¡Estás en tu peso ideal! 🎉", fontSize = 13.sp, color = Color(0xFF00C853))
                            }
                        }
                    }

                    val edadVal = edad.toIntOrNull()
                    if (edadVal != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val grasaEstimada = if (esMasculino) {
                            (1.20 * imc) + (0.23 * edadVal) - 16.2
                        } else {
                            (1.20 * imc) + (0.23 * edadVal) - 5.4
                        }
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).padding(12.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("% Grasa corporal estimado", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                                Text("%.1f%%".format(grasaEstimada.coerceIn(3.0, 50.0)), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
                            }
                        }
                    }
                }
            }
        }

        if (historial.size >= 2) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(Color(0xFF111111)).padding(20.dp)
            ) {
                Column {
                    Text("📈 Evolución del IMC", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))

                    val datos = historial.reversed()
                    val maxImc = datos.maxOf { it.second }.coerceAtLeast(30.0)
                    val minImc = datos.minOf { it.second }.coerceAtMost(18.0)
                    val rango = (maxImc - minImc).coerceAtLeast(1.0)

                    Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val paso = if (datos.size > 1) w / (datos.size - 1) else w

                            for (i in 0 until datos.size - 1) {
                                val x1 = i * paso
                                val x2 = (i + 1) * paso
                                val y1 = h * (1 - ((datos[i].second - minImc) / rango).toFloat()).coerceIn(0f, 1f)
                                val y2 = h * (1 - ((datos[i + 1].second - minImc) / rango).toFloat()).coerceIn(0f, 1f)
                                drawLine(color = color, start = androidx.compose.ui.geometry.Offset(x1, y1), end = androidx.compose.ui.geometry.Offset(x2, y2), strokeWidth = 3.dp.toPx())
                            }

                            datos.forEachIndexed { i, (_, imcH, _) ->
                                val x = i * paso
                                val y = h * (1 - ((imcH - minImc) / rango).toFloat()).coerceIn(0f, 1f)
                                drawCircle(color = color, radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val datos2 = historial.reversed()
                        if (datos2.isNotEmpty()) {
                            Text(datos2.first().first, fontSize = 10.sp, color = Color(0xFF666666))
                            Text(datos2.last().first, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (historial.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF111111)).padding(20.dp)) {
                Column {
                    Text("🗓 Historial", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    historial.take(5).forEach { (fecha, imcH, pesoH) ->
                        val colorH = when {
                            imcH < 18.5 -> Color(0xFF00B8D4)
                            imcH < 25.0 -> Color(0xFF00C853)
                            imcH < 30.0 -> Color(0xFFFFD600)
                            else -> Color(0xFFFF6D00)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(fecha, fontSize = 13.sp, color = Color(0xFFAAAAAA))
                            Text("%.1f kg".format(pesoH), fontSize = 13.sp, color = Color.White)
                            Text("IMC %.1f".format(imcH), fontSize = 13.sp, color = colorH, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color(0xFF1A1A1A), thickness = 1.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}