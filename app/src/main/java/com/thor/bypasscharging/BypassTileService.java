package com.thor.bypasscharging;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class BypassTileService extends TileService {
    private void refresh() { Tile t=getQsTile(); if(t==null)return; boolean on=RootShell.isBypassEnabled(); t.setState(on?Tile.STATE_ACTIVE:Tile.STATE_INACTIVE); t.setLabel(on?"Bypass ON":"Bypass Charging"); t.setContentDescription(on?"Bypass charging is enabled":"Bypass charging is disabled"); t.updateTile(); }
    @Override public void onStartListening(){ super.onStartListening(); refresh(); }
    @Override public void onClick(){ super.onClick(); if(!RootShell.isRootAvailable()) { refresh(); return; } RootShell.setBypass(!RootShell.isBypassEnabled()); refresh(); }
}
