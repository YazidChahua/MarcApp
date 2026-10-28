
package com.marcapp.app.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcapp.app.core.designsystem.MarcAppColors as C

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit = { _, _ -> }
) {
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var recordar by rememberSaveable { mutableStateOf(true) }

    val coloresCampo = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = C.VerdeAccion,
        unfocusedBorderColor = C.Borde,
        focusedContainerColor = C.Campo,
        unfocusedContainerColor = C.Campo
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Fondo)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // CABECERA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp)
                .background(C.VerdeOscuro)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        C.VerdeAccion,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    "MarcApp",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "BuyPal Store",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }

        // FORMULARIO
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                "Iniciar sesión",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = C.Texto
            )

            Text(
                "Marca tu asistencia de forma segura.",
                fontSize = 14.sp,
                color = C.TextoSecundario
            )

            Spacer(Modifier.height(28.dp))

            Text(
                "Correo electrónico",
                color = C.Texto,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                placeholder = {
                    Text("colaborador@buypal.com.pe")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                colors = coloresCampo,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "Contraseña",
                color = C.Texto,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = clave,
                onValueChange = { clave = it },
                placeholder = { Text("Tu contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = coloresCampo,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = recordar,
                    onCheckedChange = { recordar = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = C.VerdeAccion
                    )
                )

                Text(
                    "Recordarme",
                    color = C.TextoSecundario,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    "¿Olvidaste tu contraseña?",
                    color = C.VerdeOscuro,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onLogin(correo, clave) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = C.VerdeAccion
                )
            ) {
                Text(
                    "Ingresar a MarcApp",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(28.dp))

            HorizontalDivider(color = C.Borde)

            Spacer(Modifier.height(16.dp))

            Text(
                "Acceso seguro para personal de BuyPal",
                color = C.TextoSecundario,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(95.dp))

            Text(
                "¿Eres administrador? Acceder al panel web",
                color = C.VerdeOscuro,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(
    name = "MarcApp - Login",
    showBackground = true,
    widthDp = 360,
    heightDp = 780
)
@Composable
private fun LoginPreview() {
    MaterialTheme {
        LoginScreen()
    }
}
