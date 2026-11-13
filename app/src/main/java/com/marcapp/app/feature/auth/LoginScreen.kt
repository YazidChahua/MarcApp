package com.marcapp.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
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

@Composable
fun LoginScreen(
    estado: LoginUiState,
    onLogin: (String, String) -> Unit
) {
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var verClave by rememberSaveable { mutableStateOf(false) }
    var recordar by rememberSaveable { mutableStateOf(false) }
    var recuperar by rememberSaveable { mutableStateOf(false) }

    // Fondo blanco de base: se funde con la barra de gestos blanca del celular
    // para que la parte de abajo no se vea cortada ni con una franja rara.
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // ================================================================
        // CABECERA (hero) estilo Shalom: ocupa todo el ancho y llega hasta
        // el borde superior (debajo de la barra de estado). Aquí irá la
        // imagen de bienvenida; por ahora el marco está reservado y vacío.
        // ================================================================
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(MarcAppColors.VerdeOscuro, Color(0xFF0A5247))
                    )
                )
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // empuja el contenido debajo del reloj/batería
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp, bottom = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ---- MARCO RESERVADO PARA LA IMAGEN (vacío por ahora) ----
                // Cuando tengas la foto, se reemplaza este Box por un Image(...)
                // con el mismo tamaño y esquinas redondeadas y quedará encuadrada.
                Box(
                    Modifier
                        .size(96.dp)
                        .background(Color(0x1FFFFFFF), RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color(0x66FFFFFF),
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text("Bienvenido a", color = Color(0xFFBFE4DA), fontSize = 16.sp)
                Text(
                    "MarcApp",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text("BuyPal Store", color = Color(0xFF7FCDB6), fontSize = 13.sp)
            }
        }

        // ================================================================
        // HOJA DE CONTENIDO: tarjeta blanca con esquinas superiores
        // redondeadas que sube y se superpone a la cabecera (como Shalom).
        // ================================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-26).dp),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // respeta la barra de gestos del celular
                    .padding(horizontal = 26.dp)
                    .padding(top = 32.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                TituloSeccion(
                    if (recuperar) "Recuperar contraseña" else "Iniciar sesión",
                    if (recuperar) "La recuperación estará disponible al integrar el servicio de correo."
                    else "Ingresa tus datos para marcar tu asistencia."
                )

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
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                        TextButton(
                            onClick = { recuperar = true },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
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

                    Spacer(Modifier.height(4.dp))
                    Separador()
                    Text(
                        "Acceso seguro para el personal de BuyPal Store",
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MarcAppColors.Secundario
                    )
                } else {
                    Aviso("RF03–RF04: el backend actual no ofrece endpoints de recuperación/restablecimiento.", true)
                    OutlinedButton(
                        onClick = { recuperar = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Volver al inicio de sesión") }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "MarcApp · Inicio de sesión")
@Composable
private fun LoginPreview() { MarcAppTheme { LoginScreen(LoginUiState(), { _, _ -> }) } }
