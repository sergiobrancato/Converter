package dev.currency.converter.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.currency.converter.ConverterApplication
import dev.currency.converter.QuickConvertActivity
import dev.currency.converter.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings entry point.
 *
 * The tile is intentionally stateless: tapping it is a one-shot action. A dialog
 * activity is launched instead of the full app so the user can convert and get
 * back to whatever they were doing in under a second.
 *
 * Android 14 (API 34) deprecated the Intent overload of
 * `startActivityAndCollapse`. The PendingIntent overload is required there and
 * also avoids background-activity-launch restrictions.
 */
class ConverterTileService : TileService() {

    /**
     * DataStore writes are suspending, so the tile owns a small scope. Work is
     * best-effort: a failed write must never crash the Quick Settings panel.
     */
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Adding the tile makes the in-app "Add tile" promo obsolete, so hide the
     * card automatically. The main screen observes the same DataStore key.
     */
    override fun onTileAdded() {
        super.onTileAdded()

        val container = (application as? ConverterApplication)?.container ?: return
        persistenceScope.launch {
            runCatching { container.currencyRepository.dismissQuickSettingsCard() }
        }
    }

    override fun onDestroy() {
        persistenceScope.cancel()
        super.onDestroy()
    }

    override fun onStartListening() {
        super.onStartListening()

        // The tile is actionable, not a toggle, so keep it in the neutral state.
        qsTile?.apply {
            label = getString(R.string.quick_tile_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = getString(R.string.quick_tile_subtitle)
            }
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        val launchIntent = Intent(this, QuickConvertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_OPEN_CONVERTER,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            startActivityAndCollapseLegacy(launchIntent)
        }
    }

    /**
     * The PendingIntent overload only exists on Android 14+, so older releases
     * must keep using the Intent overload. It is safe there: the deprecation only
     * turns into a crash for apps *targeting* API 34+, and this branch never runs
     * on those versions.
     */
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun startActivityAndCollapseLegacy(intent: Intent) {
        @Suppress("DEPRECATION")
        startActivityAndCollapse(intent)
    }

    private companion object {
        const val REQUEST_CODE_OPEN_CONVERTER = 1001
    }
}
