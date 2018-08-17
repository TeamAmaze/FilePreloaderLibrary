package com.amaze.filepreloaderlibrary

import android.app.Activity
import android.util.Log
import kotlinx.coroutines.experimental.launch

/**
 * This class deals with preloading files for a given type [D], it uses [processor] to perform
 * the actual load.
 */
class SpecializedPreloader<out D: DataContainer>(private val clazz: Class<D>,
                                                 private val fetcher: FetcherFunction<D>) {
    private val processor: Processor<D> = Processor(clazz)

    /**
     * Asynchly preload every subfolder in this [path].
     */
    fun preloadFrom(path: String) {
        processor.workFrom(ProcessUnit(path, fetcher))
    }

    /**
     * Asynchly preload folder (denoted by its [path]),
     */
    fun preload(path: String) {
        processor.work(ProcessUnit(path, fetcher))
    }

    /**
     * Get the loaded data. [getList] will run on UI thread.
     */
    fun load(activity: Activity, path: String, getList: (List<D>) -> Unit) {
        launch {
            val t: Pair<Boolean, List<DataContainer>>? = processor.getLoaded(path)

            if (t != null) {
                if(t.first) {
                    activity.runOnUiThread { getList(t.second as List<D>) }
                } else {
                    processor.setCompletionListener(path) {
                        it ?: throw NullPointerException()
                        activity.runOnUiThread { getList(it) }
                    }
                }
            } else {
                var path = path
                if (!path.endsWith(DIVIDER)) path += DIVIDER

                val list = KFile(path).list()?.map {
                    val data = fetcher(path + it)
                    Log.w("FilePreloader.Special", "Manually loaded $data!")
                    return@map data
                } ?: listOf()

                activity.runOnUiThread { getList(list) }
            }
        }
    }

    /**
     * This function clears every file metadata loaded by this [SpecializedPreloader].
     * It's usage is not recommended as the [Processor] already has a more efficient cleaning
     * algorithm (see [Processor.deletionQueue]).
     */
    internal fun clear() = processor.clear()
}