package com.example.hepub.translation

import kotlinx.coroutines.tasks.await
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class MlKitTranslationEngine : TranslationEngine {
    private val translator: Translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.SPANISH)
            .build()
    )
    private val modelManager = RemoteModelManager.getInstance()

    override suspend fun isModelAvailable(): Boolean {
        val models = modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
        val languages = models.mapNotNull { it.language }.toSet()
        return TranslateLanguage.ENGLISH in languages && TranslateLanguage.SPANISH in languages
    }

    override suspend fun prepareModel(requireWifi: Boolean) {
        val conditions = DownloadConditions.Builder().apply { if (requireWifi) requireWifi() }.build()
        translator.downloadModelIfNeeded(conditions).await()
    }

    override suspend fun translate(text: String): String = translator.translate(text).await()

    override fun close() = translator.close()
}
