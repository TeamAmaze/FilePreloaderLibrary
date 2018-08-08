package com.amaze.filepreloaderlibrary

import java.io.File
import java.io.FileFilter

/**
 * This correctly casts some member functions from [File] into nullable.
 */
class KFile(path: String): File(path) {

    override fun list(): Array<String>? = super.list()

    /**
     * @deprecated Use [listFilesToList]
     */
    override fun listFiles(): Array<KFile>? = listFilesToList()?.toTypedArray()

    fun listFilesToList(): List<KFile>? {
        val ss = list() ?: return null

        return ss.map {
            KFile(it)
        }
    }

    /**
     * @deprecated Use [listFilesToList]
     */
    override fun listFiles(filter: FileFilter): Array<KFile>? = listFilesToList(filter)?.toTypedArray()

    fun listFilesToList(filter: FileFilter): List<KFile>? {
        val ss = list() ?: return null

        return ss.map {
            KFile(it)
        }.filter {
            filter.accept(it)
        }
    }

    fun listDirectoriesToList() = listFilesToList(FileFilter { it.isDirectory })

    override fun getParent(): String? = super.getParent()

    override fun getParentFile(): KFile? {
        val path = super.getParentFile()?.path ?: return null
        return KFile(path)
    }
}