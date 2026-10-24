package com.example.hepub.data

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubImporterTest {
    private val importer: EpubImporter
        get() = EpubImporter(InstrumentationRegistry.getInstrumentation().targetContext.contentResolver, File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "books"))

    @Test fun validNamespacedPackageExtractsMetadata() {
        val file = epub("""
            <package xmlns="http://www.idpf.org/2007/opf"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Crème &amp; Café</dc:title><dc:creator>Ana &amp; José</dc:creator><meta name="cover" content="cover"/></metadata><manifest><item id="cover" href="images/cover.jpg"/></manifest></package>
        """.trimIndent())
        val metadata = importer.parse(file)
        assertEquals("Crème & Café", metadata.title)
        assertEquals(listOf("Ana & José"), metadata.authors)
        assertEquals("images/cover.jpg", metadata.coverEntry)
    }

    @Test fun doctypeIsRejectedWithoutExternalResolution() {
        val file = epub("<!DOCTYPE package [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><package><metadata><title>&xxe;</title></metadata></package>")
        assertThrows(EpubImportException::class.java) { importer.parse(file) }
    }

    @Test fun nonEpubZipIsRejected() {
        val file = File.createTempFile("not-epub", ".zip")
        ZipOutputStream(file.outputStream()).use { it.putNextEntry(ZipEntry("file.txt")); it.write("no".toByteArray()); it.closeEntry() }
        assertThrows(EpubImportException::class.java) { importer.parse(file) }
    }

    @Test fun corruptArchiveIsRejected() {
        val file = File.createTempFile("corrupt", ".epub").apply { writeText("not a zip") }
        assertThrows(EpubImportException::class.java) { importer.parse(file) }
    }

    private fun epub(opf: String): File {
        val file = File.createTempFile("test", ".epub")
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("mimetype")); zip.write("application/epub+zip".toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry("META-INF/container.xml")); zip.write("<container><rootfile full-path=\"OEBPS/content.opf\"/></container>".toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry("OEBPS/content.opf")); zip.write(opf.toByteArray()); zip.closeEntry()
        }
        return file
    }
}
