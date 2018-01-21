package com.amaze.filepreloaderlibrary

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *                      on 15/1/2018, at 14:05.
 */ 

class PreloadedFolder(private val foldersToContain: Int): HashSet<DataContainer>() {
    override fun add(element: DataContainer): Boolean {
        return super.add(element)
    }

    fun isComplete(): Boolean {
        return size == foldersToContain
    }
}