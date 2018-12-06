package com.amaze.filepreloaderlibrary

import com.amaze.filepreloaderlibrary.PreloadedManager.getDeleteQueue
import com.amaze.filepreloaderlibrary.PreloadedManager.getPreloadMap
import com.amaze.filepreloaderlibrary.PreloadedManager.getPreloadMapMutex
import com.amaze.filepreloaderlibrary.datastructures.*
import com.amaze.filepreloaderlibrary.utils.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
* Basically means call `[ProcessUnit].fetcherFunction` on each of `[ProcessUnit].path`'s files.
* This is done asynchly.
*
* @see Processor.work
*/
internal data class ProcessUnit<out D: DataContainer>(val path: String, val fetcherFunction: FetcherFunction<D>)

/**
 * Contains the [ProcessedUnit].metadataObject for a file inside [ProcessedUnit].path
 */
internal data class ProcessedUnit<out D: DataContainer>(val path: String, val metadataObject: D)

/**
 * Singleton charged with writing to [preloadPriorityQueue] and starting the preload
 * and, afterwards reading from [preloadedList] and returning the output.
 */
internal class Processor<D: DataContainer>(private val clazz: Class<D>) {

    init {
        if(PreloadedManager.get(clazz) == null) {
            PreloadedManager.add(clazz)
        }
    }

    /**
     * Thread safe.
     * All the callable executions to load all the folders.
     *
     * 'Load a folder' means that the function `[unit].second` will be called
     * on each file (represented by its path) inside the folder.
     */
    private val preloadPriorityQueue: UniquePriorityBlockingQueue<PreloadableUnit<D>> = UniquePriorityBlockingQueue()

    private val isWorking = AtomicBoolean(false)

    /**
     * Calls each function in [preloadPriorityQueue] (removing it).
     * Then adds the result [(path, data)] to `[getPreloadMap].get(path)`.
     */
    internal fun work() {
        if(isWorking.get()) return
        isWorking.set(true)

        GlobalScope.launch {
            while (preloadPriorityQueue.isNotEmpty()) {
                val elem = preloadPriorityQueue.poll() ?: throw IllegalStateException("Polled element cannot be null!")
                val (path, data) = elem.future.await()

                DebugLog.log("FilePreloader.Processor", "[P${elem.priority}] Loading from $path: $data")

                val list = getPreloadMap(clazz)[path]
                        ?: throw IllegalStateException("A list has been deleted before elements were added. We are VERY out of memory!")
                list.add(data)
            }

            isWorking.set(false)
        }
    }

    internal suspend fun add(element: PreloadableUnit<D>) {
        preloadPriorityQueue.add(element)
    }

    /**
     * Clear everything, all data loaded will be discarded.
     */
    internal suspend fun clear() {
        preloadPriorityQueue.clear()
    }

}