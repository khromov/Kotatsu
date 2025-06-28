package org.koitharu.kotatsu.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = "edge_bounds",
	indices = [
		Index(value = ["created_at"], name = "index_edge_bounds_created_at")
	]
)
data class EdgeBoundsEntity(
	@PrimaryKey
	@ColumnInfo(name = "page_url")
	val pageUrl: String,
	@ColumnInfo(name = "left_edge")
	val leftEdge: Int,
	@ColumnInfo(name = "top_edge")
	val topEdge: Int,
	@ColumnInfo(name = "right_edge")
	val rightEdge: Int,
	@ColumnInfo(name = "bottom_edge")
	val bottomEdge: Int,
	@ColumnInfo(name = "created_at")
	val createdAt: Long,
)