package net.storm.plugins.tasks;

import lombok.extern.slf4j.Slf4j;
import net.storm.api.domain.tiles.ITileObject;
import net.storm.api.plugins.Task;
import net.storm.api.widgets.ProductionQuantity;
import net.storm.plugins.CannonballsPlugin;
import net.storm.plugins.misc.Constants;
import net.storm.sdk.entities.Players;
import net.storm.sdk.entities.TileObjects;
import net.storm.sdk.items.Inventory;
import net.storm.sdk.movement.Movement;
import net.storm.sdk.widgets.Dialog;
import net.storm.sdk.widgets.Production;

@Slf4j
public class Smelting implements Task {

    private final int LAST_XP_THRESHOLD = 6_000;
    private final CannonballsPlugin plugin;

    public Smelting(CannonballsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean validate() {
        return Inventory.contains(Constants.STEEL_BAR);
    }

    @Override
    public int execute() {

        plugin.status = "Smelting Cannonballs...";

        // If we have gained XP within 6 seconds of our last xp gain, then we can assume we still smelting.
        if (System.currentTimeMillis() < (plugin.lastXpTime + LAST_XP_THRESHOLD)) {
            return -1;
        }

        ITileObject furnace = TileObjects.getFirstAt(Constants.FURNACE_TILE, Constants.FURNACE);

        // Furnace is not available for us to interact with, let's walk to it.
        if (furnace == null) {
            // If we are not walking, then we should walk. This check is used to avoid spam clicking.
            if (!Movement.isWalking()) {
                log.info("Walking to furnace!");
                Movement.walkTo(Constants.FURNACE_TILE);
            }
            return -1;
        }

        // If the production dialog is open
        if (Production.isOpen()) {
            // We need to make sure that production is set to ALL, if not, we should set it to ALL.
            if (Production.getSelectedQuantity() != ProductionQuantity.ALL) {
                log.info("Setting production quantity to ALL!");
                Production.selectQuantity(ProductionQuantity.ALL);
                return -1;
            }

            // Since the quantity is confirmed to be set, we can start smelting.
            Dialog.continueSpace();
            // Since we just started smelting, let's also update our xp time.
            plugin.lastXpTime = System.currentTimeMillis();
            return -1;
        }

        // Again, to prevent spam clicking while walking, we check if we are not walking before interacting with the furnace.
        if (!Movement.isWalking()) {
            log.info("Interacting with furnace!");
            furnace.interact("Smelt");
        }

        return -1;
    }
}
