package com.thor.bypasscharging;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class BypassTileService extends TileService {
    private void refresh() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean root = RootShell.isRootAvailable();
        boolean node = !BypassNodeDetector.find().isEmpty();
        boolean on = node && RootShell.isBypassEnabled();

        tile.setState(!root || !node
                ? Tile.STATE_UNAVAILABLE
                : (on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE));
        tile.setLabel(on ? "Bypass ON" : "Bypass Charging");
        tile.setContentDescription(on
                ? "Bypass charging is enabled"
                : "Bypass charging is disabled");
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            tile.setStateDescription(on ? "Enabled" : "Disabled");
        }
        tile.updateTile();
    }

    @Override public void onStartListening() {
        super.onStartListening();
        refresh();
    }

    @Override public void onClick() {
        super.onClick();
        if (!RootShell.isRootAvailable()) {
            refresh();
            return;
        }
        RootShell.setBypass(!RootShell.isBypassEnabled());
        refresh();
    }
}