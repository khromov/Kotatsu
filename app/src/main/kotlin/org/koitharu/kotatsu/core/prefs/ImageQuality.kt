package org.koitharu.kotatsu.core.prefs

import androidx.annotation.Keep

@Keep
enum class ImageQuality(val id: Int) {

	PERFORMANCE(1),
	BALANCED(2),
	QUALITY(3),
	;

	val maxDownSampling: Int
		get() = when (this) {
			PERFORMANCE -> 8
			BALANCED -> 4
			QUALITY -> 2
		}

	val isFilteringEnabled: Boolean
		get() = when (this) {
			PERFORMANCE -> false
			BALANCED -> true
			QUALITY -> true
		}

	val useAdvancedFiltering: Boolean
		get() = when (this) {
			PERFORMANCE -> false
			BALANCED -> false
			QUALITY -> true
		}

	companion object {

		fun valueOf(id: Int) = entries.firstOrNull { it.id == id } ?: BALANCED
	}
}