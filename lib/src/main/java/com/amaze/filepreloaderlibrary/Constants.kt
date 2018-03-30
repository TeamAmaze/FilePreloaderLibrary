package com.amaze.filepreloaderlibrary

/**
 * Contains a hash map linking paths to [PreloadedFolder]s.
 */
typealias PreloadedFoldersMap<D> = MutableMap<String, PreloadedFolder<D>>

/**
 * Standard divider for unix
 */
const val DIVIDER = "/"