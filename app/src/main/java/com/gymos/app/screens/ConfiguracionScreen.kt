package com.gymos.app.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.gymos.app.cancelarRecordatorio
import com.gymos.app.programarRecordatorio

val coloresDisponibles = listOf(
    Pair("Naranja", Color(0xFFFF4500)),
    Pair("Azul", Color(0xFF2979FF)),
    Pair("Púrpura", Color(0xFFAA00FF)),
    Pair("Verde", Color(0xFF00C853)),
    Pair("Rojo", Color(0xFFD50000)),
    Pair("Cyan", Color(0xFF00B8D4)),
    Pair("Rosa", Color(0xFFF50057)),
)

@Composable
fun ConfiguracionScreen(onColorCambiado: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gymos_perfil", Context.MODE_PRIVATE) }
    val prefsConfig = remember { context.getSharedPreferences("gymos_config", Context.MODE_PRIVATE) }

    var nombre by remember { mutableStateOf(prefs.getString("nombre", "") ?: "") }
    var editandoNombre by remember { mutableStateOf(false) }
    var inputNombre by remember { mutableStateOf(nombre) }
    var metaPasos by remember { mutableStateOf(prefsConfig.getInt("meta_pasos", 10000).toString()) }
    var metaVasos by remember { mutableStateOf(prefsConfig.getInt("meta_vasos", 8).toString()) }
    var recordatorio by remember { mutableStateOf(prefsConfig.getBoolean("recordatorio_agua", false)) }
    var horasRecordatorio by remember { mutableIntStateOf(prefsConfig.getInt("recordatorio_horas", 2)) }
    var colorSeleccionado by remember { mutableStateOf(prefsConfig.getInt("color_index", 0)) }
    var mostrarDialogoBorrar by remember { mutableStateOf(false) }

    if (mostrarDialogoBorrar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoBorrar = false },
            title = { Text("¿Borrar todos los datos?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Esta acción no se puede deshacer.", color = Color(0xFFAAAAAA)) },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().clear().apply()
                    prefsConfig.edit().clear().apply()
                    cancelarRecordatorio(context)
                    mostrarDialogoBorrar = false
                }) {
                    Text("Borrar", color = Color(0xFFD50000), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoBorrar = false }) {
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(20.dp)
        ) {
            Text("⚙️ Configuración", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // PERFIL
            SeccionConfig(titulo = "👤 Perfil") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nombre", fontSize = 14.sp, color = Color(0xFFAAAAAA))
                        if (editandoNombre) {
                            OutlinedTextField(
                                value = inputNombre,
                                onValueChange = { inputNombre = it },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = coloresDisponibles[colorSeleccionado].second,
                                    unfocusedBorderColor = Color(0xFF333333),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (inputNombre.isNotBlank()) {
                                        nombre = inputNombre.trim()
                                        prefs.edit().putString("nombre", nombre).apply()
                                        editandoNombre = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = coloresDisponibles[colorSeleccionado].second)
                            ) {
                                Text("Guardar")
                            }
                        } else {
                            Text(nombre.ifEmpty { "Sin nombre" }, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!editandoNombre) {
                        IconButton(onClick = { editandoNombre = true; inputNombre = nombre }) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = coloresDisponibles[colorSeleccionado].second)
                        }
                    }
                }
            }

            // COLOR PRINCIPAL
            SeccionConfig(titulo = "🎨 Color principal") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    coloresDisponibles.forEachIndexed { index, (_, color) ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(if (colorSeleccionado == index) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                                .clickable {
                                    colorSeleccionado = index
                                    prefsConfig.edit().putInt("color_index", index).apply()
                                    onColorCambiado()
                                }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Color actual: ${coloresDisponibles[colorSeleccionado].first}",
                    fontSize = 13.sp,
                    color = coloresDisponibles[colorSeleccionado].second,
                    fontWeight = FontWeight.Bold
                )
            }

            // PODÓMETRO
            SeccionConfig(titulo = "🚶 Podómetro") {
                Text("Meta diaria de pasos", fontSize = 14.sp, color = Color(0xFFAAAAAA))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = metaPasos,
                    onValueChange = {
                        metaPasos = it
                        it.toIntOrNull()?.let { v -> prefsConfig.edit().putInt("meta_pasos", v).apply() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = coloresDisponibles[colorSeleccionado].second,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // HIDRATACIÓN
            SeccionConfig(titulo = "💧 Hidratación") {
                Text("Meta diaria de vasos", fontSize = 14.sp, color = Color(0xFFAAAAAA))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = metaVasos,
                    onValueChange = {
                        metaVasos = it
                        it.toIntOrNull()?.let { v -> prefsConfig.edit().putInt("meta_vasos", v).apply() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = coloresDisponibles[colorSeleccionado].second,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recordatorio de hidratación", fontSize = 14.sp, color = Color.White)
                    Switch(
                        checked = recordatorio,
                        onCheckedChange = { activo ->
                            recordatorio = activo
                            prefsConfig.edit().putBoolean("recordatorio_agua", activo).apply()
                            if (activo) {
                                programarRecordatorio(context, horasRecordatorio)
                            } else {
                                cancelarRecordatorio(context)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = coloresDisponibles[colorSeleccionado].second)
                    )
                }

                if (recordatorio) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Recordar cada:", fontSize = 14.sp, color = Color(0xFFAAAAAA))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 4).forEach { horas ->
                            val sel = horasRecordatorio == horas
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) coloresDisponibles[colorSeleccionado].second else Color(0xFF1A1A1A))
                                    .clickable {
                                        horasRecordatorio = horas
                                        prefsConfig.edit().putInt("recordatorio_horas", horas).apply()
                                        if (recordatorio) programarRecordatorio(context, horas)
                                    }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${horas}h",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) Color.White else Color(0xFFAAAAAA)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "✅ Recordatorio activo cada ${horasRecordatorio}h",
                        fontSize = 12.sp,
                        color = Color(0xFF00FF88)
                    )
                }
            }

            // APP
            SeccionConfig(titulo = "📱 App") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Versión", fontSize = 14.sp, color = Color(0xFFAAAAAA))
                    Text("1.0.0", fontSize = 14.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { mostrarDialogoBorrar = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A0000))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD50000))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Borrar todos los datos", color = Color(0xFFD50000))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SeccionConfig(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
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
            contenido()
        }
    }
}