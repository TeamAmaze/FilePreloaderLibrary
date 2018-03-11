package com.amaze.filepreloaderlibrary

/**
 * This is a [HashSet] with completeness checks.
 */
class PreloadedFolder <D: DataContainer> (private val foldersToContain: Int): HashSet<D>() {

    internal var listener: ((PreloadedFolder<D>)->Unit)? = null

    override fun add(element: D): Boolean {
        if(size+1 > foldersToContain) throw IllegalStateException("Too many elements, max is $foldersToContain")
        val r = super.add(element)
        if(isComplete()) listener?.invoke(this)
        return r
    }

    fun isComplete(): Boolean {
        return size == foldersToContain
    }
}