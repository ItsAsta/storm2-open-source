package net.storm.plugins;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayManager;
import net.storm.api.domain.actors.IPlayer;
import net.storm.api.events.ConfigChanged;
import net.storm.api.events.ExperienceGained;
import net.storm.api.plugins.PluginDescriptor;
import net.storm.api.plugins.Task;
import net.storm.api.plugins.TaskPlugin;
import net.storm.api.plugins.config.ConfigManager;
import net.storm.plugins.ge.GETracker;
import net.storm.plugins.restock.BuyItem;
import net.storm.plugins.restock.SellItem;
import net.storm.plugins.tasks.Banking;
import net.storm.plugins.tasks.Smelting;
import net.storm.sdk.entities.Players;
import org.pf4j.Extension;

import java.util.HashMap;
import java.util.Map;

@PluginDescriptor(name = "Cannonballs")
@Slf4j
@Extension
public class CannonballsPlugin extends TaskPlugin {

    public String status;
    public long startTime, lastXpTime;
    public boolean isPaused, isRestocking;
    private Task[] tasks, restockTasks;

    @Inject
    private CannonballsConfig config;

    @Inject
    private GETracker geTracker;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private CannonballsOverlay overlay;

    @Override
    public void startUp() {
        // Initiates start time.
        startTime = System.currentTimeMillis();
        overlayManager.add(overlay);

        tasks = new Task[]{
                new Banking(this, config),
                new Smelting(this)
        };

        restockTasks = new Task[]{
                new SellItem(this, geTracker),
                new BuyItem(this, config, geTracker)
        };
    }

    @Override
    public void shutDown() {
        overlayManager.remove(overlay);
    }

    @Override
    public Task[] getTasks() {
        // If plugin is paused, return null, blocks executing the rest of the tasks.
        if (isPaused) {
            status = "Paused...";
            return null;
        }

        IPlayer local = Players.getLocal();

        if (local == null) {
            return null;
        }

        if (isRestocking) {
            return restockTasks;
        }


        // Loops the initiated tasks that are in the startUp method.
        return tasks;
    }

    @Provides
    public CannonballsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(CannonballsConfig.class);
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged e) {
        // This check is done to make sure that our config would only trigger if the config group matches this plugins config group.
        if (!e.getGroup().equals(CannonballsConfig.GROUP)) {
            return;
        }

        // If the key equals our pause button in the config.
        if (e.getKey().equals("pause")) {
            // Toggles boolean between true/false.
            isPaused = !isPaused;
        }
    }

    @Subscribe
    private void onExperienceGained(ExperienceGained e) {
        // If the gained XP is not from Smithing then we should do an early return to avoid the code continuing
        if (e.getSkill() != Skill.SMITHING) {
            return;
        }

        // Reset our xp timer every time we gain XP in smithing.
        lastXpTime = System.currentTimeMillis();
    }
}
