package com.gymos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymos.app.ui.theme.GymosTheme
import com.gymos.app.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymosTheme {
                GymosApp()
            }
        }
    }
}

@Composable
fun GymosApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    var splashListo by remember { mutableStateOf(false) }
    var mostrarDialogoSalir by remember { mutableStateOf(false) }
    var colorActual by remember { mutableStateOf(getColorGuardado(context)) }

    if (!splashListo) {
        SplashScreen(onFinish = { splashListo = true })
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    BackHandler(enabled = currentRoute == "inicio") {
        mostrarDialogoSalir = true
    }

    BackHandler(enabled = currentRoute != "inicio" && currentRoute != null) {
        navController.navigate("inicio") {
            popUpTo("inicio") { inclusive = true }
        }
    }

    if (mostrarDialogoSalir) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSalir = false },
            title = { Text("¿Salir de Gymos?", color = Color.White) },
            text = { Text("¿Estás seguro que querés salir?", color = Color(0xFFAAAAAA)) },
            confirmButton = {
                TextButton(onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }) {
                    Text("Salir", color = colorActual)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoSalir = false }) {
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    val items = listOf(
        NavItem("inicio", "Inicio", Icons.Default.Home),
        NavItem("asistente_ia", "Asistente", Icons.Default.Chat),
        NavItem("progreso", "Progreso", Icons.Default.TrendingUp),
        NavItem("configuracion", "Config", Icons.Default.Settings),
    )

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppColor provides colorActual
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF111111)
                ) {
                    items.forEach { item ->
                        val seleccionado = currentRoute == item.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    tint = if (seleccionado) colorActual else Color(0xFF666666)
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    color = if (seleccionado) colorActual else Color(0xFF666666)
                                )
                            },
                            selected = seleccionado,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = false
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = colorActual.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "inicio",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("inicio") { InicioScreen(navController) }
                composable("asistente_ia") { AsistenteIAScreen() }
                composable("progreso") { ProgresoScreen() }
                composable("configuracion") {
                    ConfiguracionScreen(onColorCambiado = {
                        colorActual = getColorGuardado(context)
                    })
                }
                composable("podometro") { PodometroScreen() }
                composable("imc") { ImcScreen() }
                composable("cronometro") { CronometroScreen() }
                composable("hidratacion") { HidratacionScreen() }
                composable("rutina_ia") { RutinaIAScreen() }
                composable("dieta_ia") { DietaIAScreen() }
            }
        }
    }
}

data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)