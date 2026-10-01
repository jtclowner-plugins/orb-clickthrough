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
import net.runelite.client.ui.overlay.Overlay;

/** Composites each supported orb drawing once, so overlapping fills don't undo its transparency. */
@Singleton
class OrbOverlayRenderer
{
    private final Client client;
    private BufferedImage buffer;
    private Rectangle dirty;

    @Inject
    OrbOverlayRenderer(Client client)
    {
        this.client = client;
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
        // An overlay can draw beyond its reported bounds or return null after drawing.
        // Capture the canvas rather than guessing another plugin's offsets or text layout.
        AffineTransform transform = graphics.getTransform();
        Rectangle device = new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
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
        if (buffer == null || buffer.getWidth() != device.width || buffer.getHeight() != device.height)
        {
            buffer = new BufferedImage(device.width, device.height, BufferedImage.TYPE_INT_ARGB);
            dirty = null;
        }
        Graphics2D offscreen = buffer.createGraphics();
        DrawingBoundsGraphics2D.Drawing drawing = new DrawingBoundsGraphics2D.Drawing(device);
        Dimension size;
        try
        {
            offscreen.setComposite(AlphaComposite.Clear);
            if (dirty != null) offscreen.fillRect(dirty.x, dirty.y, dirty.width, dirty.height);
            offscreen.setComposite(AlphaComposite.SrcOver);
            offscreen.setRenderingHints(graphics.getRenderingHints());
            offscreen.setFont(graphics.getFont());
            offscreen.setPaint(graphics.getPaint());
            offscreen.setStroke(graphics.getStroke());
            offscreen.setBackground(graphics.getBackground());
            offscreen.translate(-device.x, -device.y);
            offscreen.transform(transform);
            offscreen.setClip(graphics.getClip());
            size = overlay.render(new DrawingBoundsGraphics2D(offscreen, drawing));
        }
        finally
        {
            offscreen.dispose();
            dirty = drawing.bounds;
            if (dirty != null) composite(graphics, dirty, opacity);
        }
        return size;
    }

    private void composite(Graphics2D graphics, Rectangle device, float opacity)
    {
        if (opacity == 0f) return;
        Graphics2D output = (Graphics2D) graphics.create();
        try
        {
            AlphaComposite composite = graphics.getComposite() instanceof AlphaComposite
                ? (AlphaComposite) graphics.getComposite() : AlphaComposite.SrcOver;
            output.setComposite(composite.derive(composite.getAlpha() * opacity));
            output.setTransform(new AffineTransform());
            output.drawImage(buffer, device.x, device.y, device.x + device.width, device.y + device.height,
                device.x, device.y, device.x + device.width, device.y + device.height, null);
        }
        finally
        {
            output.dispose();
        }
    }

    void clear()
    {
        buffer = null;
        dirty = null;
    }

}
