package com.amaze.filepreloaderlibrary

class PreloadedFolder(private val foldersToContain: Int): HashSet<DataContainer>() {
    override fun add(element: DataContainer): Boolean {
        if(size+1 > foldersToContain) throw IllegalStateException("Too many elements, max is $foldersToContain")
        return super.add(element)
    }

    fun isComplete(): Boolean {
        return size == foldersToContain
    }
}