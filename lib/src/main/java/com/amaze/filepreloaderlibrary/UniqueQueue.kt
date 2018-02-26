package com.amaze.filepreloaderlibrary

import java.util.*

/**
 * @see Processor.deletionQueue
 */
class UniqueQueue {

    private val lock: Any = Any()
    private val queue: Queue = Queue()
    private val map: HashMap<String, Queue.Node> = HashMap()

    fun add(path: String) = synchronized(lock) {
        val n = queue.addFirst(path)

        val v = map[path]
        if (v != null) queue.remove(v)

        map[path] = n
    }

    fun clear() = synchronized(lock) {
        queue.clear()
        map.clear()
    }

    fun remove(): String = synchronized(lock) { queue.remove() }

    fun isEmpty(): Boolean = synchronized(lock) { queue.isEmpty() }

    /**
     * This a queue implemented with a linked list.
     *
     * I am reinventing the wheel because [LinkedList].remove(E) is O(n),
     * here [Queue].remove(Node) is O(1).
     */
    private class Queue {
        private var first: Node? = null
        private var last: Node? = null

        data class Node(var next: Node?, var last: Node?, val data: String) {
            override fun toString() = "\'$data\'"
        }

        fun addFirst(path: String): Node {
            val n = Node(first, null, path)
            if (last == null) last = n
            first?.last = n
            first = n
            return n
        }

        fun remove(): String {
            val n = last ?: throw NoSuchElementException()
            if(n.last != null) {
                last = n.last
                last!!.next = null
            } else {
                first = null
            }
            return n.data
        }

        fun remove(n: Node) {
            n.last?.next = n.next
            n.next?.last = n.last
        }

        fun isEmpty() = last == null

        fun clear() {
            first = null
            last = null
        }

        override fun toString(): String {
            val str = StringBuilder()
            var next: Node? = first
            while(next != null) {
                str.append("$next->")
                next = next.next
            }
            return str.removeSuffix("->").toString()
        }
    }
}

