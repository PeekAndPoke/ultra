//package de.peekandpoke.ultra.common.cache
//
//import io.ktor.util.collections.ConcurrentSet
//
//interface CacheEvictor<K, V> {
//    fun add(key: K)
//
//    fun evict(): List<K>
//}
//
//class FifoCacheEvictor<K, V>(val maxSize: Int) : CacheEvictor<K, V> {
//    var queue = mutableListOf<K>()
//
//    override fun add(key: K) {
//        queue.add(key)
//    }
//
//    override fun evict(): List<K> {
//        val overflow = queue.size - maxSize
//
//        if (overflow <= 0) return emptyList()
//
//        val evicted = queue.takeLast(overflow)
//        queue = queue.dropLast(overflow).toMutableList()
//
//        return evicted
//    }
//}
//
//class LifoCacheEvictor<K, V>(val maxSize: Int) : CacheEvictor<K, V> {
//    var queue = mutableListOf<K>()
//
//    override fun add(key: K) {
//        queue.add(key)
//    }
//
//    override fun evict(): List<K> {
//        val overflow = queue.size - maxSize
//
//        if (overflow <= 0) return emptyList()
//
//        val evicted = queue.take(overflow)
//        queue = queue.drop(overflow).toMutableList()
//
//        return evicted
//    }
//}
//
//class LruCacheEvictor<K, V>(val maxSize: Int) : CacheEvictor<K, V> {
//
//
//    var cache = mutableMapOf<K, V>()
//    var lastAccess = mutableMapOf<K, Long>()
//    var size = 0L
//
//    val x = ConcurrentSet<>()
//
//    override fun add(key: K) {}
//}
