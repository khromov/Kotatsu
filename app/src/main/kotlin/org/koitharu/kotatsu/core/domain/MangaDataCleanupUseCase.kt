package org.koitharu.kotatsu.core.domain

import androidx.core.net.toUri
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.util.ext.URI_SCHEME_ZIP
import org.koitharu.kotatsu.core.util.ext.isFileUri
import org.koitharu.kotatsu.parsers.model.Manga
import javax.inject.Inject

class MangaDataCleanupUseCase @Inject constructor(
	private val database: MangaDatabase,
) {

	/**
	 * Clean up all data related to a manga when it's deleted
	 */
	suspend fun cleanupMangaData(mangaId: Long) {
		// Clean up edge bounds cache for all pages of this manga
		// We use a pattern to match all URLs that might belong to this manga
		database.getEdgeBoundsDao().deleteByUrlPattern("%/manga/$mangaId/%")
	}

	/**
	 * Clean up old edge bounds entries (older than 30 days)
	 */
	suspend fun cleanupOldEdgeBounds() {
		val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
		database.getEdgeBoundsDao().deleteOlderThan(thirtyDaysAgo)
	}
}
