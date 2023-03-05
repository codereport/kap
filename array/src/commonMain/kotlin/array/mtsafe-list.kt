package array

import kotlin.jvm.Volatile

class MTSafeArrayList<T>() : AbstractMutableList<T>() {
    @Volatile
    private var list = ArrayList<T>()
    private val lock = MPLock()

    override val size get() = list.size

    private inline fun <R> withImpl(fn: (ArrayList<T>) -> R): R {
        return lock.withLocked {
            fn(list)
        }
    }

    override fun add(element: T): Boolean {
        return withImpl { l ->
            addAtIndexInternal(l, l.size, element)
            true
        }
    }

    override fun add(index: Int, element: T) {
        withImpl { l ->
            addAtIndexInternal(l, index, element)
        }
    }

    private fun addAtIndexInternal(l: ArrayList<T>, index: Int, element: T) {
        val updated = ArrayList<T>(l.size + 1)
        updated.addAll(l.subList(0, index))
        updated.add(element)
        updated.addAll(l.subList(index, l.size))
        list = updated
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        return withImpl { l ->
            addAllInternal(l, elements, index)
        }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        return withImpl { l ->
            addAllInternal(l, elements, l.size)
        }
    }

    private fun addAllInternal(l: ArrayList<T>, elements: Collection<T>, index: Int): Boolean {
        return if (elements.isEmpty()) {
            false
        } else {
            val updated = ArrayList<T>(l.size + elements.size)
            updated.addAll(l.subList(0, index))
            updated.addAll(elements)
            updated.addAll(l.subList(index, l.size))
            list = updated
            true
        }
    }

    override fun get(index: Int) = list[index]

    override fun removeAt(index: Int): T = withImpl { l ->
        val updated = ArrayList<T>(l.size - 1)
        updated.addAll(l.subList(0, index))
        updated.addAll(l.subList(index + 1, l.size))
        list = updated
        l[index]
    }

    override fun set(index: Int, element: T): T = withImpl { l ->
        val updated = ArrayList<T>(l.size)
        updated.addAll(l.subList(0, index))
        updated.add(element)
        updated.addAll(l.subList(index + 1, l.size))
        list = updated
        l[index]
    }

    override fun clear() {
        withImpl {
            list = ArrayList()
        }
    }

    override fun contains(element: T): Boolean {
        return list.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return list.containsAll(elements)
    }

    override fun indexOf(element: T): Int {
        return list.indexOf(element)
    }

    override fun isEmpty(): Boolean {
        return list.isEmpty()
    }

    override fun lastIndexOf(element: T): Int {
        return list.lastIndexOf(element)
    }

    override fun remove(element: T): Boolean {
        return withImpl { l ->
            val updated = ArrayList<T>(l.size)
            var found = false
            l.forEach { e ->
                if (!found && e == element) {
                    found = true
                } else {
                    updated.add(e)
                }
            }
            list = updated
            found
        }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return withImpl { l ->
            val updated = ArrayList<T>(l.size)
            var found = false
            l.forEach { e ->
                if (elements.contains(e)) {
                    found = true
                } else {
                    updated.add(e)
                }
            }
            list = updated
            found
        }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        return withImpl { l ->
            val updated = ArrayList<T>(l.size)
            var wasUpdated = false
            l.forEach { e ->
                if (!elements.contains(e)) {
                    wasUpdated = true
                } else {
                    updated.add(e)
                }
            }
            list = updated
            wasUpdated
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        return list.subList(fromIndex, toIndex)
    }

    override fun iterator(): MutableIterator<T> {
        return MTSafeIterator(list)
    }

    override fun listIterator(): MutableListIterator<T> {
        return MTSafeListIterator(list, 0)
    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        return MTSafeListIterator(list, index)
    }
}

private class MTSafeIterator<T>(list: ArrayList<T>) : MutableIterator<T> {
    private val impl = list.iterator()

    override fun hasNext() = impl.hasNext()
    override fun next() = impl.next()
    override fun remove() = throw UnsupportedOperationException()
}

private class MTSafeListIterator<T>(list: ArrayList<T>, index: Int) : MutableListIterator<T> {
    private val impl = list.listIterator(index)

    override fun add(element: T) = throw UnsupportedOperationException()
    override fun hasNext() = impl.hasNext()
    override fun hasPrevious() = impl.hasPrevious()
    override fun next() = impl.next()
    override fun nextIndex() = impl.nextIndex()
    override fun previous() = impl.previous()
    override fun previousIndex() = impl.previousIndex()
    override fun remove() = throw UnsupportedOperationException()
    override fun set(element: T) = throw UnsupportedOperationException()
}
