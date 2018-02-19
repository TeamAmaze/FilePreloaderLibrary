package com.amaze.filepreloaderlibrary

import android.app.Activity
import kotlinx.coroutines.experimental.launch
import java.io.File

/**
 * Use this class to interact with the library.
 */
object FilePreloader {

    /**
     * Asynchly preload every subfolder in this [path] (exept '.'),
     * the [instantiator] is used to create the `[D]: DataContainer` objects.
     */
    fun <D: DataContainer>preloadFrom(path: String, instantiator: (String) -> D) {
        Processor.workFrom(ProcessUnit(path, instantiator))
    }

    /**
     * Asynchly preload folder (denoted by its [path]),
     * the [instantiator] is used to create the `[D]: DataContainer` objects
     */
    fun <D: DataContainer>preload(path: String, instantiator: (String) -> D) {
        Processor.work(ProcessUnit(path, instantiator))
    }

    /**
     * Get the loaded data, this will load the data in the current thread if it's not loaded.
     *
     * @see preload
     */
    fun <D: DataContainer>load(activity: Activity, path: String, instatiator: (String) -> D,
                               getList: (List<D>) -> Unit) {
        launch {
            val t: Pair<Boolean, List<DataContainer>>? = Processor.getLoaded(path)

            if (t != null && t.first) {
                activity.runOnUiThread { getList(t.second as List<D>) }
            } else {
                var path = path
                if (!path.endsWith(DIVIDER)) path += DIVIDER

                val list = File(path).list()?.map { instatiator.invoke(path + it) } ?: listOf()

                activity.runOnUiThread { getList(list) }
            }
        }
    }

    /**
     * *This function is only to test what data is being preloaded.*
     * Get all the loaded data, this will load the data in the current thread if it's not loaded.
     */
    fun <D: DataContainer>getAllDataLoaded(activity: Activity, getList: (List<D>?) -> Unit) {
        launch {
            val preloaded = Processor.getAllData()

            activity.runOnUiThread {
                if (preloaded != null && preloaded.isNotEmpty()) getList(preloaded as List<D>)//todo fix
                else getList(null)
            }
        }
    }
}

