package com.amaze.filepreloaderlibrary

import java.io.File
import java.util.*

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *                      on 10/1/2018, at 17:05.
 */

typealias ProcessUnit = Pair<String, (String) -> DataContainer>

private val PRELOAD_LIST: MutableList<ProcessUnit> = Collections.synchronizedList(mutableListOf())
private val PRELOADED_MAP: MutableMap<String, DataContainer> =
        Collections.synchronizedMap(hashMapOf<String, DataContainer>())

object Processor {
    fun work(unit: ProcessUnit) {
        Thread() {
            val file = File(unit.first)
            for (path in file.list()) {
                Processor.addToProcess(ProcessUnit(path, unit.second))
            }
            Threader.work()
        }.start()
    }

    fun cleanUp() {
        PRELOAD_LIST.clear()
        PRELOADED_MAP.clear()
    }

    private fun addToProcess(s: ProcessUnit) = PRELOAD_LIST.add(s)

    fun getFromProcess(path: String): List<DataContainer>? {
        return PRELOADED_MAP.map { it.value }//fixme not return whole map
    }
}

object Threader {
    val MAX_WORKLOAD_PER_THREAD = 100

    fun work() {
        synchronized(PRELOAD_LIST) {
            var lastAmount = 0

            for(i in 0..PRELOAD_LIST.size step MAX_WORKLOAD_PER_THREAD) {
                if(i + MAX_WORKLOAD_PER_THREAD >= PRELOAD_LIST.size) {
                    lastAmount = i
                    break
                }
                LoaderThread(PRELOAD_LIST.subList(i, i + MAX_WORKLOAD_PER_THREAD)).start()
            }

            if(lastAmount > 0) {
                LoaderThread(PRELOAD_LIST.subList(lastAmount, PRELOAD_LIST.size-1)).start()
            }
        }
    }
}

class LoaderThread(private val units:List<ProcessUnit>): Thread() {
    override fun run() {
        for (unit in units) {
            PRELOADED_MAP.put(unit.first, unit.second.invoke(unit.first))
        }
    }
}