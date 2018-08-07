package com.amaze.filepreloaderlibrary

/**
 * Contains a hash map linking paths to [PreloadedFolder]s.
 */
typealias PreloadedFoldersMap<D> = MutableMap<String, PreloadedFolder<D>>

/**
 * Standard divider for unix
 */
const val DIVIDER = "/"

/**
 * Load NOW (immediately), normally used for things that need to be shown to the user in 1/60s
 */
const val PRIORITY_NOW = 0
/**
 * Load "NEXT immediately", used for thing that are to be shown after 1/60s
 */
const val PRIORITY_NEXT = 1
/**
 * Probably will need to be loaded in the FUTURE
 */
const val PRIORITY_FUTURE = 2
/**
 * There is some small probability this will need to be shown
 */
const val PRIORITY_POSSIBLY = 3