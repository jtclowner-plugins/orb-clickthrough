package com.orbclickthrough;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.BufferProvider;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CompassAppearanceTest
{
    @Test
    public void fadesActualNeedleAndFramePixelsInEveryLayoutWithoutChangingSurroundings()
    {
        for (boolean gpu : new boolean[]{false, true})
            for (int id : new int[]{InterfaceID.ToplevelPreEoc.MAP_MINIMAP_GRAPHIC6,
                InterfaceID.ToplevelOsrsStretch.MAP_MINIMAP_GRAPHIC6, InterfaceID.Toplevel.MAPCONTAINER_GRAPHIC3})
            {
                Client client = mock(Client.class);
                OrbClickthroughPlugin plugin = mock(OrbClickthroughPlugin.class);
                OverlayManager manager = mock(OverlayManager.class);
                List<Overlay> overlays = new ArrayList<>();
                when(manager.add(any())).thenAnswer(call -> overlays.add(call.getArgument(0)));
                BufferProvider buffer = mock(BufferProvider.class);
                int[] pixels = new int[100 * 100];
                when(buffer.getPixels()).thenReturn(pixels);
                when(buffer.getWidth()).thenReturn(100);
                when(buffer.getHeight()).thenReturn(100);
                when(client.getBufferProvider()).thenReturn(buffer);
                when(client.isGpu()).thenReturn(gpu);
                boolean resized = id != InterfaceID.Toplevel.MAPCONTAINER_GRAPHIC3;
                when(client.isResized()).thenReturn(resized);
                Widget compass = mock(Widget.class);
                when(compass.getBounds()).thenReturn(new Rectangle(30, 30, 35, 35));
                when(compass.getContentType()).thenReturn(1339);
                when(client.getWidget(id)).thenReturn(compass);
                CompassAppearance appearance = new CompassAppearance(client, plugin, manager);
                for (float opacity : new float[]{.5f, 0f, 1f})
                {
                    when(plugin.nativeOrbOpacity("compass")).thenReturn(opacity);
                    Arrays.fill(pixels, 0xff224466);
                    appearance.beginFrame();
                    overlays.get(0).render(null); // scene before widgets
                    for (int y = 26; y < 69; y++)
                        for (int x = 26; x < 69; x++) pixels[y * 100 + x] = 0xffcc8844;
                    overlays.get(1).render(null); // game-drawn compass and shared frame
                    int expectedBackground = resized ? 0xff224466 : (gpu ? 0 : 0xff000000);
                    assertEquals(CompassAppearance.blend(0xffcc8844, expectedBackground, opacity), pixels[47 * 100 + 47]);
                    assertEquals(0xffcc8844, pixels[26 * 100 + 26]); // outside circular compass
                    // Outline/shadow beyond the old needle-mask + two-pixel crop.
                    assertEquals(CompassAppearance.blend(0xffcc8844, expectedBackground, opacity), pixels[47 * 100 + 27]);
                    assertEquals(0xff224466, pixels[80 * 100 + 80]); // minimap/world outside compass
                    overlays.get(1).render(null);
                    assertEquals(CompassAppearance.blend(0xffcc8844, expectedBackground, opacity), pixels[47 * 100 + 47]);
                    if (!resized && opacity != 1f)
                    {
                        // The next frame can reuse the native compass without repainting it.
                        appearance.beginFrame();
                        assertEquals(0xffcc8844, pixels[47 * 100 + 47]);
                        overlays.get(1).render(null);
                        assertEquals(CompassAppearance.blend(0xffcc8844, expectedBackground, opacity), pixels[47 * 100 + 47]);
                        // A subsequent drawing (e.g. a menu) must survive restoration.
                        pixels[47 * 100 + 47] = 0xffabcdef;
                    }
                    appearance.stop();
                    assertEquals(!resized && opacity != 1f ? 0xffabcdef : 0xffcc8844, pixels[47 * 100 + 47]);
                    // restart installs fresh hooks
                    overlays.clear();
                }
            }
    }

    @Test
    public void premultipliedGpuFadeChangesColourAndAlphaTogether()
    {
        assertEquals(0x80604020, CompassAppearance.blend(0xffc08040, 0, .5f));
        assertEquals(0, CompassAppearance.blend(0xffc08040, 0, 0f));
        assertEquals(0xffc08040, CompassAppearance.blend(0xffc08040, 0, 1f));
    }
}
