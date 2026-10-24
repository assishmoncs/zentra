package com.hsissa.zentra.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

@RequiresApi(Build.VERSION_CODES.N)
class FocusTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val currentlyActive = FocusSessionService.isSessionActive || tile.state == Tile.STATE_ACTIVE

        if (currentlyActive) {
            val stopIntent = Intent(this, FocusSessionService::class.java).apply {
                action = FocusSessionService.ACTION_STOP_SESSION
            }
            startService(stopIntent)
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Focus: Off"
        } else {
            val startIntent = Intent(this, FocusSessionService::class.java).apply {
                action = FocusSessionService.ACTION_START_SESSION
                putExtra(FocusSessionService.EXTRA_DURATION_MINUTES, FocusSessionService.DEFAULT_DURATION_MINUTES)
            }
            ContextCompat.startForegroundService(this, startIntent)
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Focus: On"
        }
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        if (FocusSessionService.isSessionActive) {
            tile.label = "Focus: On"
            tile.state = Tile.STATE_ACTIVE
        } else {
            tile.label = "Zentra Focus"
            tile.state = Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }
}
