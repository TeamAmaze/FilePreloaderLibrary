package com.amaze.filepreloaderlibrary

import java.io.File
import java.io.FileFilter

/**
 * This correctly casts some member functions from [File] into nullable.
 */
class KFile: File {

    constructor(path: String): super(path) {}
    constructor(parentPath: String, path: String): super(parentPath, path) { }

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
    override fun listFiles(filter: FileFilter): Array<KFile>? = listFilesToList { filter.accept(it as File) }?.toTypedArray()

    fun listFilesToList(filter: (KFile) -> Boolean): List<KFile>? {
        val ss = list() ?: return null

        return ss.map {
            KFile(absolutePath, it)
        }.filter {
            filter(it)
        }
    }

    fun listDirectoriesToList() = listFilesToList { it.isDirectory }

    override fun getParent(): String? = super.getParent()

    override fun getParentFile(): KFile? {
        val path = super.getParentFile()?.path ?: return null
        return KFile(path)
    }
}