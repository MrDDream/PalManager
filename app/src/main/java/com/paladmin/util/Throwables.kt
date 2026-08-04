package com.paladmin.util

/** Inclut le type de l'exception (ex: "SSLException") en plus du message pour un diagnostic réseau exploitable. */
fun Throwable.describe(fallback: String): String {
    val detail = message ?: fallback
    return "${javaClass.simpleName}: $detail"
}
