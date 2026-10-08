package com.thor.bypasscharging;

import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BypassTileService extends TileService {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private void refreshAsync() {
        executor.execute(() -> {
            boolean root = RootShell.isRootAvailable();
            String path = BypassNodeDetector.find();
            boolean on = !path.isEmpty() && RootShell.isBypassEnabled();
            main.post(() -> apply(root, !path.isEmpty(), on));
        });
    }

    private void apply(boolean root, boolean node, boolean on) {
        Tile tile = getQsTile();
        if (tile == null) return;

        int newState = !root || !node
                ? Tile.STATE_UNAVAILABLE
                : (on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);

        tile.setState(newState);
        tile.setLabel(on ? "Bypass ON" : "Bypass");
        tile.setContentDescription(on
                ? "Bypass charging enabled"
                : "Bypass charging disabled");

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            tile.setStateDescription(on ? "Enabled" : "Disabled");
        }
        tile.updateTile();
    }

    @Override public void onStartListening() {
        super.onStartListening();
        refreshAsync();
    }

    @Override public void onClick() {
        super.onClick();

        executor.execute(() -> {
            if (!RootShell.isRootAvailable()) {
                refreshAsync();
                return;
            }

            boolean target = !RootShell.isBypassEnabled();
            RootShell.setBypass(target);

            main.post(this::refreshAsync);
        });
    }

    @Override public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}