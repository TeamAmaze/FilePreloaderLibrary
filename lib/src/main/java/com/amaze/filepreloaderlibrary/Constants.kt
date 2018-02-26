package com.amaze.filepreloaderlibrary

import java.io.File
import java.io.FileFilter

/**
 * Contains a hash map linking paths to [PreloadedFolder]s.
 */
typealias PreloadedFoldersMap<D> = MutableMap<String, PreloadedFolder<D>>

/**
 * Standard divider for unix
 */
const val DIVIDER = "/"

/**
 * This correctly casts some member functions from [File] into nullable.
 * The cost is O(N) for Arrays and Lists and O(1) for single element returns
 */
class KFile(path: String): File(path) {

    override fun list(): Array<String>? = super.list()

    override fun listFiles(): Array<KFile>? {
        val a: Array<File> = super.listFiles() ?: return null
        return Array(a.size, { KFile(a[it].path) })
    }

    override fun listFiles(filter: FileFilter): Array<KFile>? {
        val a: Array<File> = super.listFiles(filter) ?: return null
        return Array(a.size, { KFile(a[it].path) })
    }

    override fun getParent(): String? = super.getParent()

    override fun getParentFile(): KFile? {
        val path = super.getParentFile()?.path ?: return null
        return KFile(path)
    }
}