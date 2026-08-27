package com.gymos.app.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymos.app.PodometroService
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

@Composable
fun PodometroScreen() {
    val context = LocalContext.current
    val prefsConfig = remember { context.getSharedPreferences("gymos_config", Context.MODE_PRIVATE) }
    val prefsPodo = remember { context.getSharedPreferences("gymos_podometro", Context.MODE_PRIVATE) }
    val color = LocalAppColor.current
    val activity = context as? androidx.activity.ComponentActivity

    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val meta = remember { prefsConfig.getInt("meta_pasos", 10000) }
    var pasos by remember { mutableIntStateOf(0) }
    var disponible by remember { mutableStateOf(true) }
    var calibrado by remember { mutableStateOf(prefsPodo.getFloat("umbral", 0f) > 0f) }
    var calibrando by remember { mutableStateOf(false) }
    var mostrarCalib by remember { mutableStateOf(!calibrado) }
    var tiempoActivo by remember { mutableLongStateOf(0L) }
    var corriendo by remember { mutableStateOf(false) }
    var umbral by remember { mutableFloatStateOf(prefsPodo.getFloat("umbral", 12f)) }
    var zancada by remember { mutableFloatStateOf(prefsPodo.getFloat("zancada", 0.76f)) }
    var mostrarResumen by remember { mutableStateOf(false) }
    var pasosFinales by remember { mutableIntStateOf(0) }
    var tiempoFinal by remember { mutableLongStateOf(0L) }

    val muestrasCalib = remember { mutableListOf<Float>() }
    var pasosCalib by remember { mutableIntStateOf(0) }

    val hoy = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val historialSemanal = remember {
        val json = prefsPodo.getString("historial", "{}") ?: "{}"
        try { JSONObject(json) } catch (e: Exception) { JSONObject() }
    }

    fun guardarPasosHoy() {
        historialSemanal.put(hoy, pasos)
        prefsPodo.edit().putString("historial", historialSemanal.toString()).apply()
    }

    LaunchedEffect(corriendo) {
        while (corriendo) {
            delay(1000L)
            tiempoActivo += 1000
        }
    }

    var ultimaMagnitud = remember { 0f }
    var enPico = remember { false }
    var ultimoPaso = remember { 0L }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            disponible = false
            return@DisposableEffect onDispose {}
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!corriendo && !calibrando) return
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                val magnitud = sqrt(x * x + y * y + z * z)
                if (calibrando) muestrasCalib.add(magnitud)
                val ahora = System.currentTimeMillis()
                if (magnitud > umbral && ultimaMagnitud <= umbral && !enPico) {
                    if (ahora - ultimoPaso > 250L) {
                        enPico = true
                        if (corriendo) pasos++
                        if (calibrando) pasosCalib++
                        ultimoPaso = ahora
                    }
                } else if (magnitud < umbral - 2f) {
                    enPico = false
                }
                ultimaMagnitud = magnitud
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val km = pasos * zancada / 1000.0
    val cal = pasos * 0.04
    val velocidad = if (tiempoActivo > 0) (km / (tiempoActivo / 3600000.0)) else 0.0
    val porcentaje = (pasos.toFloat() / meta).coerceIn(0f, 1f)
    val metaCumplida = pasos >= meta
    val colorPasos = if (metaCumplida) Color(0xFF00FF88) else color
    val minutos = (tiempoActivo / 60000).toInt()
    val segundos = ((tiempoActivo % 60000) / 1000).toInt()

    val diasSemana = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfl = SimpleDateFormat("EEE", Locale("es"))
        (6 downTo 0).map { dias ->
            val cal2 = Calendar.getInstance()
            cal2.add(Calendar.DAY_OF_YEAR, -dias)
            val fecha = sdf.format(cal2.time)
            val label = sdfl.format(cal2.time).replaceFirstChar { it.uppercase() }
            Pair(fecha, label)
        }
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
        Text("🚶 Podómetro", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Meta: $meta pasos por día", fontSize = 13.sp, color = Color(0xFFAAAAAA), modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(24.dp))

        if (!disponible) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF111111)).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😕", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Tu dispositivo no tiene acelerómetro", color = Color(0xFFAAAAAA), fontSize = 14.sp)
                }
            }
            return@Column
        }

        if (mostrarCalib) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(color.copy(alpha = 0.15f)).padding(20.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📏 Calibración", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!calibrando) {
                        Text("Caminá 20 pasos normales para calibrar el podómetro a tu manera de caminar.", fontSize = 13.sp, color = Color(0xFFAAAAAA), textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 16.dp))
                        Button(onClick = { calibrando = true; pasosCalib = 0; muestrasCalib.clear() }, colors = ButtonDefaults.buttonColors(containerColor = color), modifier = Modifier.fillMaxWidth()) {
                            Text("Iniciar calibración", fontWeight = FontWeight.Bold)
                        }
                        if (calibrado) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { mostrarCalib = false }) { Text("Cancelar", color = Color(0xFF666666)) }
                        }
                    } else {
                        Text("Caminá 20 pasos normales...", fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                        Text("$pasosCalib / 20", fontSize = 48.sp, fontWeight = FontWeight.Black, color = color)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { (pasosCalib / 20f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = color)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (pasosCalib >= 20) {
                            if (muestrasCalib.isNotEmpty()) {
                                val maxM = muestrasCalib.max()
                                val minM = muestrasCalib.min()
                                val nuevoUmbral = (minM + (maxM - minM) * 0.6f).coerceIn(8f, 20f)
                                umbral = nuevoUmbral
                                prefsPodo.edit().putFloat("umbral", nuevoUmbral).apply()
                            }
                            calibrado = true
                            calibrando = false
                            mostrarCalib = false
                            pasos = 0
                        }
                        TextButton(onClick = { calibrando = false; pasosCalib = 0; muestrasCalib.clear() }) { Text("Cancelar", color = Color(0xFF666666)) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (!mostrarCalib) {
            if (mostrarResumen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.6f))))
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("🏁 Sesión completada", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("$pasosFinales", fontSize = 72.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("pasos", fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ResumenStat(Modifier.weight(1f), "%.2f".format(pasosFinales * zancada / 1000.0), "km")
                            ResumenStat(Modifier.weight(1f), "%.0f".format(pasosFinales * 0.04), "kcal")
                            val mf = (tiempoFinal / 60000).toInt()
                            val sf = ((tiempoFinal % 60000) / 1000).toInt()
                            ResumenStat(Modifier.weight(1f), "%02d:%02d".format(mf, sf), "min")
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { mostrarResumen = false; pasos = 0; tiempoActivo = 0L },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Nueva sesión", color = color, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (!mostrarResumen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF111111))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$pasos", fontSize = 80.sp, fontWeight = FontWeight.Black, color = colorPasos)
                        Text(text = if (metaCumplida) "¡Meta cumplida! 🎉" else "pasos", fontSize = 15.sp, color = if (metaCumplida) Color(0xFF00FF88) else Color(0xFFAAAAAA))
                        if (corriendo) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF00FF88)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("En curso", fontSize = 12.sp, color = Color(0xFF00FF88))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF222222))) {
                    Box(modifier = Modifier.fillMaxWidth(porcentaje).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.6f)))))
                }
                Text("${(porcentaje * 100).toInt()}% de la meta", fontSize = 12.sp, color = Color(0xFF666666), modifier = Modifier.padding(top = 4.dp))

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricaCard(Modifier.weight(1f), "%.2f".format(km), "km", "Distancia", color)
                    MetricaCard(Modifier.weight(1f), "%.0f".format(cal), "kcal", "Calorías", color)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricaCard(Modifier.weight(1f), "%02d:%02d".format(minutos, segundos), "min", "Tiempo", color)
                    MetricaCard(Modifier.weight(1f), "%.1f".format(velocidad), "km/h", "Velocidad", color)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            corriendo = !corriendo
                            if (corriendo) {
                                context.startService(android.content.Intent(context, PodometroService::class.java))
                            } else {
                                context.stopService(android.content.Intent(context, PodometroService::class.java))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (corriendo) Color(0xFF1A1A1A) else color)
                    ) {
                        Icon(if (corriendo) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (corriendo) "Pausar" else "Iniciar", fontWeight = FontWeight.Bold)
                    }
                    if (corriendo || pasos > 0) {
                        Button(
                            onClick = {
                                corriendo = false
                                context.stopService(android.content.Intent(context, PodometroService::class.java))
                                pasosFinales = pasos
                                tiempoFinal = tiempoActivo
                                guardarPasosHoy()
                                mostrarResumen = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000))
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Parar", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { mostrarCalib = true; calibrando = false; pasosCalib = 0; muestrasCalib.clear() },
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF666666))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recalibrar", color = Color(0xFF666666))
                }
            }

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
                    Text("📊 Esta semana", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))

                    val maxPasos = diasSemana.maxOfOrNull { (fecha, _) ->
                        if (fecha == hoy) pasos else historialSemanal.optInt(fecha, 0)
                    }?.coerceAtLeast(1) ?: 1

                    Row(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        diasSemana.forEach { (fecha, label) ->
                            val pasosDelDia = if (fecha == hoy) pasos else historialSemanal.optInt(fecha, 0)
                            val alturaRel = (pasosDelDia.toFloat() / maxPasos).coerceIn(0.05f, 1f)
                            val esHoy = fecha == hoy
                            val colorBarra = if (esHoy) color else color.copy(alpha = 0.35f)
                            val metaCumplida2 = pasosDelDia >= meta

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                if (metaCumplida2 && pasosDelDia > 0) Text("✓", fontSize = 10.sp, color = Color(0xFF00FF88))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(alturaRel)
                                        .shadow(if (esHoy) 6.dp else 0.dp, RoundedCornerShape(6.dp))
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (esHoy) Brush.verticalGradient(listOf(color, color.copy(alpha = 0.5f)))
                                            else Brush.verticalGradient(listOf(colorBarra, colorBarra.copy(alpha = 0.3f)))
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color.copy(alpha = 0.3f)))
                    Text("Meta: $meta pasos", fontSize = 10.sp, color = color.copy(alpha = 0.5f), modifier = Modifier.padding(top = 2.dp))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        diasSemana.forEach { (fecha, label) ->
                            val esHoy = fecha == hoy
                            Text(label, modifier = Modifier.weight(1f), fontSize = 10.sp, color = if (esHoy) color else Color(0xFF666666), fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ResumenStat(modifier: Modifier, valor: String, label: String) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun MetricaCard(
    modifier: Modifier = Modifier,
    valor: String,
    unidad: String,
    label: String,
    color: Color
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF111111)).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(valor, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unidad, fontSize = 12.sp, color = color, modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(label, fontSize = 12.sp, color = Color(0xFFAAAAAA))
        }
    }
}