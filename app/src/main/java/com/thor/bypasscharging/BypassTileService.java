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
    private boolean busy;

    private void refreshAsync() {
        if (busy) return;
        busy = true;

        executor.execute(() -> {
            boolean root = RootShell.isRootAvailable();
            String path = BypassNodeDetector.find();
            boolean on = !path.isEmpty() && RootShell.isBypassEnabled();
            main.post(() -> {
                busy = false;
                apply(root, !path.isEmpty(), on);
            });
        });
    }

    private void apply(boolean root, boolean node, boolean on) {
        Tile tile = getQsTile();
        if (tile == null) return;

        tile.setState(!root || !node
                ? Tile.STATE_UNAVAILABLE
                : (on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE));
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
            boolean root = RootShell.isRootAvailable();
            if (!root) {
                main.post(this::refreshAsync);
                return;
            }

            boolean current = RootShell.isBypassEnabled();
            boolean target = !current;

            main.post(() -> {
                Tile tile = getQsTile();
                if (tile != null) {
                    tile.setState(target ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
                    tile.setLabel(target ? "Bypass ON" : "Bypass");
                    tile.setStateDescription(target ? "Enabled" : "Disabled");
                    tile.updateTile();
                }
            });

            boolean ok = RootShell.setBypass(target);
            main.post(() -> {
                if (!ok) {
                    refreshAsync();
                }
            });
        });
    }

    @Override public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}