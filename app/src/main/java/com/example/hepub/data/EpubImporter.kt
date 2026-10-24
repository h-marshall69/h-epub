package com.example.hepub.data

import android.content.ContentResolver
import android.net.Uri
import android.util.Xml
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

data class EpubMetadata(val title: String, val authors: List<String>, val coverEntry: String?)

class EpubImportException(message: String) : Exception(message)

class EpubImporter(private val resolver: ContentResolver, private val storageDir: File) {
    var lastImportedFile: File? = null
        private set
    var lastImportedId: String? = null
        private set
    fun import(uri: Uri): EpubMetadata {
        val target = File(storageDir, "book-${System.currentTimeMillis()}.epub")
        storageDir.mkdirs()
        try {
            resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
                ?: throw EpubImportException("No se pudo abrir el archivo seleccionado")
            lastImportedFile = target
            lastImportedId = java.security.MessageDigest.getInstance("SHA-256")
                .digest(target.readBytes()).joinToString("") { "%02x".format(it) }
            return parse(target)
        } catch (error: Exception) {
            target.delete()
            if (error is EpubImportException) throw error
            throw EpubImportException("El archivo no es un EPUB válido")
        }
    }

    internal fun parse(file: File): EpubMetadata {
        return try { parseArchive(file) } catch (error: EpubImportException) { throw error }
        catch (_: Exception) { throw EpubImportException("El archivo no es un EPUB válido") }
    }

    private fun parseArchive(file: File): EpubMetadata {
        ZipFile(file).use { zip ->
            val mimetype = zip.getEntry("mimetype") ?: throw EpubImportException("Falta la entrada mimetype")
            if (zip.getInputStream(mimetype).use { it.readBytes().toString(Charsets.US_ASCII) } != "application/epub+zip")
                throw EpubImportException("El archivo no declara el tipo EPUB")
            val container = zip.getEntry("META-INF/container.xml") ?: throw EpubImportException("Falta META-INF/container.xml")
            val opfPath = parseContainer(zip.getInputStream(container))
                ?: throw EpubImportException("No se encontró el paquete EPUB")
            val opf = zip.getEntry(opfPath) ?: throw EpubImportException("No se encontró el paquete OPF")
            return parsePackage(zip.getInputStream(opf))
        }
    }

    private fun parseContainer(input: InputStream): String? {
        var path: String? = null
        parseXml(input) { parser, event ->
            if (event == XmlPullParser.START_TAG && parser.name.substringAfterLast(':') == "rootfile")
                path = parser.getAttributeValue(null, "full-path") ?: parser.attributeValue("full-path")
        }
        return path
    }

    private fun parsePackage(input: InputStream): EpubMetadata {
        var title = "Sin título"
        val authors = mutableListOf<String>()
        var coverId: String? = null
        val coverItems = mutableMapOf<String, String>()
        var currentText: StringBuilder? = null
        var currentName: String? = null
        parseXml(input) { parser, event ->
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name.substringAfterLast(':')) {
                    "title", "creator" -> { currentName = parser.name.substringAfterLast(':'); currentText = StringBuilder() }
                    "meta" -> if (parser.getAttributeValue(null, "name") == "cover") coverId = parser.getAttributeValue(null, "content")
                    "item" -> parser.getAttributeValue(null, "id")?.let { id -> parser.getAttributeValue(null, "href")?.let { coverItems[id] = it } }
                }
                XmlPullParser.TEXT -> currentText?.append(parser.text)
                XmlPullParser.ENTITY_REF -> currentText?.append(
                    mapOf("amp" to "&", "lt" to "<", "gt" to ">", "apos" to "'", "quot" to "\"")[parser.name] ?: ""
                )
                XmlPullParser.END_TAG -> if (parser.name.substringAfterLast(':') == currentName) {
                    val value = currentText?.toString()?.trim().orEmpty()
                    if (currentName == "title" && value.isNotEmpty()) title = value
                    if (currentName == "creator" && value.isNotEmpty()) authors += value
                    currentText = null; currentName = null
                }
            }
        }
        return EpubMetadata(title, authors, coverId?.let { coverItems[it] })
    }

    private fun parseXml(input: InputStream, handler: (XmlPullParser, Int) -> Unit) {
        input.use {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
                setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
                setInput(it, null)
            }
            try {
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.DOCDECL)
                        throw EpubImportException("El EPUB contiene una declaración XML no permitida")
                    if (event == XmlPullParser.ENTITY_REF && parser.name !in setOf("amp", "lt", "gt", "apos", "quot"))
                        throw EpubImportException("El EPUB contiene una entidad XML no permitida")
                    handler(parser, event)
                    event = parser.nextToken()
                }
            } catch (error: EpubImportException) { throw error }
            catch (_: XmlPullParserException) { throw EpubImportException("El EPUB contiene XML inválido") }
            catch (_: Exception) { throw EpubImportException("El EPUB contiene XML inválido") }
        }
    }

    private fun XmlPullParser.attributeValue(name: String): String? =
        (0 until attributeCount).firstNotNullOfOrNull { index ->
            if (getAttributeName(index).substringAfterLast(':') == name) getAttributeValue(index) else null
        }
}
