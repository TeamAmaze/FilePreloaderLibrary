package com.amaze.filepreloaderlibrary

import android.app.Activity
import android.util.Log
import com.amaze.filepreloaderlibrary.datastructures.DataContainer
import com.amaze.filepreloaderlibrary.datastructures.FetcherFunction
import com.amaze.filepreloaderlibrary.utils.DIVIDER
import com.amaze.filepreloaderlibrary.utils.KFile
import kotlinx.coroutines.experimental.launch

/**
 * This class deals with preloading files for a given type [D], it uses [loader] to perform
 * the actual load.
 */
class SpecializedPreloader<out D: DataContainer>(private val clazz: Class<D>,
                                                 private val fetcher: FetcherFunction<D>) {
    private val loader: Loader<D> = Loader(clazz)

    /**
     * Asynchly preload every subfolder in this [path].
     */
    fun preloadFrom(path: String) {
        loader.loadFrom(ProcessUnit(path, fetcher))
    }

    /**
     * Asynchly preload folder (denoted by its [path]),
     */
    fun preload(path: String) {
        loader.loadFolder(ProcessUnit(path, fetcher))
    }

    /**
     * Get the loaded data. [getList] will run on UI thread.
     */
    fun load(activity: Activity, path: String, getList: (List<D>) -> Unit) {
        launch {
            val t: Pair<Boolean, List<DataContainer>>? = loader.getLoaded(path)

            if (t != null) {
                if(t.first) {
                    activity.runOnUiThread { getList(t.second as List<D>) }
                } else {
                    loader.setCompletionListener(path) {
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
    internal suspend fun clear() = loader.clear()
}