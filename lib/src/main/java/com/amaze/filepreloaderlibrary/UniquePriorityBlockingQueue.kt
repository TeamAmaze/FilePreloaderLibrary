package com.amaze.filepreloaderlibrary

import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import java.util.*
import java.util.concurrent.PriorityBlockingQueue
import kotlin.collections.HashSet

internal class UniquePriorityBlockingQueue<E: PreloadableUnit<*>> {
    private val preloadPriorityQueue: PriorityBlockingQueue<E> = PriorityBlockingQueue()
    private val containedCheck = Collections.synchronizedSet(HashSet<E>())
    private val mutex = Mutex()

    suspend fun add(element: E) = mutex.withLock {
        if(containedCheck.contains(element)) {
            preloadPriorityQueue.forEach {
                if(it == element) {
                    it.future.cancel()
                }
            }
        } else {
            containedCheck.add(element);
        }

        preloadPriorityQueue.add(element)
    }

    suspend fun poll(): E? = mutex.withLock {
        val element = preloadPriorityQueue.poll()
        containedCheck.remove(element)
        return element
    }

    suspend fun clear() = mutex.withLock {
        preloadPriorityQueue.clear()
        preloadPriorityQueue.clear()
    }

    fun isNotEmpty() = containedCheck.isNotEmpty()
}