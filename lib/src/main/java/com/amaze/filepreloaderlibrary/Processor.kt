package com.amaze.filepreloaderlibrary

import java.io.File
import java.io.FileFilter
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *                      on 10/1/2018, at 17:05.
 */

/**
 * Basically means call `[ProcessUnit].second` on each of `[ProcessUnit].first`'s files.
 * This is done asynchly.
 *
 * @see Processor.workFrom
 * @see Processor.work
 */
typealias ProcessUnit = Pair<String, (String) -> DataContainer>

val DIVIDER = "/"

/**
 * Thread safe.
 * Maps each folder to a list of [ProcessUnit] to preload. The first folder is passed
 * in [Processor.workFrom] or [Processor.work] to get the result of the load folder operation.
 *
 * 'Load a folder' means that the function `[unit].second` will be called
 * on each file (represented by its path) inside the folder.
 */
private val PRELOAD_MAP: MutableMap<String, MutableList<ProcessUnit>> =
        Collections.synchronizedMap(hashMapOf<String, MutableList<ProcessUnit>>())

/**
 * Thread safe.
 * Maps each folder to a list of [DataContainer] with each file's data.
 * The first folder is passed in [Processor.workFrom] or [Processor.work] to get the result of the
 * load folder operation.
 *
 * 'Load a folder' means that the function `[unit].second` will be called
 * on each file (represented by its path) inside the folder.
 */
private val PRELOADED_MAP: MutableMap<String, MutableList<DataContainer>> =
        Collections.synchronizedMap(hashMapOf<String, MutableList<DataContainer>>())

/**
 * Singleton charged with writing to [PRELOAD_MAP] and starting the preload
 * and, afterwards reading from [PRELOADED_MAP] and returning the output.
 */
object Processor {
    /**
     * Asynchly load every folder inside the path `[unit].first` except itself (aka '.').
     * It will even load the parent (aka '..'), as this method implies that the user can go up
     * (in the filesystem tree).
     *
     * 'Load a folder' means that the function `[unit].second` will be called
     * on each file (represented by its path) inside the folder.
     */
    fun workFrom(unit: ProcessUnit) {
        Thread {
            val file = File(unit.first)
            file.listFiles(FileFilter { it.isDirectory })
                    .forEach {
                        for (path in it.list()) {
                            Processor.addToProcess(it.name, ProcessUnit(path, unit.second))
                        }
                    }
            file.parentFile.listFiles()?.forEach {
                Processor.addToProcess(it.name, ProcessUnit(file.parent, unit.second))
            }
            Threader.work()
        }.start()
    }

    /**
     * Asynchly loads the folder in `[unit].first`.
     *
     * 'Load a folder' means that the function `[unit].second` will be called
     * on each file (represented by its path) inside the folder.
     */
    fun work(unit: ProcessUnit) {
        Thread {
            val file = File(unit.first)
            for (path in file.list()) {
                Processor.addToProcess(file.name, ProcessUnit(path, unit.second))
            }
            Threader.work()
        }.start()
    }

    /**
     * Clear everything, all data loaded will be discarded.
     */
    fun cleanUp() {
        PRELOAD_MAP.clear()
        PRELOADED_MAP.clear()
    }

    /**
     * Add file (represented by [unit]) to the [PRELOAD_MAP] to be preloaded by [Threader].
     */
    private fun addToProcess(foldername: String, unit: ProcessUnit) {
        val list = PRELOAD_MAP.get(foldername)

        if(list == null) PRELOAD_MAP.put(foldername, mutableListOf(unit))
        else list.add(unit)
    }

    /**
     * Gets the loaded files inside the folders (except '.') in the folder denoted by the [path].
     * The parameter [clean] indicates if the data should be cleaned from the library's memory.
     *
     * @see workFrom to understand.
     */
    fun getLoadedFrom(path: String, clean: Boolean = true): List<DataContainer>? {
        val completeList = PRELOADED_MAP[path]
        if(clean) PRELOADED_MAP.clear()
        return completeList
    }

    /**
     * Gets the loaded files from the folder denoted by [path].
     *
     * @see work to understand.
     */
    fun getLoaded(path: String, clean: Boolean = true): List<DataContainer>? {
        val completeList = PRELOADED_MAP[path.removeRange(0, path.lastIndexOf(DIVIDER))]
        if(clean) PRELOADED_MAP.clear()
        return completeList
    }

    /**
     * *This function is only to test what data is being preloaded.*
     * Gets all the loaded files inside the folders (except '.') in the folder denoted by the [path].
     * The parameter [clean] indicates if the data should be cleaned from the library's memory.
     *
     * @see workFrom to understand.
     */
    fun getAllData(): List<DataContainer>? {
        val completeList = mutableListOf<DataContainer>()
        PRELOADED_MAP.map { completeList.addAll(it.value) }
        return completeList
    }
}

/**
 * Singleton in charge of creating and loading [LoaderThread] instances to the [LoaderThreadPool]
 * to be executed.
 */
object Threader {
    var executor:ThreadPoolExecutor? = null

    fun work() {
        synchronized(PRELOAD_MAP) {
            executor = LoaderThreadPool()
            executor!!.prestartAllCoreThreads()
            PRELOAD_MAP.forEach {
                val foldername = it.key
                it.value.forEach{ executor?.submit(LoaderThread(foldername, it))}
                //PRELOAD_MAP.remove(it.key) todo fix this, it causes a crash
            }
        }
    }
}

val MAX_THREADS = 10
val KEEP_ALIVE_TIME = 5L

/**
 * The thread pool, each thread is a [LoaderThread].
 */
class LoaderThreadPool() : ThreadPoolExecutor(MAX_THREADS, MAX_THREADS, KEEP_ALIVE_TIME,
        TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())

/**
 * Each [LoaderThread] contains en element whose metadata it will get.
 * Basically runs `[unit].second.invoke([unit].first)` and saves the result.
 */
class LoaderThread(private val foldername:String, private val unit:ProcessUnit): Runnable {
    override fun run() {
        val list:MutableList<DataContainer>? = PRELOADED_MAP.get(foldername)
        val data = unit.second.invoke(unit.first)

        if(list == null) PRELOADED_MAP.put(foldername, mutableListOf(data))
        else list.add(data)
    }
}