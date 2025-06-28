package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration26To27 : Migration(26, 27) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL(
			"""CREATE TABLE IF NOT EXISTS `edge_bounds` (
				`page_url` TEXT NOT NULL,
				`left_edge` INTEGER NOT NULL,
				`top_edge` INTEGER NOT NULL,
				`right_edge` INTEGER NOT NULL,
				`bottom_edge` INTEGER NOT NULL,
				`created_at` INTEGER NOT NULL,
				PRIMARY KEY(`page_url`)
			)""",
		)

		// Create index for faster cleanup by timestamp
		db.execSQL("CREATE INDEX IF NOT EXISTS `index_edge_bounds_created_at` ON `edge_bounds` (`created_at`)")
	}
}