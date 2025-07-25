package net.storm.plugins;

import net.storm.api.plugins.SoxExclude;
import net.storm.api.plugins.config.Config;
import net.storm.api.plugins.config.ConfigGroup;
import net.storm.api.plugins.config.ConfigItem;

@ConfigGroup(CannonballsConfig.GROUP)
@SoxExclude // Exclude from obfuscation
public interface CannonballsConfig extends Config {
    String GROUP = "ast-cannonballs";

    @ConfigItem(
            position = 0,
            keyName = "pause",
            name = "Pause?",
            description = "Pause the plugin from executing."
    )
    default boolean pause() {
        return false;
    }

    @ConfigItem(
            position = 1,
            keyName = "restock",
            name = "Restock?",
            description = "Restock on steel bars by selling cannonballs."
    )
    default boolean restock() {
        return false;
    }

    @ConfigItem(
            position = 2,
            keyName = "steelBarPrice",
            name = "Steel Bar Price",
            description = "The price to buy steel bars for."
    )
    default int steelBarPrice() {
        return 0;
    }
}
