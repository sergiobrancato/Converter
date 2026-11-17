package dev.currency.converter.tile

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import dev.currency.converter.R

/**
 * Asks the system to add the tile to the Quick Settings panel.
 *
 * `StatusBarManager.requestAddTileService` exists on Android 13+ (API 33) and must
 * be called while the app is in the foreground. On older versions there is no public
 * API: users add the tile by editing Quick Settings manually.
 */
object QuickSettingsTileHelper {

    fun requestAddTile(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val statusBarManager = activity.getSystemService(StatusBarManager::class.java) ?: return
        val componentName = ComponentName(activity, ConverterTileService::class.java)

        statusBarManager.requestAddTileService(
            componentName,
            activity.getString(R.string.quick_tile_label),
            Icon.createWithResource(activity, R.drawable.ic_qs_convert),
            ContextCompat.getMainExecutor(activity),
        ) {
            // The dialog is system-owned; a generic confirmation avoids depending on
            // result-code constants that differ between OEM/Quick Settings versions.
            Toast.makeText(
                activity,
                R.string.quick_settings_requested,
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
