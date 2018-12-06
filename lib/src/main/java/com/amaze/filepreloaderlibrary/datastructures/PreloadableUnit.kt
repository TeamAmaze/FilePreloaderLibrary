package com.amaze.filepreloaderlibrary.datastructures

import com.amaze.filepreloaderlibrary.ProcessedUnit
import kotlinx.coroutines.Deferred

internal data class PreloadableUnit<D: DataContainer>(val future: Deferred<ProcessedUnit<D>>, val priority: Int, val hash: Int): Comparable<PreloadableUnit<D>> {
    override fun compareTo(other: PreloadableUnit<D>) = priority.compareTo(other.priority)
    override fun hashCode() = hash
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreloadableUnit<*>

        if (hash != other.hash) return false

        return true
    }
}