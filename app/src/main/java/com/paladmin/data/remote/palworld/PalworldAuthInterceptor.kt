package com.paladmin.data.remote.palworld

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import java.nio.charset.StandardCharsets

/** Le nom d'utilisateur de l'API REST Palworld est fixé par le serveur lui-même à "admin" — ce
 * n'est pas un identifiant que l'hébergeur ou l'utilisateur choisit, donc pas de champ éditable. */
class PalworldAuthInterceptor(
    private val password: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Credentials.basic() encode en ISO-8859-1 par défaut : un mot de passe avec des
        // caractères non-ASCII (accents...) produirait un Base64 différent de ce que le
        // serveur attend s'il compare côté UTF-8. On force donc l'UTF-8 explicitement.
        val request = chain.request().newBuilder()
            .header("Authorization", Credentials.basic("admin", password, StandardCharsets.UTF_8))
            .build()
        return chain.proceed(request)
    }
}
