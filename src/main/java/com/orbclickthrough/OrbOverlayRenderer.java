package com.orbclickthrough;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.Overlay;

/** Composites each supported orb drawing once, so overlapping fills don't undo its transparency. */
@Singleton
class OrbOverlayRenderer
{
    private final Client client;
    private final ConfigManager configs;
    private BufferedImage buffer;

    @Inject
    OrbOverlayRenderer(Client client, ConfigManager configs)
    {
        this.client = client;
        this.configs = configs;
    }

    Dimension render(Overlay overlay, Graphics2D graphics, String orb, float opacity, float health, float special)
    {
        if ("regeneration".equals(orb))
        {
            Graphics2D rings = new RegenGraphics2D((Graphics2D) graphics.create(), health, special);
            try
            {
                return overlay.render(rings);
            }
            finally
            {
                rings.dispose();
            }
        }
        if (opacity == 1f)
        {
            return overlay.render(graphics);
        }
        Rectangle region = drawingBounds(overlay.getClass().getName());
        if (region == null)
        {
            // Preserve producer side effects (including tooltips) even with no visible orb.
            return overlay.render(graphics);
        }
        AffineTransform transform = graphics.getTransform();
        Rectangle device = transform.createTransformedShape(region).getBounds();
        device = device.intersection(new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight()));
        if (graphics.getClip() != null)
        {
            device = device.intersection(transform.createTransformedShape(graphics.getClip()).getBounds());
        }
        if (device.isEmpty())
        {
            // Execute tooltip producers on a disposable surface: some supported overlays
            // replace their clip, so an empty clip on the live canvas would not suffice.
            Graphics2D clipped = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
            try
            {
                clipped.clipRect(0, 0, 0, 0);
                return overlay.render(clipped);
            }
            finally
            {
                clipped.dispose();
            }
        }
        if (buffer == null || buffer.getWidth() < device.width || buffer.getHeight() < device.height)
        {
            buffer = new BufferedImage(Math.max(device.width, buffer == null ? 0 : buffer.getWidth()),
                Math.max(device.height, buffer == null ? 0 : buffer.getHeight()), BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D offscreen = buffer.createGraphics();
        Dimension size;
        try
        {
            offscreen.setComposite(AlphaComposite.Clear);
            offscreen.fillRect(0, 0, device.width, device.height);
            offscreen.setComposite(AlphaComposite.SrcOver);
            offscreen.setRenderingHints(graphics.getRenderingHints());
            offscreen.setFont(graphics.getFont());
            offscreen.setPaint(graphics.getPaint());
            offscreen.setStroke(graphics.getStroke());
            offscreen.setBackground(graphics.getBackground());
            offscreen.translate(-device.x, -device.y);
            offscreen.transform(transform);
            offscreen.setClip(graphics.getClip());
            size = overlay.render(offscreen);
        }
        finally
        {
            offscreen.dispose();
        }
        Graphics2D output = (Graphics2D) graphics.create();
        try
        {
            AlphaComposite composite = graphics.getComposite() instanceof AlphaComposite
                ? (AlphaComposite) graphics.getComposite() : AlphaComposite.SrcOver;
            output.setComposite(composite.derive(composite.getAlpha() * opacity));
            output.setTransform(new AffineTransform());
            output.drawImage(buffer, device.x, device.y, device.x + device.width, device.y + device.height,
                0, 0, device.width, device.height, null);
        }
        finally
        {
            output.dispose();
        }
        return size;
    }

    private Rectangle drawingBounds(String name)
    {
        Rectangle bounds;
        switch (name)
        {
            case "net.runelite.client.plugins.prayer.PrayerDoseOverlay":
            case "net.runelite.client.plugins.prayer.PrayerFlickOverlay":
                bounds = widgetBounds(InterfaceID.Orbs.PRAYERBUTTON, InterfaceID.OrbsNomap.PRAYERBUTTON);
                if (bounds != null)
                {
                    bounds.add(new Rectangle(bounds.x + 24, bounds.y - 1, bounds.height, bounds.height));
                    bounds.grow(4, 4);
                }
                return bounds;
            case "com.soulreaperaxeqol.SoulreaperAxeQoLNativeOrbOverlay":
            case "com.soulreaperaxeqol.SoulreaperAxeQoLExtraOrbOverlay":
                bounds = widgetBounds(InterfaceID.Orbs.ORB_SPECENERGY, InterfaceID.OrbsNomap.ORB_SPECENERGY);
                if (bounds != null)
                {
                    if (name.endsWith("ExtraOrbOverlay"))
                    {
                        bounds.translate(setting("soulreaperaxeqol", "offsetX", 22), setting("soulreaperaxeqol", "offsetY", 25));
                    }
                    // Includes the side text, border, icon and antialiased timer strokes.
                    bounds.grow(64, 32);
                }
                return bounds;
            case "com.github.corhen.poisonring.PoisonRingOverlay":
                bounds = widgetBounds(InterfaceID.Orbs.ORB_HEALTH, InterfaceID.OrbsNomap.ORB_HEALTH);
                if (bounds != null)
                {
                    int diameter = bounds.height + setting("poisonring", "diameter", -1);
                    bounds = new Rectangle(bounds.x + bounds.width - bounds.height + setting("poisonring", "shiftX", 1),
                        bounds.y + setting("poisonring", "shiftY", 1), Math.max(0, diameter), Math.max(0, diameter));
                    int padding = Math.max(0, setting("poisonring", "lineThickness", 2)) / 2 + 4;
                    bounds.grow(padding, padding);
                }
                return bounds;
            default: return null;
        }
    }

    private Rectangle widgetBounds(int normal, int noMap)
    {
        Widget widget = client.getWidget(normal);
        if (widget == null || widget.isHidden())
        {
            widget = client.getWidget(noMap);
        }
        return widget == null || widget.isHidden() ? null : new Rectangle(widget.getBounds());
    }

    private int setting(String group, String key, int fallback)
    {
        Integer value = configs.getConfiguration(group, key, Integer.class);
        return value == null ? fallback : value;
    }
}
