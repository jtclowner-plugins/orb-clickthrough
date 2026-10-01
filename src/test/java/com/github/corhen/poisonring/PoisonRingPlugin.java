package com.github.corhen.poisonring;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.HitsplatID;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
    name = "Poison Ring",
    description = "Adds a ring around the HP orb showing time until next poison/venom tick",
    tags = {"poison", "venom", "hp", "orb", "timer", "combat"}
)
public class PoisonRingPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private PoisonRingConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PoisonRingOverlay overlay;

    private int poisonValue = 0;
    private int ticksUntilDamage = 0;
    private boolean isVenom = false;

    private static final int POISON_TICK_RATE = 30;

    @Provides
    PoisonRingConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PoisonRingConfig.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        checkPoisonState();
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        poisonValue = 0;
        ticksUntilDamage = 0;
        isVenom = false;
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        if (event.getVarpId() == VarPlayer.POISON) {
            checkPoisonState();
        }
    }

    private void checkPoisonState() {
        int newValue = client.getVarpValue(VarPlayer.POISON);

        if (newValue <= 0) {
            poisonValue = 0;
            ticksUntilDamage = 0;
            isVenom = false;
        } else {
            if (poisonValue <= 0 || (newValue >= 1000000 && poisonValue < 1000000)) {
                ticksUntilDamage = POISON_TICK_RATE;
            }
            poisonValue = newValue;
            isVenom = newValue >= 1000000;
        }
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {
        if (event.getActor() == client.getLocalPlayer()) {
            int hitsplatType = event.getHitsplat().getHitsplatType();
            if (hitsplatType == HitsplatID.POISON || hitsplatType == HitsplatID.VENOM) {
                ticksUntilDamage = POISON_TICK_RATE;
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (poisonValue > 0 && ticksUntilDamage > 0) {
            ticksUntilDamage--;
        }
    }

    public int getTicksUntilDamage() { return ticksUntilDamage; }
    public boolean isPoisoned() {
        return poisonValue > 0 || config.testMode();
    }
    public boolean isVenom() { return isVenom; }
    public int getPoisonTickRate() { return POISON_TICK_RATE; }
}
