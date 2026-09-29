package tv.own.owntv.core.sync

import tv.own.owntv.core.model.SourceType

data class SyncProgressCounts(
    val live: Int,
    val movies: Int,
    val series: Int,
    val liveActive: Boolean,
    val moviesActive: Boolean,
    val seriesActive: Boolean,
    /** The connection measurement's position, when it is the thing currently running. */
    val measuringStream: Int = 0,
    val measuringOf: Int = 0,
) {
    val hasItems: Boolean
        get() = live > 0 || movies > 0 || series > 0

    val measuringConnections: Boolean get() = measuringOf > 0
}

enum class SyncProgressPhase { PREPARING, CONNECTING, SYNCING, MEASURING_CONNECTIONS }

data class SyncProgressDisplay(
    val counts: SyncProgressCounts?,
    val phase: SyncProgressPhase,
)

fun ImportStage.progressCounts(): SyncProgressCounts = SyncProgressCounts(
    live = liveProcessed,
    movies = moviesProcessed,
    series = seriesProcessed,
    liveActive = liveActive,
    moviesActive = moviesActive,
    seriesActive = seriesActive,
    measuringStream = measuringStream,
    measuringOf = measuringOf,
)

fun ImportStage.importProgressDisplay(): SyncProgressDisplay =
    importProgressDisplay(progressCounts())

fun syncProgressCountsForSource(
    sourceType: SourceType,
    liveProcessed: Int,
    moviesProcessed: Int,
    seriesProcessed: Int,
    liveActive: Boolean,
    moviesActive: Boolean,
    seriesActive: Boolean,
): SyncProgressCounts = when (sourceType) {
    SourceType.M3U -> SyncProgressCounts(
        live = liveProcessed,
        movies = 0,
        series = 0,
        liveActive = true,
        moviesActive = false,
        seriesActive = false,
    )
    SourceType.XTREAM -> {
        val hasActivePhase = liveActive || moviesActive || seriesActive
        SyncProgressCounts(
            live = liveProcessed,
            movies = moviesProcessed,
            series = seriesProcessed,
            liveActive = if (hasActivePhase) liveActive else true,
            moviesActive = if (hasActivePhase) moviesActive else true,
            seriesActive = if (hasActivePhase) seriesActive else true,
        )
    }
    SourceType.LOCAL_BACKUP -> SyncProgressCounts(
        live = 0,
        movies = 0,
        series = 0,
        liveActive = false,
        moviesActive = false,
        seriesActive = false,
    )
    // Stalker: LIVE ships (Phase C-1); VOD/series arrive in Phase D. Progress is shaped like Xtream
    // so a later Phase D needs no change here.
    SourceType.STALKER -> {
        val hasActivePhase = liveActive || moviesActive || seriesActive
        SyncProgressCounts(
            live = liveProcessed,
            movies = moviesProcessed,
            series = seriesProcessed,
            liveActive = if (hasActivePhase) liveActive else true,
            moviesActive = if (hasActivePhase) moviesActive else true,
            seriesActive = if (hasActivePhase) seriesActive else true,
        )
    }
}

fun importProgressDisplay(counts: SyncProgressCounts?): SyncProgressDisplay = SyncProgressDisplay(
    counts = counts,
    phase = when {
        counts == null -> SyncProgressPhase.PREPARING
        // Before anything has been fetched, so it is checked before the item counts.
        counts.measuringConnections -> SyncProgressPhase.MEASURING_CONNECTIONS
        counts.hasItems -> SyncProgressPhase.SYNCING
        else -> SyncProgressPhase.CONNECTING
    },
)

fun resyncProgressPercent(baseItemCount: Int, totalProcessed: Int): Int? =
    if (baseItemCount > 0 && totalProcessed > 0) {
        ((totalProcessed * 100L) / baseItemCount).coerceIn(1, 99).toInt()
    } else {
        null
    }

fun syncProgressDisplay(counts: SyncProgressCounts?): SyncProgressDisplay = importProgressDisplay(counts)

fun syncProgressCountsOrNull(counts: SyncProgressCounts): SyncProgressCounts? =
    counts.takeIf { it.hasItems }
