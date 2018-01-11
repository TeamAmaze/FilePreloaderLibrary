package com.amaze.filepreloaderlibrary

import java.io.File

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *              on 10/1/2018, at 15:04.
 */

object FilePreloader {
    fun <D: DataContainer>preload(path: String, instatiator: (String) -> D) {
        Processor.work(ProcessUnit(path, instatiator))
    }

    fun <D: DataContainer>load(path: String, instatiator: (String) -> D): List<D> {
        val preloaded = Processor.getFromProcess(path)
        return if(preloaded != null && preloaded.isNotEmpty()) preloaded as List<D>//todo fix
        else File(path).list().map { instatiator.invoke(it) }
    }
}

