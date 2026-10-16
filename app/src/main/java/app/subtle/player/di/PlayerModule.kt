package app.subtle.player.di

import app.subtle.player.core.player.PlayerControllerFactory
import app.subtle.player.engine.vlc.VlcPlayerControllerFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Only `di/` may reference `engine/vlc`; everything else depends on the contract. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    abstract fun bindPlayerControllerFactory(impl: VlcPlayerControllerFactory): PlayerControllerFactory
}
