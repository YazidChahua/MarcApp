
package com.marcapp.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import com.marcapp.app.feature.auth.presentation.LoginScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                LoginScreen(
                    onLogin = { _, _ ->
                        Toast.makeText(
                            this,
                            "MarcApp funcionando correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}
