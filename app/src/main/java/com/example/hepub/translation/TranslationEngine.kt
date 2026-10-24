package com.example.hepub.translation

interface TranslationEngine : AutoCloseable {
    suspend fun isModelAvailable(): Boolean
    suspend fun prepareModel(requireWifi: Boolean = true)
    suspend fun translate(text: String): String
    override fun close()
}
