package com.paladmin.data.local.dataset

import java.text.Normalizer

/** Minuscules + accents supprimés, pour permettre une recherche LIKE insensible aux accents FR. */
object SearchNormalizer {
    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{M}"), "").lowercase()
    }
}
