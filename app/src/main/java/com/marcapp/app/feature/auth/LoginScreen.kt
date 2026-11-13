package com.marcapp.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcapp.app.core.designsystem.*
import com.marcapp.app.core.ui.*
import com.marcapp.app.domain.model.Rol

@Composable
fun LoginScreen(
    estado: LoginUiState,
    onLogin: (String, String) -> Unit,
    onDemo: (Rol) -> Unit
) {
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var verClave by rememberSaveable { mutableStateOf(false) }
    var recordar by rememberSaveable { mutableStateOf(false) }
    var recuperar by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MarcAppColors.Fondo)
            .verticalScroll(rememberScrollState())
    ) {
        // ----------------------------------------------------------------
        // CABECERA (zona superior tipo "hero").
        // Aquí irá la imagen de bienvenida más adelante: el recuadro de la
        // izquierda está reservado y vacío a propósito. Cuando Luis decida
        // qué foto poner, se reemplaza el Box "RESERVADO PARA IMAGEN".
        // ----------------------------------------------------------------
        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(MarcAppColors.VerdeOscuro, Color(0xFF0A5247))
                    )
                )
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- RESERVADO PARA IMAGEN (por ahora vacío) ---
                Box(
                    Modifier
                        .size(96.dp)
                        .background(Color(0x22FFFFFF), RoundedCornerShape(20.dp))
                )
                Spacer(Modifier.width(18.dp))
                Column {
                    Text("Bienvenido a", color = Color(0xFFCDE7DF), fontSize = 18.sp)
                    Text(
                        "MarcApp",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("BuyPal Store", color = Color(0xFF8FD4BE), fontSize = 13.sp)
                }
            }
        }

        // ----------------------------------------------------------------
        // TARJETA DE CONTENIDO (se superpone a la cabecera con esquinas
        // superiores redondeadas, como en el modelo de referencia).
        // ----------------------------------------------------------------
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-28).dp),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            shadowElevation = 6.dp
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TituloSeccion(
                    if (recuperar) "Recuperar contraseña" else "Iniciar sesión",
                    if (recuperar) "La recuperación estará disponible al integrar el servicio de correo."
                    else "Ingresa tus datos para marcar tu asistencia."
                )
                Spacer(Modifier.height(2.dp))

                CampoTexto("Correo electrónico", correo, { correo = it })

                if (!recuperar) {
                    // --- Contraseña con ojito para mostrar / ocultar ---
                    OutlinedTextField(
                        value = clave,
                        onValueChange = { clave = it },
                        label = { Text("Contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (verClave) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { verClave = !verClave }) {
                                Icon(
                                    imageVector = if (verClave) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = if (verClave) "Ocultar contraseña"
                                    else "Mostrar contraseña",
                                    tint = MarcAppColors.Secundario
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MarcAppColors.VerdeAccion,
                            unfocusedBorderColor = MarcAppColors.Borde,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    // --- Recuérdame + ¿Olvidaste tu contraseña? ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = recordar,
                            onCheckedChange = { recordar = it },
                            enabled = false,
                            colors = CheckboxDefaults.colors(checkedColor = MarcAppColors.VerdeAccion)
                        )
                        Text(
                            "Recordarme",
                            Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = MarcAppColors.Secundario
                        )
                        TextButton(onClick = { recuperar = true }) {
                            Text(
                                "¿Olvidaste tu contraseña?",
                                fontSize = 12.sp,
                                color = MarcAppColors.VerdeAccion,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (estado.error != null) Aviso(estado.error, true)

                    BotonPrincipal(
                        "Iniciar sesión",
                        { onLogin(correo, clave) },
                        Modifier.fillMaxWidth(),
                        habilitado = !estado.cargando
                    )
                    if (estado.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())

                    Separador()

                    Text(
                        "Acceso seguro para personal de BuyPal",
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MarcAppColors.Secundario
                    )

                    Aviso("Para probar sin servidor, elige un perfil de demostración. Los datos no son reales.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onDemo(Rol.TRABAJADOR) }, modifier = Modifier.weight(1f)) {
                            Text("Demo trabajador", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { onDemo(Rol.ADMINISTRADOR) }, modifier = Modifier.weight(1f)) {
                            Text("Demo admin", fontSize = 12.sp)
                        }
                    }
                } else {
                    Aviso("RF03–RF04: el backend actual no ofrece endpoints de recuperación/restablecimiento.", true)
                    OutlinedButton(onClick = { recuperar = false }) { Text("Volver al inicio de sesión") }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "MarcApp · Inicio de sesión")
@Composable
private fun LoginPreview() { MarcAppTheme { LoginScreen(LoginUiState(), { _, _ -> }, { }) } }
