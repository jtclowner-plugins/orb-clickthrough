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
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Executes the unmodified companion overlays, not look-alike test implementations. */
public class OrbOverlayCompatibilityTest
{
    private final Client client = mock(Client.class);
    private final OrbClickthroughConfig config = mock(OrbClickthroughConfig.class);
    private final net.runelite.client.ui.overlay.OverlayManager manager = mock(net.runelite.client.ui.overlay.OverlayManager.class);
    private final java.util.ArrayList<Overlay> registered = new java.util.ArrayList<>();
    private OrbOverlayCompatibility compatibility;
    private final ConfigManager otherConfigs = mock(ConfigManager.class);
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
        when(config.suppressOrbTooltips()).thenReturn(true);
        when(config.clickThroughTransparency()).thenReturn(50);
        when(config.fadePluginOverlays()).thenReturn(true);
        when(client.getMouseCanvasPosition()).thenReturn(new Point(115, 115));
        when(client.getCanvasWidth()).thenReturn(400);
        when(client.getCanvasHeight()).thenReturn(250);
        Widget orb = mock(Widget.class);
        when(orb.getBounds()).thenReturn(new Rectangle(100, 100, 56, 30));
        when(client.getWidget(anyInt())).thenReturn(orb);
        when(manager.anyMatch(any())).thenAnswer(i -> registered.stream().anyMatch(i.getArgument(0)));
        when(manager.add(any())).thenAnswer(i -> registered.add(i.getArgument(0)));
        when(manager.remove(any())).thenAnswer(i -> registered.remove((Object) i.getArgument(0)));
        compatibility = new OrbOverlayCompatibility(manager, tooltips, provider, new OrbOverlayRenderer(client));
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
            when(config.suppressOrbTooltips()).thenReturn(true);
            render(overlay);
            assertEquals(overlay.getClass().getName(), 1, tooltips.getTooltips().size());
            assertEquals(attack, tooltips.getTooltips().get(0).getText());
            when(config.suppressOrbTooltips()).thenReturn(false);
            render(overlay);
            assertEquals(overlay.getClass().getName(), 2, tooltips.getTooltips().size());
        }
    }

    @Test
    public void unknownOverlaysAndMouseTooltipsRemainUnwrapped() throws Exception
    {
        Overlay unknown = new Overlay()
        {
            @Override public java.awt.Dimension render(Graphics2D graphics) { return null; }
        };
        Overlay mouse = construct("net.runelite.client.plugins.mousehighlight.MouseHighlightOverlay", client, tooltips,
            defaults("net.runelite.client.plugins.mousehighlight.MouseHighlightConfig", Map.of()));
        registered.add(unknown);
        registered.add(mouse);
        compatibility.sync();
        assertEquals(List.of(unknown, mouse), registered);
    }

    @Test
    public void supportedOverlayGainingDrawHooksRestoresOriginal() throws Exception
    {
        Overlay adapter = overlay("net.runelite.client.plugins.poison.PoisonOverlay",
            stub("net.runelite.client.plugins.poison.PoisonPlugin", Map.of()), client, tooltips);
        Overlay original = registered.get(0);
        net.runelite.client.ui.overlay.OverlayLayer layer = adapter.getLayer();
        original.getDrawHooks().add(InterfaceID.Orbs.ORB_HEALTH);
        compatibility.sync();
        assertEquals(List.of(original), registered);
        assertEquals(layer, original.getLayer());
        original.getDrawHooks().clear();
        compatibility.sync();
        assertEquals(2, registered.size());
        assertEquals(net.runelite.client.ui.overlay.OverlayLayer.MANUAL, original.getLayer());
    }

    @Test
    public void specRegenTimerSuppressesBothTooltipVariantsOnlyWhileSpecIsClickthrough() throws Exception
    {
        Overlay adapter = overlay("com.bram91.specregen.SpecRegenTimerOverlay", client,
            stub("com.bram91.specregen.SpecRegenTimerPlugin", Map.of()), tooltips);
        Overlay original = registered.get(0);
        field(original, "specCount", 1);
        field(original, "maxSpecs", 2);
        field(original, "remainingMinutes", 1);
        field(original, "remainingSecondsString", "30");
        for (double spec : new double[]{50, 100})
        {
            field(original, "currentSpec", spec);
            for (OrbClickthroughActivationMode mode : OrbClickthroughActivationMode.values())
            {
                when(config.activationMode()).thenReturn(mode);
                for (boolean active : new boolean[]{false, true})
                {
                    field(provider, "hotkeyHeld", mode == OrbClickthroughActivationMode.HOLD_TO_RESTORE_CLICKS ? !active : active);
                    field(provider, "toggleActive", active);
                    tooltips.clear();
                    addActualNpcMouseTooltip();
                    tooltips.add(new net.runelite.client.ui.overlay.tooltip.Tooltip("Take Coins"));
                    tooltips.add(new net.runelite.client.ui.overlay.tooltip.Tooltip("Open Door"));
                    List<net.runelite.client.ui.overlay.tooltip.Tooltip> existing = List.copyOf(tooltips.getTooltips());
                    render(adapter);
                    assertEquals(active ? 3 : 4, tooltips.getTooltips().size());
                    assertEquals(existing, tooltips.getTooltips().subList(0, 3));
                    if (!active)
                    {
                        String text = tooltips.getTooltips().get(3).getText();
                        assertTrue(text.contains("Available special attacks: 1"));
                        assertEquals(spec < 100, text.contains("Time Remaining till next spec: 1:30"));
                    }
                }
            }
        }
        // The last loop leaves toggle mode active. Either opt-out restores the tooltip.
        when(config.manageSpecialAttackOrb()).thenReturn(false);
        tooltips.clear();
        render(adapter);
        assertEquals(1, tooltips.getTooltips().size());
        when(config.manageSpecialAttackOrb()).thenReturn(true);
        when(config.suppressOrbTooltips()).thenReturn(false);
        tooltips.clear();
        render(adapter);
        assertEquals(1, tooltips.getTooltips().size());
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
            when(config.suppressOrbTooltips()).thenReturn(true);
            assertEquals(0, alphaSum(render(preview)));
            assertEquals(1, tooltips.getTooltips().size());
            when(config.suppressOrbTooltips()).thenReturn(false);
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
    public void restoresLayersAndDoesNotResurrectStoppedPlugins() throws Exception
    {
        Overlay adapter = overlay("net.runelite.client.plugins.runenergy.RunEnergyOverlay",
            stub("net.runelite.client.plugins.runenergy.RunEnergyPlugin", Map.of()), client,
            defaults("net.runelite.client.plugins.runenergy.RunEnergyConfig", Map.of()), tooltips);
        Overlay original = registered.get(0);
        assertEquals(net.runelite.client.ui.overlay.OverlayLayer.MANUAL, original.getLayer());
        compatibility.sync();
        assertEquals(2, registered.size());
        registered.remove(original);
        render(adapter);
        assertTrue(tooltips.getTooltips().isEmpty());
        compatibility.sync();
        assertTrue(registered.isEmpty());
        assertEquals(net.runelite.client.ui.overlay.OverlayLayer.ABOVE_WIDGETS, original.getLayer());
        registered.add(original);
        compatibility.sync();
        compatibility.stop();
        assertEquals(List.of(original), registered);
        assertEquals(net.runelite.client.ui.overlay.OverlayLayer.ABOVE_WIDGETS, original.getLayer());
        render(original);
        assertEquals(1, tooltips.getTooltips().size());
    }

    @Test
    public void modesSelectionsLogoutAndSettingControlSuppression() throws Exception
    {
        assertTrue(new OrbClickthroughConfig() {}.suppressOrbTooltips());
        assertTrue(new OrbClickthroughConfig() {}.fadePluginOverlays());
        for (OrbClickthroughActivationMode mode : OrbClickthroughActivationMode.values())
        {
            when(config.activationMode()).thenReturn(mode);
            for (boolean active : new boolean[]{false, true})
            {
                field(provider, "hotkeyHeld", mode == OrbClickthroughActivationMode.HOLD_TO_RESTORE_CLICKS ? !active : active);
                field(provider, "toggleActive", active);
                assertEquals(active, provider.suppressPluginTooltip("prayer"));
                assertEquals(active ? .5f : 1f, provider.pluginOverlayOpacity("prayer"), 0f);
                assertFalse(provider.suppressPluginTooltip("unknown"));
            }
        }
        when(config.managePrayerOrb()).thenReturn(false);
        assertFalse(provider.suppressPluginTooltip("prayer"));
        when(config.managePrayerOrb()).thenReturn(true);
        when(config.suppressOrbTooltips()).thenReturn(false);
        assertFalse(provider.suppressPluginTooltip("prayer"));
        when(config.suppressOrbTooltips()).thenReturn(true);
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        assertFalse(provider.suppressPluginTooltip("prayer"));
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        field(provider, "running", false);
        assertFalse(provider.suppressPluginTooltip("prayer"));
    }

    @Test
    public void preservesEveryExistingTooltipAndPrayerDrawingEvenOnFailure()
    {
        Overlay producer = new Overlay()
        {
            @Override
            public java.awt.Dimension render(Graphics2D graphics)
            {
                graphics.setColor(Color.BLUE);
                graphics.fillRect(0, 0, 10, 10);
                tooltips.addFront(new net.runelite.client.ui.overlay.tooltip.Tooltip("orb"));
                throw new IllegalStateException("producer failed after adding tooltip");
            }
        };
        producer.setLayer(net.runelite.client.ui.overlay.OverlayLayer.ABOVE_WIDGETS);
        registered.add(producer);
        OrbOverlayCompatibility.Adapter adapter = compatibility.new Adapter(producer, "prayer", false);
        producer.setLayer(net.runelite.client.ui.overlay.OverlayLayer.MANUAL);
        for (String text : List.of("Attack Goblin", "Talk-to Banker", "Take Coins", "Open Door", "Time Remaining: arbitrary unrelated tooltip"))
        {
            tooltips.add(new net.runelite.client.ui.overlay.tooltip.Tooltip(text));
        }
        List<net.runelite.client.ui.overlay.tooltip.Tooltip> original = List.copyOf(tooltips.getTooltips());
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try
        {
            adapter.render(graphics);
            fail("Expected producer failure");
        }
        catch (IllegalStateException expected)
        {
            assertEquals("producer failed after adding tooltip", expected.getMessage());
        }
        finally
        {
            graphics.dispose();
        }
        assertEquals(original, tooltips.getTooltips());
        assertTrue(alphaSum(image) > 0);
        assertNull(OrbOverlayCompatibility.orbFor("net.runelite.client.plugins.mousehighlight.MouseHighlightOverlay"));
    }

    @Test
    public void realOverlayManagerRoutesExactlyOnceAndRestoresOriginal() throws Exception
    {
        Constructor<?> constructor = net.runelite.client.ui.overlay.OverlayManager.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object[] dependencies = java.util.Arrays.stream(constructor.getParameterTypes()).map(type -> mock(type)).toArray();
        net.runelite.client.ui.overlay.OverlayManager real =
            (net.runelite.client.ui.overlay.OverlayManager) constructor.newInstance(dependencies);
        Overlay original = construct("net.runelite.client.plugins.prayer.PrayerDoseOverlay", client, tooltips,
            stub("net.runelite.client.plugins.prayer.PrayerPlugin", Map.of()),
            defaults("net.runelite.client.plugins.prayer.PrayerConfig", Map.of("showPrayerStatistics", true, "showPrayerDoseIndicator", true)));
        field(original, "restoreAmount", 20);
        when(client.getRealSkillLevel(net.runelite.api.Skill.PRAYER)).thenReturn(99);
        when(client.getBoostedSkillLevel(net.runelite.api.Skill.PRAYER)).thenReturn(10);
        real.add(original);
        OrbOverlayCompatibility integration = new OrbOverlayCompatibility(real, tooltips, provider, new OrbOverlayRenderer(client));
        integration.sync();
        java.lang.reflect.Method getLayer = real.getClass().getDeclaredMethod("getLayer", net.runelite.client.ui.overlay.OverlayLayer.class);
        getLayer.setAccessible(true);
        // ABOVE_WIDGETS overlays are registered on the relevant interface draw hook by RuneLite.
        java.lang.reflect.Field mapField = real.getClass().getDeclaredField("overlayMap");
        mapField.setAccessible(true);
        com.google.common.collect.Multimap<?, Overlay> map = (com.google.common.collect.Multimap<?, Overlay>) mapField.get(real);
        assertFalse(map.values().contains(original));
        Overlay adapter = map.values().stream().filter(o -> o instanceof OrbOverlayCompatibility.Adapter).findFirst().orElseThrow(AssertionError::new);
        assertTrue(alphaSum(render(adapter)) > 0);
        assertTrue(tooltips.getTooltips().isEmpty());
        integration.stop();
        map = (com.google.common.collect.Multimap<?, Overlay>) mapField.get(real);
        assertTrue(map.values().contains(original));
        assertFalse(map.values().contains(adapter));
        render(original);
        assertEquals(1, tooltips.getTooltips().size());
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
        BufferedImage fadedImage = render(overlay);
        long faded = alphaSum(fadedImage);
        if (!overlay.getName().equals("RegenMeterOverlay"))
        {
            // A layered replacement orb must be 50% transparent as a whole, not
            // become opaque again where its fills, sprite and text overlap.
            for (int y = 0; y < fadedImage.getHeight(); y++)
                for (int x = 0; x < fadedImage.getWidth(); x++)
                    assertTrue((fadedImage.getRGB(x, y) >>> 24) <= 128);
        }
        assertTrue(overlay.getClass().getName(), faded > 0 && faded < normal);
        when(config.clickThroughTransparency()).thenReturn(100);
        assertEquals(overlay.getClass().getName(), 0, alphaSum(render(overlay)));
        when(config.fadePluginOverlays()).thenReturn(false);
        assertEquals(normal, alphaSum(render(overlay)));
        when(config.fadePluginOverlays()).thenReturn(true);
    }

    @Test
    public void regenerationStrokesRemainIndependentWhenOrbsOverlap() throws Exception
    {
        Object regenPlugin = stub("net.runelite.client.plugins.regenmeter.RegenMeterPlugin",
            Map.of("getHitpointsPercentage", .5d, "getSpecialPercentage", .25d));
        Overlay regen = overlay("net.runelite.client.plugins.regenmeter.RegenMeterOverlay", client, regenPlugin,
            defaults("net.runelite.client.plugins.regenmeter.RegenMeterConfig", Map.of("showHitpoints", true, "showSpecial", true)));
        Overlay healthOnly = construct("net.runelite.client.plugins.regenmeter.RegenMeterOverlay", client, regenPlugin,
            defaults("net.runelite.client.plugins.regenmeter.RegenMeterConfig", Map.of("showHitpoints", true, "showSpecial", false)));
        when(config.manageHealthOrb()).thenReturn(false);
        when(config.clickThroughTransparency()).thenReturn(100);
        BufferedImage expected = render(healthOnly);
        BufferedImage actual = render(regen);
        assertArrayEquals(expected.getRGB(0, 0, 400, 250, null, 0, 400), actual.getRGB(0, 0, 400, 250, null, 0, 400));
    }

    @Test
    public void soulreaperOffsetLayerChangesAndTooltipToggleDoNotBreakFading() throws Exception
    {
        when(otherConfigs.getConfiguration("soulreaperaxeqol", "offsetX", Integer.class)).thenReturn(160);
        when(otherConfigs.getConfiguration("soulreaperaxeqol", "offsetY", Integer.class)).thenReturn(80);
        Overlay extra = overlay("com.soulreaperaxeqol.SoulreaperAxeQoLExtraOrbOverlay", client,
            stub("com.soulreaperaxeqol.SoulreaperAxeQoLPlugin", Map.of("isSoulreaperAxeEquipped", true,
                "getSpecialAttackPercent", 50, "getSoulreaperStackCount", 3, "getSpecRegenProgress", .5d)),
            defaults("com.soulreaperaxeqol.SoulreaperAxeQoLConfig", Map.of("getOffsetX", 160, "getOffsetY", 80)), otherConfigs);
        when(config.suppressOrbTooltips()).thenReturn(false);
        assertFades(extra);
        Overlay original = registered.get(0);
        original.setLayer(net.runelite.client.ui.overlay.OverlayLayer.UNDER_WIDGETS);
        compatibility.sync();
        assertFalse(registered.contains(extra));
        Overlay replacement = registered.stream().filter(o -> o instanceof OrbOverlayCompatibility.Adapter).findFirst().orElseThrow(AssertionError::new);
        assertEquals(net.runelite.client.ui.overlay.OverlayLayer.UNDER_WIDGETS, replacement.getLayer());
        assertFades(replacement);
        compatibility.stop();
        assertEquals(net.runelite.client.ui.overlay.OverlayLayer.UNDER_WIDGETS, original.getLayer());
    }

    @Test
    public void fadingPreservesTransformClipAndSharedComposite() throws Exception
    {
        Object location = Class.forName("net.runelite.client.plugins.prayer.PrayerFlickLocation").getEnumConstants()[1];
        Overlay flick = overlay("net.runelite.client.plugins.prayer.PrayerFlickOverlay", client,
            defaults("net.runelite.client.plugins.prayer.PrayerConfig", Map.of("prayerFlickLocation", location, "prayerFlickColor", Color.BLUE)),
            stub("net.runelite.client.plugins.prayer.PrayerPlugin", Map.of("isPrayersActive", true, "getTickProgress", .5d)));
        BufferedImage image = new BufferedImage(400, 250, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.translate(20, 10);
        graphics.scale(1.25, 1.25);
        graphics.clipRect(0, 0, 300, 180);
        graphics.setComposite(java.awt.AlphaComposite.SrcOver.derive(.5f));
        java.awt.geom.AffineTransform transform = graphics.getTransform();
        Rectangle clip = graphics.getClipBounds();
        try
        {
            flick.render(graphics);
            assertEquals(transform, graphics.getTransform());
            assertEquals(clip, graphics.getClipBounds());
            assertEquals(.5f, ((java.awt.AlphaComposite) graphics.getComposite()).getAlpha(), 0f);
        }
        finally
        {
            graphics.dispose();
        }
        assertTrue(alphaSum(image) > 0);
        for (int y = 0; y < 250; y++)
            for (int x = 0; x < 400; x++)
                assertTrue((image.getRGB(x, y) >>> 24) <= 64);
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
        registered.add(overlay);
        compatibility.sync();
        return registered.stream().filter(o -> o instanceof OrbOverlayCompatibility.Adapter)
            .reduce((first, last) -> last).orElseThrow(AssertionError::new);
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
