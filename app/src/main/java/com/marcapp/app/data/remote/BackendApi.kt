package com.marcapp.app.data.remote

import com.marcapp.app.BuildConfig
import com.marcapp.app.domain.model.*
import com.marcapp.app.domain.repository.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// Rutas contrastadas con MarkApp-main.zip. El backend aún NO devuelve nombre ni idColaborador.
data class LoginRequest(val correo: String, val password: String)
data class LoginResponse(val token: String?, val tipoUsuario: String?, val rol: String?)

interface BackendApi {
    @POST("api/auth/login/colaborador")
    suspend fun loginTrabajador(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/login/supervisor")
    suspend fun loginSupervisor(@Body request: LoginRequest): LoginResponse

    @POST("api/asistencias/ingreso/{id}")
    suspend fun ingreso(@Path("id") id: Int)

    @PATCH("api/asistencias/salida/{id}")
    suspend fun salida(@Path("id") id: Int)

    @POST("api/refrigerios/salida/{id}")
    suspend fun salidaRefrigerio(@Path("id") id: Int)

    @PATCH("api/refrigerios/regreso/{id}")
    suspend fun regresoRefrigerio(@Path("id") id: Int)
}

class HttpAuthRepository : AuthRepository {
    override fun demo(rol: Rol) = Usuario(
        nombre = if (rol == Rol.TRABAJADOR) "María Torres" else "Carlos Ramírez",
        correo = if (rol == Rol.TRABAJADOR) "colaborador@buypal.com.pe" else "admin@buypal.com.pe",
        rol = rol
    )

    override suspend fun iniciar(correo: String, clave: String): ResultadoLogin {
        val url = BuildConfig.API_BASE_URL
        if (url.isBlank()) return ResultadoLogin.Error(
            "La API todavía no está configurada. Usa 'Demo trabajador' para probar las pantallas."
        )
        if (!url.startsWith("https://") || !url.endsWith('/'))
            return ResultadoLogin.Error("Configura una API_BASE_URL HTTPS que termine en /.")
        return try {
            val api = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(BackendApi::class.java)
            val respuesta = try {
                api.loginTrabajador(LoginRequest(correo, clave))
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401 || e.code() == 403)
                    api.loginSupervisor(LoginRequest(correo, clave)) else throw e
            }
            if (respuesta.token.isNullOrBlank()) ResultadoLogin.Error("La API no devolvió token.")
            else {
                val esAdmin = respuesta.tipoUsuario?.contains("supervisor", true) == true ||
                    respuesta.rol?.contains("admin", true) == true
                // Solo alias ilustrativo: el backend debe devolver el nombre real y el ID del trabajador.
                val nombreTemporal = correo.substringBefore('@').replace('.', ' ').replaceFirstChar { it.uppercase() }
                ResultadoLogin.Correcto(Usuario(nombreTemporal, correo,
                    if (esAdmin) Rol.ADMINISTRADOR else Rol.TRABAJADOR,
                    idColaborador = null, esDemo = false))
            }
        } catch (_: Exception) {
            ResultadoLogin.Error("No fue posible iniciar sesión. Revisa credenciales, API y conexión.")
        }
    }
}
