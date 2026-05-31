package com.data.remote.client

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * De-duplicates identical in-flight NewsAPI calls.
 *
 * Example: if Home and Search request the same top-headlines page at the same time,
 * only the first call reaches the network and the other awaits the same result.
 */
object NewsApiRequestCoordinator {
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<Any?>>()

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> runDeduplicated(
        requestKey: String,
        block: suspend () -> T
    ): T = coroutineScope {
        var createdByThisCaller = false

        val deferred = mutex.withLock {
            val existing = inFlight[requestKey]
            if (existing != null) {
                existing as Deferred<T>
            } else {
                async(start = CoroutineStart.LAZY) { block() as Any? }.also { created ->
                    inFlight[requestKey] = created
                    createdByThisCaller = true
                } as Deferred<T>
            }
        }

        if (createdByThisCaller) deferred.start()

        try {
            deferred.await()
        } finally {
            if (createdByThisCaller) {
                mutex.withLock {
                    if (inFlight[requestKey] === deferred) {
                        inFlight.remove(requestKey)
                    }
                }
            }
        }
    }
}
