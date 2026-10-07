package com.gepetto.toydb.platform

import okio.Path
import okio.Sink

data class BackupPhoto(val entryName: String, val source: Path, val modified: Long)

expect object BackupArchive {
    /** Streams [textEntries] (UTF-8) then [photos] into [sink]. Sets ZipEntry.time = photo.modified. Calls [onPhoto] after each photo. Closes [sink]. */
    fun write(sink: Sink, textEntries: Map<String, String>, photos: List<BackupPhoto>, onPhoto: (done: Int, total: Int) -> Unit)

    /** Returns the text of "manifest.json", "photos.json" and every "data/<name>.json" entry (direct children of data/ only). Does not read photos. */
    fun readTextEntries(archive: Path): Map<String, String>

    /**
     * Streams every valid "images/" entry (name check, Section 5.5) into [targetDir]. Sets the modified time to [modifiedTimes][name]
     * (name without "images/"); falls back to entry.time when the name is missing and entry.time > 0.
     * [onPhoto] total = number of valid "images/" entries. Returns the count written.
     */
    fun extractPhotos(archive: Path, targetDir: Path, modifiedTimes: Map<String, Long>, onPhoto: (done: Int, total: Int) -> Unit): Int
}
