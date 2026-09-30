package com.orbclickthrough;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/** Clears hover tooltips after other overlays produce them and before TooltipOverlay draws them. */
public class OrbHoverOverlay extends Overlay
{
    private final OrbClickthroughPlugin plugin;
    private final TooltipManager tooltipManager;

    @Inject
    OrbHoverOverlay(OrbClickthroughPlugin plugin, TooltipManager tooltipManager)
    {
        this.plugin = plugin;
        this.tooltipManager = tooltipManager;
        setPosition(OverlayPosition.TOOLTIP);
        setPriority(PRIORITY_HIGHEST + 1);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        drawAfterInterface(InterfaceID.TOPLEVEL_DISPLAY);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin.shouldHideHoverTooltips())
        {
            tooltipManager.clear();
        }
        return null;
    }
}
