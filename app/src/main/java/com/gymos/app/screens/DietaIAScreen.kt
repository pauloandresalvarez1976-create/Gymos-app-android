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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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

suspend fun llamarAPIDieta(objetivo: String, actividad: String, restricciones: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.anthropic.com/v1/messages")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-api-key", ANTHROPIC_API_KEY)
            connection.setRequestProperty("anthropic-version", "2023-06-01")
            connection.doOutput = true

            val restriccionesTexto = if (restricciones.isNotEmpty()) "Restricciones: $restricciones" else "Sin restricciones"
            val prompt = "Creá un plan de alimentación diario para objetivo: $objetivo, actividad: $actividad. $restriccionesTexto. Organizalo por comidas (Desayuno, Media mañana, Almuerzo, Merienda, Cena) con cantidades y calorías. Al final el total calórico."

            val body = JSONObject().apply {
                put("model", "claude-sonnet-4-6")
                put("max_tokens", 1024)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            connection.outputStream.write(body.toString().toByteArray())
            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            json.getJSONArray("content").getJSONObject(0).getString("text")
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

@Composable
fun DietaIAScreen() {
    val color = LocalAppColor.current
    val objetivos = listOf("Pérdida de peso", "Ganancia muscular", "Definición", "Mantenimiento", "Fitness general")
    val actividades = listOf("Sedentario", "Leve (1-2 días/semana)", "Moderado (3-4 días/semana)", "Intenso (5+ días/semana)")

    var objetivoSeleccionado by remember { mutableStateOf(objetivos[0]) }
    var actividadSeleccionada by remember { mutableStateOf(actividades[0]) }
    var restricciones by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("🥗 Dieta con IA", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            "Configurá tu perfil y Claude te arma el plan perfecto",
            fontSize = 13.sp,
            color = Color(0xFFAAAAAA),
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // OBJETIVO
        SelectorSeccion(
            titulo = "Objetivo",
            opciones = objetivos,
            seleccionado = objetivoSeleccionado,
            onSeleccionar = { objetivoSeleccionado = it },
            color = color
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ACTIVIDAD
        SelectorSeccion(
            titulo = "Nivel de actividad",
            opciones = actividades,
            seleccionado = actividadSeleccionada,
            onSeleccionar = { actividadSeleccionada = it },
            color = color
        )

        Spacer(modifier = Modifier.height(16.dp))

        // RESTRICCIONES
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF111111))
                .padding(20.dp)
        ) {
            Column {
                Text("Restricciones o preferencias", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Text("Ej: vegetariano, sin gluten, sin lactosa", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = restricciones,
                    onValueChange = { restricciones = it },
                    placeholder = { Text("Opcional", color = Color(0xFF666666)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = color,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                cargando = true
                resultado = ""
                scope.launch {
                    resultado = llamarAPIDieta(objetivoSeleccionado, actividadSeleccionada, restricciones)
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
                Text("Generando dieta...")
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generar dieta con IA", fontWeight = FontWeight.Bold)
            }
        }

        if (resultado.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF111111))
                    .padding(20.dp)
            ) {
                Column {
                    Text("Tu plan alimentario", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(limpiarMarkdown(resultado), fontSize = 14.sp, color = Color.White, lineHeight = 22.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}