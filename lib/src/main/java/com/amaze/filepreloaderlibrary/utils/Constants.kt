package com.amaze.filepreloaderlibrary.utils

import android.os.AsyncTask
import com.amaze.filepreloaderlibrary.datastructures.PreloadedFolder
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlin.math.ceil

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
 * Probably will need to be loaded in the FUTURE
 */
const val PRIORITY_FUTURE = 1

private val CORES_AVAILABLE = Runtime.getRuntime().availableProcessors()

/**
 * Normally coroutines use up all the CPUs available, wasting resources, like energy.
 * This limits the amount of threads to at most half +1 of the cores of the processor.
 */
@ObsoleteCoroutinesApi
val LIB_CONTEXT = newFixedThreadPoolContext(ceil(CORES_AVAILABLE/2.toFloat()).toInt(), "FilePreloaderLibrary")