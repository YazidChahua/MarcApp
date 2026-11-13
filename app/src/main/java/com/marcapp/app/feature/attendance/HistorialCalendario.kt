package com.marcapp.app.feature.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcapp.app.core.designsystem.MarcAppColors as C
import com.marcapp.app.core.ui.*
import com.marcapp.app.domain.model.*
import com.marcapp.app.feature.admin.GestionViewModel
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// ===================================================================
//  JORNADA Y CÁLCULO DE ASISTENCIA
//  Esta es la capa lista para integrar con el panel del administrador.
//  Cuando el backend exista, solo se reemplaza JORNADA_DEMO / marcasDe()
//  por los datos reales (hora de ingreso, salida y jornada del trabajador).
// ===================================================================

/** Jornada laboral que definirá el administrador desde el panel web. */
data class Jornada(
    val horaEntrada: LocalTime,          // hora de ingreso esperada
    val horasPorDia: Double,             // horas de trabajo por día (ej. 4 = part time)
    val diasLaborables: Set<DayOfWeek>,  // días que trabaja (ej. Lun a Sáb)
    val toleranciaMinutos: Int = 10      // minutos de tolerancia para seguir siendo puntual
)

/** Valores de EJEMPLO mientras no exista el panel del administrador.
 *  Pepe: 4 h por día, de lunes a sábado, ingreso 09:00. */
val JORNADA_DEMO = Jornada(
    horaEntrada = LocalTime.of(9, 0),
    horasPorDia = 4.0,
    diasLaborables = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    ),
    toleranciaMinutos = 10
)

/** Resumen ya calculado de un día: lo que el calendario necesita pintar. */
data class ResumenDia(
    val fecha: LocalDate,
    val marcas: List<Marcacion>,
    val horasTrabajadas: Double?,   // null = jornada incompleta / sin datos
    val puntual: Boolean?,          // null = no marcó ingreso
    val horasJornada: Double?       // horas esperadas ese día (0 si no es laborable)
)

/** Calcula el resumen de un día a partir de sus marcaciones y la jornada. */
fun resumenDelDia(fecha: LocalDate, marcas: List<Marcacion>, jornada: Jornada): ResumenDia {
    val entrada = marcas.firstOrNull { it.tipo == TipoMarca.ENTRADA }?.fechaHora
    val salidaRef = marcas.firstOrNull { it.tipo == TipoMarca.SALIDA_REFRIGERIO }?.fechaHora
    val regresoRef = marcas.firstOrNull { it.tipo == TipoMarca.REGRESO_REFRIGERIO }?.fechaHora
    val salida = marcas.firstOrNull { it.tipo == TipoMarca.SALIDA }?.fechaHora

    val horas: Double? = if (entrada != null && salida != null) {
        var minutos = Duration.between(entrada, salida).toMinutes()
        if (salidaRef != null && regresoRef != null)
            minutos -= Duration.between(salidaRef, regresoRef).toMinutes()
        (minutos / 60.0).coerceAtLeast(0.0)
    } else null

    val puntual: Boolean? = entrada?.let {
        !it.toLocalTime().isAfter(jornada.horaEntrada.plusMinutes(jornada.toleranciaMinutos.toLong()))
    }
    val esLaborable = fecha.dayOfWeek in jornada.diasLaborables
    return ResumenDia(fecha, marcas, horas, puntual, if (esLaborable) jornada.horasPorDia else 0.0)
}

/** Datos de EJEMPLO (DEMO) para días pasados, para ver el calendario funcionando.
 *  Se eliminan al conectar el backend real. */
fun marcasDemoDia(fecha: LocalDate, jornada: Jornada): List<Marcacion> {
    if (fecha.dayOfWeek !in jornada.diasLaborables) return emptyList()
    val tarde = fecha.dayOfMonth % 7 == 5          // un día con tardanza
    val conExtra = fecha.dayOfMonth % 10 == 0      // un día con horas extra
    val entrada = fecha.atTime(jornada.horaEntrada).plusMinutes(if (tarde) 25 else 3)
    val inicioRef = fecha.atTime(13, 0)
    val finRef = inicioRef.plusHours(1)
    val horasTrabajo = if (conExtra) jornada.horasPorDia + 2 else jornada.horasPorDia
    val salida = entrada.plusMinutes(((horasTrabajo + 1) * 60).toLong()) // + 1 h de refrigerio
    return listOf(
        Marcacion(TipoMarca.ENTRADA, entrada, "Demo"),
        Marcacion(TipoMarca.SALIDA_REFRIGERIO, inicioRef, "Demo"),
        Marcacion(TipoMarca.REGRESO_REFRIGERIO, finRef, "Demo"),
        Marcacion(TipoMarca.SALIDA, salida, "Demo")
    )
}

// ===================================================================
//  PANTALLA DE HISTORIAL (CALENDARIO)
// ===================================================================

private val mesesLargos = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
private val diasSemana = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private val horaFmt12 = DateTimeFormatter.ofPattern("hh:mm a")

private fun fmtHoras(d: Double): String =
    if (d % 1.0 == 0.0) "${d.toInt()} h" else String.format("%.1f h", d)

private fun fmtHorasCorto(d: Double): String =
    if (d % 1.0 == 0.0) "${d.toInt()}h" else String.format("%.1fh", d)

@Composable
fun HistorialScreen(vm: AttendanceViewModel, gestion: GestionViewModel, usuario: Usuario) {
    val esDemo = usuario.esDemo
    val marcas by vm.marcaciones.collectAsState()
    val solicitudes by gestion.solicitudes.collectAsState()
    val hoy = LocalDate.now()
    var mesVisible by remember { mutableStateOf(YearMonth.from(hoy)) }
    var diaSel by remember { mutableStateOf<LocalDate?>(hoy) }

    // Marcaciones de un día: reales si existen; si es DEMO y es un día pasado, datos de ejemplo.
    fun marcasDe(fecha: LocalDate): List<Marcacion> {
        val reales = marcas.filter { it.fechaHora.toLocalDate() == fecha }
        return when {
            reales.isNotEmpty() -> reales
            esDemo && fecha.isBefore(hoy) -> marcasDemoDia(fecha, JORNADA_DEMO)
            else -> emptyList()
        }
    }

    // Totales del mes (solo hasta hoy).
    var totalTrabajadas = 0.0
    var totalEsperadas = 0.0
    var totalExtra = 0.0
    for (d in 1..mesVisible.lengthOfMonth()) {
        val f = mesVisible.atDay(d)
        if (f.isAfter(hoy)) break
        val r = resumenDelDia(f, marcasDe(f), JORNADA_DEMO)
        val w = r.horasTrabajadas ?: 0.0
        totalTrabajadas += w
        totalEsperadas += r.horasJornada ?: 0.0
        if ((r.horasJornada ?: 0.0) > 0.0) totalExtra += (w - r.horasJornada!!).coerceAtLeast(0.0)
    }

    ColumnaPantallaHist {
        TituloSeccion("Mi historial", "Revisa tu asistencia por mes")

        // ---- Navegador de mes con flechas (mes actual por defecto) ----
        NavegadorMes(
            mes = mesVisible,
            onCambio = { mesVisible = it; diaSel = if (it == YearMonth.from(hoy)) hoy else null }
        )

        // ---- Calendario del mes ----
        Tarjeta {
            // Cabecera de días de la semana (lunes a domingo).
            Row(Modifier.fillMaxWidth()) {
                listOf("L", "M", "M", "J", "V", "S", "D").forEach {
                    Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                        fontSize = 11.sp, color = C.Secundario, fontWeight = FontWeight.SemiBold)
                }
            }

            val primero = mesVisible.atDay(1)
            val offset = primero.dayOfWeek.value - 1   // lunes = 0
            val celdas = buildList<LocalDate?> {
                repeat(offset) { add(null) }
                (1..mesVisible.lengthOfMonth()).forEach { add(mesVisible.atDay(it)) }
                while (size % 7 != 0) add(null)
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                celdas.chunked(7).forEach { semana ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        semana.forEach { fecha ->
                            if (fecha == null) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                val resumen = resumenDelDia(fecha, marcasDe(fecha), JORNADA_DEMO)
                                CeldaDia(
                                    modifier = Modifier.weight(1f),
                                    resumen = resumen,
                                    esFuturo = fecha.isAfter(hoy),
                                    esHoy = fecha == hoy,
                                    seleccionado = fecha == diaSel,
                                    onClick = { diaSel = fecha }
                                )
                            }
                        }
                    }
                }
            }
            LeyendaCalendario()
        }

        // ---- Detalle del día seleccionado ----
        diaSel?.let { fecha ->
            val r = resumenDelDia(fecha, marcasDe(fecha), JORNADA_DEMO)
            val justi = solicitudes.firstOrNull {
                it.usuario.equals(usuario.correo, ignoreCase = true) &&
                    runCatching { LocalDate.parse(it.fecha) == fecha }.getOrDefault(false)
            }
            DetalleDia(r, justi)
        }

        // ---- Resumen del mes: horas trabajadas y horas extra ----
        Tarjeta {
            Text("Resumen de ${mesesLargos[mesVisible.monthValue - 1]}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = C.Texto)
            Separador()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Dato("Horas trabajadas", fmtHoras(totalTrabajadas))
                Dato("Horas de tu jornada", fmtHoras(totalEsperadas))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Horas extra", color = C.Secundario, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Surface(color = if (totalExtra > 0) Color(0xFFD7F5E9) else C.Fondo, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        fmtHoras(totalExtra),
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        color = if (totalExtra > 0) C.VerdeOscuro else C.Secundario
                    )
                }
            }
        }

        if (esDemo) Aviso("Los días anteriores muestran datos de EJEMPLO. Con el panel del administrador (hora de ingreso, salida y jornada) se mostrará tu asistencia real.")
    }
}

/** Celda de un día dentro del calendario. */
@Composable
private fun CeldaDia(
    modifier: Modifier,
    resumen: ResumenDia,
    esFuturo: Boolean,
    esHoy: Boolean,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    val fondo = when {
        esFuturo -> Color(0xFFEEF1F1)                       // futuro: apagado, no clickeable
        resumen.puntual == true -> Color(0xFFD7F5E9)        // llegó puntual: verde
        resumen.puntual == false -> Color(0xFFFFF1DE)       // marcó pero tarde: ámbar
        else -> Color.White                                 // sin datos
    }
    val colorTexto = when {
        esFuturo -> C.Secundario.copy(alpha = 0.5f)
        resumen.puntual == true -> C.VerdeOscuro
        resumen.puntual == false -> Color(0xFF8A5A17)
        else -> C.Texto
    }
    val borde = when {
        seleccionado -> C.VerdeAccion
        esHoy -> C.VerdeAccion.copy(alpha = 0.5f)
        else -> C.Borde
    }
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(9.dp))
            .background(fondo)
            .border(if (seleccionado) 2.dp else 1.dp, borde, RoundedCornerShape(9.dp))
            .clickable(enabled = !esFuturo, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${resumen.fecha.dayOfMonth}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colorTexto)
            if (!esFuturo && resumen.horasTrabajadas != null) {
                Text(fmtHorasCorto(resumen.horasTrabajadas), fontSize = 9.sp, color = colorTexto)
            }
        }
    }
}

/** Leyenda de colores del calendario. */
@Composable
private fun LeyendaCalendario() {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        PuntoLeyenda(Color(0xFFD7F5E9), "Puntual")
        PuntoLeyenda(Color(0xFFFFF1DE), "Tardanza")
        PuntoLeyenda(Color(0xFFEEF1F1), "Próximos")
    }
}

@Composable
private fun PuntoLeyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color).border(1.dp, C.Borde, RoundedCornerShape(3.dp)))
        Text(texto, fontSize = 11.sp, color = C.Secundario)
    }
}

/** Navegador de mes con flechas a los costados y el nombre del mes grande al centro. */
@Composable
fun NavegadorMes(mes: YearMonth, onCambio: (YearMonth) -> Unit) {
    val puedeAvanzar = mes.isBefore(YearMonth.now())
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilledTonalIconButton(onClick = { onCambio(mes.minusMonths(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
        }
        Text(
            "${mesesLargos[mes.monthValue - 1]} ${mes.year}",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.Texto,
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
        )
        FilledTonalIconButton(onClick = { onCambio(mes.plusMonths(1)) }, enabled = puedeAvanzar) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
        }
    }
}

/** Tarjeta de detalle del día (como aparece en Marcar): ingreso, refrigerio, salida. */
@Composable
private fun DetalleDia(r: ResumenDia, justi: Justificacion? = null) {
    Tarjeta {
        // Mensaje de justificación (si el trabajador envió una para ese día).
        if (justi != null) {
            Surface(color = Color(0xFFFFF4E5), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Justificación: ${justi.motivo}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF8A5A17))
                        Text(justi.estado, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (justi.estado == "Aprobada") C.VerdeOscuro else Color(0xFF8A5A17))
                    }
                    if (justi.detalle.isNotBlank()) Text(justi.detalle, fontSize = 12.sp, color = C.Texto)
                    if (justi.adjuntos.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            justi.adjuntos.take(4).forEach { AdjuntoVista(it, tamano = 60.dp) }
                        }
                    }
                    if (justi.estado == "Pendiente")
                        Text("En revisión por el administrador.", fontSize = 11.sp, color = C.Secundario)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${diasSemana[r.fecha.dayOfWeek.value - 1]} ${r.fecha.dayOfMonth} de ${mesesLargos[r.fecha.monthValue - 1].lowercase()}",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp, color = C.Texto
                )
                Text(
                    r.horasTrabajadas?.let { "Trabajaste ${fmtHoras(it)}" } ?: "Sin jornada completa registrada",
                    fontSize = 12.sp, color = C.Secundario
                )
            }
            if (r.puntual != null) {
                Surface(
                    color = if (r.puntual) Color(0xFFD7F5E9) else Color(0xFFFFF1DE),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (r.puntual) "Puntual" else "Tardanza",
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = if (r.puntual) C.VerdeOscuro else Color(0xFF8A5A17)
                    )
                }
            }
        }
        Separador()
        if (r.marcas.isEmpty()) {
            Text("Sin marcaciones registradas este día.", fontSize = 13.sp, color = C.Secundario)
        } else {
            TipoMarca.entries.forEach { tipo ->
                val registro = r.marcas.firstOrNull { it.tipo == tipo }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (registro != null) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        null, tint = if (registro != null) C.VerdeAccion else C.Secundario,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tipo.titulo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(tipo.detalle, fontSize = 11.sp, color = C.Secundario)
                    }
                    Text(
                        registro?.fechaHora?.format(horaFmt12) ?: "—",
                        fontSize = 13.sp,
                        color = if (registro != null) C.VerdeOscuro else C.Secundario
                    )
                }
            }
        }
    }
}

/** Columna con scroll para la pantalla de historial. */
@Composable
private fun ColumnaPantallaHist(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(C.Fondo)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}
