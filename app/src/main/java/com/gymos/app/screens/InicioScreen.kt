package com.gymos.app.screens

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun InicioScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gymos_perfil", Context.MODE_PRIVATE) }
    val color = LocalAppColor.current

    var nombre by remember { mutableStateOf(prefs.getString("nombre", "") ?: "") }
    var mostrarDialogo by remember { mutableStateOf(nombre.isEmpty()) }
    var inputNombre by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf<Uri?>(prefs.getString("foto_uri", null)?.let { Uri.parse(it) }) }
    var mostrarMenuFoto by remember { mutableStateOf(false) }
    var fotoTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val launcherGaleria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            fotoUri = uri
            fotoTimestamp = System.currentTimeMillis()
            prefs.edit().putString("foto_uri", uri.toString()).apply()
        }
    }

    val uriCamara = remember {
        val file = java.io.File(context.cacheDir, "foto_perfil_temp.jpg")
        androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val launcherCamara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito) {
            fotoUri = uriCamara
            fotoTimestamp = System.currentTimeMillis()
            prefs.edit().putString("foto_uri", uriCamara.toString()).apply()
        }
    }

    val launcherPermisoGaleria = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { otorgado ->
        if (otorgado) launcherGaleria.launch("image/*")
    }

    val launcherPermisoCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { otorgado ->
        if (otorgado) launcherCamara.launch(uriCamara)
    }

    if (mostrarMenuFoto) {
        AlertDialog(
            onDismissRequest = { mostrarMenuFoto = false },
            title = { Text("Foto de perfil", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            mostrarMenuFoto = false
                            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                launcherCamara.launch(uriCamara)
                            } else {
                                launcherPermisoCamera.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sacar foto")
                    }
                    OutlinedButton(
                        onClick = {
                            mostrarMenuFoto = false
                            val permiso = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                android.Manifest.permission.READ_MEDIA_IMAGES
                            } else {
                                android.Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            if (ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED) {
                                launcherGaleria.launch("image/*")
                            } else {
                                launcherPermisoGaleria.launch(permiso)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, color)
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = null, tint = color)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Elegir de galería", color = color)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mostrarMenuFoto = false }) {
                    Text("Cancelar", color = Color(0xFF666666))
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("¡Bienvenido a Gymos!", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("¿Cómo te llamás?", color = Color(0xFFAAAAAA), modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(
                        value = inputNombre,
                        onValueChange = { inputNombre = it },
                        placeholder = { Text("Tu nombre", color = Color(0xFF666666)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = color,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputNombre.isNotBlank()) {
                            nombre = inputNombre.trim()
                            prefs.edit().putString("nombre", nombre).apply()
                            mostrarDialogo = false
                        }
                    }
                ) {
                    Text("Listo", color = color, fontWeight = FontWeight.Bold)
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
                .background(Brush.linearGradient(colors = listOf(color, color.copy(alpha = 0.7f))))
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "GYMOS",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .border(3.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .clickable { mostrarMenuFoto = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (fotoUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("$fotoUri?t=$fotoTimestamp")
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (nombre.isNotEmpty()) "¡Hola, $nombre!" else "Bienvenido",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¿Qué hacemos hoy?",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaPremium(
                    modifier = Modifier.weight(1f),
                    icono = Icons.Default.DirectionsWalk,
                    titulo = "Podómetro",
                    subtitulo = "Tus pasos",
                    gradiente = listOf(Color(0xFF00C853), Color(0xFF1B5E20)),
                    emoji = "🚶",
                    onClick = { navController.navigate("podometro") }
                )
                TarjetaPremium(
                    modifier = Modifier.weight(1f),
                    icono = Icons.Default.MonitorWeight,
                    titulo = "IMC",
                    subtitulo = "Tu índice",
                    gradiente = listOf(Color(0xFF1565C0), Color(0xFF0D47A1)),
                    emoji = "⚖️",
                    onClick = { navController.navigate("imc") }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaPremium(
                    modifier = Modifier.weight(1f),
                    icono = Icons.Default.Timer,
                    titulo = "Cronómetro",
                    subtitulo = "Tus series",
                    gradiente = listOf(Color(0xFF6A1B9A), Color(0xFF4A148C)),
                    emoji = "⏱️",
                    onClick = { navController.navigate("cronometro") }
                )
                TarjetaPremium(
                    modifier = Modifier.weight(1f),
                    icono = Icons.Default.WaterDrop,
                    titulo = "Hidratación",
                    subtitulo = "Tu agua",
                    gradiente = listOf(Color(0xFF0288D1), Color(0xFF01579B)),
                    emoji = "💧",
                    onClick = { navController.navigate("hidratacion") }
                )
            }
            TarjetaPremiumGrande(
                icono = Icons.Default.AutoAwesome,
                titulo = "Rutina con IA",
                subtitulo = "Tu rutina personalizada con IA",
                gradiente = listOf(color, color.copy(alpha = 0.7f)),
                emoji = "🤖",
                onClick = { navController.navigate("rutina_ia") }
            )
            TarjetaPremiumGrande(
                icono = Icons.Default.Restaurant,
                titulo = "Dieta con IA",
                subtitulo = "Tu plan alimentario con IA",
                gradiente = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)),
                emoji = "🥗",
                onClick = { navController.navigate("dieta_ia") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun TarjetaPremium(
    modifier: Modifier = Modifier,
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    gradiente: List<Color>,
    emoji: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(140.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors = gradiente))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(emoji, fontSize = 28.sp)
            Column {
                Text(titulo, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White)
                Text(subtitulo, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
            Icon(icono, contentDescription = null, tint = Color.White.copy(alpha = 0.15f), modifier = Modifier.size(70.dp))
        }
    }
}

@Composable
fun TarjetaPremiumGrande(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    gradiente: List<Color>,
    emoji: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors = gradiente))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color.White)
                Text(subtitulo, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
            }
            Icon(icono, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
        }
    }
}