package com.gymos.app.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Mensaje(val texto: String, val esUsuario: Boolean)

suspend fun enviarMensaje(historial: List<Mensaje>, nuevoMensaje: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.anthropic.com/v1/messages")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-api-key", ANTHROPIC_API_KEY)
            connection.setRequestProperty("anthropic-version", "2023-06-01")
            connection.doOutput = true

            val mensajes = JSONArray()
            historial.forEach { msg ->
                mensajes.put(JSONObject().apply {
                    put("role", if (msg.esUsuario) "user" else "assistant")
                    put("content", msg.texto)
                })
            }
            mensajes.put(JSONObject().apply {
                put("role", "user")
                put("content", nuevoMensaje)
            })

            val body = JSONObject().apply {
                put("model", "claude-sonnet-4-6")
                put("max_tokens", 1024)
                put("system", "Sos un asistente experto en fitness, nutrición y entrenamiento. Respondé siempre en español, de forma clara y motivadora. Ayudá al usuario con sus dudas sobre ejercicios, dietas, rutinas y salud en general.")
                put("messages", mensajes)
            }

            connection.outputStream.write(body.toString().toByteArray())
            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            json.getJSONArray("content").getJSONObject(0).getString("text")
        } catch (e: Exception) {
            "Error al conectar con el asistente. Verificá tu conexión."
        }
    }
}

@Composable
fun AsistenteIAScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gymos_asistente", Context.MODE_PRIVATE) }
    val color = LocalAppColor.current

    var input by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val historial = remember {
        val json = prefs.getString("historial", "[]") ?: "[]"
        try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                Mensaje(obj.getString("texto"), obj.getBoolean("esUsuario"))
            }.toMutableList()
        } catch (e: Exception) { mutableListOf() }
    }

    var historialState by remember { mutableStateOf(historial.toList()) }

    fun guardarHistorial() {
        val arr = JSONArray()
        historialState.takeLast(50).forEach { msg ->
            val obj = JSONObject()
            obj.put("texto", msg.texto)
            obj.put("esUsuario", msg.esUsuario)
            arr.put(obj)
        }
        prefs.edit().putString("historial", arr.toString()).apply()
    }

    LaunchedEffect(historialState.size) {
        if (historialState.isNotEmpty()) {
            listState.animateScrollToItem(historialState.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("🤖 Asistente IA", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Tu entrenador personal con IA", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                }
                if (historialState.isNotEmpty()) {
                    IconButton(onClick = {
                        historialState = emptyList()
                        prefs.edit().remove("historial").apply()
                    }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Borrar historial", tint = Color(0xFF666666))
                    }
                }
            }
        }

        // MENSAJES
        if (historialState.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏋️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("¿En qué te puedo ayudar?", fontSize = 16.sp, color = Color(0xFFAAAAAA))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Preguntame sobre ejercicios,", fontSize = 13.sp, color = Color(0xFF666666))
                    Text("rutinas, nutrición o dietas.", fontSize = 13.sp, color = Color(0xFF666666))
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(historialState) { mensaje ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mensaje.esUsuario) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = if (mensaje.esUsuario) 18.dp else 4.dp,
                                        bottomEnd = if (mensaje.esUsuario) 4.dp else 18.dp
                                    )
                                )
                                .background(if (mensaje.esUsuario) color else Color(0xFF1A1A1A))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = limpiarMarkdown(mensaje.texto),
                                fontSize = 14.sp,
                                color = Color.White,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                if (cargando) {
                    item {
                        Row {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF1A1A1A))
                                    .padding(12.dp)
                            ) {
                                CircularProgressIndicator(color = color, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }

        // INPUT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Escribí tu pregunta...", color = Color(0xFF666666)) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = color,
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.isNotBlank() && !cargando) {
                        val pregunta = input.trim()
                        input = ""
                        historialState = historialState + Mensaje(pregunta, true)
                        cargando = true
                        scope.launch {
                            val respuesta = enviarMensaje(historialState.dropLast(1), pregunta)
                            historialState = historialState + Mensaje(respuesta, false)
                            cargando = false
                            guardarHistorial()
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (input.isNotBlank()) color else Color(0xFF333333))
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.White)
            }
        }
    }
}