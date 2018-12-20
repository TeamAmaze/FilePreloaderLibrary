package com.amaze.filepreloaderlibrary

import android.app.Activity
import com.amaze.filepreloaderlibrary.datastructures.DataContainer
import com.amaze.filepreloaderlibrary.datastructures.FetcherFunction
import com.amaze.filepreloaderlibrary.utils.LIB_CONTEXT
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Use this class to interact with the library.
 */
object FilePreloader {

    var DEBUG = true

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
    inline fun <reified D: DataContainer>with(noinline f: FetcherFunction<D>): SpecializedPreloader<D> {
        val v = SpecializedPreloader(D::class.java, f)
        weakList.add(WeakReference(v))
        return v
    }

    /**
     * For compatibity with Java
     *
     * @see [with].
     */
    fun <D: DataContainer>with(clazz: Class<D>, f: FetcherFunction<D>): SpecializedPreloader<D> {
        val v = SpecializedPreloader(clazz, f)
        weakList.add(WeakReference(v))
        return v
    }

    /**
     * *ONLY USE FOR DEBUGGING*
     * Get all the loaded data, this will load the data in the current thread if it's not loaded.
     */
    fun getAllDataLoaded(activity: Activity, getList: (List<DataContainer>?) -> Unit) {
        GlobalScope.launch(LIB_CONTEXT) {
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
        GlobalScope.launch(LIB_CONTEXT) {
            weakList.forEach {
                it.get()?.clear()
            }
        }
    }
}

