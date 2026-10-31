package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AspectRatioOption
import com.example.data.model.EditParameters
import com.example.data.model.FilterPreset
import com.example.data.model.MediaItem
import com.example.editor.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentParams = MutableStateFlow(EditParameters())
    val currentParams: StateFlow<EditParameters> = _currentParams

    private val history = mutableListOf<EditParameters>(EditParameters())
    private var historyIndex = 0

    val canUndo = MutableStateFlow(false)
    val canRedo = MutableStateFlow(false)

    val previewBitmap = MutableStateFlow<Bitmap?>(null)
    val isSaving = MutableStateFlow(false)
    val saveSuccessMessage = MutableStateFlow<String?>(null)
    val saveErrorMessage = MutableStateFlow<String?>(null)

    private var baseOriginalBitmap: Bitmap? = null
    var targetMediaItem: MediaItem? = null
        private set

    fun initializeWithMedia(mediaItem: MediaItem) {
        targetMediaItem = mediaItem
        _currentParams.value = EditParameters()
        history.clear()
        history.add(EditParameters())
        historyIndex = 0
        updateUndoRedoStates()

        viewModelScope.launch {
            // Load base preview bitmap scaled to 1280x1280 for fast responsive preview editing
            val bmp = ImageProcessor.decodeSampledBitmapFromUri(
                getApplication(),
                mediaItem.uri,
                1280,
                1280
            )
            baseOriginalBitmap = bmp
            updatePreview()
        }
    }

    private fun updateUndoRedoStates() {
        canUndo.value = historyIndex > 0
        canRedo.value = historyIndex < history.size - 1
    }

    private fun pushState(newParams: EditParameters) {
        if (historyIndex < history.size - 1) {
            history.subList(historyIndex + 1, history.size).clear()
        }
        history.add(newParams)
        historyIndex = history.size - 1
        _currentParams.value = newParams
        updateUndoRedoStates()
        updatePreview()
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            _currentParams.value = history[historyIndex]
            updateUndoRedoStates()
            updatePreview()
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            _currentParams.value = history[historyIndex]
            updateUndoRedoStates()
            updatePreview()
        }
    }

    fun resetAll() {
        pushState(EditParameters())
    }

    fun updateParams(newParams: EditParameters) {
        pushState(newParams)
    }

    // Direct parameter updates
    fun setAspectRatio(ratio: AspectRatioOption) {
        pushState(_currentParams.value.copy(aspectRatio = ratio))
    }

    fun rotate90() {
        val newRot = (_currentParams.value.rotationDegrees + 90) % 360
        pushState(_currentParams.value.copy(rotationDegrees = newRot))
    }

    fun setFineRotation(angle: Float) {
        pushState(_currentParams.value.copy(fineRotationAngle = angle))
    }

    fun toggleFlipHorizontal() {
        pushState(_currentParams.value.copy(flipHorizontal = !_currentParams.value.flipHorizontal))
    }

    fun toggleFlipVertical() {
        pushState(_currentParams.value.copy(flipVertical = !_currentParams.value.flipVertical))
    }

    fun setBrightness(value: Float) {
        pushState(_currentParams.value.copy(brightness = value))
    }

    fun setExposure(value: Float) {
        pushState(_currentParams.value.copy(exposure = value))
    }

    fun setContrast(value: Float) {
        pushState(_currentParams.value.copy(contrast = value))
    }

    fun setHighlights(value: Float) {
        pushState(_currentParams.value.copy(highlights = value))
    }

    fun setShadows(value: Float) {
        pushState(_currentParams.value.copy(shadows = value))
    }

    fun setSaturation(value: Float) {
        pushState(_currentParams.value.copy(saturation = value))
    }

    fun setTemperature(value: Float) {
        pushState(_currentParams.value.copy(temperature = value))
    }

    fun setTint(value: Float) {
        pushState(_currentParams.value.copy(tint = value))
    }

    fun setVibrance(value: Float) {
        pushState(_currentParams.value.copy(vibrance = value))
    }

    fun setFilter(preset: FilterPreset) {
        pushState(_currentParams.value.copy(filter = preset))
    }

    fun setFilterIntensity(intensity: Float) {
        pushState(_currentParams.value.copy(filterIntensity = intensity))
    }

    fun setVignette(value: Float) {
        pushState(_currentParams.value.copy(vignette = value))
    }

    fun setSharpen(value: Float) {
        pushState(_currentParams.value.copy(sharpen = value))
    }

    private fun updatePreview() {
        val base = baseOriginalBitmap ?: return
        viewModelScope.launch {
            val edited = ImageProcessor.applyEdits(base, _currentParams.value)
            previewBitmap.value = edited
        }
    }

    fun saveImage(onSuccess: (Uri) -> Unit) {
        val item = targetMediaItem ?: return
        isSaving.value = true
        viewModelScope.launch {
            val result = ImageProcessor.saveEditedImage(
                getApplication(),
                item.uri,
                _currentParams.value
            )
            isSaving.value = false
            result.fold(
                onSuccess = { uri ->
                    saveSuccessMessage.value = "Saved successfully to Pictures/PureGallery"
                    onSuccess(uri)
                },
                onFailure = { error ->
                    saveErrorMessage.value = "Failed to save: ${error.message}"
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        baseOriginalBitmap?.recycle()
        baseOriginalBitmap = null
        previewBitmap.value?.recycle()
        previewBitmap.value = null
    }
}
