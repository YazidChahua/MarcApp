package com.marcapp.app.core.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.marcapp.app.core.designsystem.MarcAppColors as C
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Copia una imagen elegida al almacenamiento interno de la app y devuelve su Uri (texto). */
fun copiarAInterno(context: Context, origen: Uri, nombre: String): String? = runCatching {
    val destino = File(context.filesDir, nombre)
    context.contentResolver.openInputStream(origen)?.use { entrada ->
        destino.outputStream().use { entrada.copyTo(it) }
    }
    Uri.fromFile(destino).toString()
}.getOrNull()

/** Crea un Uri temporal (vía FileProvider) donde la cámara guardará la foto. */
fun crearUriFoto(context: Context): Uri {
    val dir = File(context.cacheDir, "fotos").apply { mkdirs() }
    val archivo = File(dir, "jus_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
}

/** Decodifica una imagen reduciéndola para que no sature la memoria. */
private fun decodificar(context: Context, uri: Uri, maxPx: Int = 1024): ImageBitmap? = runCatching {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
    var muestra = 1
    val mayor = maxOf(limites.outWidth, limites.outHeight)
    while (mayor > 0 && mayor / muestra > maxPx) muestra *= 2
    val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opciones)?.asImageBitmap()
    }
}.getOrNull()

/** Miniatura cuadrada de una imagen a partir de su Uri (texto). */
@Composable
fun Miniatura(uriTexto: String, tamano: Dp = 72.dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uri = remember(uriTexto) { Uri.parse(uriTexto) }
    var imagen by remember(uriTexto) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uriTexto) { imagen = withContext(Dispatchers.IO) { decodificar(context, uri) } }

    Box(
        modifier.size(tamano).clip(RoundedCornerShape(10.dp))
            .background(C.Fondo).border(1.dp, C.Borde, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        val img = imagen
        if (img != null) {
            Image(img, contentDescription = "Adjunto", modifier = Modifier.size(tamano).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.Image, contentDescription = null, tint = C.Secundario)
        }
    }
}

/** ¿El adjunto es una imagen (para mostrar miniatura) o un documento? */
fun esImagen(context: Context, uri: Uri): Boolean =
    runCatching { context.contentResolver.getType(uri) }.getOrNull()?.startsWith("image/") == true

/** Nombre legible de un documento adjunto. */
fun nombreArchivo(context: Context, uri: Uri): String {
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) return c.getString(idx)
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "archivo"
}

/** Vista de un adjunto: miniatura si es imagen, o tarjeta de documento. Con "X" si onQuitar != null. */
@Composable
fun AdjuntoVista(uriTexto: String, tamano: Dp = 72.dp, onQuitar: (() -> Unit)? = null) {
    val context = LocalContext.current
    val uri = remember(uriTexto) { Uri.parse(uriTexto) }
    val imagen = remember(uriTexto) { esImagen(context, uri) }
    Box {
        if (imagen) {
            Miniatura(uriTexto, tamano)
        } else {
            val nombre = remember(uriTexto) { nombreArchivo(context, uri) }
            Box(
                Modifier.size(tamano).clip(RoundedCornerShape(10.dp))
                    .background(C.Fondo).border(1.dp, C.Borde, RoundedCornerShape(10.dp)).padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Description, null, tint = C.VerdeOscuro)
                    Text(nombre, fontSize = 8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center, color = C.Secundario, lineHeight = 10.sp)
                }
            }
        }
        if (onQuitar != null) {
            Surface(
                color = Color(0xCC000000), shape = CircleShape,
                modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(20.dp).clickable { onQuitar() }
            ) {
                Icon(Icons.Default.Close, "Quitar", tint = Color.White, modifier = Modifier.padding(3.dp))
            }
        }
    }
}

/** Foto de perfil circular; si no hay foto, muestra la inicial del nombre. */
@Composable
fun FotoPerfil(uriTexto: String?, inicial: String, tamano: Dp = 72.dp) {
    val context = LocalContext.current
    var img by remember(uriTexto) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uriTexto) {
        img = if (uriTexto.isNullOrBlank()) null
        else withContext(Dispatchers.IO) { decodificar(context, Uri.parse(uriTexto), 512) }
    }
    Box(
        Modifier.size(tamano).clip(CircleShape).background(C.VerdeAccion),
        contentAlignment = Alignment.Center
    ) {
        val b = img
        if (b != null) Image(b, "Foto de perfil", Modifier.size(tamano).clip(CircleShape), contentScale = ContentScale.Crop)
        else Text(inicial, color = Color.White, fontSize = (tamano.value * 0.4f).sp, fontWeight = FontWeight.Bold)
    }
}
