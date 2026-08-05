package com.paladmin.data.remote

import com.paladmin.debug.DebugLogger
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Un client par profil serveur : host/port/credentials changent par serveur, donc pas de
 * singleton global. [trustAllCerts] ne doit jamais être activé implicitement : c'est le
 * toggle explicite par profil pour les certificats HTTPS auto-signés côté API Palworld.
 *
 * Pas de réutilisation de connexion (pool désactivé + "Connection: close") : le serveur REST
 * intégré de Palworld ferme la connexion après chaque réponse, et OkHttp qui tente de la
 * réutiliser pour la requête suivante obtient "unexpected end of stream" / connexion fermée.
 */
private val CLOSE_CONNECTION_INTERCEPTOR = Interceptor { chain ->
    chain.proceed(chain.request().newBuilder().header("Connection", "close").build())
}

object NetworkClients {

    fun build(authInterceptor: Interceptor, trustAllCerts: Boolean = false, debugLogger: DebugLogger? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
            .retryOnConnectionFailure(true)
            .addInterceptor(authInterceptor)
            .addInterceptor(CLOSE_CONNECTION_INTERCEPTOR)
            .addInterceptor(
                HttpLoggingInterceptor { message ->
                    HttpLoggingInterceptor.Logger.DEFAULT.log(message)
                    debugLogger?.log(message)
                }.apply { level = HttpLoggingInterceptor.Level.BASIC },
            )

        if (trustAllCerts) {
            val trustAllManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<TrustManager>(trustAllManager), SecureRandom())
            }
            builder
                .sslSocketFactory(sslContext.socketFactory, trustAllManager)
                .hostnameVerifier { _, _ -> true }
        }

        return builder.build()
    }
}
