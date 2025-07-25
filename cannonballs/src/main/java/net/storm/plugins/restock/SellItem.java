package net.storm.plugins.restock;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.client.plugins.grandexchange.GrandExchangeClient;
import net.storm.api.domain.actors.IPlayer;
import net.storm.api.domain.items.IBankItem;
import net.storm.api.movement.pathfinder.model.BankLocation;
import net.storm.api.plugins.Task;
import net.storm.plugins.CannonballsPlugin;
import net.storm.plugins.ge.GETracker;
import net.storm.plugins.ge.ItemState;
import net.storm.plugins.misc.Constants;
import net.storm.plugins.tasks.Banking;
import net.storm.sdk.entities.Players;
import net.storm.sdk.items.Bank;
import net.storm.sdk.items.GrandExchange;
import net.storm.sdk.items.Inventory;
import net.storm.sdk.movement.Movement;

@Slf4j
public class SellItem implements Task {

    private final CannonballsPlugin plugin;
    private final GETracker geTracker;

    public SellItem(CannonballsPlugin plugin, GETracker geTracker) {
        this.plugin = plugin;
        this.geTracker = geTracker;
    }

    @Override
    public boolean validate() {
        ItemState state = geTracker.getState(Constants.CANNONBALL);

        return state != null && state != ItemState.COMPLETED;
    }

    @Override
    public int execute() {

        plugin.status = "Selling Cannonballs...";

        IPlayer local = Players.getLocal();

        ItemState state = geTracker.getState(Constants.CANNONBALL);

        // If we are not within the predefined GE area, we should walk to it. Again, using .isWalking to avoid repeated interactions.
        if (!Constants.GRAND_EXCHANGE_AREA.contains(local.getWorldLocation()) && !Movement.isWalking()) {
            Movement.walkTo(BankLocation.GRAND_EXCHANGE_BANK);
            return -1;
        }


        switch (state) {
            case NOT_CHECKED:
                // If the item is in our inventory, we can assume that we are ready to sell the item.
                if (Inventory.contains(Constants.CANNONBALL)) {
                    geTracker.setState(Constants.CANNONBALL, ItemState.CHECKED);
                    return -1;
                }

                getCannonballs();
                break;

            case CHECKED:
                // If there is coins in our inventory, then we can assume that we have successfully sold our item.
                if (Inventory.contains(Constants.COINS) && geTracker.isState(Constants.CANNONBALL, ItemState.SOLD)) {
                    geTracker.setState(Constants.CANNONBALL, ItemState.COMPLETED);
                    return -1;
                }

                sellCannonballs();
                break;

            case SOLD:

                /*
                * This check is done to make sure that we deposit all the coins.
                * Because we will be opening up the bank later, to grab how much coins we have to buy steel bars.
                */
                if (!Inventory.contains(Constants.COINS)) {
                    geTracker.setState(Constants.CANNONBALL, ItemState.COMPLETED);
                    return -1;
                }

                depositCoins();
                break;

            case COMPLETED:
                log.warn("Completed selling {}!", Constants.CANNONBALL);
                break;
        }

        return -1;
    }

    private void getCannonballs() {

        if (!Bank.isOpen()) {
            log.info("Opening Bank!");
            Bank.open();
            return;
        }

        // Please refer to Banking class to know more about these variables.
        IBankItem cBall = Bank.getFirst(i -> i != null && Constants.CANNONBALL == i.getId() && !i.isPlaceholder());

        if (cBall != null) {
            log.info("Withdrawing {}", Constants.CANNONBALL);
            Bank.withdrawAll(Constants.CANNONBALL);
            return;
        }

        IBankItem coins = Bank.getFirst(i -> i != null && Constants.COINS == i.getId() && !i.isPlaceholder());

        if (coins != null) {
            log.info("We have {}", Constants.COINS);
            geTracker.setState(Constants.CANNONBALL, ItemState.COMPLETED);
        }
    }


    private void sellCannonballs() {

        // We have successfully collected the coins from our sale. We can set our state to sold.
        if (Inventory.contains(Constants.COINS)) {
            log.info("Sold {}", Constants.CANNONBALL);
            geTracker.setState(Constants.CANNONBALL, ItemState.SOLD);
            return;
        }

        // If there is GE offers available to collect, let's collect them.

        if (!GrandExchange.getOffers().isEmpty()) {

            /*
            * We iterating over all offers (A stream can be used for this), this is redundant since we will be collecting all the offers later.
            * But for demonstration purposes, I'll be showing you how to compare each other.
            */
            for (GrandExchangeOffer offer : GrandExchange.getOffers()) {

                /*
                * This if statement, we are comparing the offers item ID with our item ID. If it's true, we can do whatever with it.
                * In this demonstration, I'm just collecting all offers.
                */
                if (offer.getItemId() == Constants.CANNONBALL) {
                    GrandExchange.collect();
                    log.info("Collecting {}", Constants.COINS);
                    break;
                }
            }

            return;

            // Below is an example of a stream. It's cleaner and more readable. (Has been commented)

            /*

            boolean hasOffer = GrandExchange.getOffers().stream()
                    .anyMatch(offer -> offer.getItemId() == Constants.CANNONBALL);

            if (hasOffer) {
                GrandExchange.collect();
                log.info("Collecting {}", Constants.COINS);
                return;
            }

            */
        }

        if (Inventory.contains(Constants.CANNONBALL)) {
            log.info("Selling {}", Constants.CANNONBALL);
            if (Bank.isOpen()) {
                Bank.close();
                return;
            }

            GrandExchange.sell(Constants.CANNONBALL, Inventory.getCount(true, Constants.CANNONBALL), 50);
        }
    }

    private void depositCoins() {
        if (!Bank.isOpen()) {
            Bank.open();
            return;
        }

        log.info("Depositing {}", Constants.COINS);
        Bank.depositInventory();
    }
}
