package com.gymos.app.screens

import android.content.Context
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymos.app.CronometroService
import kotlinx.coroutines.delay

@Composable
fun CronometroScreen() {
    val context = LocalContext.current
    val color = LocalAppColor.current
    val prefs = remember { context.getSharedPreferences("gymos_cronometro", Context.MODE_PRIVATE) }

    val activity = context as? ComponentActivity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var modoSeleccionado by remember { mutableStateOf(0) }
    var corriendo by remember { mutableStateOf(false) }
    var milisegundos by remember { mutableLongStateOf(0L) }
    var tiempoConfig by remember { mutableStateOf("60") }
    var tiempoRestante by remember { mutableLongStateOf(0L) }
    var tempCorriendo by remember { mutableStateOf(false) }
    var tiempoTrabajo by remember { mutableStateOf("40") }
    var tiempoDescanso by remember { mutableStateOf("20") }
    var seriesConfig by remember { mutableStateOf("3") }
    var serieActual by remember { mutableIntStateOf(1) }
    var seriesCompletadas by remember { mutableIntStateOf(0) }
    var enTrabajo by remember { mutableStateOf(true) }
    var intervalCorriendo by remember { mutableStateOf(false) }
    var tiempoInterval by remember { mutableLongStateOf(0L) }

    val presets = remember {
        listOf(
            Triple("HIIT", 40, 20),
            Triple("Series", 60, 30),
            Triple("Tabata", 20, 10),
            Triple("Cardio", 180, 60),
        )
    }

    fun vibrar() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    fun beep() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 80)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
        } catch (e: Exception) { }
    }

    fun beepFin() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 800)
        } catch (e: Exception) { }
    }

    LaunchedEffect(corriendo) {
        while (corriendo) {
            delay(10L)
            milisegundos += 10
        }
    }

    LaunchedEffect(tempCorriendo) {
        while (tempCorriendo && tiempoRestante > 0) {
            delay(100L)
            tiempoRestante -= 100
            if (tiempoRestante in 4900..5000) beep()
            if (tiempoRestante <= 0) {
                beepFin()
                vibrar()
                tempCorriendo = false
            }
        }
    }

    LaunchedEffect(intervalCorriendo) {
        while (intervalCorriendo) {
            delay(100L)
            tiempoInterval -= 100
            if (tiempoInterval in 4900..5000) beep()
            if (tiempoInterval <= 0) {
                beepFin()
                vibrar()
                if (enTrabajo) {
                    enTrabajo = false
                    tiempoInterval = (tiempoDescanso.toLongOrNull() ?: 20L) * 1000L
                } else {
                    seriesCompletadas++
                    val totalSeries = seriesConfig.toIntOrNull() ?: 3
                    if (seriesCompletadas >= totalSeries) {
                        intervalCorriendo = false
                        seriesCompletadas = 0
                        serieActual = 1
                        enTrabajo = true
                    } else {
                        serieActual++
                        enTrabajo = true
                        tiempoInterval = (tiempoTrabajo.toLongOrNull() ?: 40L) * 1000L
                    }
                }
            }
        }
    }

    val minCrono = (milisegundos / 60000).toInt()
    val segCrono = ((milisegundos % 60000) / 1000).toInt()
    val centCrono = ((milisegundos % 1000) / 10).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("⏱️ Cronómetro", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF111111)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Cronómetro", "Temporizador", "Intervalos").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (modoSeleccionado == index) color else Color.Transparent)
                        .clickable { modoSeleccionado = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (modoSeleccionado == index) Color.White else Color(0xFF666666))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (modoSeleccionado) {
            0 -> {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF111111)).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%02d:%02d.%02d".format(minCrono, segCrono, centCrono),
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Black,
                            color = if (corriendo) color else Color.White
                        )
                        if (corriendo) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF00FF88)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("En curso", fontSize = 12.sp, color = Color(0xFF00FF88))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            corriendo = !corriendo
                            val intent = android.content.Intent(context, CronometroService::class.java)
                            intent.action = if (corriendo) "START" else "PAUSE"
                            context.startService(intent)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (corriendo) Color(0xFF333333) else color)
                    ) {
                        Icon(if (corriendo) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (corriendo) "Pausar" else "Iniciar", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            corriendo = false
                            milisegundos = 0L
                            context.stopService(android.content.Intent(context, CronometroService::class.java))
                        },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, color)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = color)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reiniciar", color = color)
                    }
                }
            }

            1 -> {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF111111)).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val minTemp = (tiempoRestante / 60000).toInt()
                        val segTemp = ((tiempoRestante % 60000) / 1000).toInt()
                        val colorTemp = when {
                            tiempoRestante <= 5000 -> Color(0xFFD50000)
                            tiempoRestante <= 10000 -> Color(0xFFFFD600)
                            else -> if (tempCorriendo) color else Color.White
                        }
                        Text(text = "%02d:%02d".format(minTemp, segTemp), fontSize = 72.sp, fontWeight = FontWeight.Black, color = colorTemp)
                        if (tiempoRestante > 0) {
                            val total = (tiempoConfig.toLongOrNull() ?: 60L) * 1000L
                            val progreso = (tiempoRestante.toFloat() / total).coerceIn(0f, 1f)
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = colorTemp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!tempCorriendo && tiempoRestante == 0L) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF111111)).padding(20.dp)) {
                        Column {
                            Text("Duración (segundos)", fontSize = 14.sp, color = Color(0xFFAAAAAA))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = tiempoConfig,
                                onValueChange = { tiempoConfig = it },
                                placeholder = { Text("Ej: 60", color = Color(0xFF555555)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = color, unfocusedBorderColor = Color(0xFF333333), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (!tempCorriendo && tiempoRestante == 0L) {
                                tiempoRestante = (tiempoConfig.toLongOrNull() ?: 60L) * 1000L
                            }
                            tempCorriendo = !tempCorriendo
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (tempCorriendo) Color(0xFF333333) else color)
                    ) {
                        Icon(if (tempCorriendo) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (tempCorriendo) "Pausar" else "Iniciar", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { tempCorriendo = false; tiempoRestante = 0L },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, color)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = color)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reiniciar", color = color)
                    }
                }
            }

            2 -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    presets.forEach { (nombre, trabajo, descanso) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(color.copy(alpha = 0.15f))
                                .clickable { tiempoTrabajo = trabajo.toString(); tiempoDescanso = descanso.toString() }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(nombre, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                                Text("${trabajo}/${descanso}s", fontSize = 10.sp, color = Color(0xFF666666))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(if (enTrabajo) listOf(color.copy(alpha = 0.3f), Color(0xFF111111)) else listOf(Color(0xFF1565C0).copy(alpha = 0.3f), Color(0xFF111111))))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (enTrabajo) "💪 TRABAJO" else "😮‍💨 DESCANSO", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (enTrabajo) color else Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.height(8.dp))
                        val minI = (tiempoInterval / 60000).toInt()
                        val segI = ((tiempoInterval % 60000) / 1000).toInt()
                        Text("%02d:%02d".format(minI, segI), fontSize = 72.sp, fontWeight = FontWeight.Black, color = if (enTrabajo) color else Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Serie $serieActual de ${seriesConfig.toIntOrNull() ?: 3}", fontSize = 14.sp, color = Color(0xFFAAAAAA))
                        if (intervalCorriendo) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val totalInterval = if (enTrabajo) (tiempoTrabajo.toLongOrNull() ?: 40L) * 1000L else (tiempoDescanso.toLongOrNull() ?: 20L) * 1000L
                            val progreso = (tiempoInterval.toFloat() / totalInterval).coerceIn(0f, 1f)
                            LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = if (enTrabajo) color else Color(0xFF3B82F6))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!intervalCorriendo) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF111111)).padding(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Trabajo (seg)", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                                    OutlinedTextField(value = tiempoTrabajo, onValueChange = { tiempoTrabajo = it }, placeholder = { Text("40", color = Color(0xFF555555)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = color, unfocusedBorderColor = Color(0xFF333333), focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Descanso (seg)", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                                    OutlinedTextField(value = tiempoDescanso, onValueChange = { tiempoDescanso = it }, placeholder = { Text("20", color = Color(0xFF555555)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = color, unfocusedBorderColor = Color(0xFF333333), focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                }
                            }
                            Column {
                                Text("Series", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                                OutlinedTextField(value = seriesConfig, onValueChange = { seriesConfig = it }, placeholder = { Text("3", color = Color(0xFF555555)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = color, unfocusedBorderColor = Color(0xFF333333), focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (!intervalCorriendo && tiempoInterval == 0L) {
                                tiempoInterval = (tiempoTrabajo.toLongOrNull() ?: 40L) * 1000L
                                enTrabajo = true
                                serieActual = 1
                                seriesCompletadas = 0
                            }
                            intervalCorriendo = !intervalCorriendo
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (intervalCorriendo) Color(0xFF333333) else color)
                    ) {
                        Icon(if (intervalCorriendo) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (intervalCorriendo) "Pausar" else "Iniciar", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { intervalCorriendo = false; tiempoInterval = 0L; serieActual = 1; seriesCompletadas = 0; enTrabajo = true },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, color)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = color)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reiniciar", color = color)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}