package app.lifeguide.core.data

import app.lifeguide.core.corpus.CorpusChanges
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class UpdateStatus { Idle, Checking }

/** 需要告诉用户的更新结果。自动检查时只报「更新了」，没更新、失败都不打扰。 */
sealed interface UpdateEvent {
    data class Updated(val manifest: CorpusManifest) : UpdateEvent
    data object UpToDate : UpdateEvent
    data class Failed(val reason: String) : UpdateEvent
}

/**
 * 什么时候查更新、查完之后做什么：启动后自动查（开关打开且距上次成功检查满 12 小时），设置页可手动查。
 * 跑在进程级的 [scope] 里，离开设置页不会打断正在进行的下载。
 */
@OptIn(ExperimentalTime::class)
class ContentUpdateService(
    private val updater: CorpusUpdater,
    private val repository: CorpusRepository,
    private val prefs: UserPreferences,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
) {
    private val log = Logger.withTag("ContentUpdate")

    private val _status = MutableStateFlow(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    private val _events = MutableSharedFlow<UpdateEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<UpdateEvent> = _events.asSharedFlow()

    fun checkIfDue() {
        scope.launch {
            if (!prefs.autoUpdate.first()) return@launch
            if (clock.now().toEpochMilliseconds() - prefs.lastCheck() < CHECK_INTERVAL_MS) return@launch
            run(manual = false)
        }
    }

    fun checkNow() {
        scope.launch { run(manual = true) }
    }

    private suspend fun run(manual: Boolean) {
        if (!_status.compareAndSet(UpdateStatus.Idle, UpdateStatus.Checking)) return
        try {
            repository.ensureLoaded()
            val r = updater.check()
            when (r) {
                is UpdateResult.Updated -> {
                    val before = repository.state.value
                    val after = repository.reload()
                    // 记下新增和改动的条目，清单顶部提示一次，用户关掉为止
                    if (before != null) {
                        val changes = CorpusChanges.between(before.corpus, after.corpus)
                        if (!changes.isEmpty) prefs.setRecentChanges(RecentChanges(r.manifest.commitDate, changes))
                    }
                    log.i { "正文已更新到 ${r.manifest.commit}" }
                    _events.emit(UpdateEvent.Updated(r.manifest))
                }
                UpdateResult.UpToDate -> if (manual) _events.emit(UpdateEvent.UpToDate)
                is UpdateResult.Failed -> {
                    log.w { "更新正文失败：${r.reason}" }
                    if (manual) _events.emit(UpdateEvent.Failed(r.reason))
                }
            }
            // 失败不记时间，下次启动还会再试
            if (r !is UpdateResult.Failed) prefs.setLastCheck(clock.now().toEpochMilliseconds())
        } finally {
            _status.value = UpdateStatus.Idle
        }
    }

    companion object {
        const val CHECK_INTERVAL_MS = 12 * 60 * 60 * 1000L
    }
}
