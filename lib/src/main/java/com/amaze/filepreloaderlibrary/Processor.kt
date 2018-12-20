package com.amaze.filepreloaderlibrary

import com.amaze.filepreloaderlibrary.PreloadedManager.getPreloadMap
import com.amaze.filepreloaderlibrary.datastructures.*
import com.amaze.filepreloaderlibrary.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel

import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
* Basically means call `[ProcessUnit].fetcherFunction` on each of `[ProcessUnit].path`'s files.
* This is done asynchly.
*
* @see Processor.workHighPriority
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

    private val ranCoroutines = Collections.synchronizedSet(hashSetOf<Job>())
    private val workingWithHighPriority = AtomicInteger(0)

    /**
     * Calls each function in [preloadPriorityQueue] (removing it).
     * Then adds the result [(path, data)] to `[getPreloadMap].get(path)`.
     */
    internal fun workHighPriority(producer: ReceiveChannel<PreloadableUnit<D>?>) {
        work(producer, {
            workingWithHighPriority.incrementAndGet()
        }) {
            workingWithHighPriority.decrementAndGet()
        }
    }

    internal fun workLowPriority(producer: ReceiveChannel<PreloadableUnit<D>?>) {
        work(producer, {
            while (workingWithHighPriority.get() != 0) yield()
        })
    }

    private fun work(producer: ReceiveChannel<PreloadableUnit<D>?>, onStart: suspend () -> Unit, onEnd: suspend () -> Unit  = {}) {
        val job = GlobalScope.launch(LIB_CONTEXT) {
            onStart()

            for (elem in producer) {
                elem ?: throw IllegalStateException("Polled element cannot be null!")
                val (path, data) = elem.future.await()

                DebugLog.log("FilePreloader.Processor", "[P${elem.priority}] Loading from $path: $data")

                val list = getPreloadMap(clazz)[path]
                        ?: throw IllegalStateException("A list has been deleted before elements were added. We are VERY out of memory!")
                list.add(data)
            }

            onEnd()
        }

        ranCoroutines.add(job)

        job.invokeOnCompletion {
            ranCoroutines.remove(job)
        }
    }

    internal fun clear() {
        GlobalScope.launch(LIB_CONTEXT) {
            ranCoroutines.forEach {
                it.cancelAndJoin()
            }

            ranCoroutines.clear()
        }
    }

}