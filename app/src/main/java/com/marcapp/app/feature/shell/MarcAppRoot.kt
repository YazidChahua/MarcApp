package com.marcapp.app.feature.shell

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcapp.app.core.designsystem.MarcAppColors as C
import com.marcapp.app.domain.model.*
import com.marcapp.app.feature.auth.*
import com.marcapp.app.feature.attendance.*
import com.marcapp.app.feature.admin.*
import kotlinx.coroutines.launch

private data class Tab(val titulo: String, val icono: ImageVector)
private val trabajadorTabs = listOf(
    Tab("Inicio", Icons.Default.Home), Tab("Marcar", Icons.Default.AccessTime),
    Tab("Historial", Icons.Default.History), Tab("Solicitudes", Icons.Default.Description),
    Tab("Perfil", Icons.Default.Person)
)
private val adminTabs = listOf(
    Tab("Inicio", Icons.Default.Home), Tab("Personal", Icons.Default.Groups),
    Tab("Gestión", Icons.Default.Settings), Tab("Reportes", Icons.Default.Assessment),
    Tab("Perfil", Icons.Default.Person)
)

@Composable
fun MarcAppRoot(sesion: SessionViewModel, asistencia: AttendanceViewModel, gestion: GestionViewModel) {
    val usuario by sesion.usuario.collectAsState()
    val loginUi by sesion.loginState.collectAsState()
    val contexto = LocalContext.current

    // Apenas el usuario inicia sesión, se le pide el permiso de ubicación una sola vez.
    // Así el mapa funciona de inmediato y ya no hace falta ningún botón extra.
    val permisoUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* el mapa reacciona solo cuando se concede */ }

    LaunchedEffect(usuario) {
        if (usuario != null) {
            asistencia.cargar(usuario!!)
            // Baja el turno de hoy y agenda los recordatorios/mensajes de ánimo del trabajador.
            asistencia.configurarNotificaciones(contexto.applicationContext, usuario!!)
            val concedido = ContextCompat.checkSelfPermission(
                contexto, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!concedido) permisoUbicacion.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Cada vez que cambian las marcaciones (p.ej. tras marcar), se actualiza el estado que usan
    // las notificaciones para decidir entre felicitar o recordar a la hora del evento.
    val marcaciones by asistencia.marcaciones.collectAsState()
    LaunchedEffect(marcaciones, usuario) {
        if (usuario?.rol == Rol.TRABAJADOR) {
            asistencia.sincronizarMarcasNotificaciones(contexto.applicationContext)
        }
    }
    Surface(Modifier.fillMaxSize(), color = Color.White) {
        // El login ocupa toda la pantalla (cabecera inmersiva estilo Shalom) y
        // gestiona sus propios márgenes de sistema. El resto de la app sí mantiene
        // el safeDrawingPadding para no chocar con las barras del celular.
        if (usuario == null) LoginScreen(loginUi, sesion::entrar)
        else key(usuario!!.rol) {
            Box(Modifier.safeDrawingPadding()) {
                NavegacionPrincipal(usuario!!, asistencia, gestion, sesion::cerrarSesion, sesion::actualizarCorreo)
            }
        }
    }
}

@Composable
private fun NavegacionPrincipal(
    usuario: Usuario, asistencia: AttendanceViewModel, gestion: GestionViewModel,
    cerrarSesion: () -> Unit, cambiarCorreo: (String) -> Unit
) {
    val tabs = if (usuario.rol == Rol.TRABAJADOR) trabajadorTabs else adminTabs
    val pager = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    Scaffold(
        containerColor = C.Fondo,
        topBar = {
            Surface(color = C.VerdeOscuro, shadowElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().height(65.dp).padding(horizontal = 17.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(30.dp).background(C.VerdeAccion, androidx.compose.foundation.shape.RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center) {
                            Text("M", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(9.dp))
                        Text("MarcApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    }
                    Text("Hola, ${usuario.nombre}", modifier = Modifier.widthIn(max = 170.dp), color = Color.White, fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 5.dp) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(selected = pager.currentPage == index,
                        onClick = { scope.launch { pager.animateScrollToPage(index) } },
                        icon = { Icon(tab.icono, tab.titulo) }, label = { Text(tab.titulo, fontSize = 10.sp) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = C.VerdeOscuro, selectedTextColor = C.VerdeOscuro,
                            indicatorColor = Color(0xFFD7F5E9), unselectedIconColor = C.Secundario,
                            unselectedTextColor = C.Secundario))
                }
            }
        }
    ) { padding ->
        // El header y la barra son persistentes. Swipe horizontal para cambiar de sección.
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().padding(padding),
            beyondViewportPageCount = 1) { index ->
            if (usuario.rol == Rol.TRABAJADOR) when (index) {
                0 -> InicioTrabajador(usuario, asistencia) { scope.launch { pager.animateScrollToPage(1) } }
                1 -> MarcarScreen(usuario, asistencia)
                2 -> HistorialScreen(asistencia, gestion, usuario)
                3 -> SolicitudesScreen(usuario, gestion)
                else -> PerfilScreen(usuario, cambiarCorreo, cerrarSesion)
            } else when (index) {
                0 -> InicioAdministrador(gestion) { scope.launch { pager.animateScrollToPage(1) } }
                1 -> PersonalScreen(gestion)
                2 -> GestionScreen(gestion)
                3 -> ReportesScreen(gestion)
                else -> PerfilScreen(usuario, cambiarCorreo, cerrarSesion)
            }
        }
    }
}
