package com.amaze.filepreloaderlibrary

import java.io.File

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *              on 10/1/2018, at 15:04.
 */

object FilePreloader {

    /**
     * Preload every subfolder in this [path], the [instantiator] is used to create the [D]: DataContainer
     * objects
     */
    fun <D: DataContainer>preloadFrom(path: String, instantiator: (String) -> D) {
        Processor.workFrom(ProcessUnit(path, instantiator))
    }

    /**
     * Preload folder (denoted by its [path]), the [instantiator] is used to create the
     * [D]: DataContainer objects
     */
    fun <D: DataContainer>preload(path: String, instatiator: (String) -> D) {
        Processor.work(ProcessUnit(path, instatiator))
    }

    fun <D: DataContainer>loadFrom(path: String, instatiator: (String) -> D): List<D> {
        val preloaded = Processor.getLoadedFrom(path)
        if(preloaded != null && preloaded.isNotEmpty()) return preloaded as List<D>//todo fix
        else {
            val list = mutableListOf<D>()
            val file = File(path)

            file.listFiles().forEach { list.addAll(it.list().map(instatiator)) }
            list.addAll(file.parentFile.list().map(instatiator))

            return list
        }
    }

    fun <D: DataContainer>load(path: String, instatiator: (String) -> D): List<D> {
        val preloaded = Processor.getLoaded(path)
        return if(preloaded != null && preloaded.isNotEmpty()) preloaded as List<D>//todo fix
        else File(path).list().map { instatiator.invoke(it) }
    }
}

