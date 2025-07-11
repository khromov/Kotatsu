package org.koitharu.kotatsu.reader.ui.config

import android.graphics.Bitmap
import android.view.View
import androidx.annotation.CheckResult
import androidx.collection.scatterSetOf
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageRegionDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaPooledImageRegionDecoder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderBackground
import org.koitharu.kotatsu.core.prefs.ImageQuality
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.core.util.MediatorStateFlow
import org.koitharu.kotatsu.core.util.ext.isLowRamDevice
import org.koitharu.kotatsu.core.util.ext.processLifecycleScope
import org.koitharu.kotatsu.reader.domain.ReaderColorFilter

data class ReaderSettings(
	val zoomMode: ZoomMode,
	val background: ReaderBackground,
	val colorFilter: ReaderColorFilter?,
	val isReaderOptimizationEnabled: Boolean,
	val bitmapConfig: Bitmap.Config,
	val imageQuality: ImageQuality,
	val isPagesNumbersEnabled: Boolean,
	val isPagesCropEnabledStandard: Boolean,
	val isPagesCropEnabledWebtoon: Boolean,
) {

	private constructor(settings: AppSettings, colorFilterOverride: ReaderColorFilter?) : this(
		zoomMode = settings.zoomMode,
		background = settings.readerBackground,
		colorFilter = colorFilterOverride?.takeUnless { it.isEmpty } ?: settings.readerColorFilter,
		isReaderOptimizationEnabled = settings.isReaderOptimizationEnabled,
		bitmapConfig = if (settings.is32BitColorsEnabled) {
			Bitmap.Config.ARGB_8888
		} else {
			Bitmap.Config.RGB_565
		},
		imageQuality = settings.imageQuality,
		isPagesNumbersEnabled = settings.isPagesNumbersEnabled,
		isPagesCropEnabledStandard = settings.isPagesCropEnabled(ReaderMode.STANDARD),
		isPagesCropEnabledWebtoon = settings.isPagesCropEnabled(ReaderMode.WEBTOON),
	)

	fun applyBackground(view: View) {
		view.background = background.resolve(view.context)
		view.backgroundTintList = if (background.isLight(view.context)) {
			colorFilter?.getBackgroundTint()
		} else {
			null
		}
	}

	fun isPagesCropEnabled(isWebtoon: Boolean) = if (isWebtoon) {
		isPagesCropEnabledWebtoon
	} else {
		isPagesCropEnabledStandard
	}

	@CheckResult
	fun applyBitmapConfig(ssiv: SubsamplingScaleImageView): Boolean {
		val config = bitmapConfig
		val needsUpdate = ssiv.regionDecoderFactory.bitmapConfig != config
		
		if (needsUpdate) {
			ssiv.regionDecoderFactory = if (ssiv.context.isLowRamDevice()) {
				SkiaImageRegionDecoder.Factory(config)
			} else {
				SkiaPooledImageRegionDecoder.Factory(config)
			}
			ssiv.bitmapDecoderFactory = SkiaImageDecoder.Factory(config)
		}
		
		// Apply filtering settings regardless of bitmap config changes
		applyImageFiltering(ssiv)
		
		return needsUpdate
	}

	private fun applyImageFiltering(ssiv: SubsamplingScaleImageView) {
		// The image quality and filtering are primarily controlled through:
		// 1. BitmapConfig (already set above)
		// 2. Down sampling ratios (handled in BasePageHolder.applyDownSampling)
		// 3. Using the standard Skia decoders which handle filtering automatically
		
		// SubsamplingScaleImageView's built-in decoders already provide good quality
		// The main moire reduction comes from:
		// - Appropriate downsampling ratios based on imageQuality.maxDownSampling
		// - Higher quality bitmap configs when enabled
		// - The decoders already set in applyBitmapConfig() above
		
		// Additional quality improvements happen through the downsampling algorithm
		// in BasePageHolder which now respects the imageQuality setting
	}

	class Producer @AssistedInject constructor(
		@Assisted private val mangaId: Flow<Long>,
		private val settings: AppSettings,
		private val mangaDataRepository: MangaDataRepository,
	) : MediatorStateFlow<ReaderSettings>(ReaderSettings(settings, null)) {

		private val settingsKeys = scatterSetOf(
			AppSettings.KEY_ZOOM_MODE,
			AppSettings.KEY_PAGES_NUMBERS,
			AppSettings.KEY_READER_BACKGROUND,
			AppSettings.KEY_32BIT_COLOR,
			AppSettings.KEY_IMAGE_QUALITY,
			AppSettings.KEY_READER_OPTIMIZE,
			AppSettings.KEY_CF_CONTRAST,
			AppSettings.KEY_CF_BRIGHTNESS,
			AppSettings.KEY_CF_INVERTED,
			AppSettings.KEY_CF_GRAYSCALE,
			AppSettings.KEY_READER_CROP,
		)
		private var job: Job? = null

		override fun onActive() {
			assert(job?.isActive != true)
			job?.cancel()
			job = processLifecycleScope.launch(Dispatchers.Default) {
				observeImpl()
			}
		}

		override fun onInactive() {
			job?.cancel()
			job = null
		}

		private suspend fun observeImpl() {
			combine(
				mangaId.flatMapLatest { mangaDataRepository.observeColorFilter(it) },
				settings.observe().filter { x -> x == null || x in settingsKeys }.onStart { emit(null) },
			) { mangaCf, settingsKey ->
				ReaderSettings(settings, mangaCf)
			}.collect {
				publishValue(it)
			}
		}

		@AssistedFactory
		interface Factory {

			fun create(mangaId: Flow<Long>): Producer
		}
	}
}
