package com.gepetto.toydb.platform

import okio.Path
import okio.Sink

actual object BackupArchive {
    actual fun write(
        sink: Sink,
        textEntries: Map<String, String>,
        photos: List<BackupPhoto>,
        onPhoto: (done: Int, total: Int) -> Unit
    ) {
        throw UnsupportedOperationException("BackupArchive is not supported on web")
    }

    actual fun readTextEntries(archive: Path): Map<String, String> {
        throw UnsupportedOperationException("BackupArchive is not supported on web")
    }

    actual fun extractPhotos(
        archive: Path,
        targetDir: Path,
        modifiedTimes: Map<String, Long>,
        onPhoto: (done: Int, total: Int) -> Unit
    ): Int {
        throw UnsupportedOperationException("BackupArchive is not supported on web")
    }
}
