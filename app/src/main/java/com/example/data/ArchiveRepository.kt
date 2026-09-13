package com.example.data

import android.content.Context
import com.example.model.ArchiveItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ArchiveRepository(private val context: Context) {

    private val rootDir: File
        get() {
            val dir = File(context.filesDir, "rar_storage")
            if (!dir.exists()) {
                dir.mkdirs()
                createSampleFiles(dir)
            }
            return dir
        }

    private fun createSampleFiles(dir: File) {
        try {
            val docsDir = File(dir, "Documents")
            docsDir.mkdirs()
            val mediaDir = File(dir, "Media")
            mediaDir.mkdirs()

            File(docsDir, "welcome.txt").writeText("Welcome to RAR Archiver for Android!\nThis app lets you create, view, and extract ZIP, RAR, and other archive files seamlessly.")
            File(docsDir, "notes.spec").writeText("Project Specifications:\n- Jetpack Compose UI\n- Material Design 3\n- Fast archiving engine")

            val sampleZip = File(dir, "sample_archive.zip")
            if (!sampleZip.exists()) {
                val zos = ZipOutputStream(FileOutputStream(sampleZip))
                val entryText = "Hello from inside the ZIP archive!\nEnjoy fast extraction."
                zos.putNextEntry(ZipEntry("inside_file.txt"))
                zos.write(entryText.toByteArray())
                zos.closeEntry()
                zos.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRootDirectory(): File = rootDir

    suspend fun getItemsInPath(currentPath: String): List<ArchiveItem> = withContext(Dispatchers.IO) {
        val targetDir = if (currentPath.isEmpty()) rootDir else File(rootDir, currentPath)
        if (!targetDir.exists() || !targetDir.isDirectory) return@withContext emptyList()

        targetDir.listFiles()?.map { file ->
            ArchiveItem(
                name = file.name,
                path = file.absolutePath.removePrefix(rootDir.absolutePath).removePrefix("/"),
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) (file.listFiles()?.size?.toLong() ?: 0L) else file.length(),
                lastModified = file.lastModified()
            )
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
    }

    suspend fun createDirectory(currentPath: String, name: String): Boolean = withContext(Dispatchers.IO) {
        val parent = if (currentPath.isEmpty()) rootDir else File(rootDir, currentPath)
        val newDir = File(parent, name)
        if (!newDir.exists()) newDir.mkdirs() else false
    }

    suspend fun deleteItem(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(rootDir, path)
        if (file.exists()) {
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        } else false
    }

    suspend fun createZipArchive(currentPath: String, archiveName: String, selectedPaths: List<String>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val parent = if (currentPath.isEmpty()) rootDir else File(rootDir, currentPath)
            val nameWithExt = if (archiveName.endsWith(".zip", ignoreCase = true)) archiveName else "$archiveName.zip"
            val zipFile = File(parent, nameWithExt)

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                for (relPath in selectedPaths) {
                    val fileToAdd = File(rootDir, relPath)
                    addFileToZip(zos, fileToAdd, fileToAdd.name)
                }
            }
            Result.success(zipFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        if (file.isDirectory) {
            val children = file.listFiles() ?: return
            for (child in children) {
                addFileToZip(zos, child, "$entryName/${child.name}")
            }
        } else {
            zos.putNextEntry(ZipEntry(entryName))
            FileInputStream(file).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
        }
    }

    suspend fun extractArchive(archivePath: String, targetSubPath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val archiveFile = File(rootDir, archivePath)
            val extractDir = File(rootDir, if (targetSubPath.isEmpty()) "${archiveFile.nameWithoutExtension}_extracted" else "$targetSubPath/${archiveFile.nameWithoutExtension}")
            if (!extractDir.exists()) extractDir.mkdirs()

            ZipInputStream(FileInputStream(archiveFile)).use { zis ->
                var zipEntry = zis.nextEntry
                while (zipEntry != null) {
                    val newFile = File(extractDir, zipEntry.name)
                    if (zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    zipEntry = zis.nextEntry
                }
            }
            Result.success(extractDir.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getArchiveEntries(archivePath: String): List<ArchiveItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ArchiveItem>()
        try {
            val archiveFile = File(rootDir, archivePath)
            ZipInputStream(FileInputStream(archiveFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    list.add(
                        ArchiveItem(
                            name = entry.name,
                            path = entry.name,
                            isDirectory = entry.isDirectory,
                            size = entry.size.coerceAtLeast(0L),
                            lastModified = entry.time
                        )
                    )
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun readFileContent(path: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(rootDir, path)
            if (file.exists() && !file.isDirectory) {
                file.readText()
            } else "File not found or is a directory."
        } catch (e: Exception) {
            "Error reading file: ${e.localizedMessage}"
        }
    }
}
