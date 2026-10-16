package app.subtle.player.core.common

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** Serialized dispatcher for all native player-engine calls. Never the main thread. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayerDispatcher
