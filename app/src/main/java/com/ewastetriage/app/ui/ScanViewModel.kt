package com.ewastetriage.app.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ewastetriage.app.detection.Detector
import com.ewastetriage.app.guidance.GuidanceEntry
import com.ewastetriage.app.guidance.GuidanceRepository
import com.ewastetriage.app.history.HistoryEntry
import com.ewastetriage.app.history.HistoryManager
import com.ewastetriage.app.triage.TriageConfig
import com.ewastetriage.app.triage.TriageEngine
import com.ewastetriage.app.triage.TriageResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed class ScanState {
    object Initializing : ScanState()
    object Scanning : ScanState()
    data class Detected(val results: List<TriageResult>) : ScanState()
    object NothingFound : ScanState()
    data class Error(val message: String) : ScanState()
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<ScanState>(ScanState.Initializing)
    val state: StateFlow<ScanState> = _state

    private val _isFrozen = MutableStateFlow(false)
    val isFrozen: StateFlow<Boolean> = _isFrozen

    private val _strictnessOffset = MutableStateFlow(0.0f)
    val strictnessOffset: StateFlow<Float> = _strictnessOffset

    private val _historyList = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val historyList: StateFlow<List<HistoryEntry>> = _historyList

    private var detector: Detector? = null
    private var triageEngine: TriageEngine? = null
    private val historyManager = HistoryManager(application.applicationContext)
    private var guidanceRepo: GuidanceRepository? = null

    private val mutex = Mutex()
    private var isProcessing = false

    init {
        loadHistory()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = application.applicationContext
                val guidance = GuidanceRepository(ctx)
                val config = TriageConfig.load(ctx)
                val det = Detector(ctx, "best.tflite", "classes.txt")
                val engine = TriageEngine(config, displayNames = guidance.displayNames())
                detector = det
                triageEngine = engine
                guidanceRepo = guidance
                _state.value = ScanState.Scanning
            } catch (e: Exception) {
                _state.value = ScanState.Error("Init failed: ${e.message}")
            }
        }
    }

    fun onFrame(bitmap: Bitmap) {
        if (_isFrozen.value) return // freeze frame
        if (isProcessing) return
        isProcessing = true
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val dets = detector?.detect(bitmap) ?: return@withLock
                    val results = triageEngine?.process(dets, _strictnessOffset.value) ?: return@withLock
                    _state.value = if (results.isEmpty()) ScanState.NothingFound else ScanState.Detected(results)
                } catch (e: Exception) {
                    // silently skip bad frames
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    fun toggleFreeze() {
        _isFrozen.value = !_isFrozen.value
    }

    fun setStrictnessOffset(offset: Float) {
        _strictnessOffset.value = offset.coerceIn(-0.15f, 0.30f)
    }

    fun saveCurrentDetections(results: List<TriageResult>, source: String = "Live Camera") {
        viewModelScope.launch(Dispatchers.IO) {
            historyManager.saveScan(results, source)
            loadHistory()
        }
    }

    fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _historyList.value = historyManager.getHistory()
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyManager.clearHistory()
            _historyList.value = emptyList()
        }
    }

    fun deleteHistoryEntry(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyManager.deleteEntry(id)
            loadHistory()
        }
    }

    fun restoreHistoryEntry(entry: com.ewastetriage.app.history.HistoryEntry, index: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            historyManager.restoreEntry(entry, index)
            loadHistory()
        }
    }

    suspend fun analyzeStillImage(bitmap: Bitmap): List<TriageResult> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val dets = detector?.detect(bitmap) ?: return@withLock emptyList()
            triageEngine?.processSingle(dets, _strictnessOffset.value) ?: emptyList()
        }
    }

    fun getGuidance(className: String): GuidanceEntry? = guidanceRepo?.get(className)

    fun getAllGuidance(): Map<String, GuidanceEntry> = guidanceRepo?.getAllEntries() ?: emptyMap()

    override fun onCleared() {
        super.onCleared()
        detector?.close()
    }
}
