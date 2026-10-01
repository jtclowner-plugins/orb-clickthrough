package com.orbclickthrough.compat;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.util.Map;
import java.util.function.BiConsumer;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;

/**
 * Optional integration using RuneLite's existing synchronous PluginMessage bus.
 * Copy into a consumer's own package; consumers do not depend on our plugin jar.
 */
public final class OrbAppearance
{
    private float opacity = 1f;
    private boolean suppressTooltip;

    private OrbAppearance()
    {
    }

    public static OrbAppearance query(EventBus eventBus, String orb)
    {
        OrbAppearance appearance = new OrbAppearance();
        BiConsumer<Float, Boolean> reply = (opacity, suppressTooltip) -> {
            if (opacity != null && Float.isFinite(opacity))
            {
                appearance.opacity = Math.max(0f, Math.min(1f, opacity));
            }
            appearance.suppressTooltip = Boolean.TRUE.equals(suppressTooltip);
        };
        eventBus.post(new PluginMessage("orbclickthrough", "appearance-v1",
                Map.of("orb", orb, "reply", reply)));
        return appearance;
    }

    public float getOpacity()
    {
        return opacity;
    }

    public boolean suppressTooltip()
    {
        return suppressTooltip;
    }

    /** Caller must dispose the returned graphics. The input graphics is untouched. */
    public Graphics2D createGraphics(Graphics2D original)
    {
        Graphics2D graphics = (Graphics2D) original.create();
        Composite composite = graphics.getComposite();
        if (composite instanceof AlphaComposite)
        {
            AlphaComposite alpha = (AlphaComposite) composite;
            graphics.setComposite(alpha.derive(alpha.getAlpha() * opacity));
        }
        else if (opacity < 1f)
        {
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        }
        return graphics;
    }
}
