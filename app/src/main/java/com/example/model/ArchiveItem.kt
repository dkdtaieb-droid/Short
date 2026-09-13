package com.example.model

data class ArchiveItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = System.currentTimeMillis(),
    val extension: String = name.substringAfterLast('.', "")
) {
    val isArchive: Boolean
        get() = extension.lowercase() in listOf("zip", "rar", "7z", "tar", "gz", "bz2")
}
