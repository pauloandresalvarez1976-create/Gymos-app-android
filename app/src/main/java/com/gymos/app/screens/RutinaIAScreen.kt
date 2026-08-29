package com.gymos.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

suspend fun generarRutina(objetivo: String, nivel: String, dias: Int): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_BASE_URL/api/rutina")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val body = JSONObject().apply {
                put("objetivo", objetivo)
                put("nivel", nivel)
                put("dias", dias)
            }

            connection.outputStream.write(body.toString().toByteArray())
            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            json.getString("resultado")
        } catch (e: Exception) {
            "Error al generar la rutina. Verificá tu conexión e intentá de nuevo."
        }
    }
}

@Composable
fun RutinaIAScreen() {
    val color = LocalAppColor.current
    val objetivos = listOf("Musculación", "Pérdida de peso", "Definición", "Fitness general")
    val niveles = listOf("Principiante", "Intermedio", "Avanzado")
    val diasOpciones = listOf(2, 3, 4, 5)

    var objetivoSeleccionado by remember { mutableStateOf(objetivos[0]) }
    var nivelSeleccionado by remember { mutableStateOf(niveles[0]) }
    var diasSeleccionados by remember { mutableIntStateOf(3) }
    var rutina by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("🤖 Rutina con IA", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            "Configurá tu perfil y Claude te arma la rutina perfecta",
            fontSize = 13.sp,
            color = Color(0xFFAAAAAA),
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        SelectorSeccion(
            titulo = "Objetivo",
            opciones = objetivos,
            seleccionado = objetivoSeleccionado,
            onSeleccionar = { objetivoSeleccionado = it },
            color = color
        )

        Spacer(modifier = Modifier.height(16.dp))

        SelectorSeccion(
            titulo = "Nivel",
            opciones = niveles,
            seleccionado = nivelSeleccionado,
            onSeleccionar = { nivelSeleccionado = it },
            color = color
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF111111))
                .padding(20.dp)
        ) {
            Column {
                Text("Días por semana", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    diasOpciones.forEach { d ->
                        val seleccionado = d == diasSeleccionados
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (seleccionado) color else Color(0xFF222222))
                                .then(Modifier.wrapContentSize()),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(onClick = { diasSeleccionados = d }) {
                                Text(
                                    "$d",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (seleccionado) Color.White else Color(0xFFAAAAAA)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                cargando = true
                rutina = ""
                scope.launch {
                    rutina = generarRutina(objetivoSeleccionado, nivelSeleccionado, diasSeleccionados)
                    cargando = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = color),
            enabled = !cargando
        ) {
            if (cargando) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generando rutina...")
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generar rutina con IA", fontWeight = FontWeight.Bold)
            }
        }

        if (rutina.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF111111))
                    .padding(20.dp)
            ) {
                Column {
                    Text("Tu rutina personalizada", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(limpiarMarkdown(rutina), fontSize = 14.sp, color = Color.White, lineHeight = 22.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}