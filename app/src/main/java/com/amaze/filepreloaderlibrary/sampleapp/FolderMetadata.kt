package com.amaze.filepreloaderlibrary.sampleapp

import com.amaze.filepreloaderlibrary.datastructures.DataContainer
import java.io.File

class FileMetadata(path: String): DataContainer(path) {

    val name: String
    val filePath: String
    val extension: String
    val isDirectory: Boolean

    init {
        val file = File(path)
        name = file.name
        filePath = file.absolutePath
        extension = file.extension
        isDirectory = file.isDirectory
    }

    override fun toString(): String {
        return "'$name': {'$filePath', $isDirectory, *.$extension}"
    }
}