package com.amaze.filepreloaderlibrary

import java.io.File

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *                      on 10/1/2018, at 16:40.
 */
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