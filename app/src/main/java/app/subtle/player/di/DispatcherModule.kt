package app.subtle.player.di

import app.subtle.player.core.common.DefaultDispatcher
import app.subtle.player.core.common.IoDispatcher
import app.subtle.player.core.common.PlayerDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @PlayerDispatcher
    fun providePlayerDispatcher(): CoroutineDispatcher =
        Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "player") }.asCoroutineDispatcher()
}
