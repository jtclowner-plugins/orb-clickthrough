package com.orbclickthrough;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/** Adapters for inspected, unmodified orb overlays. Uses public overlay registration APIs only. */
@Singleton
class OrbOverlayCompatibility
{
    private final OverlayManager overlays;
    private final TooltipManager tooltips;
    private final OrbClickthroughPlugin plugin;
    private final OrbOverlayRenderer renderer;
    private final Map<Overlay, Adapter> adapters = new IdentityHashMap<>();

    @Inject
    OrbOverlayCompatibility(OverlayManager overlays, TooltipManager tooltips, OrbClickthroughPlugin plugin, OrbOverlayRenderer renderer)
    {
        this.overlays = overlays;
        this.tooltips = tooltips;
        this.plugin = plugin;
        this.renderer = renderer;
    }

    void sync()
    {
        List<Overlay> registered = new ArrayList<>();
        // The public predicate API also lets us inspect registrations without reflection.
        overlays.anyMatch(overlay -> { registered.add(overlay); return false; });
        Iterator<Map.Entry<Overlay, Adapter>> iterator = adapters.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<Overlay, Adapter> entry = iterator.next();
            if (!registered.contains(entry.getKey()) || !registered.contains(entry.getValue())
                || entry.getKey().getLayer() != OverlayLayer.MANUAL
                || !entry.getKey().getDrawHooks().isEmpty()
                || entry.getKey().getPosition() != entry.getValue().getPosition()
                || entry.getKey().getPriority() != entry.getValue().getPriority())
            {
                restore(entry.getValue());
                iterator.remove();
            }
        }
        for (Overlay overlay : registered)
        {
            String orb = orbFor(overlay.getClass().getName());
            if (overlay instanceof Adapter || orb == null || adapters.containsKey(overlay) || !overlay.getDrawHooks().isEmpty()
                || (overlay.getPosition() != OverlayPosition.DYNAMIC && overlay.getPosition() != OverlayPosition.TOOLTIP && overlay.getPosition() != OverlayPosition.DETACHED)
                || overlay.getLayer() == OverlayLayer.MANUAL)
            {
                continue;
            }
            Adapter adapter = new Adapter(overlay, orb,
                overlay.getClass().getName().equals("io.hydrox.quickprayerpreview.QuickPrayerPreviewOverlay"));
            // Keep the original registered so its owner's remove() still works on plugin shutdown.
            // With no manual draw hooks it stops rendering; adding our adapter rebuilds the layers.
            overlay.setLayer(OverlayLayer.MANUAL);
            adapters.put(overlay, adapter);
            overlays.add(adapter);
        }
    }

    static String orbFor(String className)
    {
        switch (className)
        {
            case "net.runelite.client.plugins.prayer.PrayerDoseOverlay":
            case "net.runelite.client.plugins.prayer.PrayerFlickOverlay":
            case "io.hydrox.quickprayerpreview.QuickPrayerPreviewOverlay": return "prayer";
            case "net.runelite.client.plugins.runenergy.RunEnergyOverlay": return "run";
            case "net.runelite.client.plugins.poison.PoisonOverlay":
            case "com.github.corhen.poisonring.PoisonRingOverlay": return "health";
            case "com.soulreaperaxeqol.SoulreaperAxeQoLNativeOrbOverlay":
            case "com.bram91.specregen.SpecRegenTimerOverlay":
            case "com.soulreaperaxeqol.SoulreaperAxeQoLExtraOrbOverlay": return "special";
            case "net.runelite.client.plugins.regenmeter.RegenMeterOverlay": return "regeneration";
            default: return null;
        }
    }

    void stop()
    {
        for (Adapter adapter : adapters.values())
        {
            restore(adapter);
        }
        adapters.clear();
        renderer.clear();
    }

    private void restore(Adapter adapter)
    {
        if (adapter.original.getLayer() == OverlayLayer.MANUAL)
        {
            adapter.original.setLayer(adapter.originalLayer);
        }
        // Never re-add an original removed by its owner. Removal rebuilds layer registrations.
        overlays.remove(adapter);
    }

    final class Adapter extends Overlay
    {
        private final Overlay original;
        private final OverlayLayer originalLayer;
        private final String orb;
        private final boolean directTooltip;

        Adapter(Overlay original, String orb, boolean directTooltip)
        {
            this.original = original;
            this.originalLayer = original.getLayer();
            this.orb = orb;
            this.directTooltip = directTooltip;
            setPosition(original.getPosition());
            setLayer(originalLayer);
            setPriority(original.getPriority());
        }

        @Override
        public String getName()
        {
            return original.getName();
        }

        @Override
        public Dimension render(Graphics2D graphics)
        {
            // Owner may have stopped after sync(), before this rendering pass.
            if (original.getLayer() != OverlayLayer.MANUAL || !overlays.anyMatch(o -> o == original))
            {
                return null;
            }
            boolean suppress = plugin.suppressPluginTooltip(orb);
            if (suppress && directTooltip)
            {
                original.getBounds().setSize(0, 0);
                return null;
            }
            original.getBounds().setLocation(getBounds().getLocation());
            Set<Tooltip> existing = Collections.newSetFromMap(new IdentityHashMap<>());
            if (suppress)
            {
                existing.addAll(tooltips.getTooltips());
            }
            try
            {
                Dimension size = renderer.render(original, graphics, orb,
                    directTooltip ? 1f : plugin.pluginOverlayOpacity(orb),
                    plugin.pluginOverlayOpacity("health"), plugin.pluginOverlayOpacity("special"));
                original.getBounds().setSize(size == null ? new Dimension() : size);
                return size;
            }
            finally
            {
                if (suppress)
                {
                    // Rendering is synchronous on the client thread. Remove only objects this
                    // known producer just added, preserving every pre-existing tooltip by identity.
                    tooltips.getTooltips().removeIf(tooltip -> !existing.contains(tooltip));
                }
            }
        }
    }
}
