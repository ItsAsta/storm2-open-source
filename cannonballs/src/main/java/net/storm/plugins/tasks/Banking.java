package net.storm.plugins.tasks;

import lombok.extern.slf4j.Slf4j;
import net.storm.api.domain.items.IBankItem;
import net.storm.api.items.WithdrawMode;
import net.storm.api.plugins.Task;
import net.storm.plugins.CannonballsConfig;
import net.storm.plugins.CannonballsPlugin;
import net.storm.plugins.misc.Constants;
import net.storm.sdk.items.Bank;
import net.storm.sdk.items.Inventory;
import net.storm.sdk.plugins.Plugins;

@Slf4j
public class Banking implements Task {

    private final CannonballsPlugin plugin;
    private final CannonballsConfig config;

    public Banking(CannonballsPlugin plugin, CannonballsConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean validate() {
        return !Inventory.contains(Constants.STEEL_BAR) || !Inventory.contains(Constants.AMMO_MOULD);
    }

    @Override
    public int execute() {

        plugin.status = "Depositing Bars...";

        if (!Bank.isOpen()) {
            Bank.open();
            return -1;
        }


//        if (!Inventory.isEmpty() && !Inventory.containsAll(Constants.AMMO_MOULD, Constants.STEEL_BAR)) {
//            log.info("Test test");
//            Bank.depositAllExcept(Constants.AMMO_MOULD, Constants.STEEL_BAR);
//            return -1;
//        }

        IBankItem steelbar = Bank.getFirst(i -> i != null && Constants.STEEL_BAR == i.getId() && !i.isPlaceholder());

        if (!Inventory.isFull()) {

            if (steelbar != null && !Inventory.contains(Constants.AMMO_MOULD)) {
                withdrawMould();
                return -1;
            }

            withdrawBars();
        }

        return -1;
    }

    private void withdrawBars() {

        // We are using a predicate to check if the steel bar matches with id we have fetched as well as making sure it's not a placeholder.
        IBankItem steelbar = Bank.getFirst(i -> i != null && Constants.STEEL_BAR == i.getId() && !i.isPlaceholder());

        // We have steel bars in the bank, let's withdraw them.
        if (steelbar != null) {
            log.info("Withdrawing steel bars!");
            Bank.withdrawAll(Constants.STEEL_BAR);
            return;
        }

        // If we do not have steel bars in the bank, we can prepare our inventory for restocking by depositing everything.
        if (!Inventory.isEmpty()) {
            log.info("Depositing inventory to start restocking!");
            Bank.depositInventory();
            return;
        }

        // If restocking has been enabled in the config.
        if (config.restock()) {
            log.warn("Restocking!");
            plugin.isRestocking = true;
        } else {
            log.warn("Stopping {} plugin! Restocking is disabled!", plugin.getName());
            Plugins.stopPlugin(plugin);
        }
    }

    private void withdrawMould() {
        // Another predicate to check if we have ammo mould and that it's not a placeholder.
        IBankItem ammoMould = Bank.getFirst(i -> i != null && Constants.AMMO_MOULD == i.getId() && !i.isPlaceholder());

        // If the ammo mould exists, we should withdraw it.
        if (ammoMould != null) {
            log.info("Withdrawing ammo mould!");
            // To make sure that we don't accidentally withdraw it as a noted item, we specifically set the withdraw mode to ITEM.
            Bank.withdraw(Constants.AMMO_MOULD, 1, WithdrawMode.ITEM);
        }
    }
}
