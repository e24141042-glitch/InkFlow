package com.vic.inkflow.util

import android.graphics.Bitmap
import android.util.LruCache

object ThumbnailCacheManager {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    // Use 1/16th of the available memory for this document cover cache.
    private val cacheSize = maxMemory / 16
    
    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            // The cache size will be measured in kilobytes rather than number of items.
            return bitmap.byteCount / 1024
        }
    }

    fun get(key: String): Bitmap? {
        return cache.get(key)
    }

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
    
    fun remove(key: String) {
        cache.remove(key)
    }

    fun clear() {
        cache.evictAll()
    }
}