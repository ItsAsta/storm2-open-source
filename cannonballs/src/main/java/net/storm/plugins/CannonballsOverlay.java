package net.storm.plugins;

import com.google.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;

public class CannonballsOverlay  extends OverlayPanel {
    private final CannonballsPlugin plugin;

    @Inject
    public CannonballsOverlay(CannonballsPlugin plugin) {
        this.plugin = plugin;
    }


    @Override
    public Dimension render(Graphics2D graphics) {
        // Set properties of our panel, such as the size, gap, colour etc...
        this.panelComponent.setPreferredSize(new Dimension(200, 0));
        this.panelComponent.setGap(new Point(5, 5));


        this.panelComponent.getChildren().add(createTitleComponent());
        this.panelComponent.getChildren().add(createRunningTimeComponent());
        this.panelComponent.getChildren().add(createStatusComponent());
        return super.render(graphics);
    }

    private TitleComponent createTitleComponent() {
        return TitleComponent.builder()
                .text("Cannonballs - Asta")
                .color(Color.RED)
                .build();
    }

    private LineComponent createRunningTimeComponent() {
        return LineComponent.builder()
                .left("Runtime")
                .right(formatRuntime(plugin.startTime))
                .rightColor(Color.ORANGE)
                .build();
    }

    private LineComponent createStatusComponent() {
        return LineComponent.builder()
                .left("Status")
                .right(plugin.status)
                .rightColor(Color.GREEN)
                .build();
    }

    // Formats our runtime into HH:MM:SS
    public static String formatRuntime(long startTime) {
        long runTime = System.currentTimeMillis() - startTime;

        long seconds = (runTime / 1000) % 60;
        long minutes = (runTime / (1000 * 60)) % 60;
        long hours = (runTime / (1000 * 60 * 60));

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}