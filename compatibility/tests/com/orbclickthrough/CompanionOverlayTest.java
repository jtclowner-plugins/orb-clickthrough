package com.orbclickthrough;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.TooltipPositionType;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Executes the actual patched companion overlays, not look-alike test implementations. */
public class CompanionOverlayTest
{
    private final Client client = mock(Client.class);
    private final OrbClickthroughConfig config = mock(OrbClickthroughConfig.class);
    private final EventBus bus = new EventBus();
    private final TooltipManager tooltips = new TooltipManager();
    private final OrbClickthroughPlugin provider = new OrbClickthroughPlugin();

    @Before
    public void setup() throws Exception
    {
        field(provider, "client", client);
        field(provider, "config", config);
        field(provider, "running", true);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(config.activationMode()).thenReturn(OrbClickthroughActivationMode.HOLD_TO_RESTORE_CLICKS);
        when(config.manageHealthOrb()).thenReturn(true);
        when(config.managePrayerOrb()).thenReturn(true);
        when(config.manageRunOrb()).thenReturn(true);
        when(config.manageSpecialAttackOrb()).thenReturn(true);
        when(config.fadePluginOverlays()).thenReturn(true);
        when(config.suppressPluginTooltips()).thenReturn(true);
        when(config.clickThroughTransparency()).thenReturn(50);
        when(client.getMouseCanvasPosition()).thenReturn(new Point(115, 115));
        when(client.getCanvasWidth()).thenReturn(400);
        when(client.getCanvasHeight()).thenReturn(250);
        Widget orb = mock(Widget.class);
        when(orb.getBounds()).thenReturn(new Rectangle(100, 100, 56, 30));
        when(client.getWidget(anyInt())).thenReturn(orb);
        bus.register(provider);
    }

    @Test
    public void prayerRunAndPoisonSuppressOnlyTheirOwnTooltip() throws Exception
    {
        Overlay[] overlays = {
            overlay("net.runelite.client.plugins.prayer.PrayerDoseOverlay", client, tooltips,
                stub("net.runelite.client.plugins.prayer.PrayerPlugin", Map.of()),
                defaults("net.runelite.client.plugins.prayer.PrayerConfig", Map.of("showPrayerStatistics", true))),
            overlay("net.runelite.client.plugins.runenergy.RunEnergyOverlay",
                stub("net.runelite.client.plugins.runenergy.RunEnergyPlugin", Map.of()), client,
                defaults("net.runelite.client.plugins.runenergy.RunEnergyConfig", Map.of()), tooltips),
            overlay("net.runelite.client.plugins.poison.PoisonOverlay",
                stub("net.runelite.client.plugins.poison.PoisonPlugin", Map.of("getLastDamage", 6, "createTooltip", "Poison damage")),
                client, tooltips)
        };
        for (Overlay overlay : overlays)
        {
            tooltips.clear();
            addActualNpcMouseTooltip();
            assertEquals(1, tooltips.getTooltips().size());
            String attack = tooltips.getTooltips().get(0).getText();
            assertTrue(attack.contains("Attack"));
            when(config.suppressPluginTooltips()).thenReturn(true);
            render(overlay);
            assertEquals(overlay.getClass().getName(), 1, tooltips.getTooltips().size());
            assertEquals(attack, tooltips.getTooltips().get(0).getText());
            when(config.suppressPluginTooltips()).thenReturn(false);
            render(overlay);
            assertEquals(overlay.getClass().getName(), 2, tooltips.getTooltips().size());
        }
    }

    @Test
    public void quickPrayerPreviewCoversBothQueuedAndDirectPanels() throws Exception
    {
        Class<?> prayerType = Class.forName("io.hydrox.quickprayerpreview.Prayer");
        Object prayer = prayerType.getEnumConstants()[0];
        BufferedImage sprite = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D spriteGraphics = sprite.createGraphics();
        spriteGraphics.setColor(Color.BLUE);
        spriteGraphics.fillRect(0, 0, 10, 10);
        spriteGraphics.dispose();
        Object plugin = stub("io.hydrox.quickprayerpreview.QuickPrayerPreviewPlugin",
                Map.of("getQuickPrayers", List.of(prayer), "getSprite", sprite));
        Class<?> position = Class.forName("io.hydrox.quickprayerpreview.QuickPrayerPreviewConfig$TooltipPosition");
        for (Object mode : position.getEnumConstants())
        {
            Overlay preview = overlay("io.hydrox.quickprayerpreview.QuickPrayerPreviewOverlay", client, plugin,
                    defaults("io.hydrox.quickprayerpreview.QuickPrayerPreviewConfig", Map.of("tooltipPosition", mode)),
                    defaults("net.runelite.client.config.RuneLiteConfig", Map.of("tooltipPosition", TooltipPositionType.UNDER_CURSOR)), tooltips);
            tooltips.clear();
            addActualNpcMouseTooltip();
            when(config.suppressPluginTooltips()).thenReturn(true);
            assertEquals(0, alphaSum(render(preview)));
            assertEquals(1, tooltips.getTooltips().size());
            when(config.suppressPluginTooltips()).thenReturn(false);
            BufferedImage shown = render(preview);
            if (mode.toString().equals("WITH_OTHERS"))
            {
                assertEquals(2, tooltips.getTooltips().size());
            }
            else
            {
                assertTrue(alphaSum(shown) > 0);
            }
        }
    }

    @Test
    public void prayerFlickIndicatorFadesAndUnselectedOrbIsUnaffected() throws Exception
    {
        Object location = Class.forName("net.runelite.client.plugins.prayer.PrayerFlickLocation").getEnumConstants()[1];
        Overlay flick = overlay("net.runelite.client.plugins.prayer.PrayerFlickOverlay", client,
                defaults("net.runelite.client.plugins.prayer.PrayerConfig", Map.of("prayerFlickLocation", location, "prayerFlickColor", Color.BLUE)),
                stub("net.runelite.client.plugins.prayer.PrayerPlugin", Map.of("isPrayersActive", true, "getTickProgress", .5d)));
        assertFades(flick);
        when(config.managePrayerOrb()).thenReturn(false);
        when(config.clickThroughTransparency()).thenReturn(100);
        assertTrue(alphaSum(render(flick)) > 0);
    }

    @Test
    public void regenerationMeterTreatsHpAndSpecIndependently() throws Exception
    {
        Overlay regen = overlay("net.runelite.client.plugins.regenmeter.RegenMeterOverlay", client,
                stub("net.runelite.client.plugins.regenmeter.RegenMeterPlugin", Map.of("getHitpointsPercentage", .5d, "getSpecialPercentage", .5d)),
                defaults("net.runelite.client.plugins.regenmeter.RegenMeterConfig", Map.of("showHitpoints", true, "showSpecial", true)));
        Widget spec = mock(Widget.class);
        when(spec.getBounds()).thenReturn(new Rectangle(240, 100, 56, 30));
        when(client.getWidget(InterfaceID.Orbs.ORB_SPECENERGY)).thenReturn(spec);
        assertFades(regen);
        when(config.manageHealthOrb()).thenReturn(false);
        when(config.clickThroughTransparency()).thenReturn(100);
        BufferedImage image = render(regen);
        assertTrue(alphaSum(image.getSubimage(90, 90, 90, 60)) > 0);
        assertEquals(0, alphaSum(image.getSubimage(230, 90, 90, 60)));
    }

    @Test
    public void soulreaperNativeDuplicateAndTextVariantsFade() throws Exception
    {
        Object plugin = stub("com.soulreaperaxeqol.SoulreaperAxeQoLPlugin", Map.of(
                "isSoulreaperAxeEquipped", true, "getSpecialAttackPercent", 50,
                "getSoulreaperStackCount", 3, "getSpecRegenProgress", .5d, "getSoulreaperRegenProgress", .5d));
        for (boolean text : new boolean[]{false, true})
        {
            Object settings = defaults("com.soulreaperaxeqol.SoulreaperAxeQoLConfig", Map.of("showTextOnOrb", text));
            for (String name : new String[]{"SoulreaperAxeQoLNativeOrbOverlay", "SoulreaperAxeQoLExtraOrbOverlay"})
            {
                assertFades(overlay("com.soulreaperaxeqol." + name, client, plugin, settings, mock(ConfigManager.class)));
            }
        }
    }

    @Test
    public void poisonRingFades() throws Exception
    {
        Overlay ring = overlay("com.github.corhen.poisonring.PoisonRingOverlay", client,
                stub("com.github.corhen.poisonring.PoisonRingPlugin", Map.of("isPoisoned", true, "getTicksUntilDamage", 15, "getPoisonTickRate", 30)),
                defaults("com.github.corhen.poisonring.PoisonRingConfig", Map.of()));
        assertFades(ring);
    }

    private void assertFades(Overlay overlay)
    {
        when(config.clickThroughTransparency()).thenReturn(0);
        long normal = alphaSum(render(overlay));
        assertTrue(overlay.getClass().getName(), normal > 0);
        when(config.clickThroughTransparency()).thenReturn(50);
        long faded = alphaSum(render(overlay));
        assertTrue(overlay.getClass().getName(), faded > 0 && faded < normal);
        when(config.clickThroughTransparency()).thenReturn(100);
        assertEquals(overlay.getClass().getName(), 0, alphaSum(render(overlay)));
        when(config.fadePluginOverlays()).thenReturn(false);
        assertEquals(normal, alphaSum(render(overlay)));
        when(config.fadePluginOverlays()).thenReturn(true);
    }

    private void addActualNpcMouseTooltip() throws Exception
    {
        MenuEntry attack = mock(MenuEntry.class);
        when(attack.getOption()).thenReturn("Attack");
        when(attack.getTarget()).thenReturn("Goblin");
        when(attack.getType()).thenReturn(MenuAction.NPC_SECOND_OPTION);
        when(client.getMenuEntries()).thenReturn(new MenuEntry[]{attack});
        Overlay mouse = construct("net.runelite.client.plugins.mousehighlight.MouseHighlightOverlay", client, tooltips,
                defaults("net.runelite.client.plugins.mousehighlight.MouseHighlightConfig", Map.of()));
        render(mouse);
    }

    private Overlay overlay(String name, Object... args) throws Exception
    {
        Overlay overlay = construct(name, args);
        field(overlay, "eventBus", bus);
        return overlay;
    }

    private static Overlay construct(String name, Object... args) throws Exception
    {
        for (Constructor<?> constructor : Class.forName(name).getDeclaredConstructors())
        {
            if (constructor.getParameterCount() == args.length)
            {
                constructor.setAccessible(true);
                return (Overlay) constructor.newInstance(args);
            }
        }
        throw new AssertionError("No matching constructor: " + name);
    }

    private static Object defaults(String type, Map<String, Object> values) throws Exception
    {
        return mock(Class.forName(type), invocation -> values.containsKey(invocation.getMethod().getName())
                ? values.get(invocation.getMethod().getName()) : Answers.CALLS_REAL_METHODS.answer(invocation));
    }

    private static Object stub(String type, Map<String, Object> values) throws Exception
    {
        return mock(Class.forName(type), invocation -> values.containsKey(invocation.getMethod().getName())
                ? values.get(invocation.getMethod().getName()) : Answers.RETURNS_DEFAULTS.answer(invocation));
    }

    private static void field(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static BufferedImage render(Overlay overlay)
    {
        BufferedImage image = new BufferedImage(400, 250, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try
        {
            overlay.render(graphics);
        }
        finally
        {
            graphics.dispose();
        }
        return image;
    }

    private static long alphaSum(BufferedImage image)
    {
        long sum = 0;
        for (int y = 0; y < image.getHeight(); y++)
        {
            for (int x = 0; x < image.getWidth(); x++)
            {
                sum += image.getRGB(x, y) >>> 24;
            }
        }
        return sum;
    }
}
