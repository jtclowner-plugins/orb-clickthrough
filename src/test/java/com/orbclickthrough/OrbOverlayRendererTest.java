package com.orbclickthrough;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OrbOverlayRendererTest
{
    @Test
    public void childContextsTextImagesAndShapesFadeWithoutLeavingPixelsFromPreviousOverlay()
    {
        Client client = mock(Client.class);
        when(client.getCanvasWidth()).thenReturn(300);
        when(client.getCanvasHeight()).thenReturn(200);
        OrbOverlayRenderer renderer = new OrbOverlayRenderer(client);
        BufferedImage icon = new BufferedImage(15, 15, BufferedImage.TYPE_INT_ARGB);
        Graphics2D iconGraphics = icon.createGraphics();
        iconGraphics.setColor(Color.RED);
        iconGraphics.fillRect(0, 0, 15, 15);
        iconGraphics.dispose();
        Overlay drawing = new Overlay()
        {
            @Override public Dimension render(Graphics2D graphics)
            {
                Graphics2D child = (Graphics2D) graphics.create();
                child.translate(60, 30);
                child.rotate(.2);
                child.setClip(null);
                child.setColor(Color.BLUE);
                child.fillOval(20, 10, 30, 30);
                child.drawString("Floating text", 25.5f, 75.5f);
                child.drawLine(90, 80, 120, 90);
                child.drawImage(icon, 90, 15, null);
                child.dispose();
                return null;
            }
        };
        BufferedImage reference = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D normal = reference.createGraphics();
        drawing.render(normal);
        normal.dispose();
        BufferedImage actual = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D faded = actual.createGraphics();
        renderer.render(drawing, faded, "prayer", .5f, 1f, 1f);
        faded.dispose();
        boolean painted = false;
        for (int y = 0; y < 200; y++)
            for (int x = 0; x < 300; x++)
            {
                int expectedAlpha = Math.round((reference.getRGB(x, y) >>> 24) * .5f);
                assertEquals("pixel " + x + "," + y, expectedAlpha, actual.getRGB(x, y) >>> 24);
                painted |= expectedAlpha != 0;
            }
        assertTrue(painted);
        Overlay different = new Overlay()
        {
            @Override public Dimension render(Graphics2D graphics)
            {
                graphics.setColor(Color.GREEN);
                graphics.fillRect(250, 170, 10, 10);
                return null;
            }
        };
        BufferedImage next = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D nextGraphics = next.createGraphics();
        renderer.render(different, nextGraphics, "special", .5f, 1f, 1f);
        nextGraphics.dispose();
        for (int y = 0; y < 200; y++)
            for (int x = 0; x < 300; x++)
                assertEquals(x >= 250 && x < 260 && y >= 170 && y < 180 ? 128 : 0, next.getRGB(x, y) >>> 24);
    }
}
