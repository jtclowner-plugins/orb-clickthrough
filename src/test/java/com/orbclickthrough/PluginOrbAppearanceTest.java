package com.orbclickthrough;

import com.orbclickthrough.compat.OrbAppearance;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PluginOrbAppearanceTest
{
    private final EventBus eventBus = new EventBus();
    private final Client client = mock(Client.class);
    private final OrbClickthroughConfig config = mock(OrbClickthroughConfig.class);
    private final OrbClickthroughPlugin plugin = new OrbClickthroughPlugin();

    @Before
    public void setup() throws Exception
    {
        OrbAppearanceTest.set(plugin, "client", client);
        OrbAppearanceTest.set(plugin, "config", config);
        OrbAppearanceTest.set(plugin, "running", true);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(config.activationMode()).thenReturn(OrbClickthroughActivationMode.HOLD_TO_RESTORE_CLICKS);
        when(config.managePrayerOrb()).thenReturn(true);
        when(config.clickThroughTransparency()).thenReturn(50);
        when(config.fadePluginOverlays()).thenReturn(true);
        when(config.suppressPluginTooltips()).thenReturn(true);
        eventBus.register(plugin);
    }

    @Test
    public void bothSettingsDefaultOn()
    {
        OrbClickthroughConfig defaults = new OrbClickthroughConfig() {};
        assertTrue(defaults.fadePluginOverlays());
        assertTrue(defaults.suppressPluginTooltips());
    }

    @Test
    public void participatesOnlyForSelectedOrbsAndEveryHotkeyMode() throws Exception
    {
        for (OrbClickthroughActivationMode mode : OrbClickthroughActivationMode.values())
        {
            when(config.activationMode()).thenReturn(mode);
            for (boolean active : new boolean[]{false, true})
            {
                OrbAppearanceTest.set(plugin, "hotkeyHeld", mode == OrbClickthroughActivationMode.HOLD_TO_RESTORE_CLICKS ? !active : active);
                OrbAppearanceTest.set(plugin, "toggleActive", active);
                OrbAppearance prayer = OrbAppearance.query(eventBus, "prayer");
                assertEquals(active ? .5f : 1f, prayer.getOpacity(), 0f);
                assertEquals(active, prayer.suppressTooltip());
                assertNormal("health");
                assertNormal("unknown-orb");
            }
        }
    }

    @Test
    public void settingsAreIndependentOfEachOtherAndNativeHoverSetting()
    {
        assertFalse(config.disableHoverEffects());
        when(config.fadePluginOverlays()).thenReturn(false);
        OrbAppearance state = OrbAppearance.query(eventBus, "prayer");
        assertEquals(1f, state.getOpacity(), 0f);
        assertTrue(state.suppressTooltip());
        when(config.fadePluginOverlays()).thenReturn(true);
        when(config.suppressPluginTooltips()).thenReturn(false);
        state = OrbAppearance.query(eventBus, "prayer");
        assertEquals(.5f, state.getOpacity(), 0f);
        assertFalse(state.suppressTooltip());
    }

    @Test
    public void absenceDisableAndLogoutImmediatelyRestoreNormalBehavior() throws Exception
    {
        OrbAppearanceTest.set(plugin, "running", false);
        assertNormal("prayer");
        OrbAppearanceTest.set(plugin, "running", true);
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        assertNormal("prayer");
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        eventBus.unregister(plugin);
        assertNormal("prayer");
    }

    @Test
    public void queryingNeverRemovesUnderlyingMouseTooltips()
    {
        TooltipManager tooltips = new TooltipManager();
        Tooltip attack = new Tooltip("Attack Goblin");
        tooltips.add(attack);
        assertTrue(OrbAppearance.query(eventBus, "prayer").suppressTooltip());
        assertEquals(1, tooltips.getTooltips().size());
        assertSame(attack, tooltips.getTooltips().get(0));
    }

    @Test
    public void drawingMultipliesExistingAlphaWithoutChangingSharedGraphics()
    {
        BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D original = image.createGraphics();
        original.setColor(Color.BLUE);
        original.setComposite(AlphaComposite.SrcOver.derive(.5f));
        Graphics2D faded = OrbAppearance.query(eventBus, "prayer").createGraphics(original);
        faded.fillRect(0, 0, 1, 1);
        faded.dispose();
        assertEquals(.5f, ((AlphaComposite) original.getComposite()).getAlpha(), 0f);
        original.fillRect(1, 0, 1, 1);
        original.dispose();
        assertEquals(64, image.getRGB(0, 0) >>> 24);
        assertEquals(128, image.getRGB(1, 0) >>> 24);
    }

    @Test
    public void transparencyEndpointsAndOutOfRangeConfigAreClamped()
    {
        when(config.clickThroughTransparency()).thenReturn(100);
        assertEquals(0f, OrbAppearance.query(eventBus, "prayer").getOpacity(), 0f);
        when(config.clickThroughTransparency()).thenReturn(-1);
        assertEquals(1f, OrbAppearance.query(eventBus, "prayer").getOpacity(), 0f);
        when(config.clickThroughTransparency()).thenReturn(101);
        assertEquals(0f, OrbAppearance.query(eventBus, "prayer").getOpacity(), 0f);
    }

    private void assertNormal(String orb)
    {
        OrbAppearance state = OrbAppearance.query(eventBus, orb);
        assertEquals(1f, state.getOpacity(), 0f);
        assertFalse(state.suppressTooltip());
    }
}
