package com.amaze.filepreloaderlibrary

import android.app.Activity
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

/**
 * Use this class to interact with the library.
 */
object FilePreloader {

    var DEBUG = false

    /**
     * Save a [WeakReference] to every [SpecializedPreloader] created so that [getAllDataLoaded] and
     * [cleanUp] can be called.
     */
    val weakList: MutableSet<WeakReference<SpecializedPreloader<DataContainer>>> = mutableSetOf()

    /**
     * This gets the object type [D] and function [f] to load data.
     * Reification is so that the type of [D] can be known, and the correct object can
     * be loaded ([SpecializedPreloader.preload]) and returned ([SpecializedPreloader.load]).
     *
     * @see [PreloadedManager].
     */
    inline fun <reified D: DataContainer>with(noinline f: (String) -> D): SpecializedPreloader<D> {
        val v = SpecializedPreloader(D::class.java, FetcherFunction(f))
        weakList.add(WeakReference(v))
        return v
    }

    /**
     * *ONLY USE FOR DEBUGGING*
     * Get all the loaded data, this will load the data in the current thread if it's not loaded.
     */
    fun getAllDataLoaded(activity: Activity, getList: (List<DataContainer>?) -> Unit) {
        launch {
            val preloaded = PreloadedManager.getAllLoaded()

            activity.runOnUiThread {
                if (preloaded.isNotEmpty()) getList(preloaded)
                else getList(null)
            }
        }
    }

    /**
     * Clear everything, all metadata loaded will be discarded.
     */
    fun cleanUp() {
        weakList.forEach {
            it.get()?.clear()
        }
    }
}

