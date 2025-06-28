package org.koitharu.kotatsu.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.koitharu.kotatsu.core.db.entity.EdgeBoundsEntity

@Dao
interface EdgeBoundsDao {

	@Query("SELECT * FROM edge_bounds WHERE page_url = :pageUrl")
	suspend fun findByPageUrl(pageUrl: String): EdgeBoundsEntity?

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(edgeBounds: EdgeBoundsEntity)

	@Query("DELETE FROM edge_bounds WHERE created_at < :timestamp")
	suspend fun deleteOlderThan(timestamp: Long)

	@Query("DELETE FROM edge_bounds WHERE page_url LIKE :urlPattern")
	suspend fun deleteByUrlPattern(urlPattern: String)

	@Query("DELETE FROM edge_bounds")
	suspend fun deleteAll()

	@Query("SELECT COUNT(*) FROM edge_bounds")
	suspend fun count(): Int
}