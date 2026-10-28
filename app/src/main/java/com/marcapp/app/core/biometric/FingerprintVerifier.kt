package com.marcapp.app.core.biometric

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper

/**
 * SOLO huella digital: usa FingerprintManager porque BiometricPrompt no ofrece
 * un selector público para forzar la modalidad huella en todos los dispositivos.
 *
 * API OBSOLETA: apta únicamente para prototipos y dispositivos validados.
 * La aplicación no captura, lee ni guarda plantillas de huellas.
 */
sealed interface EstadoHuella {
    data object Lista : EstadoHuella
    data object SinSensor : EstadoHuella
    data object SinRegistrar : EstadoHuella
    data object NoDisponible : EstadoHuella
}

sealed interface ResultadoHuella {
    data object Aprobada : ResultadoHuella
    data object NoReconocida : ResultadoHuella
    data class Mensaje(val texto: String) : ResultadoHuella
    data class Error(val texto: String) : ResultadoHuella
}

interface VerificadorHuella {
    fun estado(): EstadoHuella
    /** Inicia verificación y devuelve una operación para cancelarla. */
    fun verificar(onResultado: (ResultadoHuella) -> Unit): () -> Unit
}

@Suppress("DEPRECATION")
class AndroidVerificadorHuella(private val contexto: Context) : VerificadorHuella {

    private fun gestor(): FingerprintManager? =
        contexto.getSystemService(FingerprintManager::class.java)

    override fun estado(): EstadoHuella {
        if (contexto.checkSelfPermission(Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            return EstadoHuella.NoDisponible
        }
        return try {
            val fingerprint = gestor() ?: return EstadoHuella.SinSensor
            when {
                !fingerprint.isHardwareDetected -> EstadoHuella.SinSensor
                !fingerprint.hasEnrolledFingerprints() -> EstadoHuella.SinRegistrar
                else -> EstadoHuella.Lista
            }
        } catch (_: Exception) {
            EstadoHuella.NoDisponible
        }
    }

    override fun verificar(onResultado: (ResultadoHuella) -> Unit): () -> Unit {
        val cancelacion = CancellationSignal()
        var activo = true
        val cancelar = {
            activo = false
            if (!cancelacion.isCanceled) cancelacion.cancel()
        }

        if (estado() != EstadoHuella.Lista) {
            onResultado(ResultadoHuella.Error("No hay una huella registrada y disponible en este teléfono."))
            return cancelar
        }

        try {
            val fingerprint = gestor()
            if (fingerprint == null) {
                onResultado(ResultadoHuella.Error("Este teléfono no tiene lector de huellas disponible."))
                return cancelar
            }
            fingerprint.authenticate(
                null, // DEMO: para producción usar un CryptoObject y prueba criptográfica en servidor.
                cancelacion,
                0,
                object : FingerprintManager.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
                        if (activo) {
                            activo = false
                            onResultado(ResultadoHuella.Aprobada)
                        }
                    }

                    override fun onAuthenticationFailed() {
                        if (activo) onResultado(ResultadoHuella.NoReconocida)
                    }

                    override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
                        if (activo) onResultado(ResultadoHuella.Mensaje(helpString.toString()))
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (activo) {
                            activo = false
                            onResultado(ResultadoHuella.Error(errString.toString()))
                        }
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (error: Exception) {
            if (activo) {
                activo = false
                onResultado(ResultadoHuella.Error("No se pudo iniciar el sensor: ${error.message ?: "error desconocido"}"))
            }
        }
        return cancelar
    }
}
