package com.github.corhen.poisonring;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class PoisonRingOverlay extends Overlay {
    private final Client client;
    private final PoisonRingPlugin plugin;
    private final PoisonRingConfig config;

    @Inject
    public PoisonRingOverlay(Client client, PoisonRingPlugin plugin, PoisonRingConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DETACHED);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.isPoisoned()) {
            return null;
        }

        Widget healthOrb = client.getWidget(ComponentID.MINIMAP_HEALTH_ORB);
        if (healthOrb == null || healthOrb.isHidden()) {
            return null;
        }

        Rectangle bounds = healthOrb.getBounds();

        int baseOrbX = bounds.x + (bounds.width - bounds.height);
        int baseOrbY = bounds.y;

        int finalX = baseOrbX + config.shiftX();
        int finalY = baseOrbY + config.shiftY();
        int finalDiameter = bounds.height + config.diameter();

        double progress;
        if (config.testMode() && plugin.getTicksUntilDamage() == 0) {
            progress = 0.5; // Show a half-circle in test mode so you can see the arc
        } else {
            progress = (double) plugin.getTicksUntilDamage() / plugin.getPoisonTickRate();
        }

        int startAngle = 90;
        int extent = (int) -(360 * progress);

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setColor(plugin.isVenom() ? config.venomColor() : config.poisonColor());
        graphics.setStroke(new BasicStroke(config.lineThickness()));

        Arc2D.Double arc = new Arc2D.Double(
            finalX,
            finalY,
            finalDiameter,
            finalDiameter,
            startAngle,
            extent,
            Arc2D.OPEN
        );

        graphics.draw(arc);

        return null;
    }
}
