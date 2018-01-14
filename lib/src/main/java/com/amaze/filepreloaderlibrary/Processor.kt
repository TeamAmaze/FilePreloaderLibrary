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

typealias ProcessUnit = Pair<String, (String) -> DataContainer>

val DIVIDER = "/"

private val PRELOAD_MAP: MutableMap<String, MutableList<ProcessUnit>> = Collections.synchronizedMap(hashMapOf<String, MutableList<ProcessUnit>>())
private val PRELOADED_MAP: MutableMap<String, MutableList<DataContainer>> =
        Collections.synchronizedMap(hashMapOf<String, MutableList<DataContainer>>())

object Processor {
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

    fun work(unit: ProcessUnit) {
        Thread {
            val file = File(unit.first)
            for (path in file.list()) {
                Processor.addToProcess(file.name, ProcessUnit(path, unit.second))
            }
            Threader.work()
        }.start()
    }

    fun cleanUp() {
        PRELOAD_MAP.clear()
        PRELOADED_MAP.clear()
    }

    private fun addToProcess(foldername: String, unit: ProcessUnit) {
        val list = PRELOAD_MAP.get(foldername)

        if(list == null) PRELOAD_MAP.put(foldername, mutableListOf(unit))
        else list.add(unit)
    }

    fun getLoadedFrom(path: String): List<DataContainer>? {
        val completeList = mutableListOf<DataContainer>()
        PRELOADED_MAP.map { completeList.addAll(it.value) }
        return completeList
    }

    fun getLoaded(path: String): List<DataContainer>? {
        return PRELOADED_MAP.get(path.removeRange(0, path.lastIndexOf(DIVIDER)))
    }
}

object Threader {
    var executor:ThreadPoolExecutor? = null

    fun work() {
        synchronized(PRELOAD_MAP) {
            executor = LoaderThreadPool()
            executor!!.prestartAllCoreThreads()
            PRELOAD_MAP.forEach {
                val foldername = it.key
                it.value.forEach{ executor?.submit(LoaderThread(foldername, it))}
            }
        }
    }
}

val MAX_THREADS = 10
val KEEP_ALIVE_TIME = 5L

class LoaderThreadPool() : ThreadPoolExecutor(MAX_THREADS, MAX_THREADS, KEEP_ALIVE_TIME,
        TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())

class LoaderThread(private val foldername:String, private val unit:ProcessUnit): Runnable {
    override fun run() {
        val list:MutableList<DataContainer>? = PRELOADED_MAP.get(foldername)
        val data = unit.second.invoke(unit.first)

        if(list == null) PRELOADED_MAP.put(foldername, mutableListOf(data))
        else list.add(data)
    }
}