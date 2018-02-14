package com.amaze.filepreloaderlibrary

class PreloadedFolder(private val foldersToContain: Int): HashSet<DataContainer>() {
    override fun add(element: DataContainer): Boolean {
        return super.add(element)
    }

    fun isComplete(): Boolean {
        return size == foldersToContain
    }
}