package com.hsissa.zentra.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class FocusTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val isActive = tile.state == Tile.STATE_ACTIVE
        if (isActive) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Focus: Off"
        } else {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Focus: On"
        }
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.label = "Zentra Focus"
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
