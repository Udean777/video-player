package app.subtle.player.data.repository

import app.subtle.player.data.storage.FileVideoScanner
import app.subtle.player.domain.model.DirectoryContent
import app.subtle.player.domain.model.Video
import app.subtle.player.domain.repository.StorageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageRepository @Inject constructor(
    private val scanner: FileVideoScanner,
) : StorageRepository {

    override suspend fun allVideos(): List<Video> = scanner.allVideos()

    override suspend fun listDirectory(path: String?): DirectoryContent = scanner.listDirectory(path)
}
