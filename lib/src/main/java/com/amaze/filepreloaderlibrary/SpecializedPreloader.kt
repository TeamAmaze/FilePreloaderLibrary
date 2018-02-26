package com.amaze.filepreloaderlibrary

import android.app.Activity
import kotlinx.coroutines.experimental.launch

class SpecializedPreloader<out D: DataContainer>(private val clazz: Class<D>,
                                                 private val fetcher: FetcherFunction<D>) {
    private val processor: Processor<D> = Processor(clazz)

    /**
     * Asynchly preload every subfolder in this [path] (exept '.'),
     * the [instantiator] is used to create the `[D]: DataContainer` objects.
     */
    fun preloadFrom(path: String) {
        processor.workFrom(ProcessUnit(path, fetcher))
    }

    /**
     * Asynchly preload folder (denoted by its [path]),
     * the [instantiator] is used to create the `[D]: DataContainer` objects
     */
    fun preload(path: String) {
        processor.work(ProcessUnit(path, fetcher))
    }

    /**
     * Get the loaded data, this will load the data in the current thread if it's not loaded.
     *
     * @see preload
     */
    fun load(activity: Activity, path: String, getList: (List<D>) -> Unit) {
        launch {
            val t: Pair<Boolean, List<DataContainer>>? = processor.getLoaded(path)

            if (t != null && t.first) {
                activity.runOnUiThread { getList(t.second as List<D>) }
            } else {
                var path = path
                if (!path.endsWith(DIVIDER)) path += DIVIDER

                val list = KFile(path).list()?.map { fetcher.process(path + it) } ?: listOf()

                activity.runOnUiThread { getList(list) }
            }
        }
    }

    suspend fun getAllData() = processor.getAllData()

    internal fun clear() = processor.clear()
}