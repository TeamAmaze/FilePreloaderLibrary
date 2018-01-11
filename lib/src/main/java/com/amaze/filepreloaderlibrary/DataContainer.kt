package com.amaze.filepreloaderlibrary

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *                  on 10/1/2018, at 15:10.
 */

class FolderMetadata<D: DataContainer> (path: String) {
    val files: MutableList<D> = mutableListOf()

    fun add(data: D) {
        files.add(data)
    }

}

abstract class DataContainer(val path: String)