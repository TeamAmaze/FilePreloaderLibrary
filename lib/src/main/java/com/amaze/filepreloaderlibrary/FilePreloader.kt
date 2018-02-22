package com.amaze.filepreloaderlibrary

import android.app.Activity
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

/**
 * Use this class to interact with the library.
 */
object FilePreloader {
    private val weakList: MutableSet<WeakReference<SpecializedPreloader<DataContainer>>> = mutableSetOf()

    fun <D: DataContainer>with(f: (String) -> D): SpecializedPreloader<D> {
        val v = SpecializedPreloader(FetcherFunction(f))
        weakList.add(WeakReference(v))
        return v
    }

    /**
     * *This function is only to test what data is being preloaded.*
     * Get all the loaded data, this will load the data in the current thread if it's not loaded.
     */
    fun getAllDataLoaded(activity: Activity, getList: (List<DataContainer>?) -> Unit) {
        launch {
            val preloaded = mutableListOf<DataContainer>()
            weakList.forEach {
                it.get()?.getAllData()?.forEach { preloaded.add(it) }
            }

            activity.runOnUiThread {
                if (preloaded.isNotEmpty()) getList(preloaded)//todo fix
                else getList(null)
            }
        }
    }

    /**
     * Clear everything, all data loaded will be discarded.
     */
    fun cleanUp() {
        weakList.forEach {
            it.get()?.clear()
        }
    }
}

