package com.paladmin.util

import retrofit2.HttpException
import retrofit2.Response

/** Retrofit ne lève PAS d'exception pour un code HTTP d'erreur quand le type de retour est
 * Response<T> (contrairement aux endpoints qui retournent directement leur DTO) : un 400/401/403/500
 * est silencieusement traité comme un succès par runCatching{}.onSuccess{} si on ne vérifie pas
 * isSuccessful nous-mêmes — repéré via un "FAILED" côté log serveur PalDefender qui contredisait
 * le message de succès affiché côté app pour /SendPlayerMessage. À appeler sur tout retour
 * Response<Unit> d'action (kick/ban/give/broadcast...) avant de le traiter comme un succès. */
fun Response<*>.requireSuccess() {
    if (!isSuccessful) throw HttpException(this)
}
