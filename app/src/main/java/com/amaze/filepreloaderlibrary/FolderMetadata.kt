package com.amaze.filepreloaderlibrary

import java.io.File

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *                      on 10/1/2018, at 16:40.
 */
class FileMetadata(path: String): DataContainer(path) {
    val file = File(path)

    override fun toString(): String {
        return "'${file.name}': {'${file.absolutePath}', ${file.isDirectory}, *.${file.extension}}"
    }
}