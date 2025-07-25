package net.storm.plugins.ge;

import net.storm.plugins.misc.Constants;

import java.util.HashMap;
import java.util.Map;

public class GETracker {
    private final Map<Integer, ItemState> itemState = new HashMap<>();

    public GETracker() {
        itemState.put(Constants.CANNONBALL, ItemState.NOT_CHECKED);
        itemState.put(Constants.STEEL_BAR, ItemState.NOT_CHECKED);
    }

    public void setState(int itemId, ItemState state) {
        itemState.put(itemId, state);
    }

    public ItemState getState(int itemId) {
        return itemState.getOrDefault(itemId, ItemState.NOT_CHECKED);
    }

    public boolean isState(int itemId, ItemState expected) {
        return getState(itemId) == expected;
    }
}
