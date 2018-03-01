package com.amaze.filepreloaderlibrary

/**
 * This is a [HashSet] with completeness checks.
 */
class PreloadedFolder <D: DataContainer> (private val foldersToContain: Int): HashSet<D>() {
    override fun add(element: D): Boolean {
        if(size+1 > foldersToContain) throw IllegalStateException("Too many elements, max is $foldersToContain")
        return super.add(element)
    }

    fun isComplete(): Boolean {
        return size == foldersToContain
    }
}