package com.orbclickthrough;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.BufferProvider;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

/** The native content-type 1339 renderer bypasses widget opacity. Its frame is shared with the minimap. */
@Singleton
class CompassAppearance
{
    private final Client client;
    private final OrbClickthroughPlugin plugin;
    private final OverlayManager manager;
    private final Overlay capture;
    private final Overlay fade;
    private Rectangle region;
    private Ellipse2D circle;
    private int[] backdrop = new int[0];
    private boolean captured;
    private boolean applied;
    private boolean installed;
    private Rectangle previousRegion;
    private int[] previousPixels;
    private int previousWidth;
    private int[] original = new int[0];
    private int[] written = new int[0];

    @Inject
    CompassAppearance(Client client, OrbClickthroughPlugin plugin, OverlayManager manager)
    {
        this.client = client;
        this.plugin = plugin;
        this.manager = manager;
        capture = new Overlay()
        {
            { setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.UNDER_WIDGETS); }
            @Override public Dimension render(Graphics2D graphics)
            {
                captureBackdrop();
                return null;
            }
        };
        fade = new Overlay()
        {
            {
                setPosition(OverlayPosition.DYNAMIC);
                setLayer(OverlayLayer.MANUAL);
                drawAfterLayer(InterfaceID.ToplevelPreEoc.MAP_MINIMAP);
                drawAfterLayer(InterfaceID.ToplevelOsrsStretch.MAP_MINIMAP);
                drawAfterLayer(InterfaceID.Toplevel.MAPCONTAINER);
            }
            @Override public Dimension render(Graphics2D graphics)
            {
                fadeCompass();
                return null;
            }
        };
    }

    void beginFrame()
    {
        restorePreviousFrame();
        if (!installed)
        {
            manager.add(capture);
            manager.add(fade);
            installed = true;
        }
        captured = false;
        applied = false;
        region = null;
        if (plugin.nativeOrbOpacity("compass") == 1f) return;
        Widget compass = visible(InterfaceID.ToplevelPreEoc.MAP_MINIMAP_GRAPHIC6,
            InterfaceID.ToplevelOsrsStretch.MAP_MINIMAP_GRAPHIC6,
            InterfaceID.Toplevel.MAPCONTAINER_GRAPHIC3);
        if (compass == null || compass.getContentType() != 1339) return;
        Rectangle bounds = new Rectangle(compass.getBounds());
        bounds.grow(2, 2); // Include the shared frame's outer rim, without touching the minimap surround.
        circle = new Ellipse2D.Double(bounds.x, bounds.y, bounds.width, bounds.height);
        BufferProvider buffer = client.getBufferProvider();
        if (buffer == null) return;
        region = bounds.intersection(new Rectangle(0, 0, buffer.getWidth(), buffer.getHeight()));
        if (region.isEmpty()) { region = null; return; }
        if (backdrop.length < region.width * region.height) backdrop = new int[region.width * region.height];
        if (original.length < region.width * region.height)
        {
            original = new int[region.width * region.height];
            written = new int[original.length];
        }
    }

    private Widget visible(int... ids)
    {
        for (int id : ids)
        {
            Widget widget = client.getWidget(id);
            if (widget != null && !widget.isHidden()) return widget;
        }
        return null;
    }

    private void captureBackdrop()
    {
        // Fixed-layout compass is outside the scene viewport and is drawn before this hook.
        // Its underlying surface is black (software) or transparent (GPU), not a previous frame.
        if (region == null || !client.isResized() || applied) return;
        BufferProvider buffer = client.getBufferProvider();
        for (int y = 0; y < region.height; y++)
            System.arraycopy(buffer.getPixels(), (region.y + y) * buffer.getWidth() + region.x,
                backdrop, y * region.width, region.width);
        captured = true;
    }

    private void fadeCompass()
    {
        if (region == null || applied) return;
        float opacity = plugin.nativeOrbOpacity("compass");
        if (opacity == 1f) return;
        // In software resizable mode we need the current scene, never stale canvas pixels.
        if (client.isResized() && !client.isGpu() && !captured) return;
        BufferProvider buffer = client.getBufferProvider();
        int[] pixels = buffer.getPixels();
        for (int y = 0; y < region.height; y++)
            for (int x = 0; x < region.width; x++)
            {
                int index = (region.y + y) * buffer.getWidth() + region.x + x;
                int local = y * region.width + x;
                original[local] = written[local] = pixels[index];
                if (!circle.contains(region.x + x + .5, region.y + y + .5)) continue;
                int background = captured ? backdrop[y * region.width + x] : (client.isGpu() ? 0 : 0xff000000);
                pixels[index] = written[local] = blend(pixels[index], background, opacity);
            }
        previousRegion = new Rectangle(region);
        previousPixels = pixels;
        previousWidth = buffer.getWidth();
        applied = true;
    }

    private void restorePreviousFrame()
    {
        BufferProvider buffer = client.getBufferProvider();
        if (previousRegion != null && buffer != null && buffer.getPixels() == previousPixels
            && buffer.getWidth() == previousWidth)
        {
            for (int y = 0; y < previousRegion.height; y++)
                for (int x = 0; x < previousRegion.width; x++)
                {
                    int index = (previousRegion.y + y) * previousWidth + previousRegion.x + x;
                    int local = y * previousRegion.width + x;
                    // Do not undo a subsequent drawing or an update by another plugin.
                    if (previousPixels[index] == written[local]) previousPixels[index] = original[local];
                }
        }
        previousRegion = null;
        previousPixels = null;
    }

    // The GPU UI texture uses premultiplied alpha. Interpolate all channels together.
    static int blend(int foreground, int background, float opacity)
    {
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8)
        {
            int a = foreground >>> shift & 255;
            int b = background >>> shift & 255;
            result |= Math.round(b + (a - b) * opacity) << shift;
        }
        return result;
    }

    void stop()
    {
        restorePreviousFrame();
        if (installed) { manager.remove(capture); manager.remove(fade); }
        installed = false;
        region = null;
        backdrop = new int[0];
    }
}
