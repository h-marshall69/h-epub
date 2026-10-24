package com.example.hepub.epub

import android.content.Context
import com.example.hepub.data.local.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.readium.r2.shared.publication.Publication

class ReadiumPublicationOpener(private val context: Context) {
    suspend fun open(book: BookEntity): Result<Publication> = withContext(Dispatchers.IO) {
        runCatching {
            val httpClient = DefaultHttpClient()
            val retriever = AssetRetriever(context.contentResolver, httpClient)
            val asset = retriever.retrieve(java.io.File(book.privatePath)).getOrNull()
                ?: error("No se pudo recuperar la copia privada del EPUB")
            val parser = DefaultPublicationParser(context, httpClient, retriever, null)
            PublicationOpener(parser).open(asset, allowUserInteraction = false).getOrNull()
                ?: error("Readium no pudo abrir el EPUB; puede estar dañado o protegido por DRM")
        }
    }
}
