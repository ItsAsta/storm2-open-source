package net.storm.plugins.restock;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GrandExchangeOffer;
import net.storm.api.domain.items.IBankItem;
import net.storm.api.plugins.Task;
import net.storm.plugins.CannonballsConfig;
import net.storm.plugins.CannonballsPlugin;
import net.storm.plugins.ge.GETracker;
import net.storm.plugins.ge.ItemState;
import net.storm.plugins.misc.Constants;
import net.storm.sdk.items.Bank;
import net.storm.sdk.items.GrandExchange;
import net.storm.sdk.items.Inventory;

@Slf4j
public class BuyItem implements Task {

    private int coinsAmt;

    private final CannonballsPlugin plugin;
    private final CannonballsConfig config;
    private final GETracker geTracker;

    public BuyItem(CannonballsPlugin plugin, CannonballsConfig config, GETracker geTracker) {
        this.plugin = plugin;
        this.config = config;
        this.geTracker = geTracker;
    }

    @Override
    public boolean validate() {
        ItemState cBallState = geTracker.getState(Constants.CANNONBALL);

        /*
        * This validation is somewhat irrelevant due to tasks blocking if the validation is true.
        * Since we only have 2 tasks and the SellItem task is before this task.
        * This task won't get executed till the SellItem task returns false.
        */
        return cBallState == ItemState.COMPLETED;
    }

    @Override
    public int execute() {

        plugin.status = "Buying steel bars...";

        ItemState state = geTracker.getState(Constants.STEEL_BAR);

        switch (state) {
            case NOT_CHECKED:
                // If we have successfully checked how much coins we have, we should change the item state.
                if (coinsAmt != 0) {
                    geTracker.setState(Constants.STEEL_BAR, ItemState.CHECKED);
                    return -1;
                }

                // Otherwise, let's check our coins in the bank.
                checkCoins();
                break;

            case CHECKED:
                // We have collected our noted steel bars, let's change the state to complete.
                if (Inventory.contains(Constants.STEEL_BAR_NOTED)) {
                    geTracker.setState(Constants.STEEL_BAR, ItemState.COMPLETED);
                    return -1;
                }

                // Otherwise, let's buy steel bars.
                buySteelBars();
                break;
            case COMPLETED:
                log.warn("Completed buying {}", Constants.STEEL_BAR);

                // Before resuming smelting cannonballs, let's deposit our inventory in the bank if it's not empty.
                if (!Inventory.isEmpty()) {
                    if (!Bank.isOpen()) {
                        Bank.open();
                        return -1;
                    }

                    Bank.depositInventory();
                    return -1;
                }

                // We have finished restocking. Let's default all our variables for our next restock.
                plugin.isRestocking = false;
                geTracker.setState(Constants.CANNONBALL, ItemState.NOT_CHECKED);
                geTracker.setState(Constants.STEEL_BAR, ItemState.NOT_CHECKED);
                coinsAmt = 0;
                break;
        }

        return -1;
    }

    private void checkCoins() {

        if (!Bank.isOpen()) {
            Bank.open();
            return;
        }

        IBankItem coins = Bank.getFirst(i -> i != null && Constants.COINS == i.getId() && !i.isPlaceholder());

        if (coins != null) {
            /*
            * Let's store our coins amount from the bank. Keep an eye out for the boolean, because coins is a stackable item.
            * If we do not put a boolean value, or we put false, it'll always return 1.
            */
            coinsAmt = Bank.getCount(true, Constants.COINS);
        }
    }

    private void buySteelBars() {

        // Refer to SellItem class, method SellCannonballs.
        if (!GrandExchange.getOffers().isEmpty()) {
            log.info("Offers not empty, looping through them");
            for (GrandExchangeOffer offer : GrandExchange.getOffers()) {
                log.info("Item ID: {}", offer.getItemId());
                if (offer.getItemId() == Constants.STEEL_BAR) {
                    GrandExchange.collect();
                    break;
                }
            }

            return;
        }

        if (Bank.isOpen()) {
            Bank.close();
        }

        int quantity = coinsAmt / config.steelBarPrice();

        GrandExchange.buy(Constants.STEEL_BAR, quantity, config.steelBarPrice());
    }
}
